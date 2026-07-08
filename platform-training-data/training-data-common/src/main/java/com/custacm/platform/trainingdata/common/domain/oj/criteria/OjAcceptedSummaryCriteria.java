package com.custacm.platform.trainingdata.common.domain.oj.criteria;

import com.custacm.platform.trainingdata.common.domain.oj.value.OjNames;

import java.time.LocalDate;

public record OjAcceptedSummaryCriteria(
        String ojName,
        String authorHandle,
        LocalDate acceptedFromDateUtcPlus8,
        LocalDate acceptedToDateUtcPlus8,
        Integer minProblemRating,
        Integer maxProblemRating
) {
    public OjAcceptedSummaryCriteria(
            String authorHandle,
            LocalDate acceptedFromDateUtcPlus8,
            LocalDate acceptedToDateUtcPlus8,
            Integer minProblemRating,
            Integer maxProblemRating
    ) {
        this(
                OjNames.CODEFORCES,
                authorHandle,
                acceptedFromDateUtcPlus8,
                acceptedToDateUtcPlus8,
                minProblemRating,
                maxProblemRating
        );
    }

    public static OjAcceptedSummaryCriteria allForHandle(String authorHandle) {
        return allForHandle(OjNames.CODEFORCES, authorHandle);
    }

    public static OjAcceptedSummaryCriteria allForHandle(String ojName, String authorHandle) {
        return new OjAcceptedSummaryCriteria(
                ojName,
                authorHandle,
                null,
                null,
                null,
                null
        );
    }
}
