package com.custacm.platform.trainingdata.common.domain.oj.criteria;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class OjWarehouseCriteriaTest {
    @Test
    void handleSubmissionCriteriaCarriesHandleAndAllowsOpenTimeRange() {
        OjHandleSubmissionCriteria criteria = new OjHandleSubmissionCriteria(
                " tourist ",
                LocalDateTime.parse("2026-07-01T00:00:00"),
                null,
                null,
                null,
                100,
                200
        );

        assertThat(criteria.authorHandle()).isEqualTo(" tourist ");
        assertThat(criteria.minProblemRating()).isNull();
        assertThat(criteria.maxProblemRating()).isNull();
        assertThat(criteria.limit()).isEqualTo(100);
        assertThat(criteria.offset()).isEqualTo(200);
    }

    @Test
    void problemSubmissionCriteriaCarriesProblemKeyAndTimeRange() {
        assertThat(new OjProblemSubmissionCriteria(" 1000:A ", null, null, 100, 0).problemKey())
                .isEqualTo(" 1000:A ");

        OjProblemSubmissionCriteria criteria = new OjProblemSubmissionCriteria(
                "1000:A",
                LocalDateTime.parse("2026-07-02T00:00:00"),
                LocalDateTime.parse("2026-07-01T00:00:00"),
                50,
                150
        );

        assertThat(criteria.submittedFromUtcPlus8()).isEqualTo(LocalDateTime.parse("2026-07-02T00:00:00"));
        assertThat(criteria.submittedToUtcPlus8()).isEqualTo(LocalDateTime.parse("2026-07-01T00:00:00"));
        assertThat(criteria.limit()).isEqualTo(50);
        assertThat(criteria.offset()).isEqualTo(150);
    }

    @Test
    void handleFirstAcceptedProblemCriteriaCarriesHandleAndDefaultsProblemRatingBounds() {
        OjHandleFirstAcceptedProblemCriteria criteria = new OjHandleFirstAcceptedProblemCriteria(
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
        OjHandleSubmissionCriteria submissionCriteria = new OjHandleSubmissionCriteria(
                "tourist",
                null,
                null,
                1200,
                null,
                100,
                0
        );
        OjHandleFirstAcceptedProblemCriteria firstAcceptedCriteria =
                new OjHandleFirstAcceptedProblemCriteria(
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
        assertThat(new OjProblemFirstAcceptedHandleCriteria(" 1000:A ", null, null).problemKey())
                .isEqualTo(" 1000:A ");

        OjProblemFirstAcceptedHandleCriteria criteria = new OjProblemFirstAcceptedHandleCriteria(
                "1000:A",
                LocalDateTime.parse("2026-07-02T00:00:00"),
                LocalDateTime.parse("2026-07-01T00:00:00")
        );

        assertThat(criteria.firstAcceptedFromUtcPlus8()).isEqualTo(LocalDateTime.parse("2026-07-02T00:00:00"));
        assertThat(criteria.firstAcceptedToUtcPlus8()).isEqualTo(LocalDateTime.parse("2026-07-01T00:00:00"));
    }
}
