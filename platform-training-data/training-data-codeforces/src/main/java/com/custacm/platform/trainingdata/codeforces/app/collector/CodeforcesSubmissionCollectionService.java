package com.custacm.platform.trainingdata.codeforces.app.collector;

import com.custacm.platform.trainingdata.codeforces.app.account.CodeforcesHandleAccountService;
import com.custacm.platform.trainingdata.codeforces.app.collector.result.CodeforcesSubmissionCollectionHandleResult;
import com.custacm.platform.trainingdata.codeforces.app.collector.result.CodeforcesSubmissionCollectionHandleStatus;
import com.custacm.platform.trainingdata.codeforces.app.collector.result.CodeforcesSubmissionCollectionResult;
import com.custacm.platform.trainingdata.codeforces.app.collector.result.CodeforcesSubmissionCollectionStatus;
import com.custacm.platform.trainingdata.codeforces.app.ingest.CodeforcesOdsSubmissionIngestService;
import com.custacm.platform.trainingdata.codeforces.app.ingest.result.CodeforcesOdsBatchUpsertResult;
import com.custacm.platform.trainingdata.codeforces.collector.config.CodeforcesCollectorProperties;
import com.custacm.platform.trainingdata.codeforces.domain.collector.CodeforcesSubmissionSourceClient;
import com.custacm.platform.trainingdata.codeforces.infra.collector.CodeforcesApiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class CodeforcesSubmissionCollectionService {
    private static final Logger log = LoggerFactory.getLogger(CodeforcesSubmissionCollectionService.class);
    private static final String COLLECTOR_BATCH_ID_PREFIX = "collector-codeforces";
    private static final String ALREADY_RUNNING_ERROR_CODE = "CODEFORCES_COLLECTOR_ALREADY_RUNNING";
    private static final String HANDLE_FAILED_ERROR_CODE = "CODEFORCES_COLLECTOR_HANDLE_FAILED";

    private final CodeforcesHandleAccountService handleAccountService;
    private final CodeforcesSubmissionSourceClient sourceClient;
    private final CodeforcesOdsSubmissionIngestService ingestService;
    private final ObjectMapper objectMapper;
    private final int pageSize;
    private final int maxRequestAttempts;
    private final Duration requestInterval;
    private final Clock clock;
    private final SleepStrategy sleepStrategy;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public CodeforcesSubmissionCollectionService(
            CodeforcesHandleAccountService handleAccountService,
            CodeforcesSubmissionSourceClient sourceClient,
            CodeforcesOdsSubmissionIngestService ingestService,
            ObjectMapper objectMapper,
            CodeforcesCollectorProperties properties
    ) {
        this(
                handleAccountService,
                sourceClient,
                ingestService,
                objectMapper,
                properties.pageSize(),
                properties.maxRequestAttempts(),
                properties.requestInterval(),
                Clock.systemUTC(),
                duration -> Thread.sleep(duration.toMillis())
        );
    }

    public CodeforcesSubmissionCollectionService(
            CodeforcesHandleAccountService handleAccountService,
            CodeforcesSubmissionSourceClient sourceClient,
            CodeforcesOdsSubmissionIngestService ingestService,
            ObjectMapper objectMapper,
            int pageSize,
            int maxRequestAttempts,
            Duration requestInterval,
            Clock clock,
            SleepStrategy sleepStrategy
    ) {
        if (pageSize <= 0) {
            throw new IllegalArgumentException("pageSize must be positive");
        }
        if (maxRequestAttempts <= 0) {
            throw new IllegalArgumentException("maxRequestAttempts must be positive");
        }
        this.handleAccountService = handleAccountService;
        this.sourceClient = sourceClient;
        this.ingestService = ingestService;
        this.objectMapper = objectMapper;
        this.pageSize = pageSize;
        this.maxRequestAttempts = maxRequestAttempts;
        this.requestInterval = requestInterval == null ? Duration.ZERO : requestInterval;
        this.clock = clock;
        this.sleepStrategy = sleepStrategy;
    }

    public CodeforcesSubmissionCollectionResult collectRecentWindowForConfiguredHandles(
            Duration lookback
    ) throws JsonProcessingException {
        return collectRecentWindow(lookback, this::listHandlesForCollection);
    }

    public CodeforcesSubmissionCollectionResult collectRecentWindowForStudentIdentity(
            String studentIdentity,
            Duration lookback
    ) throws JsonProcessingException {
        String normalizedStudentIdentity = requireText(studentIdentity, "studentIdentity");
        String handle = handleAccountService.getByStudentIdentity(normalizedStudentIdentity).handle();
        String normalizedHandle = requireText(handle, "handle");
        return collectRecentWindow(lookback, () -> List.of(normalizedHandle));
    }

    private CodeforcesSubmissionCollectionResult collectRecentWindow(
            Duration lookback,
            CollectionHandleSupplier handleSupplier
    ) throws JsonProcessingException {
        Duration normalizedLookback = requirePositiveDuration(lookback, "lookback");
        Instant windowEndExclusive = clock.instant();
        return collectWindow(windowEndExclusive.minus(normalizedLookback), windowEndExclusive, handleSupplier);
    }

    private CodeforcesSubmissionCollectionResult collectWindow(
            Instant windowStartInclusive,
            Instant windowEndExclusive,
            CollectionHandleSupplier handleSupplier
    ) throws JsonProcessingException {
        requireWindow(windowStartInclusive, windowEndExclusive);
        if (!running.compareAndSet(false, true)) {
            log.warn("Codeforces submission collection skipped, errorCode={}", ALREADY_RUNNING_ERROR_CODE);
            return new CodeforcesSubmissionCollectionResult(
                    CodeforcesSubmissionCollectionStatus.SKIPPED,
                    windowStartInclusive,
                    windowEndExclusive,
                    0,
                    0,
                    0,
                    0,
                    0,
                    null,
                    null,
                    0,
                    null,
                    "Codeforces submission collection is already running",
                    List.of()
            );
        }
        try {
            return doCollectWindow(windowStartInclusive, windowEndExclusive, handleSupplier);
        } finally {
            running.set(false);
        }
    }

    private CodeforcesSubmissionCollectionResult doCollectWindow(
            Instant windowStartInclusive,
            Instant windowEndExclusive,
            CollectionHandleSupplier handleSupplier
    ) throws JsonProcessingException {
        List<String> handles = handleSupplier.listHandles();
        if (handles.isEmpty()) {
            return resultWithoutWrite(
                    CodeforcesSubmissionCollectionStatus.SUCCESS,
                    windowStartInclusive,
                    windowEndExclusive,
                    0,
                    List.of(),
                    "No Codeforces handles configured for collection"
            );
        }

        ArrayNode submissions = objectMapper.createArrayNode();
        List<CodeforcesSubmissionCollectionHandleResult> handleResults = new ArrayList<>();
        RequestThrottle throttle = new RequestThrottle();
        for (String handle : handles) {
            HandleCollectionResult handleResult = collectHandle(
                    handle,
                    windowStartInclusive,
                    windowEndExclusive,
                    throttle
            );
            handleResults.add(handleResult.result());
            if (handleResult.result().status() == CodeforcesSubmissionCollectionHandleStatus.SUCCESS) {
                handleResult.submissions().forEach(submission -> submissions.add(submission.deepCopy()));
            }
        }

        int requestedHandleCount = handles.size();
        int failedHandleCount = failedHandleCount(handleResults);
        CodeforcesSubmissionCollectionStatus status = status(requestedHandleCount, failedHandleCount);
        if (submissions.isEmpty()) {
            return resultWithoutWrite(
                    status,
                    windowStartInclusive,
                    windowEndExclusive,
                    requestedHandleCount,
                    handleResults,
                    "No Codeforces submissions matched the collection window"
            );
        }

        CodeforcesOdsBatchUpsertResult upsertResult = ingestService.upsertSubmissions(
                submissions,
                COLLECTOR_BATCH_ID_PREFIX
        );
        // TODO: Trigger downstream training-data scheduling after the scheduler/orchestrator contract is implemented.
        return new CodeforcesSubmissionCollectionResult(
                status,
                windowStartInclusive,
                windowEndExclusive,
                requestedHandleCount,
                requestedHandleCount - failedHandleCount,
                failedHandleCount,
                fetchedSubmissionCount(handleResults),
                matchedSubmissionCount(handleResults),
                upsertResult.batchId(),
                upsertResult.tableName(),
                upsertResult.writtenRows(),
                upsertResult.fetchedAt(),
                null,
                handleResults
        );
    }

    private List<String> listHandlesForCollection() {
        return handleAccountService.listAll().stream()
                .map(account -> account.handle().trim())
                .filter(handle -> !handle.isBlank())
                .distinct()
                .toList();
    }

    private HandleCollectionResult collectHandle(
            String handle,
            Instant windowStartInclusive,
            Instant windowEndExclusive,
            RequestThrottle throttle
    ) {
        String normalizedHandle = requireText(handle, "handle");
        ArrayNode matchedSubmissions = objectMapper.createArrayNode();
        int fetchedSubmissionCount = 0;
        try {
            int from = 1;
            long startEpochSecond = inclusiveStartEpochSecond(windowStartInclusive);
            long endEpochSecond = exclusiveEndEpochSecond(windowEndExclusive);
            while (true) {
                JsonNode responsePage = fetchUserStatusWithRetry(normalizedHandle, from, throttle);
                if (!responsePage.isArray()) {
                    throw new IllegalArgumentException("Codeforces user.status result must be an array");
                }
                fetchedSubmissionCount += responsePage.size();
                boolean allSubmissionsAreOlderThanWindow = !responsePage.isEmpty();
                for (JsonNode submission : responsePage) {
                    Long creationTimeSeconds = creationTimeSeconds(submission);
                    if (creationTimeSeconds == null) {
                        allSubmissionsAreOlderThanWindow = false;
                        continue;
                    }
                    if (creationTimeSeconds >= startEpochSecond && creationTimeSeconds < endEpochSecond) {
                        matchedSubmissions.add(submission.deepCopy());
                    }
                    if (creationTimeSeconds >= startEpochSecond) {
                        allSubmissionsAreOlderThanWindow = false;
                    }
                }
                if (responsePage.size() < pageSize || allSubmissionsAreOlderThanWindow) {
                    break;
                }
                from += pageSize;
            }
            return new HandleCollectionResult(
                    CodeforcesSubmissionCollectionHandleResult.success(
                            normalizedHandle,
                            fetchedSubmissionCount,
                            matchedSubmissions.size()
                    ),
                    matchedSubmissions
            );
        } catch (RuntimeException ex) {
            String errorCode = errorCode(ex);
            log.warn(
                    "Codeforces handle collection failed, errorCode={}, handleHash={}, reason={}",
                    errorCode,
                    sha256(normalizedHandle),
                    ex.getMessage()
            );
            return new HandleCollectionResult(
                    CodeforcesSubmissionCollectionHandleResult.failed(
                            normalizedHandle,
                            fetchedSubmissionCount,
                            errorCode,
                            ex.getMessage()
                    ),
                    objectMapper.createArrayNode()
            );
        }
    }

    private JsonNode fetchUserStatusWithRetry(String handle, int from, RequestThrottle throttle) {
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= maxRequestAttempts; attempt++) {
            try {
                throttle.beforeRequest();
                return sourceClient.fetchUserStatus(handle, from, pageSize);
            } catch (RuntimeException ex) {
                lastFailure = ex;
            }
        }
        throw lastFailure;
    }

    private CodeforcesSubmissionCollectionResult resultWithoutWrite(
            CodeforcesSubmissionCollectionStatus status,
            Instant windowStartInclusive,
            Instant windowEndExclusive,
            int requestedHandleCount,
            List<CodeforcesSubmissionCollectionHandleResult> handleResults,
            String message
    ) {
        int failedHandleCount = failedHandleCount(handleResults);
        return new CodeforcesSubmissionCollectionResult(
                status,
                windowStartInclusive,
                windowEndExclusive,
                requestedHandleCount,
                requestedHandleCount - failedHandleCount,
                failedHandleCount,
                fetchedSubmissionCount(handleResults),
                matchedSubmissionCount(handleResults),
                null,
                null,
                0,
                null,
                message,
                handleResults
        );
    }

    private static CodeforcesSubmissionCollectionStatus status(int requestedHandleCount, int failedHandleCount) {
        if (failedHandleCount == 0) {
            return CodeforcesSubmissionCollectionStatus.SUCCESS;
        }
        if (failedHandleCount == requestedHandleCount) {
            return CodeforcesSubmissionCollectionStatus.FAILED;
        }
        return CodeforcesSubmissionCollectionStatus.PARTIAL_SUCCESS;
    }

    private static int failedHandleCount(List<CodeforcesSubmissionCollectionHandleResult> results) {
        return (int) results.stream()
                .filter(result -> result.status() == CodeforcesSubmissionCollectionHandleStatus.FAILED)
                .count();
    }

    private static int fetchedSubmissionCount(List<CodeforcesSubmissionCollectionHandleResult> results) {
        return results.stream()
                .mapToInt(CodeforcesSubmissionCollectionHandleResult::fetchedSubmissionCount)
                .sum();
    }

    private static int matchedSubmissionCount(List<CodeforcesSubmissionCollectionHandleResult> results) {
        return results.stream()
                .mapToInt(CodeforcesSubmissionCollectionHandleResult::matchedSubmissionCount)
                .sum();
    }

    private static Long creationTimeSeconds(JsonNode submission) {
        JsonNode value = submission.path("creationTimeSeconds");
        if (!value.canConvertToLong()) {
            return null;
        }
        return value.asLong();
    }

    private static String errorCode(RuntimeException ex) {
        if (ex instanceof CodeforcesApiException codeforcesApiException) {
            return codeforcesApiException.errorCode().name();
        }
        return HANDLE_FAILED_ERROR_CODE;
    }

    private static void requireWindow(Instant windowStartInclusive, Instant windowEndExclusive) {
        if (windowStartInclusive == null) {
            throw new IllegalArgumentException("windowStartInclusive must not be null");
        }
        if (windowEndExclusive == null) {
            throw new IllegalArgumentException("windowEndExclusive must not be null");
        }
        if (!windowStartInclusive.isBefore(windowEndExclusive)) {
            throw new IllegalArgumentException("windowStartInclusive must be before windowEndExclusive");
        }
    }

    private static long inclusiveStartEpochSecond(Instant instant) {
        long epochSecond = instant.getEpochSecond();
        return instant.getNano() == 0 ? epochSecond : Math.addExact(epochSecond, 1L);
    }

    private static long exclusiveEndEpochSecond(Instant instant) {
        long epochSecond = instant.getEpochSecond();
        return instant.getNano() == 0 ? epochSecond : Math.addExact(epochSecond, 1L);
    }

    private static Duration requirePositiveDuration(Duration value, String fieldName) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is not available", ex);
        }
    }

    public interface SleepStrategy {
        void sleep(Duration duration) throws InterruptedException;
    }

    private interface CollectionHandleSupplier {
        List<String> listHandles();
    }

    private record HandleCollectionResult(
            CodeforcesSubmissionCollectionHandleResult result,
            ArrayNode submissions
    ) {
    }

    private final class RequestThrottle {
        private boolean requestSent;

        void beforeRequest() {
            if (requestSent && !requestInterval.isZero()) {
                try {
                    sleepStrategy.sleep(requestInterval);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("interrupted while rate limiting Codeforces collection", ex);
                }
            }
            requestSent = true;
        }
    }
}
