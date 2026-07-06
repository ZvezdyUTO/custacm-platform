package com.custacm.platform.trainingdata.codeforces.domain.criteria;

import java.time.LocalDateTime;

public record CodeforcesHandleSubmissionCriteria(
        String authorHandle,
        LocalDateTime submittedFromUtcPlus8,
        LocalDateTime submittedToUtcPlus8,
        Integer minProblemRating,
        Integer maxProblemRating,
        int limit,
        long offset
) {
}
