package com.custacm.platform.trainingdata.common.web.query.response;

import com.custacm.platform.trainingdata.common.app.query.result.OjHandleFirstAcceptedProblemReport;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record OjStudentFirstAcceptedProblemReportResponse(
        String username,
        String authorHandle,
        int totalAcceptedProblemCount,
        int page,
        int limit,
        long total,
        long totalPages,
        boolean hasMore,
        List<OjFirstAcceptedProblemItemResponse> problems
) {
    public static OjStudentFirstAcceptedProblemReportResponse from(
            OjHandleFirstAcceptedProblemReport report
    ) {
        return new OjStudentFirstAcceptedProblemReportResponse(
                report.username(),
                report.authorHandle(),
                report.totalAcceptedProblemCount(),
                report.page(),
                report.limit(),
                report.total(),
                report.totalPages(),
                report.hasMore(),
                report.problems().stream()
                        .map(OjFirstAcceptedProblemItemResponse::from)
                        .toList()
        );
    }

    public record OjFirstAcceptedProblemItemResponse(
            String problemKey,
            String problemIndex,
            String problemName,
            String difficulty,
            String firstAcceptedSubmissionId,
            LocalDateTime firstAcceptedAtUtcPlus8,
            LocalDate firstAcceptedDateUtcPlus8,
            String firstAcceptedLanguage,
            String firstAcceptedSourceUrl
    ) {
        private static OjFirstAcceptedProblemItemResponse from(
                OjHandleFirstAcceptedProblemReport.OjFirstAcceptedProblemItem item
        ) {
            return new OjFirstAcceptedProblemItemResponse(
                    item.problemKey(),
                    item.problemIndex(),
                    item.problemName(),
                    item.difficulty(),
                    item.firstAcceptedSubmissionId(),
                    item.firstAcceptedAtUtcPlus8(),
                    item.firstAcceptedDateUtcPlus8(),
                    item.firstAcceptedLanguage(),
                    item.firstAcceptedSourceUrl()
            );
        }
    }
}
