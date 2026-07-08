package com.custacm.platform.trainingdata.common.infra.oj.repo.query;

import com.custacm.platform.trainingdata.common.domain.oj.criteria.OjAcceptedSummaryCriteria;
import com.custacm.platform.trainingdata.common.domain.oj.model.OjDailyRatingAcceptedSummary;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcOjAcceptedSummaryRepositoryTest {
    private NamedParameterJdbcTemplate jdbcTemplate;
    private JdbcOjAcceptedSummaryRepository repository;

    @BeforeEach
    void setUp() throws Exception {
        DataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:dws_cf_" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        repository = new JdbcOjAcceptedSummaryRepository(jdbcTemplate, OjDifficultyBucketPolicies.defaults());
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V011__create_codeforces_dwd_dwm_dws_tables.sql"));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V012__rename_codeforces_warehouse_time_columns_to_utc_plus8.sql"));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V013__reshape_codeforces_dws_daily_rating_summary.sql"));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V017__reshape_codeforces_warehouse_to_common_contract.sql"));
        }
        insertSummary("tourist", "2026-07-01", 2, 1, 0, 0);
        insertSummary("tourist", "2026-07-02", 0, 0, 0, 3);
        insertSummary("tourist", "2026-07-03", 0, 0, 1, 0);
        insertSummary("other", "2026-07-01", 5, 0, 0, 0);
    }

    @Test
    void findsAllDailyRatingSummariesForHandle() {
        List<OjDailyRatingAcceptedSummary> summaries =
                repository.findDailyRatingAcceptedSummaries(OjAcceptedSummaryCriteria.allForHandle("tourist"));

        assertThat(summaries).containsExactly(
                summary("tourist", "2026-07-01", Map.of("800", 2, "1200", 1)),
                summary("tourist", "2026-07-02", Map.of("UNRATED", 3)),
                summary("tourist", "2026-07-03", Map.of("1600", 1))
        );
    }

    @Test
    void appliesDateAndRatedProblemFilters() {
        OjAcceptedSummaryCriteria query = new OjAcceptedSummaryCriteria(
                "tourist",
                LocalDate.parse("2026-07-01"),
                LocalDate.parse("2026-07-02"),
                800,
                1600
        );

        List<OjDailyRatingAcceptedSummary> summaries =
                repository.findDailyRatingAcceptedSummaries(query);

        assertThat(summaries).containsExactly(summary("tourist", "2026-07-01", Map.of("800", 2, "1200", 1)));
    }

    @Test
    void doesNotIncludeUnratedRowsWithRatedProblemFilters() {
        OjAcceptedSummaryCriteria query = new OjAcceptedSummaryCriteria(
                "tourist",
                null,
                LocalDate.parse("2026-07-02"),
                1200,
                1200
        );

        List<OjDailyRatingAcceptedSummary> summaries =
                repository.findDailyRatingAcceptedSummaries(query);

        assertThat(summaries).containsExactly(
                summary("tourist", "2026-07-01", Map.of("1200", 1))
        );
    }

    @Test
    void appliesSingleSidedMaximumProblemRatingFilter() {
        OjAcceptedSummaryCriteria query = new OjAcceptedSummaryCriteria(
                "tourist",
                null,
                null,
                null,
                1200
        );

        List<OjDailyRatingAcceptedSummary> summaries =
                repository.findDailyRatingAcceptedSummaries(query);

        assertThat(summaries).containsExactly(
                summary("tourist", "2026-07-01", Map.of("800", 2, "1200", 1))
        );
    }

    @Test
    void returnsEmptyWhenProblemRatingBoundsAreOutsideTheFixedWideColumns() {
        OjAcceptedSummaryCriteria query = new OjAcceptedSummaryCriteria(
                "tourist",
                null,
                null,
                3600,
                null
        );

        List<OjDailyRatingAcceptedSummary> summaries =
                repository.findDailyRatingAcceptedSummaries(query);

        assertThat(summaries).isEmpty();
    }

    private void insertSummary(
            String handle,
            String acceptedDateUtcPlus8,
            int rating800Count,
            int rating1200Count,
            int rating1600Count,
            int unratedCount
    ) {
        insertSummary(
                "dws_codeforces__handle_daily_rating_accepted_summary",
                handle,
                acceptedDateUtcPlus8,
                rating800Count,
                rating1200Count,
                rating1600Count,
                unratedCount
        );
    }

    private void insertSummary(
            String tableName,
            String handle,
            String acceptedDateUtcPlus8,
            int rating800Count,
            int rating1200Count,
            int rating1600Count,
            int unratedCount
    ) {
        insertDifficultySummary(tableName, handle, acceptedDateUtcPlus8, "800", rating800Count);
        insertDifficultySummary(tableName, handle, acceptedDateUtcPlus8, "1200", rating1200Count);
        insertDifficultySummary(tableName, handle, acceptedDateUtcPlus8, "1600", rating1600Count);
        insertDifficultySummary(tableName, handle, acceptedDateUtcPlus8, "UNRATED", unratedCount);
    }

    private void insertDifficultySummary(
            String tableName,
            String handle,
            String acceptedDateUtcPlus8,
            String difficulty,
            int acceptedProblemCount
    ) {
        if (acceptedProblemCount == 0) {
            return;
        }
        jdbcTemplate.update("""
                insert into %s (
                    handle,
                    accepted_date_utc_plus8,
                    difficulty,
                    accepted_problem_count
                )
                values (
                    :handle,
                    :acceptedDateUtcPlus8,
                    :difficulty,
                    :acceptedProblemCount
                )
                """.formatted(tableName), new MapSqlParameterSource()
                .addValue("handle", handle)
                .addValue("acceptedDateUtcPlus8", LocalDate.parse(acceptedDateUtcPlus8))
                .addValue("difficulty", difficulty)
                .addValue("acceptedProblemCount", acceptedProblemCount));
    }

    private static OjDailyRatingAcceptedSummary summary(
            String handle,
            String acceptedDateUtcPlus8,
            Map<String, Integer> acceptedProblemCountsByRating
    ) {
        return new OjDailyRatingAcceptedSummary(
                handle,
                LocalDate.parse(acceptedDateUtcPlus8),
                acceptedProblemCountsByRating
        );
    }
}
