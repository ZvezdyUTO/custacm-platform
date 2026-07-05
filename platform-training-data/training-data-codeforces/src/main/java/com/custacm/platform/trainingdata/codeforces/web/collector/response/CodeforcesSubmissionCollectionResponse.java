package com.custacm.platform.trainingdata.codeforces.web.collector.response;

import com.custacm.platform.trainingdata.codeforces.app.collector.result.CodeforcesSubmissionCollectionResult;
import com.custacm.platform.trainingdata.codeforces.app.collector.result.CodeforcesSubmissionCollectionStatus;

import java.time.Instant;
import java.util.List;

public record CodeforcesSubmissionCollectionResponse(
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
        List<CodeforcesSubmissionCollectionHandleResponse> handles
) {
    public static CodeforcesSubmissionCollectionResponse from(CodeforcesSubmissionCollectionResult result) {
        return new CodeforcesSubmissionCollectionResponse(
                result.status(),
                result.windowStartInclusive(),
                result.windowEndExclusive(),
                result.requestedHandleCount(),
                result.succeededHandleCount(),
                result.failedHandleCount(),
                result.fetchedSubmissionCount(),
                result.matchedSubmissionCount(),
                result.batchId(),
                result.tableName(),
                result.writtenRows(),
                result.fetchedAt(),
                result.message(),
                result.handles().stream()
                        .map(CodeforcesSubmissionCollectionHandleResponse::from)
                        .toList()
        );
    }
}
