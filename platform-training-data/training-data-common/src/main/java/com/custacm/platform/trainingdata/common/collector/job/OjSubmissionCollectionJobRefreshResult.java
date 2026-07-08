package com.custacm.platform.trainingdata.common.collector.job;

import java.util.Objects;

public record OjSubmissionCollectionJobRefreshResult(
        OjSubmissionCollectionJobRefreshStatus status,
        String message
) {
    public OjSubmissionCollectionJobRefreshResult {
        Objects.requireNonNull(status, "status must not be null");
    }

    public static OjSubmissionCollectionJobRefreshResult notRequested() {
        return new OjSubmissionCollectionJobRefreshResult(
                OjSubmissionCollectionJobRefreshStatus.NOT_REQUESTED,
                null
        );
    }

    public static OjSubmissionCollectionJobRefreshResult noBatch() {
        return new OjSubmissionCollectionJobRefreshResult(
                OjSubmissionCollectionJobRefreshStatus.NO_BATCH,
                null
        );
    }

    public static OjSubmissionCollectionJobRefreshResult failed(String message) {
        return new OjSubmissionCollectionJobRefreshResult(
                OjSubmissionCollectionJobRefreshStatus.FAILED,
                message
        );
    }
}
