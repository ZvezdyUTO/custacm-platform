package com.custacm.platform.trainingdata.codeforces.infra;

import com.custacm.platform.trainingdata.codeforces.domain.CodeforcesCollectBatch;
import com.custacm.platform.trainingdata.codeforces.domain.CodeforcesOdsSubmission;
import com.custacm.platform.trainingdata.codeforces.domain.CodeforcesOdsSubmissionWriter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.List;

public class JdbcCodeforcesOdsSubmissionWriter implements CodeforcesOdsSubmissionWriter {
    private static final String UPSERT_SQL_LOCATION = "sql/ods/upsert_ods_codeforces__submission.sql";

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final String upsertSql;

    public JdbcCodeforcesOdsSubmissionWriter(NamedParameterJdbcTemplate jdbcTemplate) {
        this(jdbcTemplate, readSql(UPSERT_SQL_LOCATION));
    }

    JdbcCodeforcesOdsSubmissionWriter(NamedParameterJdbcTemplate jdbcTemplate, String upsertSql) {
        this.jdbcTemplate = jdbcTemplate;
        this.upsertSql = upsertSql;
    }

    @Override
    public void upsertBatch(CodeforcesCollectBatch batch, List<CodeforcesOdsSubmission> submissions) {
        MapSqlParameterSource[] parameters = submissions.stream()
                .map(submission -> toParameters(batch, submission))
                .toArray(MapSqlParameterSource[]::new);
        jdbcTemplate.batchUpdate(upsertSql, parameters);
    }

    private static MapSqlParameterSource toParameters(CodeforcesCollectBatch batch, CodeforcesOdsSubmission submission) {
        return new MapSqlParameterSource()
                .addValue("codeforcesSubmissionId", submission.codeforcesSubmissionId())
                .addValue("contestId", submission.contestId())
                .addValue("creationTimeSeconds", submission.creationTimeSeconds())
                .addValue("relativeTimeSeconds", submission.relativeTimeSeconds())
                .addValue("problemContestId", submission.problemContestId())
                .addValue("problemIndex", submission.problemIndex())
                .addValue("problemName", submission.problemName())
                .addValue("problemType", submission.problemType())
                .addValue("problemPoints", decimal(submission.problemPoints()))
                .addValue("problemRating", submission.problemRating())
                .addValue("problemTagsJson", submission.problemTagsJson())
                .addValue("authorHandle", submission.authorHandle())
                .addValue("authorParticipantType", submission.authorParticipantType())
                .addValue("authorJson", submission.authorJson())
                .addValue("programmingLanguage", submission.programmingLanguage())
                .addValue("verdict", submission.verdict())
                .addValue("testset", submission.testset())
                .addValue("passedTestCount", submission.passedTestCount())
                .addValue("timeConsumedMillis", submission.timeConsumedMillis())
                .addValue("memoryConsumedBytes", submission.memoryConsumedBytes())
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

    private static Timestamp timestamp(java.time.Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }
}
