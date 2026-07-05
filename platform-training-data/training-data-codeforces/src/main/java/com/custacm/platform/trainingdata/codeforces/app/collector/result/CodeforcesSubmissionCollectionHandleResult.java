package com.custacm.platform.trainingdata.codeforces.app.collector.result;

public record CodeforcesSubmissionCollectionHandleResult(
        String handle,
        CodeforcesSubmissionCollectionHandleStatus status,
        int fetchedSubmissionCount,
        int matchedSubmissionCount,
        String errorCode,
        String message
) {
    public static CodeforcesSubmissionCollectionHandleResult success(
            String handle,
            int fetchedSubmissionCount,
            int matchedSubmissionCount
    ) {
        return new CodeforcesSubmissionCollectionHandleResult(
                handle,
                CodeforcesSubmissionCollectionHandleStatus.SUCCESS,
                fetchedSubmissionCount,
                matchedSubmissionCount,
                null,
                null
        );
    }

    public static CodeforcesSubmissionCollectionHandleResult failed(
            String handle,
            int fetchedSubmissionCount,
            String errorCode,
            String message
    ) {
        return new CodeforcesSubmissionCollectionHandleResult(
                handle,
                CodeforcesSubmissionCollectionHandleStatus.FAILED,
                fetchedSubmissionCount,
                0,
                errorCode,
                message
        );
    }
}
