package com.custacm.platform.trainingdata.codeforces.web.query.response;

import com.custacm.platform.trainingdata.codeforces.app.query.result.CodeforcesProblemFirstAcceptedHandleReport;

import java.time.LocalDateTime;
import java.util.List;

public record CodeforcesProblemFirstAcceptedHandleReportResponse(
        String problemKey,
        int acceptedHandleCount,
        List<CodeforcesFirstAcceptedHandleResponse> acceptedHandles
) {
    public static CodeforcesProblemFirstAcceptedHandleReportResponse from(
            CodeforcesProblemFirstAcceptedHandleReport report
    ) {
        return new CodeforcesProblemFirstAcceptedHandleReportResponse(
                report.problemKey(),
                report.acceptedHandleCount(),
                report.acceptedHandles().stream()
                        .map(CodeforcesFirstAcceptedHandleResponse::from)
                        .toList()
        );
    }

    public record CodeforcesFirstAcceptedHandleResponse(
            String studentIdentity,
            String authorHandle,
            LocalDateTime firstAcceptedAtUtcPlus8
    ) {
        private static CodeforcesFirstAcceptedHandleResponse from(
                CodeforcesProblemFirstAcceptedHandleReport.CodeforcesFirstAcceptedHandle item
        ) {
            return new CodeforcesFirstAcceptedHandleResponse(
                    item.studentIdentity(),
                    item.authorHandle(),
                    item.firstAcceptedAtUtcPlus8()
            );
        }
    }
}
