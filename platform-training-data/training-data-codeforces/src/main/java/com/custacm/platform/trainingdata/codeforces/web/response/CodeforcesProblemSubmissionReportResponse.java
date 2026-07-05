package com.custacm.platform.trainingdata.codeforces.web.response;

import com.custacm.platform.trainingdata.codeforces.app.result.CodeforcesProblemSubmissionReport;

import java.util.List;

public record CodeforcesProblemSubmissionReportResponse(
        String problemKey,
        List<CodeforcesSubmissionItemResponse> submissions
) {
    public static CodeforcesProblemSubmissionReportResponse from(CodeforcesProblemSubmissionReport report) {
        return new CodeforcesProblemSubmissionReportResponse(
                report.problemKey(),
                report.submissions().stream()
                        .map(CodeforcesSubmissionItemResponse::from)
                        .toList()
        );
    }
}
