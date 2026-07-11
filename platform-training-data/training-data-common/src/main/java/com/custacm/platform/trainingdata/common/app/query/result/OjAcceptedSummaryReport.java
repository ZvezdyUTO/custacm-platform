package com.custacm.platform.trainingdata.common.app.query.result;

import java.util.List;

public record OjAcceptedSummaryReport(
        String username,
        String authorHandle,
        int totalAcceptedProblemCount,
        List<OjRatingAcceptedCount> ratingCounts
) {
    public record OjRatingAcceptedCount(
            String problemRating,
            int acceptedProblemCount
    ) {
    }
}
