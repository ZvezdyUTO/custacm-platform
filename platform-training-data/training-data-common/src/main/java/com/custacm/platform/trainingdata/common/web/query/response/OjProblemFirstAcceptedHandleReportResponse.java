package com.custacm.platform.trainingdata.common.web.query.response;

import com.custacm.platform.trainingdata.common.app.query.result.OjProblemFirstAcceptedHandleReport;

import java.time.LocalDateTime;
import java.util.List;

public record OjProblemFirstAcceptedHandleReportResponse(
        String problemKey,
        int acceptedHandleCount,
        int page,
        int limit,
        long total,
        long totalPages,
        boolean hasMore,
        List<OjFirstAcceptedHandleResponse> acceptedHandles
) {
    public static OjProblemFirstAcceptedHandleReportResponse from(
            OjProblemFirstAcceptedHandleReport report
    ) {
        return new OjProblemFirstAcceptedHandleReportResponse(
                report.problemKey(),
                report.acceptedHandleCount(),
                report.page(),
                report.limit(),
                report.total(),
                report.totalPages(),
                report.hasMore(),
                report.acceptedHandles().stream()
                        .map(OjFirstAcceptedHandleResponse::from)
                        .toList()
        );
    }

    public record OjFirstAcceptedHandleResponse(
            String studentIdentity,
            String handle,
            LocalDateTime firstAcceptedAtUtcPlus8
    ) {
        private static OjFirstAcceptedHandleResponse from(
                OjProblemFirstAcceptedHandleReport.OjFirstAcceptedHandle item
        ) {
            return new OjFirstAcceptedHandleResponse(
                    item.studentIdentity(),
                    item.handle(),
                    item.firstAcceptedAtUtcPlus8()
            );
        }
    }
}
