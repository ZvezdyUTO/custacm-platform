package com.custacm.platform.trainingdata.codeforces.web.query.response;

import com.custacm.platform.trainingdata.codeforces.app.query.result.CodeforcesHandleSubmissionReport;

import java.util.List;

public record CodeforcesStudentSubmissionReportResponse(
        String studentIdentity,
        String authorHandle,
        int page,
        int limit,
        long total,
        long totalPages,
        boolean hasMore,
        List<CodeforcesSubmissionItemResponse> submissions
) {
    public static CodeforcesStudentSubmissionReportResponse from(CodeforcesHandleSubmissionReport report) {
        return new CodeforcesStudentSubmissionReportResponse(
                report.studentIdentity(),
                report.authorHandle(),
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
