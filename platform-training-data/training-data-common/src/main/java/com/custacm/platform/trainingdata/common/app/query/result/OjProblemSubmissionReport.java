package com.custacm.platform.trainingdata.common.app.query.result;

import java.util.List;

public record OjProblemSubmissionReport(
        String problemKey,
        int page,
        int limit,
        long total,
        long totalPages,
        boolean hasMore,
        List<OjSubmissionItem> submissions
) {
}
