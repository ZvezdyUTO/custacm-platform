package com.custacm.platform.trainingdata.common.web.collector.response;

import com.custacm.platform.trainingdata.common.collector.job.OjSubmissionCollectionJobItem;
import com.custacm.platform.trainingdata.common.collector.job.OjSubmissionCollectionJobItemStatus;
import com.custacm.platform.trainingdata.common.collector.job.OjSubmissionCollectionJobRefreshStatus;
import com.custacm.platform.trainingdata.common.collector.result.OjSubmissionCollectionStatus;

import java.time.Instant;

public record OjSubmissionCollectionJobItemResponse(
        String studentIdentity,
        String ojName,
        OjSubmissionCollectionJobItemStatus itemStatus,
        OjSubmissionCollectionStatus collectionStatus,
        String handle,
        String batchId,
        String tableName,
        int writtenRows,
        int fetchedSubmissionCount,
        int matchedSubmissionCount,
        Instant fetchedAt,
        String message,
        OjSubmissionCollectionJobRefreshStatus refreshStatus,
        String refreshMessage
) {
    public static OjSubmissionCollectionJobItemResponse from(OjSubmissionCollectionJobItem item) {
        return new OjSubmissionCollectionJobItemResponse(
                item.studentIdentity(),
                item.ojName(),
                item.itemStatus(),
                item.collectionStatus(),
                item.handle(),
                item.batchId(),
                item.tableName(),
                item.writtenRows(),
                item.fetchedSubmissionCount(),
                item.matchedSubmissionCount(),
                item.fetchedAt(),
                item.message(),
                item.refreshStatus(),
                item.refreshMessage()
        );
    }
}
