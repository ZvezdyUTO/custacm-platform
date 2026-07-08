package com.custacm.platform.trainingdata.common.web.query.response;

import com.custacm.platform.trainingdata.common.app.query.result.OjProblemSubmissionReport;

import java.util.List;

public record OjProblemSubmissionReportResponse(
        String problemKey,
        int page,
        int limit,
        long total,
        long totalPages,
        boolean hasMore,
        List<OjSubmissionItemResponse> submissions
) {
    public static OjProblemSubmissionReportResponse from(OjProblemSubmissionReport report) {
        return new OjProblemSubmissionReportResponse(
                report.problemKey(),
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
