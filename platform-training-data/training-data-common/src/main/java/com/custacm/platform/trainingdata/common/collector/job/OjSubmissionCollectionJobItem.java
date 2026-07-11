package com.custacm.platform.trainingdata.common.collector.job;

import com.custacm.platform.trainingdata.common.collector.result.OjSubmissionCollectionResult;
import com.custacm.platform.trainingdata.common.collector.result.OjSubmissionCollectionStatus;

import java.time.Instant;

public record OjSubmissionCollectionJobItem(
        String username,
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
    public static OjSubmissionCollectionJobItem pending(String username) {
        return pending(username, null);
    }

    public static OjSubmissionCollectionJobItem pending(String username, String ojName) {
        return new OjSubmissionCollectionJobItem(
                username,
                ojName,
                OjSubmissionCollectionJobItemStatus.PENDING,
                null,
                null,
                null,
                null,
                0,
                0,
                0,
                null,
                null,
                OjSubmissionCollectionJobRefreshStatus.NOT_REQUESTED,
                null
        );
    }

    public OjSubmissionCollectionJobItem running() {
        return new OjSubmissionCollectionJobItem(
                username,
                ojName,
                OjSubmissionCollectionJobItemStatus.RUNNING,
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

    public static OjSubmissionCollectionJobItem collected(
            String username,
            OjSubmissionCollectionResult result,
            OjSubmissionCollectionJobRefreshResult refreshResult
    ) {
        var handleResult = result.handles().isEmpty() ? null : result.handles().getFirst();
        OjSubmissionCollectionJobItemStatus itemStatus = switch (result.status()) {
            case SUCCESS, PARTIAL_SUCCESS -> OjSubmissionCollectionJobItemStatus.SUCCESS;
            case FAILED, SKIPPED -> OjSubmissionCollectionJobItemStatus.FAILED;
        };
        OjSubmissionCollectionJobRefreshResult normalizedRefreshResult = refreshResult == null
                ? OjSubmissionCollectionJobRefreshResult.notRequested()
                : refreshResult;
        return new OjSubmissionCollectionJobItem(
                username,
                result.ojName(),
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
                normalizedRefreshResult.status(),
                normalizedRefreshResult.message()
        );
    }

    public static OjSubmissionCollectionJobItem failed(String username, String message) {
        return failed(username, null, message);
    }

    public static OjSubmissionCollectionJobItem failed(String username, String ojName, String message) {
        return new OjSubmissionCollectionJobItem(
                username,
                ojName,
                OjSubmissionCollectionJobItemStatus.FAILED,
                OjSubmissionCollectionStatus.FAILED,
                null,
                null,
                null,
                0,
                0,
                0,
                null,
                message,
                OjSubmissionCollectionJobRefreshStatus.NOT_REQUESTED,
                null
        );
    }
}
