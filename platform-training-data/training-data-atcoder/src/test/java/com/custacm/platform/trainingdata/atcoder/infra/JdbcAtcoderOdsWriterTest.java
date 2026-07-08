package com.custacm.platform.trainingdata.atcoder.infra;

import com.custacm.platform.trainingdata.atcoder.domain.AtcoderCollectBatch;
import com.custacm.platform.trainingdata.atcoder.domain.AtcoderOdsProblem;
import com.custacm.platform.trainingdata.atcoder.domain.AtcoderOdsProblemModel;
import com.custacm.platform.trainingdata.atcoder.domain.AtcoderOdsSubmission;
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
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcAtcoderOdsWriterTest {
    private JdbcTemplate jdbcTemplate;
    private JdbcAtcoderOdsSubmissionWriter submissionWriter;
    private JdbcAtcoderOdsProblemWriter problemWriter;
    private JdbcAtcoderOdsProblemModelWriter problemModelWriter;

    @BeforeEach
    void setUp() throws Exception {
        DataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:atcoder_ods_writer_" + UUID.randomUUID()
                        + ";MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        jdbcTemplate = new JdbcTemplate(dataSource);
        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        submissionWriter = new JdbcAtcoderOdsSubmissionWriter(namedParameterJdbcTemplate);
        problemWriter = new JdbcAtcoderOdsProblemWriter(namedParameterJdbcTemplate);
        problemModelWriter = new JdbcAtcoderOdsProblemModelWriter(namedParameterJdbcTemplate);
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V019__create_atcoder_ods_tables.sql"));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V023__create_atcoder_problem_model_table.sql"));
        }
    }

    @Test
    void upsertsSubmissionsByAtcoderSubmissionId() {
        AtcoderCollectBatch firstBatch = new AtcoderCollectBatch(
                "batch-1",
                Instant.parse("2026-07-07T00:00:00Z")
        );
        submissionWriter.upsertBatch(firstBatch, List.of(submission(firstBatch, 5870139L, "WA")));
        submissionWriter.upsertBatch(firstBatch, List.of(submission(firstBatch, 5870139L, "WA")));

        assertThat(jdbcTemplate.queryForObject("select count(*) from ods_atcoder__submission", Integer.class))
                .isEqualTo(1);

        AtcoderCollectBatch secondBatch = new AtcoderCollectBatch(
                "batch-2",
                Instant.parse("2026-07-07T01:00:00Z")
        );
        submissionWriter.upsertBatch(secondBatch, List.of(submission(secondBatch, 5870139L, "AC")));

        assertThat(jdbcTemplate.queryForObject("""
                select result from ods_atcoder__submission where atcoder_submission_id = 5870139
                """, String.class)).isEqualTo("AC");
        assertThat(jdbcTemplate.queryForObject("""
                select batch_id from ods_atcoder__submission where atcoder_submission_id = 5870139
                """, String.class)).isEqualTo("batch-2");
        Timestamp fetchedAt = jdbcTemplate.queryForObject("""
                select fetched_at from ods_atcoder__submission where atcoder_submission_id = 5870139
                """, Timestamp.class);
        assertThat(fetchedAt.toLocalDateTime()).isEqualTo(LocalDateTime.parse("2026-07-07T09:00:00"));
    }

    @Test
    void upsertsProblemsByProblemId() {
        AtcoderCollectBatch firstBatch = new AtcoderCollectBatch(
                "problem-batch-1",
                Instant.parse("2026-07-07T00:00:00Z")
        );
        problemWriter.upsertBatch(firstBatch, List.of(problem(firstBatch, "old title")));

        AtcoderCollectBatch secondBatch = new AtcoderCollectBatch(
                "problem-batch-2",
                Instant.parse("2026-07-07T02:00:00Z")
        );
        problemWriter.upsertBatch(secondBatch, List.of(problem(secondBatch, "new title")));

        assertThat(jdbcTemplate.queryForObject("select count(*) from ods_atcoder__problem", Integer.class))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("""
                select title from ods_atcoder__problem where problem_id = 'abc121_c'
                """, String.class)).isEqualTo("new title");
        assertThat(jdbcTemplate.queryForObject("""
                select batch_id from ods_atcoder__problem where problem_id = 'abc121_c'
                """, String.class)).isEqualTo("problem-batch-2");
    }

    @Test
    void upsertsProblemModelsByProblemId() {
        AtcoderCollectBatch firstBatch = new AtcoderCollectBatch(
                "problem-model-batch-1",
                Instant.parse("2026-07-07T00:00:00Z")
        );
        problemModelWriter.upsertBatch(firstBatch, List.of(problemModel(firstBatch, 873, 873)));

        AtcoderCollectBatch secondBatch = new AtcoderCollectBatch(
                "problem-model-batch-2",
                Instant.parse("2026-07-07T02:00:00Z")
        );
        problemModelWriter.upsertBatch(secondBatch, List.of(problemModel(secondBatch, 944, 944)));

        assertThat(jdbcTemplate.queryForObject("select count(*) from ods_atcoder__problem_model", Integer.class))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("""
                select raw_difficulty from ods_atcoder__problem_model where problem_id = 'abc121_c'
                """, Integer.class)).isEqualTo(944);
        assertThat(jdbcTemplate.queryForObject("""
                select clipped_difficulty from ods_atcoder__problem_model where problem_id = 'abc121_c'
                """, Integer.class)).isEqualTo(944);
        assertThat(jdbcTemplate.queryForObject("""
                select batch_id from ods_atcoder__problem_model where problem_id = 'abc121_c'
                """, String.class)).isEqualTo("problem-model-batch-2");
    }

    private static AtcoderOdsSubmission submission(
            AtcoderCollectBatch batch,
            long submissionId,
            String result
    ) {
        return new AtcoderOdsSubmission(
                submissionId,
                1560170952L,
                "abc121_c",
                "abc121",
                "tourist",
                "C++ 20",
                new BigDecimal("300.0"),
                797,
                result,
                404,
                batch.batchId(),
                batch.fetchedAt(),
                "{\"id\":" + submissionId + "}",
                "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
        );
    }

    private static AtcoderOdsProblem problem(AtcoderCollectBatch batch, String title) {
        return new AtcoderOdsProblem(
                "abc121_c",
                "abc121",
                "C",
                "Energy Drink Collector",
                title,
                batch.batchId(),
                batch.fetchedAt(),
                "{\"id\":\"abc121_c\"}",
                "abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789"
        );
    }

    private static AtcoderOdsProblemModel problemModel(
            AtcoderCollectBatch batch,
            Integer rawDifficulty,
            Integer clippedDifficulty
    ) {
        return new AtcoderOdsProblemModel(
                "abc121_c",
                new BigDecimal("-0.001"),
                new BigDecimal("5.5"),
                new BigDecimal("0.9"),
                rawDifficulty,
                clippedDifficulty,
                new BigDecimal("0.004"),
                new BigDecimal("-260.5"),
                4554,
                false,
                batch.batchId(),
                batch.fetchedAt(),
                "{\"difficulty\":" + rawDifficulty + "}",
                "123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0"
        );
    }
}
