package com.custacm.platform.trainingdata.codeforces.web.query.response;

import com.custacm.platform.trainingdata.codeforces.app.query.result.CodeforcesProblemSubmissionReport;

import java.util.List;

public record CodeforcesProblemSubmissionReportResponse(
        String problemKey,
        int page,
        int limit,
        long total,
        long totalPages,
        boolean hasMore,
        List<CodeforcesSubmissionItemResponse> submissions
) {
    public static CodeforcesProblemSubmissionReportResponse from(CodeforcesProblemSubmissionReport report) {
        return new CodeforcesProblemSubmissionReportResponse(
                report.problemKey(),
                report.page(),
                report.limit(),
                report.total(),
                report.totalPages(),
                report.hasMore(),
                report.submissions().stream()
                        .map(CodeforcesSubmissionItemResponse::from)
                        .toList()
        );
    }
}
