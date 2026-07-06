package com.custacm.platform.trainingdata.codeforces.app.query.result;

import java.util.List;

public record CodeforcesHandleSubmissionReport(
        String studentIdentity,
        String authorHandle,
        int page,
        int limit,
        long total,
        long totalPages,
        boolean hasMore,
        List<CodeforcesSubmissionItem> submissions
) {
}
