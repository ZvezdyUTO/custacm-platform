package com.custacm.platform.trainingdata.codeforces.infra;

import com.custacm.platform.trainingdata.codeforces.domain.CodeforcesCollectBatch;
import com.custacm.platform.trainingdata.codeforces.domain.CodeforcesOdsSubmission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcCodeforcesOdsSubmissionWriterTest {
    private JdbcTemplate jdbcTemplate;
    private JdbcCodeforcesOdsSubmissionWriter writer;

    @BeforeEach
    void setUp() throws Exception {
        DataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:ods_cf_" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        jdbcTemplate = new JdbcTemplate(dataSource);
        writer = new JdbcCodeforcesOdsSubmissionWriter(new NamedParameterJdbcTemplate(dataSource));
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V010__create_ods_codeforces_submission.sql"));
        }
    }

    @Test
    void upsertBatchIsIdempotentForSameCodeforcesSubmission() {
        CodeforcesCollectBatch firstBatch = new CodeforcesCollectBatch(
                "batch-1",
                Instant.parse("2026-06-27T00:00:00Z")
        );

        writer.upsertBatch(firstBatch, List.of(
                submission(firstBatch, 379398914L, "OK"),
                submission(firstBatch, 379397782L, "WRONG_ANSWER")
        ));
        writer.upsertBatch(firstBatch, List.of(
                submission(firstBatch, 379398914L, "OK"),
                submission(firstBatch, 379397782L, "WRONG_ANSWER")
        ));

        Integer count = jdbcTemplate.queryForObject("select count(*) from ods_codeforces__submission", Integer.class);
        assertThat(count).isEqualTo(2);

        CodeforcesCollectBatch secondBatch = new CodeforcesCollectBatch(
                "batch-2",
                Instant.parse("2026-06-27T01:00:00Z")
        );
        writer.upsertBatch(secondBatch, List.of(submission(secondBatch, 379398914L, "RUNTIME_ERROR")));

        String verdict = jdbcTemplate.queryForObject("""
                select verdict
                from ods_codeforces__submission
                where codeforces_submission_id = 379398914
                """, String.class);
        String batchId = jdbcTemplate.queryForObject("""
                select batch_id
                from ods_codeforces__submission
                where codeforces_submission_id = 379398914
                """, String.class);
        Long creationTimeSeconds = jdbcTemplate.queryForObject("""
                select creation_time_seconds
                from ods_codeforces__submission
                where codeforces_submission_id = 379398914
                """, Long.class);

        assertThat(verdict).isEqualTo("RUNTIME_ERROR");
        assertThat(batchId).isEqualTo("batch-2");
        assertThat(creationTimeSeconds).isEqualTo(1781798091L);
    }

    private static CodeforcesOdsSubmission submission(
            CodeforcesCollectBatch batch,
            long submissionId,
            String verdict
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
                "tourist",
                "CONTESTANT",
                "{\"members\":[{\"handle\":\"tourist\"}]}",
                "C++23",
                verdict,
                "TESTS",
                10,
                375,
                4505600L,
                batch.batchId(),
                batch.fetchedAt(),
                "{\"id\":" + submissionId + "}",
                "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
        );
    }
}
