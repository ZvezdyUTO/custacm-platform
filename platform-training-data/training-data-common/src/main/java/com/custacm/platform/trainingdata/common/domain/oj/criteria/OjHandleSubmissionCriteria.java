package com.custacm.platform.trainingdata.common.domain.oj.criteria;

import com.custacm.platform.trainingdata.common.domain.oj.value.OjNames;

import java.time.LocalDateTime;

public record OjHandleSubmissionCriteria(
        String ojName,
        String authorHandle,
        LocalDateTime submittedFromUtcPlus8,
        LocalDateTime submittedToUtcPlus8,
        Integer minProblemRating,
        Integer maxProblemRating,
        int limit,
        long offset
) {
    public OjHandleSubmissionCriteria(
            String authorHandle,
            LocalDateTime submittedFromUtcPlus8,
            LocalDateTime submittedToUtcPlus8,
            Integer minProblemRating,
            Integer maxProblemRating,
            int limit,
            long offset
    ) {
        this(
                OjNames.CODEFORCES,
                authorHandle,
                submittedFromUtcPlus8,
                submittedToUtcPlus8,
                minProblemRating,
                maxProblemRating,
                limit,
                offset
        );
    }
}
