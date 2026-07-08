package com.custacm.platform.trainingdata.atcoder.infra;

import com.custacm.platform.trainingdata.atcoder.domain.AtcoderCollectBatch;
import com.custacm.platform.trainingdata.atcoder.domain.AtcoderOdsSubmission;
import com.custacm.platform.trainingdata.atcoder.domain.AtcoderOdsSubmissionWriter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

public class JdbcAtcoderOdsSubmissionWriter implements AtcoderOdsSubmissionWriter {
    private static final String UPSERT_SQL_LOCATION = "sql/ods/upsert_ods_atcoder__submission.sql";
    private static final ZoneOffset WAREHOUSE_ZONE_OFFSET = ZoneOffset.ofHours(8);

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final String upsertSql;

    public JdbcAtcoderOdsSubmissionWriter(NamedParameterJdbcTemplate jdbcTemplate) {
        this(jdbcTemplate, readSql(UPSERT_SQL_LOCATION));
    }

    JdbcAtcoderOdsSubmissionWriter(NamedParameterJdbcTemplate jdbcTemplate, String upsertSql) {
        this.jdbcTemplate = jdbcTemplate;
        this.upsertSql = upsertSql;
    }

    @Override
    public void upsertBatch(AtcoderCollectBatch batch, List<AtcoderOdsSubmission> submissions) {
        MapSqlParameterSource[] parameters = submissions.stream()
                .map(submission -> toParameters(batch, submission))
                .toArray(MapSqlParameterSource[]::new);
        jdbcTemplate.batchUpdate(upsertSql, parameters);
    }

    private static MapSqlParameterSource toParameters(AtcoderCollectBatch batch, AtcoderOdsSubmission submission) {
        return new MapSqlParameterSource()
                .addValue("atcoderSubmissionId", submission.atcoderSubmissionId())
                .addValue("epochSecond", submission.epochSecond())
                .addValue("problemId", submission.problemId())
                .addValue("contestId", submission.contestId())
                .addValue("userId", submission.userId())
                .addValue("language", submission.language())
                .addValue("point", decimal(submission.point()))
                .addValue("sourceCodeLength", submission.sourceCodeLength())
                .addValue("result", submission.result())
                .addValue("executionTimeMillis", submission.executionTimeMillis())
                .addValue("batchId", batch.batchId())
                .addValue("fetchedAt", timestamp(batch.fetchedAt()))
                .addValue("rawPayload", submission.rawPayload())
                .addValue("payloadHash", submission.payloadHash());
    }

    private static String readSql(String location) {
        try {
            return new ClassPathResource(location).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("failed to read SQL resource: " + location, ex);
        }
    }

    private static BigDecimal decimal(BigDecimal value) {
        return value;
    }

    private static Timestamp timestamp(Instant instant) {
        if (instant == null) {
            return null;
        }
        return Timestamp.valueOf(LocalDateTime.ofInstant(instant, WAREHOUSE_ZONE_OFFSET));
    }
}
