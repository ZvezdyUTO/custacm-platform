package com.custacm.platform.trainingdata.common.web.query.response;

import com.custacm.platform.trainingdata.common.app.query.result.OjProblemFirstAcceptedHandleReport;

import java.time.LocalDateTime;
import java.util.List;

public record OjProblemFirstAcceptedHandleReportResponse(
        String problemKey,
        int acceptedHandleCount,
        List<OjFirstAcceptedHandleResponse> acceptedHandles
) {
    public static OjProblemFirstAcceptedHandleReportResponse from(
            OjProblemFirstAcceptedHandleReport report
    ) {
        return new OjProblemFirstAcceptedHandleReportResponse(
                report.problemKey(),
                report.acceptedHandleCount(),
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
