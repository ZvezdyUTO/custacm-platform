package com.custacm.platform.trainingdata.common.web.collector.response;

import com.custacm.platform.trainingdata.common.collector.result.OjSubmissionCollectionResult;
import com.custacm.platform.trainingdata.common.collector.result.OjSubmissionCollectionStatus;

import java.time.Instant;
import java.util.List;

public record OjSubmissionCollectionResponse(
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
        List<OjSubmissionCollectionHandleResponse> handles
) {
    public static OjSubmissionCollectionResponse from(OjSubmissionCollectionResult result) {
        return new OjSubmissionCollectionResponse(
                result.ojName(),
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
                        .map(OjSubmissionCollectionHandleResponse::from)
                        .toList()
        );
    }
}
