package com.custacm.platform.trainingdata.codeforces.infra;

import com.custacm.platform.trainingdata.codeforces.domain.CodeforcesCollectBatch;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CodeforcesWarehouseSqlTaskTest {
    private static final String FIXTURE = "fixtures/codeforces/submissions_multi_user_1000.json";
    private static final String DWD_SQL = "sql/dwd/upsert_dwd_codeforces__submission.sql";
    private static final String FIRST_ACCEPTED_SQL =
            "sql/dwm/upsert_dwm_codeforces__handle_problem_first_accepted.sql";
    private static final String DAILY_SUMMARY_SQL =
            "sql/dws/upsert_dws_codeforces__handle_daily_rating_accepted_summary.sql";

    private JdbcTemplate jdbcTemplate;
    private DataSource dataSource;
    private JdbcCodeforcesOdsSubmissionWriter writer;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:warehouse_cf_" + UUID.randomUUID()
                        + ";MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        jdbcTemplate = new JdbcTemplate(dataSource);
        writer = new JdbcCodeforcesOdsSubmissionWriter(new NamedParameterJdbcTemplate(dataSource));
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V010__create_ods_codeforces_submission.sql"));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V011__create_codeforces_dwd_dwm_dws_tables.sql"));
        }
    }

    @Test
    void codeforcesWarehouseSqlTasksAreIdempotent() throws Exception {
        CodeforcesCollectBatch batch = new CodeforcesCollectBatch(
                "batch-warehouse-test",
                Instant.parse("2026-07-03T00:00:00Z")
        );
        String fixture = new ClassPathResource(FIXTURE).getContentAsString(StandardCharsets.UTF_8);
        var records = new CodeforcesSubmissionParser(new ObjectMapper()).parse(fixture, batch);
        assertThat(records).hasSize(1000);
        writer.upsertBatch(batch, records);

        runWarehouseTasks();
        runWarehouseTasks();

        assertThat(count("ods_codeforces__submission")).isEqualTo(records.size());
        assertThat(count("dwd_codeforces__submission")).isEqualTo(records.size());
        assertThat(count("dwm_codeforces__handle_problem_first_accepted"))
                .isEqualTo(expectedFirstAcceptedRows());
        assertThat(count("dws_codeforces__handle_daily_rating_accepted_summary"))
                .isEqualTo(expectedDailySummaryRows());
        assertThat(sumAcceptedProblemCount()).isEqualTo(count("dwm_codeforces__handle_problem_first_accepted"));

        String problemKey = jdbcTemplate.queryForObject("""
                select problem_key
                from dwd_codeforces__submission
                where codeforces_submission_id = 380351477
                """, String.class);
        Boolean accepted = jdbcTemplate.queryForObject("""
                select is_accepted
                from dwd_codeforces__submission
                where codeforces_submission_id = 380351477
                """, Boolean.class);

        assertThat(problemKey).isEqualTo("2239:D");
        assertThat(accepted).isTrue();
    }

    private void runWarehouseTasks() throws Exception {
        execute(DWD_SQL);
        execute(FIRST_ACCEPTED_SQL);
        execute(DAILY_SUMMARY_SQL);
    }

    private void execute(String location) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource(location));
        }
    }

    private int count(String tableName) {
        Integer count = jdbcTemplate.queryForObject("select count(*) from " + tableName, Integer.class);
        return count == null ? 0 : count;
    }

    private int expectedFirstAcceptedRows() {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*)
                from (
                    select author_handle, problem_key
                    from dwd_codeforces__submission
                    where is_accepted = 1
                      and problem_key is not null
                      and problem_contest_id is not null
                      and problem_index is not null
                      and submitted_at is not null
                      and submitted_date_utc is not null
                    group by author_handle, problem_key
                ) grouped
                """, Integer.class);
        return count == null ? 0 : count;
    }

    private int expectedDailySummaryRows() {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*)
                from (
                    select
                        author_handle,
                        first_accepted_date_utc,
                        problem_rating_key
                    from (
                        select
                            author_handle,
                            first_accepted_date_utc,
                            case
                                when problem_rating is null then 'UNRATED'
                                else concat('', problem_rating)
                            end as problem_rating_key
                        from dwm_codeforces__handle_problem_first_accepted
                    ) keyed
                    group by author_handle, first_accepted_date_utc, problem_rating_key
                ) grouped
                """, Integer.class);
        return count == null ? 0 : count;
    }

    private int sumAcceptedProblemCount() {
        Integer count = jdbcTemplate.queryForObject("""
                select coalesce(sum(accepted_problem_count), 0)
                from dws_codeforces__handle_daily_rating_accepted_summary
                """, Integer.class);
        return count == null ? 0 : count;
    }
}
