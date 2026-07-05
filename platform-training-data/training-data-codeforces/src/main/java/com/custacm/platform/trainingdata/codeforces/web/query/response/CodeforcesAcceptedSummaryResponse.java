package com.custacm.platform.trainingdata.codeforces.web.query.response;

import com.custacm.platform.trainingdata.codeforces.app.query.result.CodeforcesAcceptedSummaryReport;

import java.util.List;

public record CodeforcesAcceptedSummaryResponse(
        String studentIdentity,
        String authorHandle,
        int totalAcceptedProblemCount,
        List<CodeforcesRatingAcceptedCountResponse> ratingCounts
) {
    public static CodeforcesAcceptedSummaryResponse from(CodeforcesAcceptedSummaryReport report) {
        return new CodeforcesAcceptedSummaryResponse(
                report.studentIdentity(),
                report.authorHandle(),
                report.totalAcceptedProblemCount(),
                report.ratingCounts().stream()
                        .map(CodeforcesRatingAcceptedCountResponse::from)
                        .toList()
        );
    }

    public record CodeforcesRatingAcceptedCountResponse(
            String problemRating,
            int acceptedProblemCount
    ) {
        private static CodeforcesRatingAcceptedCountResponse from(
                CodeforcesAcceptedSummaryReport.CodeforcesRatingAcceptedCount ratingCount
        ) {
            return new CodeforcesRatingAcceptedCountResponse(
                    ratingCount.problemRating(),
                    ratingCount.acceptedProblemCount()
            );
        }
    }
}
