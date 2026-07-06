package com.custacm.platform.trainingdata.codeforces.app.collector.job;

import com.custacm.platform.trainingdata.codeforces.app.collector.result.CodeforcesSubmissionCollectionResult;
import com.custacm.platform.trainingdata.codeforces.app.collector.result.CodeforcesSubmissionCollectionStatus;

import java.time.Instant;

public record CodeforcesSubmissionCollectionJobItem(
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
    public static CodeforcesSubmissionCollectionJobItem pending(String studentIdentity) {
        return new CodeforcesSubmissionCollectionJobItem(
                studentIdentity,
                CodeforcesSubmissionCollectionJobItemStatus.PENDING,
                null,
                null,
                null,
                null,
                0,
                0,
                0,
                null,
                null,
                CodeforcesSubmissionCollectionJobRefreshStatus.NOT_REQUESTED,
                null
        );
    }

    public CodeforcesSubmissionCollectionJobItem running() {
        return new CodeforcesSubmissionCollectionJobItem(
                studentIdentity,
                CodeforcesSubmissionCollectionJobItemStatus.RUNNING,
                collectionStatus,
                handle,
                batchId,
                tableName,
                writtenRows,
                fetchedSubmissionCount,
                matchedSubmissionCount,
                fetchedAt,
                message,
                refreshStatus,
                refreshMessage
        );
    }

    public static CodeforcesSubmissionCollectionJobItem collected(
            String studentIdentity,
            CodeforcesSubmissionCollectionResult result,
            CodeforcesSubmissionCollectionJobRefreshStatus refreshStatus,
            String refreshMessage
    ) {
        var handleResult = result.handles().isEmpty() ? null : result.handles().getFirst();
        CodeforcesSubmissionCollectionJobItemStatus itemStatus = switch (result.status()) {
            case SUCCESS, PARTIAL_SUCCESS -> CodeforcesSubmissionCollectionJobItemStatus.SUCCESS;
            case FAILED, SKIPPED -> CodeforcesSubmissionCollectionJobItemStatus.FAILED;
        };
        return new CodeforcesSubmissionCollectionJobItem(
                studentIdentity,
                itemStatus,
                result.status(),
                handleResult == null ? null : handleResult.handle(),
                result.batchId(),
                result.tableName(),
                result.writtenRows(),
                result.fetchedSubmissionCount(),
                result.matchedSubmissionCount(),
                result.fetchedAt(),
                result.message() != null ? result.message() : handleResult == null ? null : handleResult.message(),
                refreshStatus,
                refreshMessage
        );
    }

    public static CodeforcesSubmissionCollectionJobItem failed(String studentIdentity, String message) {
        return new CodeforcesSubmissionCollectionJobItem(
                studentIdentity,
                CodeforcesSubmissionCollectionJobItemStatus.FAILED,
                CodeforcesSubmissionCollectionStatus.FAILED,
                null,
                null,
                null,
                0,
                0,
                0,
                null,
                message,
                CodeforcesSubmissionCollectionJobRefreshStatus.NOT_REQUESTED,
                null
        );
    }
}
