package com.custacm.platform.trainingdata.codeforces.infra.repo;

import com.custacm.platform.trainingdata.codeforces.domain.criteria.CodeforcesAcceptedSummaryCriteria;
import com.custacm.platform.trainingdata.codeforces.domain.model.CodeforcesDailyRatingAcceptedSummary;
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

class JdbcCodeforcesAcceptedSummaryRepositoryTest {
    private NamedParameterJdbcTemplate jdbcTemplate;
    private JdbcCodeforcesAcceptedSummaryRepository repository;

    @BeforeEach
    void setUp() throws Exception {
        DataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:dws_cf_" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        repository = new JdbcCodeforcesAcceptedSummaryRepository(jdbcTemplate);
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V011__create_codeforces_dwd_dwm_dws_tables.sql"));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V012__rename_codeforces_warehouse_time_columns_to_utc_plus8.sql"));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V013__reshape_codeforces_dws_daily_rating_summary.sql"));
        }
        insertSummary("tourist", "2026-07-01", 2, 1, 0, 0);
        insertSummary("tourist", "2026-07-02", 0, 0, 0, 3);
        insertSummary("tourist", "2026-07-03", 0, 0, 1, 0);
        insertSummary("other", "2026-07-01", 5, 0, 0, 0);
    }

    @Test
    void findsAllDailyRatingSummariesForHandle() {
        List<CodeforcesDailyRatingAcceptedSummary> summaries =
                repository.findDailyRatingAcceptedSummaries(CodeforcesAcceptedSummaryCriteria.allForHandle("tourist"));

        assertThat(summaries).containsExactly(
                summary("tourist", "2026-07-01", Map.of("800", 2, "1200", 1)),
                summary("tourist", "2026-07-02", Map.of("UNRATED", 3)),
                summary("tourist", "2026-07-03", Map.of("1600", 1))
        );
    }

    @Test
    void appliesDateAndRatedProblemFilters() {
        CodeforcesAcceptedSummaryCriteria query = new CodeforcesAcceptedSummaryCriteria(
                "tourist",
                LocalDate.parse("2026-07-01"),
                LocalDate.parse("2026-07-02"),
                800,
                1600
        );

        List<CodeforcesDailyRatingAcceptedSummary> summaries =
                repository.findDailyRatingAcceptedSummaries(query);

        assertThat(summaries).containsExactly(summary("tourist", "2026-07-01", Map.of("800", 2, "1200", 1)));
    }

    @Test
    void doesNotIncludeUnratedRowsWithRatedProblemFilters() {
        CodeforcesAcceptedSummaryCriteria query = new CodeforcesAcceptedSummaryCriteria(
                "tourist",
                null,
                LocalDate.parse("2026-07-02"),
                1200,
                1200
        );

        List<CodeforcesDailyRatingAcceptedSummary> summaries =
                repository.findDailyRatingAcceptedSummaries(query);

        assertThat(summaries).containsExactly(
                summary("tourist", "2026-07-01", Map.of("800", 2, "1200", 1))
        );
    }

    @Test
    void appliesSingleSidedMaximumProblemRatingFilter() {
        CodeforcesAcceptedSummaryCriteria query = new CodeforcesAcceptedSummaryCriteria(
                "tourist",
                null,
                null,
                null,
                1200
        );

        List<CodeforcesDailyRatingAcceptedSummary> summaries =
                repository.findDailyRatingAcceptedSummaries(query);

        assertThat(summaries).containsExactly(
                summary("tourist", "2026-07-01", Map.of("800", 2, "1200", 1))
        );
    }

    @Test
    void returnsEmptyWhenProblemRatingBoundsAreOutsideTheFixedWideColumns() {
        CodeforcesAcceptedSummaryCriteria query = new CodeforcesAcceptedSummaryCriteria(
                "tourist",
                null,
                null,
                3600,
                null
        );

        List<CodeforcesDailyRatingAcceptedSummary> summaries =
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
        jdbcTemplate.update("""
                insert into dws_codeforces__handle_daily_rating_accepted_summary (
                    author_handle,
                    accepted_date_utc_plus8,
                    rating_800_accepted_problem_count,
                    rating_1200_accepted_problem_count,
                    rating_1600_accepted_problem_count,
                    unrated_accepted_problem_count
                )
                values (
                    :authorHandle,
                    :acceptedDateUtcPlus8,
                    :rating800Count,
                    :rating1200Count,
                    :rating1600Count,
                    :unratedCount
                )
                """, new MapSqlParameterSource()
                .addValue("authorHandle", handle)
                .addValue("acceptedDateUtcPlus8", LocalDate.parse(acceptedDateUtcPlus8))
                .addValue("rating800Count", rating800Count)
                .addValue("rating1200Count", rating1200Count)
                .addValue("rating1600Count", rating1600Count)
                .addValue("unratedCount", unratedCount));
    }

    private static CodeforcesDailyRatingAcceptedSummary summary(
            String handle,
            String acceptedDateUtcPlus8,
            Map<String, Integer> acceptedProblemCountsByRating
    ) {
        return new CodeforcesDailyRatingAcceptedSummary(
                handle,
                LocalDate.parse(acceptedDateUtcPlus8),
                acceptedProblemCountsByRating
        );
    }
}
