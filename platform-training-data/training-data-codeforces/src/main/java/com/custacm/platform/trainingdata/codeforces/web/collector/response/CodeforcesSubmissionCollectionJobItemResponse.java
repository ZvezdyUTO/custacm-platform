package com.custacm.platform.trainingdata.codeforces.web.collector.response;

import com.custacm.platform.trainingdata.codeforces.app.collector.job.CodeforcesSubmissionCollectionJobItem;
import com.custacm.platform.trainingdata.codeforces.app.collector.job.CodeforcesSubmissionCollectionJobItemStatus;
import com.custacm.platform.trainingdata.codeforces.app.collector.job.CodeforcesSubmissionCollectionJobRefreshStatus;
import com.custacm.platform.trainingdata.codeforces.app.collector.result.CodeforcesSubmissionCollectionStatus;

import java.time.Instant;

public record CodeforcesSubmissionCollectionJobItemResponse(
        String studentIdentity,
        CodeforcesSubmissionCollectionJobItemStatus itemStatus,
        CodeforcesSubmissionCollectionStatus collectionStatus,
        String handle,
        String batchId,
        String tableName,
        int writtenRows,
        int fetchedSubmissionCount,
        int matchedSubmissionCount,
        Instant fetchedAt,
        String message,
        CodeforcesSubmissionCollectionJobRefreshStatus refreshStatus,
        String refreshMessage
) {
    public static CodeforcesSubmissionCollectionJobItemResponse from(CodeforcesSubmissionCollectionJobItem item) {
        return new CodeforcesSubmissionCollectionJobItemResponse(
                item.studentIdentity(),
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
