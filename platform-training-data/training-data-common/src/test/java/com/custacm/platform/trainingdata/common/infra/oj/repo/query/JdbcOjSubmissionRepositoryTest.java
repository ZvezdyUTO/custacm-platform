package com.custacm.platform.trainingdata.common.infra.oj.repo.query;

import com.custacm.platform.trainingdata.common.domain.oj.criteria.OjProblemSubmissionCriteria;
import com.custacm.platform.trainingdata.common.domain.oj.criteria.OjHandleSubmissionCriteria;
import com.custacm.platform.trainingdata.common.domain.oj.value.OjDifficultyBucketPolicies;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcOjSubmissionRepositoryTest {
    private NamedParameterJdbcTemplate jdbcTemplate;
    private JdbcOjSubmissionRepository repository;

    @BeforeEach
    void setUp() throws Exception {
        DataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:dwd_cf_" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        repository = new JdbcOjSubmissionRepository(jdbcTemplate, OjDifficultyBucketPolicies.defaults());
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V011__create_codeforces_dwd_dwm_dws_tables.sql"));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V012__rename_codeforces_warehouse_time_columns_to_utc_plus8.sql"));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V015__add_codeforces_submission_pagination_indexes.sql"));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V017__reshape_codeforces_warehouse_to_common_contract.sql"));
        }

        insertSubmission(1001L, "tourist", "1000:A", "2026-07-01T10:00:00", 800, "OK");
        insertSubmission(1002L, "tourist", "1000:B", "2026-07-01T11:00:00", 1200, "WRONG_ANSWER");
        insertSubmission(1003L, "tourist", "1000:C", "2026-07-02T10:00:00", null, "OK");
        insertSubmission(1004L, "other", "1000:A", "2026-07-01T10:30:00", 800, "OK");
        insertSubmission(1005L, "tourist", "1000:A", "2026-07-03T10:00:00", 1600, "OK");
    }

    @Test
    void findsHandleSubmissionsBySubmittedTimeAndMinimumProblemRatingWithoutUnratedRows() {
        OjHandleSubmissionCriteria query = new OjHandleSubmissionCriteria(
                "tourist",
                LocalDateTime.parse("2026-07-01T10:30:00"),
                LocalDateTime.parse("2026-07-02T12:00:00"),
                1200,
                null,
                100,
                0
        );

        var submissions = repository.findHandleSubmissions(query);

        assertThat(submissions)
                .extracting(submission -> submission.submissionId())
                .containsExactly("1002");
    }

    @Test
    void findsHandleSubmissionsByMaximumProblemRatingWithoutUnratedRows() {
        OjHandleSubmissionCriteria query = new OjHandleSubmissionCriteria(
                "tourist",
                null,
                null,
                null,
                800,
                100,
                0
        );

        var submissions = repository.findHandleSubmissions(query);

        assertThat(submissions)
                .extracting(submission -> submission.submissionId())
                .containsExactly("1001");
    }

    @Test
    void findsProblemSubmissionsBySubmittedTimeAcrossHandles() {
        OjProblemSubmissionCriteria query = new OjProblemSubmissionCriteria(
                "1000:A",
                LocalDateTime.parse("2026-07-01T00:00:00"),
                LocalDateTime.parse("2026-07-02T00:00:00"),
                100,
                0
        );

        var submissions = repository.findProblemSubmissions(query);

        assertThat(submissions)
                .extracting(
                        submission -> submission.submissionId(),
                        submission -> submission.handle()
                )
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("1004", "other"),
                        org.assertj.core.groups.Tuple.tuple("1001", "tourist")
                );
    }

    @Test
    void preservesStoredUtcPlus8SubmittedLocalDateTime() {
        OjProblemSubmissionCriteria query = new OjProblemSubmissionCriteria(
                "1000:A",
                LocalDateTime.parse("2026-07-01T00:00:00"),
                LocalDateTime.parse("2026-07-02T00:00:00"),
                100,
                0
        );

        var submissions = repository.findProblemSubmissions(query);

        assertThat(submissions)
                .extracting(
                        submission -> submission.submissionId(),
                        submission -> submission.submittedAtUtcPlus8()
                )
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("1004", LocalDateTime.parse("2026-07-01T10:30:00")),
                        org.assertj.core.groups.Tuple.tuple("1001", LocalDateTime.parse("2026-07-01T10:00:00"))
                );
    }

    @Test
    void countsHandleAndProblemSubmissionsWithSameFilters() {
        OjHandleSubmissionCriteria handleQuery = new OjHandleSubmissionCriteria(
                "tourist",
                LocalDateTime.parse("2026-07-01T00:00:00"),
                LocalDateTime.parse("2026-07-04T00:00:00"),
                800,
                1200,
                100,
                0
        );
        OjProblemSubmissionCriteria problemQuery = new OjProblemSubmissionCriteria(
                "1000:A",
                LocalDateTime.parse("2026-07-01T00:00:00"),
                LocalDateTime.parse("2026-07-02T00:00:00"),
                100,
                0
        );

        assertThat(repository.countHandleSubmissions(handleQuery)).isEqualTo(2);
        assertThat(repository.countProblemSubmissions(problemQuery)).isEqualTo(2);
    }

    @Test
    void pagesHandleSubmissionsWithStableSubmittedTimeDescOrder() {
        OjHandleSubmissionCriteria query = new OjHandleSubmissionCriteria(
                "tourist",
                null,
                null,
                null,
                null,
                2,
                1
        );

        var submissions = repository.findHandleSubmissions(query);

        assertThat(submissions)
                .extracting(submission -> submission.submissionId())
                .containsExactly("1003", "1002");
    }

    @Test
    void pagesProblemSubmissionsWithStableSubmittedTimeDescOrder() {
        OjProblemSubmissionCriteria query = new OjProblemSubmissionCriteria(
                "1000:A",
                null,
                null,
                1,
                1
        );

        var submissions = repository.findProblemSubmissions(query);

        assertThat(submissions)
                .extracting(
                        submission -> submission.submissionId(),
                        submission -> submission.handle()
                )
                .containsExactly(org.assertj.core.groups.Tuple.tuple("1004", "other"));
    }

    private void insertSubmission(
            long submissionId,
            String handle,
            String problemKey,
            String submittedAtUtcPlus8,
            Integer rating,
            String verdict
    ) {
        insertSubmission(
                "dwd_codeforces__submission",
                submissionId,
                handle,
                problemKey,
                submittedAtUtcPlus8,
                rating,
                verdict
        );
    }

    private void insertSubmission(
            String tableName,
            long submissionId,
            String handle,
            String problemKey,
            String submittedAtUtcPlus8,
            Integer rating,
            String verdict
    ) {
        boolean accepted = "OK".equals(verdict);
        LocalDateTime submittedAt = LocalDateTime.parse(submittedAtUtcPlus8);
        String[] problemParts = problemKey.split(":");
        jdbcTemplate.update("""
                insert into %s (
                    ods_submission_id,
                    submission_id,
                    handle,
                    submitted_at_utc_plus8,
                    submitted_date_utc_plus8,
                    problem_key,
                    problem_index,
                    problem_name,
                    difficulty,
                    language,
                    verdict,
                    is_accepted,
                    time_consumed_millis,
                    source_url,
                    ods_batch_id,
                    ods_fetched_at,
                    ods_payload_hash
                )
                values (
                    :odsSubmissionId,
                    :submissionId,
                    :handle,
                    :submittedAt,
                    :submittedDateUtcPlus8,
                    :problemKey,
                    :problemIndex,
                    :problemName,
                    :difficulty,
                    :language,
                    :verdict,
                    :accepted,
                    :timeConsumedMillis,
                    :sourceUrl,
                    :odsBatchId,
                    :odsFetchedAt,
                    :odsPayloadHash
                )
                """.formatted(tableName), new MapSqlParameterSource()
                .addValue("odsSubmissionId", submissionId)
                .addValue("submissionId", String.valueOf(submissionId))
                .addValue("handle", handle)
                .addValue("submittedAt", Timestamp.valueOf(submittedAt))
                .addValue("submittedDateUtcPlus8", LocalDate.parse(submittedAtUtcPlus8.substring(0, 10)))
                .addValue("problemKey", problemKey)
                .addValue("problemIndex", problemParts[1])
                .addValue("problemName", "Problem " + problemParts[1])
                .addValue("difficulty", rating == null ? null : String.valueOf(rating))
                .addValue("language", "C++23")
                .addValue("verdict", verdict)
                .addValue("accepted", accepted)
                .addValue("timeConsumedMillis", 100)
                .addValue("sourceUrl", "https://codeforces.com/contest/" + problemParts[0] + "/submission/" + submissionId)
                .addValue("odsBatchId", "batch-test")
                .addValue("odsFetchedAt", Timestamp.from(Instant.parse("2026-07-04T00:00:00Z")))
                .addValue("odsPayloadHash", "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"));
    }
}
