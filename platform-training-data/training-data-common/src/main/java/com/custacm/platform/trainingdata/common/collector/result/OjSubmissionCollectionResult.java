package com.custacm.platform.trainingdata.common.collector.result;

import java.time.Instant;
import java.util.List;

import static com.custacm.platform.trainingdata.common.support.Texts.requireText;

public record OjSubmissionCollectionResult(
        String ojName,
        OjSubmissionCollectionStatus status,
        Instant windowStartInclusive,
        Instant windowEndExclusive,
        int requestedHandleCount,
        int succeededHandleCount,
        int failedHandleCount,
        int fetchedSubmissionCount,
        int matchedSubmissionCount,
        String batchId,
        String tableName,
        int writtenRows,
        Instant fetchedAt,
        String message,
        List<OjSubmissionCollectionHandleResult> handles
) {
    public OjSubmissionCollectionResult {
        ojName = requireText(ojName, "ojName");
        handles = handles == null ? List.of() : List.copyOf(handles);
    }

    public static OjSubmissionCollectionResult skipped(
            String ojName,
            Instant windowStartInclusive,
            Instant windowEndExclusive,
            String message
    ) {
        return new OjSubmissionCollectionResult(
                ojName,
                OjSubmissionCollectionStatus.SKIPPED,
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
                message,
                List.of()
        );
    }

    public static OjSubmissionCollectionResult withoutWrite(
            String ojName,
            Instant windowStartInclusive,
            Instant windowEndExclusive,
            int requestedHandleCount,
            List<OjSubmissionCollectionHandleResult> handles,
            String message
    ) {
        int failedHandleCount = failedHandleCount(handles);
        return new OjSubmissionCollectionResult(
                ojName,
                status(requestedHandleCount, failedHandleCount),
                windowStartInclusive,
                windowEndExclusive,
                requestedHandleCount,
                requestedHandleCount - failedHandleCount,
                failedHandleCount,
                fetchedSubmissionCount(handles),
                matchedSubmissionCount(handles),
                null,
                null,
                0,
                null,
                message,
                handles
        );
    }

    public static OjSubmissionCollectionResult written(
            String ojName,
            Instant windowStartInclusive,
            Instant windowEndExclusive,
            int requestedHandleCount,
            List<OjSubmissionCollectionHandleResult> handles,
            OjSubmissionCollectionWriteResult writeResult
    ) {
        int failedHandleCount = failedHandleCount(handles);
        return new OjSubmissionCollectionResult(
                ojName,
                status(requestedHandleCount, failedHandleCount),
                windowStartInclusive,
                windowEndExclusive,
                requestedHandleCount,
                requestedHandleCount - failedHandleCount,
                failedHandleCount,
                fetchedSubmissionCount(handles),
                matchedSubmissionCount(handles),
                writeResult.batchId(),
                writeResult.tableName(),
                writeResult.writtenRows(),
                writeResult.fetchedAt(),
                null,
                handles
        );
    }

    private static OjSubmissionCollectionStatus status(int requestedHandleCount, int failedHandleCount) {
        if (failedHandleCount == 0) {
            return OjSubmissionCollectionStatus.SUCCESS;
        }
        if (failedHandleCount == requestedHandleCount) {
            return OjSubmissionCollectionStatus.FAILED;
        }
        return OjSubmissionCollectionStatus.PARTIAL_SUCCESS;
    }

    private static int failedHandleCount(List<OjSubmissionCollectionHandleResult> handles) {
        return normalizedHandles(handles).stream()
                .filter(handle -> handle.status() == OjSubmissionCollectionHandleStatus.FAILED)
                .mapToInt(ignored -> 1)
                .sum();
    }

    private static int fetchedSubmissionCount(List<OjSubmissionCollectionHandleResult> handles) {
        return normalizedHandles(handles).stream()
                .mapToInt(OjSubmissionCollectionHandleResult::fetchedSubmissionCount)
                .sum();
    }

    private static int matchedSubmissionCount(List<OjSubmissionCollectionHandleResult> handles) {
        return normalizedHandles(handles).stream()
                .mapToInt(OjSubmissionCollectionHandleResult::matchedSubmissionCount)
                .sum();
    }

    private static List<OjSubmissionCollectionHandleResult> normalizedHandles(
            List<OjSubmissionCollectionHandleResult> handles
    ) {
        return handles == null ? List.of() : handles;
    }
}
