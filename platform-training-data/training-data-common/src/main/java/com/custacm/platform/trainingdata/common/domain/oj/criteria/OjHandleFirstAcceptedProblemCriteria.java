package com.custacm.platform.trainingdata.common.domain.oj.criteria;

import com.custacm.platform.trainingdata.common.domain.oj.value.OjNames;

import java.time.LocalDateTime;

public record OjHandleFirstAcceptedProblemCriteria(
        String ojName,
        String authorHandle,
        LocalDateTime firstAcceptedFromUtcPlus8,
        LocalDateTime firstAcceptedToUtcPlus8,
        Integer minProblemRating,
        Integer maxProblemRating
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
                maxProblemRating
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
                null
        );
    }
}
