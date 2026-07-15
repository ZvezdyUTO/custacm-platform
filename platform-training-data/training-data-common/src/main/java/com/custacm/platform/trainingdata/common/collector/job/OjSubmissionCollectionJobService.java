package com.custacm.platform.trainingdata.common.collector.job;

import com.custacm.platform.trainingdata.common.collector.OjCollectionExecutionCoordinator;
import com.custacm.platform.trainingdata.common.collector.result.OjSubmissionCollectionResult;
import com.custacm.platform.trainingdata.common.domain.oj.value.OjNames;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import static com.custacm.platform.trainingdata.common.support.Texts.requireText;

public class OjSubmissionCollectionJobService {
    private static final int MAX_RETAINED_JOBS = 50;

    private final RecentIdentityCollector collector;
    private final RefreshHandler refreshHandler;
    private final Executor executor;
    private final Clock clock;
    private final Duration itemInterval;
    private final SleepStrategy sleepStrategy;
    private final OjCollectionExecutionCoordinator executionCoordinator;
    private final Map<String, JobState> jobs = new ConcurrentHashMap<>();

    public OjSubmissionCollectionJobService(
            RecentIdentityCollector collector,
            RefreshHandler refreshHandler,
            Executor executor
    ) {
        this(collector, refreshHandler, executor, Clock.systemUTC(), Duration.ZERO);
    }

    public OjSubmissionCollectionJobService(
            RecentIdentityCollector collector,
            RefreshHandler refreshHandler,
            Executor executor,
            Duration itemInterval
    ) {
        this(collector, refreshHandler, executor, Clock.systemUTC(), itemInterval);
    }

    public OjSubmissionCollectionJobService(
            RecentIdentityCollector collector,
            RefreshHandler refreshHandler,
            Executor executor,
            Duration itemInterval,
            OjCollectionExecutionCoordinator executionCoordinator
    ) {
        this(
                collector,
                refreshHandler,
                executor,
                Clock.systemUTC(),
                itemInterval,
                duration -> Thread.sleep(duration.toMillis()),
                executionCoordinator
        );
    }

    public OjSubmissionCollectionJobService(
            RecentIdentityCollector collector,
            RefreshHandler refreshHandler,
            Executor executor,
            Clock clock
    ) {
        this(collector, refreshHandler, executor, clock, Duration.ZERO);
    }

    public OjSubmissionCollectionJobService(
            RecentIdentityCollector collector,
            RefreshHandler refreshHandler,
            Executor executor,
            Clock clock,
            Duration itemInterval
    ) {
        this(collector, refreshHandler, executor, clock, itemInterval, duration -> Thread.sleep(duration.toMillis()));
    }

    OjSubmissionCollectionJobService(
            RecentIdentityCollector collector,
            RefreshHandler refreshHandler,
            Executor executor,
            Clock clock,
            Duration itemInterval,
            SleepStrategy sleepStrategy
    ) {
        this(
                collector,
                refreshHandler,
                executor,
                clock,
                itemInterval,
                sleepStrategy,
                new OjCollectionExecutionCoordinator()
        );
    }

    OjSubmissionCollectionJobService(
            RecentIdentityCollector collector,
            RefreshHandler refreshHandler,
            Executor executor,
            Clock clock,
            Duration itemInterval,
            SleepStrategy sleepStrategy,
            OjCollectionExecutionCoordinator executionCoordinator
    ) {
        this.collector = Objects.requireNonNull(collector, "collector must not be null");
        this.refreshHandler = Objects.requireNonNull(refreshHandler, "refreshHandler must not be null");
        this.executor = Objects.requireNonNull(executor, "executor must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.itemInterval = itemInterval == null || itemInterval.isNegative() ? Duration.ZERO : itemInterval;
        this.sleepStrategy = Objects.requireNonNull(sleepStrategy, "sleepStrategy must not be null");
        this.executionCoordinator = Objects.requireNonNull(
                executionCoordinator,
                "executionCoordinator must not be null"
        );
    }

    public OjSubmissionCollectionJobSnapshot startBatchCollection(
            List<String> usernames,
            Duration lookback,
            boolean refreshWarehouse,
            String ojName
    ) {
        List<String> identities = normalizeIdentities(usernames);
        requirePositiveDuration(lookback);
        String normalizedOjName = normalizeOjName(ojName);
        synchronized (jobs) {
            OjSubmissionCollectionJobSnapshot active = activeJob(normalizedOjName);
            if (active != null) {
                return active;
            }
            String jobId = UUID.randomUUID().toString();
            JobState state = new JobState(jobId, normalizedOjName, identities, clock.instant(), "采集任务已创建");
            jobs.put(jobId, state);
            pruneCompletedJobs();
            try {
                executor.execute(() -> executionCoordinator.runExclusive(
                        normalizedOjName,
                        () -> runJob(state, identities, lookback, refreshWarehouse, normalizedOjName)
                ));
            } catch (RuntimeException ex) {
                jobs.remove(jobId, state);
                throw ex;
            }
            return state.snapshot();
        }
    }

    public OjSubmissionCollectionJobSnapshot startBatchCollection(
            List<String> usernames,
            Duration lookback,
            boolean refreshWarehouse
    ) {
        return startBatchCollection(usernames, lookback, refreshWarehouse, null);
    }

    public OjSubmissionCollectionJobSnapshot getJob(String jobId) {
        String normalizedJobId = requireText(jobId, "jobId");
        JobState state = jobs.get(normalizedJobId);
        if (state == null) {
            throw new NoSuchElementException("OJ submission collection job not found: " + normalizedJobId);
        }
        return state.snapshot();
    }

    public List<OjSubmissionCollectionJobSnapshot> listJobs() {
        return jobs.values().stream()
                .map(JobState::snapshot)
                .sorted((left, right) -> right.startedAt().compareTo(left.startedAt()))
                .toList();
    }

    private OjSubmissionCollectionJobSnapshot activeJob(String ojName) {
        return jobs.values().stream()
                .map(JobState::snapshot)
                .filter(job -> job.status() == OjSubmissionCollectionJobStatus.RUNNING)
                .filter(job -> job.ojName().equals(ojName))
                .findFirst()
                .orElse(null);
    }

    private void runJob(
            JobState state,
            List<String> identities,
            Duration lookback,
            boolean refreshWarehouse,
            String ojName
    ) {
        state.updateMessage("采集任务运行中");
        for (int index = 0; index < identities.size(); index++) {
            String identity = identities.get(index);
            if (index > 0 && !sleepBeforeNextIdentity(state, identities.subList(index, identities.size()))) {
                break;
            }
            state.markRunning(identity);
            try {
                OjSubmissionCollectionResult collectionResult = collector.collect(ojName, identity, lookback);
                OjSubmissionCollectionJobRefreshResult refreshResult = refreshResult(collectionResult, refreshWarehouse);
                state.markCollected(identity, OjSubmissionCollectionJobItem.collected(
                        identity,
                        collectionResult,
                        refreshResult
                ));
            } catch (Exception ex) {
                state.markCollected(identity, OjSubmissionCollectionJobItem.failed(identity, ojName, ex.getMessage()));
            }
        }
        state.finish(clock.instant());
    }

    private OjSubmissionCollectionJobRefreshResult refreshResult(
            OjSubmissionCollectionResult collectionResult,
            boolean refreshWarehouse
    ) {
        if (!refreshWarehouse) {
            return OjSubmissionCollectionJobRefreshResult.notRequested();
        }
        if (collectionResult.batchId() == null) {
            return OjSubmissionCollectionJobRefreshResult.noBatch();
        }
        try {
            return Objects.requireNonNull(
                    refreshHandler.refresh(collectionResult),
                    "refresh result must not be null"
            );
        } catch (Exception ex) {
            return OjSubmissionCollectionJobRefreshResult.failed(ex.getMessage());
        }
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
                    OjSubmissionCollectionJobItem.failed(
                            identity,
                            state.ojName,
                            "interrupted while rate limiting OJ submission collection job"
                    )
            ));
            return false;
        }
    }

    private void pruneCompletedJobs() {
        List<JobState> completed = jobs.values().stream()
                .filter(state -> state.snapshot().status() != OjSubmissionCollectionJobStatus.RUNNING)
                .sorted((left, right) -> left.snapshot().startedAt().compareTo(right.snapshot().startedAt()))
                .toList();
        int removeCount = Math.max(0, jobs.size() - MAX_RETAINED_JOBS);
        for (int index = 0; index < removeCount && index < completed.size(); index++) {
            jobs.remove(completed.get(index).jobId);
        }
    }

    private static List<String> normalizeIdentities(List<String> usernames) {
        if (usernames == null) {
            throw new IllegalArgumentException("usernames must not be empty");
        }
        List<String> identities = usernames.stream()
                .map(identity -> requireText(identity, "username"))
                .distinct()
                .toList();
        if (identities.isEmpty()) {
            throw new IllegalArgumentException("usernames must not be empty");
        }
        return identities;
    }

    private static void requirePositiveDuration(Duration lookback) {
        if (lookback == null || lookback.isZero() || lookback.isNegative()) {
            throw new IllegalArgumentException("lookback must be positive");
        }
    }

    private static String normalizeOjName(String value) {
        if (value == null || value.isBlank()) {
            return OjNames.CODEFORCES;
        }
        return OjNames.normalize(value);
    }

    @FunctionalInterface
    public interface RecentIdentityCollector {
        OjSubmissionCollectionResult collect(String ojName, String username, Duration lookback) throws Exception;
    }

    @FunctionalInterface
    public interface RefreshHandler {
        OjSubmissionCollectionJobRefreshResult refresh(OjSubmissionCollectionResult result) throws Exception;
    }

    interface SleepStrategy {
        void sleep(Duration duration) throws InterruptedException;
    }

    private static final class JobState {
        private final String jobId;
        private final String ojName;
        private final Instant startedAt;
        private final Map<String, OjSubmissionCollectionJobItem> items = new LinkedHashMap<>();
        private Instant finishedAt;
        private String message;

        private JobState(String jobId, String ojName, List<String> identities, Instant startedAt, String message) {
            this.jobId = jobId;
            this.ojName = ojName;
            this.startedAt = startedAt;
            this.message = message;
            identities.forEach(identity -> items.put(identity, OjSubmissionCollectionJobItem.pending(identity, ojName)));
        }

        private synchronized void updateMessage(String message) {
            this.message = message;
        }

        private synchronized void markRunning(String identity) {
            items.computeIfPresent(identity, (key, item) -> item.running());
        }

        private synchronized void markCollected(String identity, OjSubmissionCollectionJobItem item) {
            items.put(identity, item);
        }

        private synchronized void finish(Instant finishedAt) {
            this.finishedAt = finishedAt;
            this.message = "采集任务已完成";
        }

        private synchronized OjSubmissionCollectionJobSnapshot snapshot() {
            List<OjSubmissionCollectionJobItem> itemList = new ArrayList<>(items.values());
            int completedCount = (int) itemList.stream()
                    .filter(item -> item.itemStatus() == OjSubmissionCollectionJobItemStatus.SUCCESS
                            || item.itemStatus() == OjSubmissionCollectionJobItemStatus.FAILED)
                    .count();
            int collectedCount = (int) itemList.stream()
                    .filter(item -> item.itemStatus() == OjSubmissionCollectionJobItemStatus.SUCCESS)
                    .count();
            int failedCount = (int) itemList.stream()
                    .filter(item -> item.itemStatus() == OjSubmissionCollectionJobItemStatus.FAILED)
                    .count();
            int refreshedCount = (int) itemList.stream()
                    .filter(item -> item.refreshStatus() == OjSubmissionCollectionJobRefreshStatus.SUCCESS)
                    .count();
            int writtenRows = itemList.stream()
                    .mapToInt(OjSubmissionCollectionJobItem::writtenRows)
                    .sum();
            List<String> batchIds = itemList.stream()
                    .map(OjSubmissionCollectionJobItem::batchId)
                    .filter(batchId -> batchId != null && !batchId.isBlank())
                    .distinct()
                    .toList();
            return new OjSubmissionCollectionJobSnapshot(
                    jobId,
                    ojName,
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

        private static OjSubmissionCollectionJobStatus status(
                List<OjSubmissionCollectionJobItem> items,
                Instant finishedAt
        ) {
            if (finishedAt == null) {
                return OjSubmissionCollectionJobStatus.RUNNING;
            }
            long failedCount = items.stream()
                    .filter(item -> item.itemStatus() == OjSubmissionCollectionJobItemStatus.FAILED)
                    .count();
            boolean refreshFailed = items.stream()
                    .anyMatch(item -> item.refreshStatus() == OjSubmissionCollectionJobRefreshStatus.FAILED);
            if (failedCount == 0 && !refreshFailed) {
                return OjSubmissionCollectionJobStatus.SUCCESS;
            }
            return failedCount == items.size()
                    ? OjSubmissionCollectionJobStatus.FAILED
                    : OjSubmissionCollectionJobStatus.PARTIAL_SUCCESS;
        }
    }
}
