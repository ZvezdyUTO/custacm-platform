package com.custacm.platform.trainingdata.atcoder.infra;

import com.custacm.platform.trainingdata.atcoder.domain.AtcoderCollectBatch;
import com.custacm.platform.trainingdata.atcoder.domain.AtcoderOdsProblemModel;
import com.custacm.platform.trainingdata.atcoder.domain.AtcoderOdsProblemModelWriter;
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

public class JdbcAtcoderOdsProblemModelWriter implements AtcoderOdsProblemModelWriter {
    private static final String UPSERT_SQL_LOCATION = "sql/ods/upsert_ods_atcoder__problem_model.sql";
    private static final ZoneOffset WAREHOUSE_ZONE_OFFSET = ZoneOffset.ofHours(8);

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final String upsertSql;

    public JdbcAtcoderOdsProblemModelWriter(NamedParameterJdbcTemplate jdbcTemplate) {
        this(jdbcTemplate, readSql(UPSERT_SQL_LOCATION));
    }

    JdbcAtcoderOdsProblemModelWriter(NamedParameterJdbcTemplate jdbcTemplate, String upsertSql) {
        this.jdbcTemplate = jdbcTemplate;
        this.upsertSql = upsertSql;
    }

    @Override
    public void upsertBatch(AtcoderCollectBatch batch, List<AtcoderOdsProblemModel> problemModels) {
        MapSqlParameterSource[] parameters = problemModels.stream()
                .map(problemModel -> toParameters(batch, problemModel))
                .toArray(MapSqlParameterSource[]::new);
        jdbcTemplate.batchUpdate(upsertSql, parameters);
    }

    private static MapSqlParameterSource toParameters(
            AtcoderCollectBatch batch,
            AtcoderOdsProblemModel problemModel
    ) {
        return new MapSqlParameterSource()
                .addValue("problemId", problemModel.problemId())
                .addValue("slope", problemModel.slope())
                .addValue("intercept", problemModel.intercept())
                .addValue("variance", problemModel.variance())
                .addValue("rawDifficulty", problemModel.rawDifficulty())
                .addValue("clippedDifficulty", problemModel.clippedDifficulty())
                .addValue("discrimination", problemModel.discrimination())
                .addValue("irtLogLikelihood", problemModel.irtLogLikelihood())
                .addValue("irtUsers", problemModel.irtUsers())
                .addValue("experimental", problemModel.experimental())
                .addValue("batchId", batch.batchId())
                .addValue("fetchedAt", timestamp(batch.fetchedAt()))
                .addValue("rawPayload", problemModel.rawPayload())
                .addValue("payloadHash", problemModel.payloadHash());
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
