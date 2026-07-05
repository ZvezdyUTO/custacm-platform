package com.custacm.platform.trainingdata.codeforces.app.result;

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
