package com.custacm.platform.trainingdata.codeforces.app.collector.job;

import com.custacm.platform.common.sqltask.SqlTaskRunStatus;
import com.custacm.platform.trainingdata.codeforces.app.collector.CodeforcesSubmissionCollectionService;
import com.custacm.platform.trainingdata.codeforces.app.warehouse.CodeforcesWarehouseRefreshService;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

public class CodeforcesSubmissionCollectionJobService {
    private static final int MAX_RETAINED_JOBS = 50;

    private final CodeforcesSubmissionCollectionService collectionService;
    private final CodeforcesWarehouseRefreshService warehouseRefreshService;
    private final Executor executor;
    private final Clock clock;
    private final Duration itemInterval;
    private final SleepStrategy sleepStrategy;
    private final Map<String, JobState> jobs = new ConcurrentHashMap<>();

    public CodeforcesSubmissionCollectionJobService(
            CodeforcesSubmissionCollectionService collectionService,
            CodeforcesWarehouseRefreshService warehouseRefreshService,
            Executor executor
    ) {
        this(collectionService, warehouseRefreshService, executor, Clock.systemUTC(), Duration.ZERO);
    }

    public CodeforcesSubmissionCollectionJobService(
            CodeforcesSubmissionCollectionService collectionService,
            CodeforcesWarehouseRefreshService warehouseRefreshService,
            Executor executor,
            Duration itemInterval
    ) {
        this(collectionService, warehouseRefreshService, executor, Clock.systemUTC(), itemInterval);
    }

    public CodeforcesSubmissionCollectionJobService(
            CodeforcesSubmissionCollectionService collectionService,
            CodeforcesWarehouseRefreshService warehouseRefreshService,
            Executor executor,
            Clock clock
    ) {
        this(collectionService, warehouseRefreshService, executor, clock, Duration.ZERO);
    }

    public CodeforcesSubmissionCollectionJobService(
            CodeforcesSubmissionCollectionService collectionService,
            CodeforcesWarehouseRefreshService warehouseRefreshService,
            Executor executor,
            Clock clock,
            Duration itemInterval
    ) {
        this(collectionService, warehouseRefreshService, executor, clock, itemInterval, duration -> Thread.sleep(duration.toMillis()));
    }

    CodeforcesSubmissionCollectionJobService(
            CodeforcesSubmissionCollectionService collectionService,
            CodeforcesWarehouseRefreshService warehouseRefreshService,
            Executor executor,
            Clock clock,
            Duration itemInterval,
            SleepStrategy sleepStrategy
    ) {
        this.collectionService = collectionService;
        this.warehouseRefreshService = warehouseRefreshService;
        this.executor = executor;
        this.clock = clock;
        this.itemInterval = itemInterval == null || itemInterval.isNegative() ? Duration.ZERO : itemInterval;
        this.sleepStrategy = sleepStrategy;
    }

    public CodeforcesSubmissionCollectionJobSnapshot startBatchCollection(
            List<String> studentIdentities,
            Duration lookback,
            boolean refreshWarehouse
    ) {
        List<String> identities = normalizeIdentities(studentIdentities);
        requirePositiveDuration(lookback);
        synchronized (jobs) {
            CodeforcesSubmissionCollectionJobSnapshot active = activeJob();
            if (active != null) {
                return active;
            }
            String jobId = UUID.randomUUID().toString();
            JobState state = new JobState(jobId, identities, clock.instant(), "采集任务已创建");
            jobs.put(jobId, state);
            pruneCompletedJobs();
            executor.execute(() -> runJob(state, identities, lookback, refreshWarehouse));
            return state.snapshot();
        }
    }

    public CodeforcesSubmissionCollectionJobSnapshot getJob(String jobId) {
        String normalizedJobId = requireText(jobId, "jobId");
        JobState state = jobs.get(normalizedJobId);
        if (state == null) {
            throw new NoSuchElementException("Codeforces collection job not found: " + normalizedJobId);
        }
        return state.snapshot();
    }

    public List<CodeforcesSubmissionCollectionJobSnapshot> listJobs() {
        return jobs.values().stream()
                .map(JobState::snapshot)
                .sorted((left, right) -> right.startedAt().compareTo(left.startedAt()))
                .toList();
    }

    private CodeforcesSubmissionCollectionJobSnapshot activeJob() {
        return jobs.values().stream()
                .map(JobState::snapshot)
                .filter(job -> job.status() == CodeforcesSubmissionCollectionJobStatus.RUNNING)
                .findFirst()
                .orElse(null);
    }

    private void runJob(
            JobState state,
            List<String> identities,
            Duration lookback,
            boolean refreshWarehouse
    ) {
        state.updateMessage("采集任务运行中");
        for (int index = 0; index < identities.size(); index++) {
            String identity = identities.get(index);
            if (index > 0 && !sleepBeforeNextIdentity(state, identities.subList(index, identities.size()))) {
                break;
            }
            state.markRunning(identity);
            try {
                var collectionResult = collectionService.collectRecentWindowForStudentIdentity(identity, lookback);
                CodeforcesSubmissionCollectionJobRefreshStatus refreshStatus = refreshWarehouse
                        ? CodeforcesSubmissionCollectionJobRefreshStatus.NO_BATCH
                        : CodeforcesSubmissionCollectionJobRefreshStatus.NOT_REQUESTED;
                String refreshMessage = null;
                if (refreshWarehouse && collectionResult.batchId() != null) {
                    try {
                        var refreshResult = warehouseRefreshService.refresh(collectionResult.batchId(), null);
                        refreshStatus = refreshResult.status() == SqlTaskRunStatus.SUCCESS
                                ? CodeforcesSubmissionCollectionJobRefreshStatus.SUCCESS
                                : CodeforcesSubmissionCollectionJobRefreshStatus.FAILED;
                        refreshMessage = refreshResult.status().name();
                    } catch (RuntimeException ex) {
                        refreshStatus = CodeforcesSubmissionCollectionJobRefreshStatus.FAILED;
                        refreshMessage = ex.getMessage();
                    }
                }
                state.markCollected(identity, CodeforcesSubmissionCollectionJobItem.collected(
                        identity,
                        collectionResult,
                        refreshStatus,
                        refreshMessage
                ));
            } catch (Exception ex) {
                state.markCollected(identity, CodeforcesSubmissionCollectionJobItem.failed(identity, ex.getMessage()));
            }
        }
        state.finish(clock.instant());
    }

    private boolean sleepBeforeNextIdentity(JobState state, List<String> remainingIdentities) {
        if (itemInterval.isZero()) {
            return true;
        }
        try {
            sleepStrategy.sleep(itemInterval);
            return true;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            state.updateMessage("采集任务被中断");
            remainingIdentities.forEach(identity -> state.markCollected(
                    identity,
                    CodeforcesSubmissionCollectionJobItem.failed(
                            identity,
                            "interrupted while rate limiting Codeforces collection job"
                    )
            ));
            return false;
        }
    }

    private void pruneCompletedJobs() {
        List<JobState> completed = jobs.values().stream()
                .filter(state -> state.snapshot().status() != CodeforcesSubmissionCollectionJobStatus.RUNNING)
                .sorted((left, right) -> left.snapshot().startedAt().compareTo(right.snapshot().startedAt()))
                .toList();
        int removeCount = Math.max(0, jobs.size() - MAX_RETAINED_JOBS);
        for (int index = 0; index < removeCount && index < completed.size(); index++) {
            jobs.remove(completed.get(index).jobId);
        }
    }

    private static List<String> normalizeIdentities(List<String> studentIdentities) {
        if (studentIdentities == null) {
            throw new IllegalArgumentException("studentIdentities must not be empty");
        }
        List<String> identities = studentIdentities.stream()
                .map(identity -> requireText(identity, "studentIdentity"))
                .distinct()
                .toList();
        if (identities.isEmpty()) {
            throw new IllegalArgumentException("studentIdentities must not be empty");
        }
        return identities;
    }

    private static void requirePositiveDuration(Duration lookback) {
        if (lookback == null || lookback.isZero() || lookback.isNegative()) {
            throw new IllegalArgumentException("lookback must be positive");
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    interface SleepStrategy {
        void sleep(Duration duration) throws InterruptedException;
    }

    private static final class JobState {
        private final String jobId;
        private final Instant startedAt;
        private final Map<String, CodeforcesSubmissionCollectionJobItem> items = new LinkedHashMap<>();
        private Instant finishedAt;
        private String message;

        private JobState(String jobId, List<String> identities, Instant startedAt, String message) {
            this.jobId = jobId;
            this.startedAt = startedAt;
            this.message = message;
            identities.forEach(identity -> items.put(identity, CodeforcesSubmissionCollectionJobItem.pending(identity)));
        }

        private synchronized void updateMessage(String message) {
            this.message = message;
        }

        private synchronized void markRunning(String identity) {
            items.computeIfPresent(identity, (key, item) -> item.running());
        }

        private synchronized void markCollected(String identity, CodeforcesSubmissionCollectionJobItem item) {
            items.put(identity, item);
        }

        private synchronized void finish(Instant finishedAt) {
            this.finishedAt = finishedAt;
            this.message = "采集任务已完成";
        }

        private synchronized CodeforcesSubmissionCollectionJobSnapshot snapshot() {
            List<CodeforcesSubmissionCollectionJobItem> itemList = new ArrayList<>(items.values());
            int completedCount = (int) itemList.stream()
                    .filter(item -> item.itemStatus() == CodeforcesSubmissionCollectionJobItemStatus.SUCCESS
                            || item.itemStatus() == CodeforcesSubmissionCollectionJobItemStatus.FAILED)
                    .count();
            int collectedCount = (int) itemList.stream()
                    .filter(item -> item.itemStatus() == CodeforcesSubmissionCollectionJobItemStatus.SUCCESS)
                    .count();
            int failedCount = (int) itemList.stream()
                    .filter(item -> item.itemStatus() == CodeforcesSubmissionCollectionJobItemStatus.FAILED)
                    .count();
            int refreshedCount = (int) itemList.stream()
                    .filter(item -> item.refreshStatus() == CodeforcesSubmissionCollectionJobRefreshStatus.SUCCESS)
                    .count();
            int writtenRows = itemList.stream()
                    .mapToInt(CodeforcesSubmissionCollectionJobItem::writtenRows)
                    .sum();
            List<String> batchIds = itemList.stream()
                    .map(CodeforcesSubmissionCollectionJobItem::batchId)
                    .filter(batchId -> batchId != null && !batchId.isBlank())
                    .distinct()
                    .toList();
            return new CodeforcesSubmissionCollectionJobSnapshot(
                    jobId,
                    status(itemList, finishedAt),
                    itemList.size(),
                    completedCount,
                    collectedCount,
                    failedCount,
                    refreshedCount,
                    writtenRows,
                    batchIds,
                    startedAt,
                    finishedAt,
                    message,
                    itemList
            );
        }

        private static CodeforcesSubmissionCollectionJobStatus status(
                List<CodeforcesSubmissionCollectionJobItem> items,
                Instant finishedAt
        ) {
            if (finishedAt == null) {
                return CodeforcesSubmissionCollectionJobStatus.RUNNING;
            }
            long failedCount = items.stream()
                    .filter(item -> item.itemStatus() == CodeforcesSubmissionCollectionJobItemStatus.FAILED)
                    .count();
            if (failedCount == 0) {
                return CodeforcesSubmissionCollectionJobStatus.SUCCESS;
            }
            return failedCount == items.size()
                    ? CodeforcesSubmissionCollectionJobStatus.FAILED
                    : CodeforcesSubmissionCollectionJobStatus.PARTIAL_SUCCESS;
        }
    }
}
