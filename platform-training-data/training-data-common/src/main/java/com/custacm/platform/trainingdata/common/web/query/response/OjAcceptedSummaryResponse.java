package com.custacm.platform.trainingdata.common.web.query.response;

import com.custacm.platform.trainingdata.common.app.query.result.OjAcceptedSummaryReport;

import java.util.List;

public record OjAcceptedSummaryResponse(
        String studentIdentity,
        String authorHandle,
        int totalAcceptedProblemCount,
        List<OjRatingAcceptedCountResponse> ratingCounts
) {
    public static OjAcceptedSummaryResponse from(OjAcceptedSummaryReport report) {
        return new OjAcceptedSummaryResponse(
                report.studentIdentity(),
                report.authorHandle(),
                report.totalAcceptedProblemCount(),
                report.ratingCounts().stream()
                        .map(OjRatingAcceptedCountResponse::from)
                        .toList()
        );
    }

    public record OjRatingAcceptedCountResponse(
            String problemRating,
            int acceptedProblemCount
    ) {
        private static OjRatingAcceptedCountResponse from(
                OjAcceptedSummaryReport.OjRatingAcceptedCount ratingCount
        ) {
            return new OjRatingAcceptedCountResponse(
                    ratingCount.problemRating(),
                    ratingCount.acceptedProblemCount()
            );
        }
    }
}
