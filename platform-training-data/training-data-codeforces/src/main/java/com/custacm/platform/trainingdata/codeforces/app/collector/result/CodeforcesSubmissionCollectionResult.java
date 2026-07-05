package com.custacm.platform.trainingdata.codeforces.app.collector.result;

import java.time.Instant;
import java.util.List;

public record CodeforcesSubmissionCollectionResult(
        CodeforcesSubmissionCollectionStatus status,
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
        List<CodeforcesSubmissionCollectionHandleResult> handles
) {
    public CodeforcesSubmissionCollectionResult {
        handles = handles == null ? List.of() : List.copyOf(handles);
    }
}
