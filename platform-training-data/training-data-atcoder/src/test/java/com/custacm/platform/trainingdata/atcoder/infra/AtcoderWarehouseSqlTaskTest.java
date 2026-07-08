package com.custacm.platform.trainingdata.atcoder.infra;

import com.custacm.platform.common.sqltask.SqlTaskRunStatus;
import com.custacm.platform.common.sqltask.SqlTaskRunner;
import com.custacm.platform.trainingdata.common.app.warehouse.OjWarehouseRefreshService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AtcoderWarehouseSqlTaskTest {
    private JdbcTemplate jdbcTemplate;
    private NamedParameterJdbcTemplate namedJdbcTemplate;
    private DataSource dataSource;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:warehouse_atcoder_" + UUID.randomUUID()
                        + ";MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        jdbcTemplate = new JdbcTemplate(dataSource);
        namedJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V019__create_atcoder_ods_tables.sql"));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V020__create_atcoder_warehouse_tables.sql"));
        }
    }

    @Test
    void atcoderWarehouseSqlTasksAreIdempotentAndPopulateCommonWarehouseTables() {
        insertProblem("abc100_a", "abc100", "A", "Problem A", "ABC100 A - Problem A");
        insertProblem("abc100_b", "abc100", "B", "Problem B", "ABC100 B - Problem B");
        insertSubmission("batch-warehouse-test", 101L, "tourist", "abc100_a", "abc100",
                "2026-07-02T08:00:00", "AC");
        insertSubmission("batch-warehouse-test", 102L, "tourist", "abc100_a", "abc100",
                "2026-07-02T09:00:00", "WA");
        insertSubmission("batch-warehouse-test", 103L, "tourist", "abc100_b", "abc100",
                "2026-07-02T10:00:00", "AC");
        insertSubmission("batch-warehouse-test", 104L, "other", "abc100_a", "abc100",
                "2026-07-02T11:00:00", "AC");

        OjWarehouseRefreshService service = refreshService();
        assertThat(service.refresh("batch-warehouse-test", null).status()).isEqualTo(SqlTaskRunStatus.SUCCESS);
        assertThat(service.refresh("batch-warehouse-test", null).status()).isEqualTo(SqlTaskRunStatus.SUCCESS);

        assertThat(count("ods_atcoder__submission")).isEqualTo(4);
        assertThat(count("dwd_atcoder__submission")).isEqualTo(4);
        assertThat(count("dwm_atcoder__handle_problem_first_accepted")).isEqualTo(3);
        assertThat(count("dws_atcoder__handle_daily_rating_accepted_summary")).isEqualTo(2);
        assertThat(sumAcceptedProblemCount()).isEqualTo(3);

        assertThat(problemKey("101")).isEqualTo("abc100_a");
        assertThat(problemName("101")).isEqualTo("ABC100 A - Problem A");
        assertThat(sourceUrl("101")).isEqualTo("https://atcoder.jp/contests/abc100/submissions/101");
        assertThat(firstAcceptedSubmissionId("tourist", "abc100_a")).isEqualTo("101");
        assertThat(unratedAcceptedCountOnDate("tourist", "2026-07-02")).isEqualTo(2);
        assertThat(unratedAcceptedCountOnDate("other", "2026-07-02")).isEqualTo(1);
    }

    @Test
    void refreshExpandsToExistingFirstAcceptedDateWhenFirstAcceptedMovesEarlier() {
        insertProblem("abc100_a", "abc100", "A", "Problem A", "ABC100 A - Problem A");
        insertSubmission("batch-initial", 201L, "tourist", "abc100_a", "abc100",
                "2026-07-10T08:00:00", "AC");
        OjWarehouseRefreshService service = refreshService();

        assertThat(service.refresh("batch-initial", null).status()).isEqualTo(SqlTaskRunStatus.SUCCESS);
        assertThat(firstAcceptedDate("tourist", "abc100_a")).isEqualTo(LocalDate.parse("2026-07-10"));
        assertThat(unratedAcceptedCountOnDate("tourist", "2026-07-10")).isEqualTo(1);

        insertSubmission("batch-target", 202L, "tourist", "abc100_a", "abc100",
                "2026-07-01T08:00:00", "AC");

        assertThat(service.refresh("batch-target", null).status()).isEqualTo(SqlTaskRunStatus.SUCCESS);

        assertThat(firstAcceptedSubmissionId("tourist", "abc100_a")).isEqualTo("202");
        assertThat(firstAcceptedDate("tourist", "abc100_a")).isEqualTo(LocalDate.parse("2026-07-01"));
        assertThat(unratedAcceptedCountOnDate("tourist", "2026-07-01")).isEqualTo(1);
        assertThat(unratedAcceptedCountOnDate("tourist", "2026-07-10")).isZero();
    }

    private OjWarehouseRefreshService refreshService() {
        return new OjWarehouseRefreshService(
                new SqlTaskRunner(
                        namedJdbcTemplate,
                        new DataSourceTransactionManager(dataSource),
                        new DefaultResourceLoader()
                ),
                new JdbcAtcoderWarehouseRefreshIntervalRepository(namedJdbcTemplate),
                "classpath:sql/tasks/atcoder-warehouse-refresh.yml",
                "batchId has no AtCoder submissions"
        );
    }

    private void insertProblem(
            String problemId,
            String contestId,
            String problemIndex,
            String problemName,
            String title
    ) {
        jdbcTemplate.update("""
                insert into ods_atcoder__problem (
                    problem_id,
                    contest_id,
                    problem_index,
                    problem_name,
                    title,
                    batch_id,
                    fetched_at,
                    raw_payload,
                    payload_hash
                ) values (?, ?, ?, ?, ?, 'batch-problems', timestamp '2026-07-08 00:00:00', '{}', ?)
                """, problemId, contestId, problemIndex, problemName, title, hash(problemId.hashCode()));
    }

    private void insertSubmission(
            String batchId,
            long submissionId,
            String handle,
            String problemId,
            String contestId,
            String submittedAtUtcPlus8,
            String result
    ) {
        LocalDateTime submittedAt = LocalDateTime.parse(submittedAtUtcPlus8);
        jdbcTemplate.update("""
                insert into ods_atcoder__submission (
                    atcoder_submission_id,
                    epoch_second,
                    problem_id,
                    contest_id,
                    user_id,
                    language,
                    point,
                    source_code_length,
                    result,
                    execution_time_millis,
                    batch_id,
                    fetched_at,
                    raw_payload,
                    payload_hash
                ) values (?, ?, ?, ?, ?, 'C++23', 100.0, 1024, ?, 46, ?,
                    timestamp '2026-07-08 00:00:00', '{}', ?)
                """,
                submissionId,
                submittedAt.toEpochSecond(ZoneOffset.ofHours(8)),
                problemId,
                contestId,
                handle,
                result,
                batchId,
                hash(submissionId));
    }

    private String problemKey(String submissionId) {
        return jdbcTemplate.queryForObject("""
                select problem_key
                from dwd_atcoder__submission
                where submission_id = ?
                """, String.class, submissionId);
    }

    private String problemName(String submissionId) {
        return jdbcTemplate.queryForObject("""
                select problem_name
                from dwd_atcoder__submission
                where submission_id = ?
                """, String.class, submissionId);
    }

    private String sourceUrl(String submissionId) {
        return jdbcTemplate.queryForObject("""
                select source_url
                from dwd_atcoder__submission
                where submission_id = ?
                """, String.class, submissionId);
    }

    private String firstAcceptedSubmissionId(String handle, String problemKey) {
        return jdbcTemplate.queryForObject("""
                select first_accepted_submission_id
                from dwm_atcoder__handle_problem_first_accepted
                where handle = ?
                  and problem_key = ?
                """, String.class, handle, problemKey);
    }

    private LocalDate firstAcceptedDate(String handle, String problemKey) {
        Date date = jdbcTemplate.queryForObject("""
                select first_accepted_date_utc_plus8
                from dwm_atcoder__handle_problem_first_accepted
                where handle = ?
                  and problem_key = ?
                """, Date.class, handle, problemKey);
        return date == null ? null : date.toLocalDate();
    }

    private int unratedAcceptedCountOnDate(String handle, String date) {
        List<Integer> counts = jdbcTemplate.queryForList("""
                select accepted_problem_count
                from dws_atcoder__handle_daily_rating_accepted_summary
                where handle = ?
                  and accepted_date_utc_plus8 = ?
                  and difficulty = 'UNRATED'
                """, Integer.class, handle, Date.valueOf(LocalDate.parse(date)));
        return counts.isEmpty() ? 0 : counts.get(0);
    }

    private int count(String tableName) {
        Integer count = jdbcTemplate.queryForObject("select count(*) from " + tableName, Integer.class);
        return count == null ? 0 : count;
    }

    private int sumAcceptedProblemCount() {
        Integer count = jdbcTemplate.queryForObject("""
                select coalesce(sum(accepted_problem_count), 0)
                from dws_atcoder__handle_daily_rating_accepted_summary
                """, Integer.class);
        return count == null ? 0 : count;
    }

    private static String hash(long seed) {
        return String.format("%064d", Math.abs(seed));
    }
}
