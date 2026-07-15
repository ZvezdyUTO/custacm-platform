package com.custacm.platform.trainingdata.common.collector.result;

public record OjSubmissionCollectionHandleResult(
        String handle,
        OjSubmissionCollectionHandleStatus status,
        int fetchedSubmissionCount,
        int matchedSubmissionCount,
        String errorCode,
        String message
) {
    public static OjSubmissionCollectionHandleResult success(
            String handle,
            int fetchedSubmissionCount,
            int matchedSubmissionCount
    ) {
        return new OjSubmissionCollectionHandleResult(
                handle,
                OjSubmissionCollectionHandleStatus.SUCCESS,
                fetchedSubmissionCount,
                matchedSubmissionCount,
                null,
                null
        );
    }

    public static OjSubmissionCollectionHandleResult failed(
            String handle,
            int fetchedSubmissionCount,
            int matchedSubmissionCount,
            String errorCode,
            String message
    ) {
        return new OjSubmissionCollectionHandleResult(
                handle,
                OjSubmissionCollectionHandleStatus.FAILED,
                fetchedSubmissionCount,
                matchedSubmissionCount,
                errorCode,
                message
        );
    }

    public static OjSubmissionCollectionHandleResult failed(
            String handle,
            int fetchedSubmissionCount,
            String errorCode,
            String message
    ) {
        return failed(handle, fetchedSubmissionCount, 0, errorCode, message);
    }
}
