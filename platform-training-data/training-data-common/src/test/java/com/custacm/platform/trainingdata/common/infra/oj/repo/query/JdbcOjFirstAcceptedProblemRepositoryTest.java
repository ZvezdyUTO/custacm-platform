package com.custacm.platform.trainingdata.common.infra.oj.repo.query;

import com.custacm.platform.trainingdata.common.domain.oj.criteria.OjHandleFirstAcceptedProblemCriteria;
import com.custacm.platform.trainingdata.common.domain.oj.criteria.OjProblemFirstAcceptedHandleCriteria;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcOjFirstAcceptedProblemRepositoryTest {
    private NamedParameterJdbcTemplate jdbcTemplate;
    private JdbcOjFirstAcceptedProblemRepository repository;

    @BeforeEach
    void setUp() throws Exception {
        DataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:dwm_cf_" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        repository = new JdbcOjFirstAcceptedProblemRepository(jdbcTemplate, OjDifficultyBucketPolicies.defaults());
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V011__create_codeforces_dwd_dwm_dws_tables.sql"));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V012__rename_codeforces_warehouse_time_columns_to_utc_plus8.sql"));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V017__reshape_codeforces_warehouse_to_common_contract.sql"));
        }

        insertFirstAccepted("tourist", "1000:A", "2026-07-01T10:00:00", 800, 1001L);
        insertFirstAccepted("tourist", "1000:B", "2026-07-01T11:00:00", 1200, 1002L);
        insertFirstAccepted("tourist", "1000:C", "2026-07-02T10:00:00", null, 1003L);
        insertFirstAccepted("other", "1000:A", "2026-07-01T12:00:00", 800, 1004L);
        insertFirstAccepted("later", "1000:A", "2026-07-03T10:00:00", 800, 1005L);
    }

    @Test
    void findsHandleFirstAcceptedProblemsByTimeAndMinimumProblemRatingWithoutUnratedRows() {
        OjHandleFirstAcceptedProblemCriteria query = new OjHandleFirstAcceptedProblemCriteria(
                "tourist",
                LocalDateTime.parse("2026-07-01T10:30:00"),
                LocalDateTime.parse("2026-07-02T12:00:00"),
                1200,
                null
        );

        var firstAcceptedProblems = repository.findHandleFirstAcceptedProblems(query);

        assertThat(firstAcceptedProblems)
                .extracting(problem -> problem.problemKey())
                .containsExactly("1000:B");
    }

    @Test
    void findsHandleFirstAcceptedProblemsByMaximumProblemRatingWithoutUnratedRows() {
        OjHandleFirstAcceptedProblemCriteria query = new OjHandleFirstAcceptedProblemCriteria(
                "tourist",
                null,
                null,
                null,
                800
        );

        var firstAcceptedProblems = repository.findHandleFirstAcceptedProblems(query);

        assertThat(firstAcceptedProblems)
                .extracting(problem -> problem.problemKey())
                .containsExactly("1000:A");
    }

    @Test
    void findsProblemFirstAcceptedHandlesByTimeAcrossHandles() {
        OjProblemFirstAcceptedHandleCriteria query = new OjProblemFirstAcceptedHandleCriteria(
                "1000:A",
                LocalDateTime.parse("2026-07-01T00:00:00"),
                LocalDateTime.parse("2026-07-02T00:00:00")
        );

        var firstAcceptedProblems = repository.findProblemFirstAcceptedHandles(query);

        assertThat(firstAcceptedProblems)
                .extracting(
                        problem -> problem.handle(),
                        problem -> problem.firstAcceptedSubmissionId()
                )
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("other", "1004"),
                        org.assertj.core.groups.Tuple.tuple("tourist", "1001")
                );
    }

    @Test
    void preservesStoredUtcPlus8FirstAcceptedLocalDateTime() {
        OjProblemFirstAcceptedHandleCriteria query = new OjProblemFirstAcceptedHandleCriteria(
                "1000:A",
                LocalDateTime.parse("2026-07-01T00:00:00"),
                LocalDateTime.parse("2026-07-02T00:00:00")
        );

        var firstAcceptedProblems = repository.findProblemFirstAcceptedHandles(query);

        assertThat(firstAcceptedProblems)
                .extracting(
                        problem -> problem.firstAcceptedSubmissionId(),
                        problem -> problem.firstAcceptedAtUtcPlus8()
                )
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("1004", LocalDateTime.parse("2026-07-01T12:00:00")),
                        org.assertj.core.groups.Tuple.tuple("1001", LocalDateTime.parse("2026-07-01T10:00:00"))
                );
    }

    @Test
    void countsAndPagesHandleFirstAcceptedProblems() {
        OjHandleFirstAcceptedProblemCriteria query = new OjHandleFirstAcceptedProblemCriteria(
                "CODEFORCES",
                "tourist",
                null,
                null,
                null,
                null,
                2,
                1
        );

        assertThat(repository.countHandleFirstAcceptedProblems(query)).isEqualTo(3);
        assertThat(repository.findHandleFirstAcceptedProblems(query))
                .extracting(problem -> problem.problemKey())
                .containsExactly("1000:B", "1000:A");
    }

    @Test
    void countsAndPagesProblemFirstAcceptedHandles() {
        OjProblemFirstAcceptedHandleCriteria query = new OjProblemFirstAcceptedHandleCriteria(
                "CODEFORCES",
                "1000:A",
                null,
                null,
                2,
                1
        );

        assertThat(repository.countProblemFirstAcceptedHandles(query)).isEqualTo(3);
        assertThat(repository.findProblemFirstAcceptedHandles(query))
                .extracting(problem -> problem.handle())
                .containsExactly("other", "tourist");
    }

    private void insertFirstAccepted(
            String handle,
            String problemKey,
            String firstAcceptedAtUtcPlus8,
            Integer rating,
            long submissionId
    ) {
        insertFirstAccepted(
                "dwm_codeforces__handle_problem_first_accepted",
                handle,
                problemKey,
                firstAcceptedAtUtcPlus8,
                rating,
                submissionId
        );
    }

    private void insertFirstAccepted(
            String tableName,
            String handle,
            String problemKey,
            String firstAcceptedAtUtcPlus8,
            Integer rating,
            long submissionId
    ) {
        LocalDateTime firstAcceptedAt = LocalDateTime.parse(firstAcceptedAtUtcPlus8);
        String[] problemParts = problemKey.split(":");
        jdbcTemplate.update("""
                insert into %s (
                    handle,
                    problem_key,
                    problem_index,
                    problem_name,
                    difficulty,
                    first_accepted_submission_id,
                    first_accepted_at_utc_plus8,
                    first_accepted_date_utc_plus8,
                    first_accepted_language,
                    first_accepted_source_url
                )
                values (
                    :handle,
                    :problemKey,
                    :problemIndex,
                    :problemName,
                    :difficulty,
                    :firstAcceptedSubmissionId,
                    :firstAcceptedAt,
                    :firstAcceptedDateUtcPlus8,
                    :firstAcceptedLanguage,
                    :firstAcceptedSourceUrl
                )
                """.formatted(tableName), new MapSqlParameterSource()
                .addValue("handle", handle)
                .addValue("problemKey", problemKey)
                .addValue("problemIndex", problemParts[1])
                .addValue("problemName", "Problem " + problemParts[1])
                .addValue("difficulty", rating == null ? null : String.valueOf(rating))
                .addValue("firstAcceptedSubmissionId", String.valueOf(submissionId))
                .addValue("firstAcceptedAt", Timestamp.valueOf(firstAcceptedAt))
                .addValue("firstAcceptedDateUtcPlus8", LocalDate.parse(firstAcceptedAtUtcPlus8.substring(0, 10)))
                .addValue("firstAcceptedLanguage", "C++23")
                .addValue("firstAcceptedSourceUrl", "https://codeforces.com/contest/" + problemParts[0] + "/submission/" + submissionId));
    }
}
