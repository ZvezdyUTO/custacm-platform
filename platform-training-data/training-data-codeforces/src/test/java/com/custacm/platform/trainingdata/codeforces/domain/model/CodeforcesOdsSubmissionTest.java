package com.custacm.platform.trainingdata.codeforces.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CodeforcesOdsSubmissionTest {
    @Test
    void acceptsCodeforcesNativeSubmissionFields() {
        CodeforcesCollectBatch batch = new CodeforcesCollectBatch("batch-1", Instant.parse("2026-06-27T00:00:00Z"));

        CodeforcesOdsSubmission submission = submission(batch, 379398914L, "tourist");

        assertThat(submission.codeforcesSubmissionId()).isEqualTo(379398914L);
        assertThat(submission.creationTimeSeconds()).isEqualTo(1781798091L);
        assertThat(submission.problemIndex()).isEqualTo("G");
        assertThat(submission.problemPoints()).isEqualByComparingTo("2750.0");
        assertThat(submission.authorHandle()).isEqualTo("tourist");
        assertThat(submission.fetchedAt()).isEqualTo(batch.fetchedAt());
    }

    @Test
    void rejectsMissingRequiredCodeforcesFields() {
        CodeforcesCollectBatch batch = new CodeforcesCollectBatch("batch-1", Instant.parse("2026-06-27T00:00:00Z"));

        assertThatThrownBy(() -> submission(batch, null, "tourist"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("codeforcesSubmissionId");
        assertThatThrownBy(() -> submission(batch, 379398914L, ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("authorHandle");
    }

    @Test
    void rejectsInvalidCollectBatch() {
        assertThatThrownBy(() -> new CodeforcesCollectBatch("", Instant.parse("2026-06-27T00:00:00Z")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("batchId");
        assertThatThrownBy(() -> new CodeforcesCollectBatch("batch-1", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("fetchedAt");
    }

    private static CodeforcesOdsSubmission submission(
            CodeforcesCollectBatch batch,
            Long submissionId,
            String authorHandle
    ) {
        return new CodeforcesOdsSubmission(
                submissionId,
                2237L,
                1781798091L,
                4791,
                2237L,
                "G",
                "Send GCDs",
                "PROGRAMMING",
                new BigDecimal("2750.0"),
                2900,
                "[\"math\"]",
                authorHandle,
                "CONTESTANT",
                "{\"members\":[{\"handle\":\"tourist\"}]}",
                "C++23",
                "OK",
                "TESTS",
                10,
                375,
                4505600L,
                batch.batchId(),
                batch.fetchedAt(),
                "{}",
                "hash"
        );
    }
}
