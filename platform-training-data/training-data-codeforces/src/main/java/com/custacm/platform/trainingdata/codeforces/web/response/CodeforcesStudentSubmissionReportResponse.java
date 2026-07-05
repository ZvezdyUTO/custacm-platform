package com.custacm.platform.trainingdata.codeforces.web.response;

import com.custacm.platform.trainingdata.codeforces.app.result.CodeforcesHandleSubmissionReport;

import java.util.List;

public record CodeforcesStudentSubmissionReportResponse(
        String studentIdentity,
        String authorHandle,
        List<CodeforcesSubmissionItemResponse> submissions
) {
    public static CodeforcesStudentSubmissionReportResponse from(CodeforcesHandleSubmissionReport report) {
        return new CodeforcesStudentSubmissionReportResponse(
                report.studentIdentity(),
                report.authorHandle(),
                report.submissions().stream()
                        .map(CodeforcesSubmissionItemResponse::from)
                        .toList()
        );
    }
}
