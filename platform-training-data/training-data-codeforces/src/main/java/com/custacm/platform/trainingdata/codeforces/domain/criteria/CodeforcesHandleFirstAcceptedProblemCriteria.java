package com.custacm.platform.trainingdata.codeforces.domain.criteria;

import java.time.LocalDateTime;

public record CodeforcesHandleFirstAcceptedProblemCriteria(
        String authorHandle,
        LocalDateTime firstAcceptedFromUtcPlus8,
        LocalDateTime firstAcceptedToUtcPlus8,
        Integer minProblemRating,
        Integer maxProblemRating
) {
    public static CodeforcesHandleFirstAcceptedProblemCriteria allForHandle(String authorHandle) {
        return new CodeforcesHandleFirstAcceptedProblemCriteria(
                authorHandle,
                null,
                null,
                null,
                null
        );
    }
}
