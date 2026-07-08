package com.custacm.platform.trainingdata.atcoder.infra;

import com.custacm.platform.trainingdata.atcoder.domain.AtcoderCollectBatch;
import com.custacm.platform.trainingdata.atcoder.domain.AtcoderOdsProblem;
import com.custacm.platform.trainingdata.atcoder.domain.AtcoderOdsProblemWriter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

public class JdbcAtcoderOdsProblemWriter implements AtcoderOdsProblemWriter {
    private static final String UPSERT_SQL_LOCATION = "sql/ods/upsert_ods_atcoder__problem.sql";
    private static final ZoneOffset WAREHOUSE_ZONE_OFFSET = ZoneOffset.ofHours(8);

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final String upsertSql;

    public JdbcAtcoderOdsProblemWriter(NamedParameterJdbcTemplate jdbcTemplate) {
        this(jdbcTemplate, readSql(UPSERT_SQL_LOCATION));
    }

    JdbcAtcoderOdsProblemWriter(NamedParameterJdbcTemplate jdbcTemplate, String upsertSql) {
        this.jdbcTemplate = jdbcTemplate;
        this.upsertSql = upsertSql;
    }

    @Override
    public void upsertBatch(AtcoderCollectBatch batch, List<AtcoderOdsProblem> problems) {
        MapSqlParameterSource[] parameters = problems.stream()
                .map(problem -> toParameters(batch, problem))
                .toArray(MapSqlParameterSource[]::new);
        jdbcTemplate.batchUpdate(upsertSql, parameters);
    }

    private static MapSqlParameterSource toParameters(AtcoderCollectBatch batch, AtcoderOdsProblem problem) {
        return new MapSqlParameterSource()
                .addValue("problemId", problem.problemId())
                .addValue("contestId", problem.contestId())
                .addValue("problemIndex", problem.problemIndex())
                .addValue("problemName", problem.problemName())
                .addValue("title", problem.title())
                .addValue("batchId", batch.batchId())
                .addValue("fetchedAt", timestamp(batch.fetchedAt()))
                .addValue("rawPayload", problem.rawPayload())
                .addValue("payloadHash", problem.payloadHash());
    }

    private static String readSql(String location) {
        try {
            return new ClassPathResource(location).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("failed to read SQL resource: " + location, ex);
        }
    }

    private static Timestamp timestamp(Instant instant) {
        if (instant == null) {
            return null;
        }
        return Timestamp.valueOf(LocalDateTime.ofInstant(instant, WAREHOUSE_ZONE_OFFSET));
    }
}
