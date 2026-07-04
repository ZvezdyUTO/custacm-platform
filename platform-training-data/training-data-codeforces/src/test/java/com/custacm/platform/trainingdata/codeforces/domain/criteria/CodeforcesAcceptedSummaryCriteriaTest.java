package com.custacm.platform.trainingdata.codeforces.domain.criteria;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class CodeforcesAcceptedSummaryCriteriaTest {
    @Test
    void carriesHandleAndDefaultsToNoProblemRatingBounds() {
        CodeforcesAcceptedSummaryCriteria criteria = new CodeforcesAcceptedSummaryCriteria(
                " tourist ",
                null,
                null,
                null,
                null
        );

        assertThat(criteria.authorHandle()).isEqualTo(" tourist ");
        assertThat(criteria.minProblemRating()).isNull();
        assertThat(criteria.maxProblemRating()).isNull();
    }

    @Test
    void carriesDateRangeWithoutExtraValidation() {
        CodeforcesAcceptedSummaryCriteria criteria = new CodeforcesAcceptedSummaryCriteria(
                "tourist",
                LocalDate.parse("2026-07-03"),
                LocalDate.parse("2026-07-01"),
                null,
                null
        );

        assertThat(criteria.acceptedFromDateUtcPlus8()).isEqualTo(LocalDate.parse("2026-07-03"));
        assertThat(criteria.acceptedToDateUtcPlus8()).isEqualTo(LocalDate.parse("2026-07-01"));
    }

    @Test
    void carriesProblemRatingBoundsWithoutExtraValidation() {
        CodeforcesAcceptedSummaryCriteria criteria = new CodeforcesAcceptedSummaryCriteria(
                "tourist",
                null,
                null,
                800,
                1600
        );

        assertThat(criteria.minProblemRating()).isEqualTo(800);
        assertThat(criteria.maxProblemRating()).isEqualTo(1600);
    }
}
