package com.custacm.platform.trainingdata.codeforces.app.query.result;

import java.util.List;

public record CodeforcesAcceptedSummaryReport(
        String studentIdentity,
        String authorHandle,
        int totalAcceptedProblemCount,
        List<CodeforcesRatingAcceptedCount> ratingCounts
) {
    public record CodeforcesRatingAcceptedCount(
            String problemRating,
            int acceptedProblemCount
    ) {
    }
}
