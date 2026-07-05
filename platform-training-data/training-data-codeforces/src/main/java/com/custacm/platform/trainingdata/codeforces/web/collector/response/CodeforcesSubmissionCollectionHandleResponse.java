package com.custacm.platform.trainingdata.codeforces.web.collector.response;

import com.custacm.platform.trainingdata.codeforces.app.collector.result.CodeforcesSubmissionCollectionHandleResult;
import com.custacm.platform.trainingdata.codeforces.app.collector.result.CodeforcesSubmissionCollectionHandleStatus;

public record CodeforcesSubmissionCollectionHandleResponse(
        String handle,
        CodeforcesSubmissionCollectionHandleStatus status,
        int fetchedSubmissionCount,
        int matchedSubmissionCount,
        String errorCode,
        String message
) {
    public static CodeforcesSubmissionCollectionHandleResponse from(
            CodeforcesSubmissionCollectionHandleResult result
    ) {
        return new CodeforcesSubmissionCollectionHandleResponse(
                result.handle(),
                result.status(),
                result.fetchedSubmissionCount(),
                result.matchedSubmissionCount(),
                result.errorCode(),
                result.message()
        );
    }
}
