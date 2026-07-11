package com.custacm.platform.trainingdata.common.web.query.response;

import com.custacm.platform.trainingdata.common.app.query.result.OjHandleSubmissionReport;

import java.util.List;

public record OjStudentSubmissionReportResponse(
        String username,
        String authorHandle,
        int page,
        int limit,
        long total,
        long totalPages,
        boolean hasMore,
        List<OjSubmissionItemResponse> submissions
) {
    public static OjStudentSubmissionReportResponse from(OjHandleSubmissionReport report) {
        return new OjStudentSubmissionReportResponse(
                report.username(),
                report.authorHandle(),
                report.page(),
                report.limit(),
                report.total(),
                report.totalPages(),
                report.hasMore(),
                report.submissions().stream()
                        .map(OjSubmissionItemResponse::from)
                        .toList()
        );
    }
}
