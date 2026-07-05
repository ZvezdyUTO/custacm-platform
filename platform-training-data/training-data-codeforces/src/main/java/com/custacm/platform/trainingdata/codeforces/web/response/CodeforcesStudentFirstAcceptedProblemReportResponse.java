package com.custacm.platform.trainingdata.codeforces.web.response;

import com.custacm.platform.trainingdata.codeforces.app.result.CodeforcesHandleFirstAcceptedProblemReport;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record CodeforcesStudentFirstAcceptedProblemReportResponse(
        String studentIdentity,
        String authorHandle,
        int totalAcceptedProblemCount,
        List<CodeforcesFirstAcceptedProblemItemResponse> problems
) {
    public static CodeforcesStudentFirstAcceptedProblemReportResponse from(
            CodeforcesHandleFirstAcceptedProblemReport report
    ) {
        return new CodeforcesStudentFirstAcceptedProblemReportResponse(
                report.studentIdentity(),
                report.authorHandle(),
                report.totalAcceptedProblemCount(),
                report.problems().stream()
                        .map(CodeforcesFirstAcceptedProblemItemResponse::from)
                        .toList()
        );
    }

    public record CodeforcesFirstAcceptedProblemItemResponse(
            String problemKey,
            long problemContestId,
            String problemIndex,
            String problemName,
            String problemType,
            BigDecimal problemPoints,
            Integer problemRating,
            String problemTagsJson,
            long firstAcceptedSubmissionId,
            LocalDateTime firstAcceptedAtUtcPlus8,
            LocalDate firstAcceptedDateUtcPlus8,
            String firstAcceptedLanguage
    ) {
        private static CodeforcesFirstAcceptedProblemItemResponse from(
                CodeforcesHandleFirstAcceptedProblemReport.CodeforcesFirstAcceptedProblemItem item
        ) {
            return new CodeforcesFirstAcceptedProblemItemResponse(
                    item.problemKey(),
                    item.problemContestId(),
                    item.problemIndex(),
                    item.problemName(),
                    item.problemType(),
                    item.problemPoints(),
                    item.problemRating(),
                    item.problemTagsJson(),
                    item.firstAcceptedSubmissionId(),
                    item.firstAcceptedAtUtcPlus8(),
                    item.firstAcceptedDateUtcPlus8(),
                    item.firstAcceptedLanguage()
            );
        }
    }
}
