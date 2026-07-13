package com.custacm.platform.trainingdata.common.collector;

import com.custacm.platform.trainingdata.common.collector.result.OjHandleCollectionOutcome;
import com.custacm.platform.trainingdata.common.collector.result.OjSubmissionCollectionHandleResult;
import com.custacm.platform.trainingdata.common.collector.result.OjSubmissionCollectionHandleStatus;
import com.custacm.platform.trainingdata.common.collector.result.OjSubmissionCollectionResult;
import com.custacm.platform.trainingdata.common.collector.result.OjSubmissionCollectionWriteResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.custacm.platform.trainingdata.common.support.Texts.requireText;

public class OjSubmissionCollectionService {
    private static final Logger log = LoggerFactory.getLogger(OjSubmissionCollectionService.class);
    private static final String ALREADY_RUNNING_ERROR_CODE = "OJ_COLLECTOR_ALREADY_RUNNING";

    private final OjCollectionHandleResolver handleResolver;
    private final OjSubmissionCollectionAdapter adapter;
    private final int maxRequestAttempts;
    private final Duration requestInterval;
    private final Clock clock;
    private final OjCollectionRequestExecutor.SleepStrategy sleepStrategy;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public OjSubmissionCollectionService(
            OjCollectionHandleResolver handleResolver,
            OjSubmissionCollectionAdapter adapter,
            int maxRequestAttempts,
            Duration requestInterval,
            Clock clock,
            OjCollectionRequestExecutor.SleepStrategy sleepStrategy
    ) {
        this.handleResolver = Objects.requireNonNull(handleResolver, "handleResolver must not be null");
        this.adapter = Objects.requireNonNull(adapter, "adapter must not be null");
        this.maxRequestAttempts = maxRequestAttempts;
        this.requestInterval = requestInterval == null || requestInterval.isNegative()
                ? Duration.ZERO
                : requestInterval;
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.sleepStrategy = Objects.requireNonNull(sleepStrategy, "sleepStrategy must not be null");
    }

    public OjSubmissionCollectionResult collectRecentWindowForConfiguredHandles(
            Duration lookback
    ) throws JsonProcessingException {
        return collectRecentWindowForConfiguredHandles(adapter.defaultOjName(), lookback);
    }

    public OjSubmissionCollectionResult collectRecentWindowForConfiguredHandles(
            String ojName,
            Duration lookback
    ) throws JsonProcessingException {
        String normalizedOjName = requireOjName(ojName);
        return collectRecentWindow(
                normalizedOjName,
                lookback,
                () -> normalizeHandles(handleResolver.listHandlesForCollection(normalizedOjName))
        );
    }

    public OjSubmissionCollectionResult collectRecentWindowForUsername(
            String username,
            Duration lookback
    ) throws JsonProcessingException {
        return collectRecentWindowForUsername(adapter.defaultOjName(), username, lookback);
    }

    public OjSubmissionCollectionResult collectRecentWindowForUsername(
            String ojName,
            String username,
            Duration lookback
    ) throws JsonProcessingException {
        String normalizedOjName = requireOjName(ojName);
        String normalizedUsername = requireText(username, "username");
        String handle = requireText(
                handleResolver.getHandleByUsername(normalizedOjName, normalizedUsername),
                "handle"
        );
        return collectRecentWindow(normalizedOjName, lookback, () -> List.of(handle));
    }

    private OjSubmissionCollectionResult collectRecentWindow(
            String ojName,
            Duration lookback,
            CollectionHandleSupplier handleSupplier
    ) throws JsonProcessingException {
        Duration normalizedLookback = requirePositiveDuration(lookback, "lookback");
        Instant windowEndExclusive = clock.instant();
        return collectWindow(
                ojName,
                normalizedLookback,
                windowEndExclusive,
                handleSupplier
        );
    }

    private OjSubmissionCollectionResult collectWindow(
            String ojName,
            Duration lookback,
            Instant windowEndExclusive,
            CollectionHandleSupplier handleSupplier
    ) throws JsonProcessingException {
        Instant fallbackWindowStartInclusive = overlapWindowStart(windowEndExclusive, lookback);
        requireWindow(fallbackWindowStartInclusive, windowEndExclusive);
        if (!running.compareAndSet(false, true)) {
            log.warn(
                    "OJ submission collection skipped, errorCode={}, ojName={}",
                    ALREADY_RUNNING_ERROR_CODE,
                    ojName
            );
            return OjSubmissionCollectionResult.skipped(
                    ojName,
                    fallbackWindowStartInclusive,
                    windowEndExclusive,
                    adapter.displayName(ojName) + " submission collection is already running"
            );
        }
        try {
            return doCollectWindow(
                    ojName,
                    lookback,
                    fallbackWindowStartInclusive,
                    windowEndExclusive,
                    handleSupplier
            );
        } finally {
            running.set(false);
        }
    }

    private OjSubmissionCollectionResult doCollectWindow(
            String ojName,
            Duration lookback,
            Instant fallbackWindowStartInclusive,
            Instant windowEndExclusive,
            CollectionHandleSupplier handleSupplier
    ) throws JsonProcessingException {
        List<String> handles = handleSupplier.listHandles();
        if (handles.isEmpty()) {
            return OjSubmissionCollectionResult.withoutWrite(
                    ojName,
                    fallbackWindowStartInclusive,
                    windowEndExclusive,
                    0,
                    List.of(),
                    "No " + adapter.displayName(ojName) + " handles configured for collection"
            );
        }

        List<OjHandleCollectionOutcome> matchedOutcomes = new ArrayList<>();
        List<OjSubmissionCollectionHandleResult> handleResults = new ArrayList<>();
        List<OjHandleCollectionOutcome> successfulOutcomes = new ArrayList<>();
        Instant earliestWindowStartInclusive = null;
        OjCollectionRequestExecutor requestExecutor = new OjCollectionRequestExecutor(
                maxRequestAttempts,
                requestInterval,
                sleepStrategy
        );
        for (String handle : handles) {
            Instant windowStartInclusive = collectionWindowStart(
                    handleResolver.getLastCollectedAt(ojName, handle),
                    lookback,
                    windowEndExclusive
            );
            if (earliestWindowStartInclusive == null || windowStartInclusive.isBefore(earliestWindowStartInclusive)) {
                earliestWindowStartInclusive = windowStartInclusive;
            }
            OjHandleCollectionOutcome outcome = adapter.collectHandle(
                    ojName,
                    handle,
                    windowStartInclusive,
                    windowEndExclusive,
                    requestExecutor
            );
            handleResults.add(outcome.result());
            if (outcome.result().status() == OjSubmissionCollectionHandleStatus.SUCCESS) {
                successfulOutcomes.add(outcome);
                if (!outcome.submissions().isEmpty()) {
                    matchedOutcomes.add(outcome);
                }
            }
        }

        int requestedHandleCount = handles.size();
        Instant resultWindowStartInclusive = earliestWindowStartInclusive == null
                ? fallbackWindowStartInclusive
                : earliestWindowStartInclusive;
        if (matchedOutcomes.isEmpty()) {
            markSuccessfulHandleCollections(ojName, successfulOutcomes, windowEndExclusive);
            return OjSubmissionCollectionResult.withoutWrite(
                    ojName,
                    resultWindowStartInclusive,
                    windowEndExclusive,
                    requestedHandleCount,
                    handleResults,
                    "No " + adapter.displayName(ojName) + " submissions matched the collection window"
            );
        }

        OjSubmissionCollectionWriteResult writeResult = adapter.writeBatch(ojName, matchedOutcomes);
        markSuccessfulHandleCollections(ojName, successfulOutcomes, windowEndExclusive);
        return OjSubmissionCollectionResult.written(
                ojName,
                resultWindowStartInclusive,
                windowEndExclusive,
                requestedHandleCount,
                handleResults,
                writeResult
        );
    }

    private void markSuccessfulHandleCollections(
            String ojName,
            List<OjHandleCollectionOutcome> successfulOutcomes,
            Instant collectedAt
    ) {
        for (OjHandleCollectionOutcome outcome : successfulOutcomes) {
            handleResolver.markHandleCollected(
                    ojName,
                    outcome.result().handle(),
                    collectedAt
            );
        }
    }

    private static Instant collectionWindowStart(
            Instant lastCollectedAt,
            Duration lookback,
            Instant windowEndExclusive
    ) {
        if (lastCollectedAt == null) {
            return Instant.EPOCH;
        }
        Instant effectiveLastCollectedAt = lastCollectedAt.isAfter(windowEndExclusive)
                ? windowEndExclusive
                : lastCollectedAt;
        return overlapWindowStart(effectiveLastCollectedAt, lookback);
    }

    private static Instant overlapWindowStart(Instant referenceTime, Duration lookback) {
        if (!referenceTime.isAfter(Instant.EPOCH)) {
            return Instant.EPOCH;
        }
        Instant candidate = referenceTime.minus(lookback);
        return candidate.isBefore(Instant.EPOCH) ? Instant.EPOCH : candidate;
    }

    private static List<String> normalizeHandles(List<String> handles) {
        if (handles == null) {
            return List.of();
        }
        return handles.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(handle -> !handle.isBlank())
                .distinct()
                .toList();
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

    private static Duration requirePositiveDuration(Duration value, String fieldName) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    private static String requireOjName(String value) {
        return requireText(value, "ojName").toUpperCase(Locale.ROOT);
    }

    private interface CollectionHandleSupplier {
        List<String> listHandles();
    }
}
