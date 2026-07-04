package com.custacm.platform.trainingdata.codeforces.domain.criteria;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CodeforcesWarehouseCriteriaTest {
    @Test
    void handleSubmissionCriteriaCarriesHandleAndAllowsOpenTimeRange() {
        CodeforcesHandleSubmissionCriteria criteria = new CodeforcesHandleSubmissionCriteria(
                " tourist ",
                LocalDateTime.parse("2026-07-01T00:00:00"),
                null,
                null,
                null
        );

        assertThat(criteria.authorHandle()).isEqualTo(" tourist ");
        assertThat(criteria.minProblemRating()).isNull();
        assertThat(criteria.maxProblemRating()).isNull();
    }

    @Test
    void problemSubmissionCriteriaCarriesProblemKeyAndTimeRange() {
        assertThat(new CodeforcesProblemSubmissionCriteria(" 1000:A ", null, null).problemKey())
                .isEqualTo(" 1000:A ");

        CodeforcesProblemSubmissionCriteria criteria = new CodeforcesProblemSubmissionCriteria(
                "1000:A",
                LocalDateTime.parse("2026-07-02T00:00:00"),
                LocalDateTime.parse("2026-07-01T00:00:00")
        );

        assertThat(criteria.submittedFromUtcPlus8()).isEqualTo(LocalDateTime.parse("2026-07-02T00:00:00"));
        assertThat(criteria.submittedToUtcPlus8()).isEqualTo(LocalDateTime.parse("2026-07-01T00:00:00"));
    }

    @Test
    void handleFirstAcceptedProblemCriteriaCarriesHandleAndDefaultsProblemRatingBounds() {
        CodeforcesHandleFirstAcceptedProblemCriteria criteria = new CodeforcesHandleFirstAcceptedProblemCriteria(
                " tourist ",
                null,
                LocalDateTime.parse("2026-07-02T00:00:00"),
                null,
                null
        );

        assertThat(criteria.authorHandle()).isEqualTo(" tourist ");
        assertThat(criteria.minProblemRating()).isNull();
        assertThat(criteria.maxProblemRating()).isNull();
    }

    @Test
    void handleCriteriaCarryProblemRatingBoundsWithoutExtraValidation() {
        CodeforcesHandleSubmissionCriteria submissionCriteria = new CodeforcesHandleSubmissionCriteria(
                "tourist",
                null,
                null,
                1200,
                null
        );
        CodeforcesHandleFirstAcceptedProblemCriteria firstAcceptedCriteria =
                new CodeforcesHandleFirstAcceptedProblemCriteria(
                        "tourist",
                        null,
                        null,
                        null,
                        1600
                );

        assertThat(submissionCriteria.minProblemRating()).isEqualTo(1200);
        assertThat(submissionCriteria.maxProblemRating()).isNull();
        assertThat(firstAcceptedCriteria.minProblemRating()).isNull();
        assertThat(firstAcceptedCriteria.maxProblemRating()).isEqualTo(1600);
    }

    @Test
    void problemFirstAcceptedHandleCriteriaCarriesProblemKeyAndTimeRange() {
        assertThat(new CodeforcesProblemFirstAcceptedHandleCriteria(" 1000:A ", null, null).problemKey())
                .isEqualTo(" 1000:A ");

        CodeforcesProblemFirstAcceptedHandleCriteria criteria = new CodeforcesProblemFirstAcceptedHandleCriteria(
                "1000:A",
                LocalDateTime.parse("2026-07-02T00:00:00"),
                LocalDateTime.parse("2026-07-01T00:00:00")
        );

        assertThat(criteria.firstAcceptedFromUtcPlus8()).isEqualTo(LocalDateTime.parse("2026-07-02T00:00:00"));
        assertThat(criteria.firstAcceptedToUtcPlus8()).isEqualTo(LocalDateTime.parse("2026-07-01T00:00:00"));
    }
}
