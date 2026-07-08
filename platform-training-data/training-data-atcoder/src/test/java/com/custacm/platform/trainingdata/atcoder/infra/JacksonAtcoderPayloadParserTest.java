package com.custacm.platform.trainingdata.atcoder.infra;

import com.custacm.platform.trainingdata.atcoder.domain.AtcoderCollectBatch;
import com.custacm.platform.trainingdata.atcoder.domain.AtcoderOdsProblem;
import com.custacm.platform.trainingdata.atcoder.domain.AtcoderOdsSubmission;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JacksonAtcoderPayloadParserTest {
    private static final AtcoderCollectBatch BATCH = new AtcoderCollectBatch(
            "batch-atcoder-1",
            Instant.parse("2026-07-07T01:00:00Z")
    );

    private final JacksonAtcoderPayloadParser parser = new JacksonAtcoderPayloadParser(new ObjectMapper());

    @Test
    void parsesKenkooooSubmissions() {
        var records = parser.parseSubmissions("""
                [
                  {
                    "id": 5870139,
                    "epoch_second": 1560170952,
                    "problem_id": "abc121_c",
                    "contest_id": "abc121",
                    "user_id": "tourist",
                    "language": "C++ 20 (gcc 12.2)",
                    "point": 300.0,
                    "length": 797,
                    "result": "AC",
                    "execution_time": 404
                  }
                ]
                """, BATCH);

        assertThat(records).singleElement().satisfies(record -> {
            assertThat(record.atcoderSubmissionId()).isEqualTo(5870139L);
            assertThat(record.epochSecond()).isEqualTo(1560170952L);
            assertThat(record.problemId()).isEqualTo("abc121_c");
            assertThat(record.contestId()).isEqualTo("abc121");
            assertThat(record.userId()).isEqualTo("tourist");
            assertThat(record.point()).isEqualByComparingTo(new BigDecimal("300.0"));
            assertThat(record.sourceCodeLength()).isEqualTo(797);
            assertThat(record.result()).isEqualTo("AC");
            assertThat(record.executionTimeMillis()).isEqualTo(404);
            assertThat(record.batchId()).isEqualTo(BATCH.batchId());
            assertThat(record.fetchedAt()).isEqualTo(BATCH.fetchedAt());
            assertThat(record.rawPayload()).contains("\"id\":5870139");
            assertThat(record.payloadHash()).hasSize(64);
        });
    }

    @Test
    void parsesKenkooooProblems() {
        var records = parser.parseProblems("""
                [
                  {
                    "id": "abc121_c",
                    "contest_id": "abc121",
                    "problem_index": "C",
                    "name": "Energy Drink Collector",
                    "title": "ABC121 C - Energy Drink Collector"
                  }
                ]
                """, BATCH);

        assertThat(records).singleElement().satisfies(record -> {
            assertThat(record.problemId()).isEqualTo("abc121_c");
            assertThat(record.contestId()).isEqualTo("abc121");
            assertThat(record.problemIndex()).isEqualTo("C");
            assertThat(record.problemName()).isEqualTo("Energy Drink Collector");
            assertThat(record.title()).isEqualTo("ABC121 C - Energy Drink Collector");
            assertThat(record.batchId()).isEqualTo(BATCH.batchId());
            assertThat(record.payloadHash()).hasSize(64);
        });
    }

    @Test
    void rejectsMissingRequiredSubmissionFields() {
        assertThatThrownBy(() -> parser.parseSubmissions("""
                [{ "id": 1, "epoch_second": 1560170952 }]
                """, BATCH))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing AtCoder field: user_id");
    }

    @Test
    void validatesOdsRecords() {
        assertThatThrownBy(() -> new AtcoderOdsSubmission(
                null,
                1L,
                null,
                null,
                "tourist",
                null,
                null,
                null,
                null,
                null,
                BATCH.batchId(),
                BATCH.fetchedAt(),
                "{}",
                "0".repeat(64)
        ))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("atcoderSubmissionId must not be null");

        assertThatThrownBy(() -> new AtcoderOdsProblem(
                " ",
                null,
                null,
                null,
                null,
                BATCH.batchId(),
                BATCH.fetchedAt(),
                "{}",
                "0".repeat(64)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("problemId must not be blank");
    }
}
