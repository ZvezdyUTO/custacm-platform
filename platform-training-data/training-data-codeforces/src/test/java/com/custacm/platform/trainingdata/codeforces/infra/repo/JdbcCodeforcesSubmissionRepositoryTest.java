package com.custacm.platform.trainingdata.codeforces.infra.repo;

import com.custacm.platform.trainingdata.codeforces.domain.criteria.CodeforcesProblemSubmissionCriteria;
import com.custacm.platform.trainingdata.codeforces.domain.criteria.CodeforcesHandleSubmissionCriteria;
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

class JdbcCodeforcesSubmissionRepositoryTest {
    private NamedParameterJdbcTemplate jdbcTemplate;
    private JdbcCodeforcesSubmissionRepository repository;

    @BeforeEach
    void setUp() throws Exception {
        DataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:dwd_cf_" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        repository = new JdbcCodeforcesSubmissionRepository(jdbcTemplate);
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V011__create_codeforces_dwd_dwm_dws_tables.sql"));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V012__rename_codeforces_warehouse_time_columns_to_utc_plus8.sql"));
        }

        insertSubmission(1001L, "tourist", "1000:A", "2026-07-01T10:00:00", 800, "OK");
        insertSubmission(1002L, "tourist", "1000:B", "2026-07-01T11:00:00", 1200, "WRONG_ANSWER");
        insertSubmission(1003L, "tourist", "1000:C", "2026-07-02T10:00:00", null, "OK");
        insertSubmission(1004L, "other", "1000:A", "2026-07-01T10:30:00", 800, "OK");
        insertSubmission(1005L, "tourist", "1000:A", "2026-07-03T10:00:00", 1600, "OK");
    }

    @Test
    void findsHandleSubmissionsBySubmittedTimeAndMinimumProblemRatingWithoutUnratedRows() {
        CodeforcesHandleSubmissionCriteria query = new CodeforcesHandleSubmissionCriteria(
                "tourist",
                LocalDateTime.parse("2026-07-01T10:30:00"),
                LocalDateTime.parse("2026-07-02T12:00:00"),
                1200,
                null
        );

        var submissions = repository.findHandleSubmissions(query);

        assertThat(submissions)
                .extracting(submission -> submission.codeforcesSubmissionId())
                .containsExactly(1002L);
    }

    @Test
    void findsHandleSubmissionsByMaximumProblemRatingWithoutUnratedRows() {
        CodeforcesHandleSubmissionCriteria query = new CodeforcesHandleSubmissionCriteria(
                "tourist",
                null,
                null,
                null,
                800
        );

        var submissions = repository.findHandleSubmissions(query);

        assertThat(submissions)
                .extracting(submission -> submission.codeforcesSubmissionId())
                .containsExactly(1001L);
    }

    @Test
    void findsProblemSubmissionsBySubmittedTimeAcrossHandles() {
        CodeforcesProblemSubmissionCriteria query = new CodeforcesProblemSubmissionCriteria(
                "1000:A",
                LocalDateTime.parse("2026-07-01T00:00:00"),
                LocalDateTime.parse("2026-07-02T00:00:00")
        );

        var submissions = repository.findProblemSubmissions(query);

        assertThat(submissions)
                .extracting(
                        submission -> submission.codeforcesSubmissionId(),
                        submission -> submission.authorHandle()
                )
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(1001L, "tourist"),
                        org.assertj.core.groups.Tuple.tuple(1004L, "other")
                );
    }

    @Test
    void preservesStoredUtcPlus8SubmittedLocalDateTime() {
        CodeforcesProblemSubmissionCriteria query = new CodeforcesProblemSubmissionCriteria(
                "1000:A",
                LocalDateTime.parse("2026-07-01T00:00:00"),
                LocalDateTime.parse("2026-07-02T00:00:00")
        );

        var submissions = repository.findProblemSubmissions(query);

        assertThat(submissions)
                .extracting(
                        submission -> submission.codeforcesSubmissionId(),
                        submission -> submission.submittedAtUtcPlus8()
                )
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(1001L, LocalDateTime.parse("2026-07-01T10:00:00")),
                        org.assertj.core.groups.Tuple.tuple(1004L, LocalDateTime.parse("2026-07-01T10:30:00"))
                );
    }

    private void insertSubmission(
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
                insert into dwd_codeforces__submission (
                    ods_submission_id,
                    codeforces_submission_id,
                    author_handle,
                    submitted_at_utc_plus8,
                    submitted_date_utc_plus8,
                    problem_key,
                    problem_contest_id,
                    problem_index,
                    problem_name,
                    problem_rating,
                    programming_language,
                    verdict,
                    is_accepted,
                    ods_batch_id,
                    ods_fetched_at,
                    ods_payload_hash
                )
                values (
                    :odsSubmissionId,
                    :codeforcesSubmissionId,
                    :authorHandle,
                    :submittedAt,
                    :submittedDateUtcPlus8,
                    :problemKey,
                    :problemContestId,
                    :problemIndex,
                    :problemName,
                    :problemRating,
                    :programmingLanguage,
                    :verdict,
                    :accepted,
                    :odsBatchId,
                    :odsFetchedAt,
                    :odsPayloadHash
                )
                """, new MapSqlParameterSource()
                .addValue("odsSubmissionId", submissionId)
                .addValue("codeforcesSubmissionId", submissionId)
                .addValue("authorHandle", handle)
                .addValue("submittedAt", Timestamp.valueOf(submittedAt))
                .addValue("submittedDateUtcPlus8", LocalDate.parse(submittedAtUtcPlus8.substring(0, 10)))
                .addValue("problemKey", problemKey)
                .addValue("problemContestId", Long.parseLong(problemParts[0]))
                .addValue("problemIndex", problemParts[1])
                .addValue("problemName", "Problem " + problemParts[1])
                .addValue("problemRating", rating)
                .addValue("programmingLanguage", "C++23")
                .addValue("verdict", verdict)
                .addValue("accepted", accepted)
                .addValue("odsBatchId", "batch-test")
                .addValue("odsFetchedAt", Timestamp.from(Instant.parse("2026-07-04T00:00:00Z")))
                .addValue("odsPayloadHash", "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"));
    }
}
