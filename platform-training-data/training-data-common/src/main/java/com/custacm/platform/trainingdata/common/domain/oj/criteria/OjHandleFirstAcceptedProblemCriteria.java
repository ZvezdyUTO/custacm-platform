package com.custacm.platform.trainingdata.common.domain.oj.criteria;

import com.custacm.platform.trainingdata.common.domain.oj.value.OjNames;

import java.time.LocalDateTime;

public record OjHandleFirstAcceptedProblemCriteria(
        String ojName,
        String authorHandle,
        LocalDateTime firstAcceptedFromUtcPlus8,
        LocalDateTime firstAcceptedToUtcPlus8,
        Integer minProblemRating,
        Integer maxProblemRating,
        int limit,
        long offset
) {
    public OjHandleFirstAcceptedProblemCriteria(
            String authorHandle,
            LocalDateTime firstAcceptedFromUtcPlus8,
            LocalDateTime firstAcceptedToUtcPlus8,
            Integer minProblemRating,
            Integer maxProblemRating
    ) {
        this(
                OjNames.CODEFORCES,
                authorHandle,
                firstAcceptedFromUtcPlus8,
                firstAcceptedToUtcPlus8,
                minProblemRating,
                maxProblemRating,
                Integer.MAX_VALUE,
                0
        );
    }

    public OjHandleFirstAcceptedProblemCriteria(
            String ojName,
            String authorHandle,
            LocalDateTime firstAcceptedFromUtcPlus8,
            LocalDateTime firstAcceptedToUtcPlus8,
            Integer minProblemRating,
            Integer maxProblemRating
    ) {
        this(
                ojName,
                authorHandle,
                firstAcceptedFromUtcPlus8,
                firstAcceptedToUtcPlus8,
                minProblemRating,
                maxProblemRating,
                Integer.MAX_VALUE,
                0
        );
    }

    public OjHandleFirstAcceptedProblemCriteria(
            String authorHandle,
            LocalDateTime firstAcceptedFromUtcPlus8,
            LocalDateTime firstAcceptedToUtcPlus8,
            Integer minProblemRating,
            Integer maxProblemRating,
            int limit,
            long offset
    ) {
        this(
                OjNames.CODEFORCES,
                authorHandle,
                firstAcceptedFromUtcPlus8,
                firstAcceptedToUtcPlus8,
                minProblemRating,
                maxProblemRating,
                limit,
                offset
        );
    }

    public static OjHandleFirstAcceptedProblemCriteria allForHandle(String authorHandle) {
        return allForHandle(OjNames.CODEFORCES, authorHandle);
    }

    public static OjHandleFirstAcceptedProblemCriteria allForHandle(String ojName, String authorHandle) {
        return new OjHandleFirstAcceptedProblemCriteria(
                ojName,
                authorHandle,
                null,
                null,
                null,
                null,
                Integer.MAX_VALUE,
                0
        );
    }
}
