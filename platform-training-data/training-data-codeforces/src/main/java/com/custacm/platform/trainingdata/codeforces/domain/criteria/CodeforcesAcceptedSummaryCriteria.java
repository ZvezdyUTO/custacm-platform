package com.custacm.platform.trainingdata.codeforces.domain.criteria;

import java.time.LocalDate;

public record CodeforcesAcceptedSummaryCriteria(
        String authorHandle,
        LocalDate acceptedFromDateUtcPlus8,
        LocalDate acceptedToDateUtcPlus8,
        Integer minProblemRating,
        Integer maxProblemRating
) {
    public static CodeforcesAcceptedSummaryCriteria allForHandle(String authorHandle) {
        return new CodeforcesAcceptedSummaryCriteria(
                authorHandle,
                null,
                null,
                null,
                null
        );
    }
}
