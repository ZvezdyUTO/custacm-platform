package com.custacm.platform.trainingdata.codeforces.infra;

import com.custacm.platform.common.sqltask.SqlTaskRunStatus;
import com.custacm.platform.common.sqltask.SqlTaskRunner;
import com.custacm.platform.trainingdata.common.app.warehouse.OjWarehouseRefreshService;
import com.custacm.platform.trainingdata.common.domain.oj.model.OjWarehouseRefreshInterval;
import com.custacm.platform.trainingdata.codeforces.domain.CodeforcesCollectBatch;
import com.custacm.platform.trainingdata.codeforces.domain.CodeforcesOdsSubmission;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
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
    private NamedParameterJdbcTemplate namedJdbcTemplate;
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
        namedJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        writer = new JdbcCodeforcesOdsSubmissionWriter(namedJdbcTemplate);
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V010__create_ods_codeforces_submission.sql"));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V011__create_codeforces_dwd_dwm_dws_tables.sql"));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V012__rename_codeforces_warehouse_time_columns_to_utc_plus8.sql"));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V013__reshape_codeforces_dws_daily_rating_summary.sql"));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V017__reshape_codeforces_warehouse_to_common_contract.sql"));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V022__attribute_codeforces_submissions_to_collected_handle.sql"));
        }
    }

    @Test
    void codeforcesWarehouseSqlTasksAreIdempotent() throws Exception {
        CodeforcesCollectBatch batch = new CodeforcesCollectBatch(
                "batch-warehouse-test",
                Instant.parse("2026-07-03T00:00:00Z")
        );
        String fixture = new ClassPathResource(FIXTURE).getContentAsString(StandardCharsets.UTF_8);
        var records = new JacksonSubmissionPayloadParser(new ObjectMapper()).parse(fixture, batch);
        assertThat(records.stream().map(record -> record.codeforcesSubmissionId()).distinct().count())
                .isEqualTo(1000);
        assertThat(records).hasSize(1000);
        writer.upsertBatch(batch, records);

        runWarehouseTasks(batch.batchId());
        runWarehouseTasks(batch.batchId());

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
                where submission_id = '380351477'
                """, String.class);
        Boolean accepted = jdbcTemplate.queryForObject("""
                select is_accepted
                from dwd_codeforces__submission
                where submission_id = '380351477'
                """, Boolean.class);

        assertThat(problemKey).isEqualTo("2239:D");
        assertThat(accepted).isTrue();

        Timestamp submittedAtUtcPlus8 = jdbcTemplate.queryForObject("""
                select submitted_at_utc_plus8
                from dwd_codeforces__submission
                where submission_id = '375842134'
                """, Timestamp.class);
        Date submittedDateUtcPlus8 = jdbcTemplate.queryForObject("""
                select submitted_date_utc_plus8
                from dwd_codeforces__submission
                where submission_id = '375842134'
                """, Date.class);

        assertThat(submittedAtUtcPlus8).isNotNull();
        assertThat(submittedDateUtcPlus8).isNotNull();
        assertThat(submittedAtUtcPlus8.toLocalDateTime())
                .isEqualTo(LocalDateTime.parse("2026-05-24T01:04:27"));
        assertThat(submittedDateUtcPlus8.toLocalDate())
                .isEqualTo(LocalDate.parse("2026-05-24"));
    }

    @Test
    void warehouseSqlTasksCountTeamSubmissionForCollectedHandleOnly() throws Exception {
        CodeforcesCollectBatch batch = new CodeforcesCollectBatch(
                "batch-team-members",
                Instant.parse("2026-01-01T00:00:00Z")
        );
        writer.upsertBatch(batch, List.of(teamSubmission(100L, "bob")));

        runWarehouseTasks(batch.batchId());

        assertThat(handlesForDwdSubmission("100")).containsExactly("bob");
        assertThat(handlesForFirstAcceptedProblem("1000:A")).containsExactly("bob");
        assertThat(rating800AcceptedCountForHandleOnDate("alice", "2024-01-05")).isZero();
        assertThat(rating800AcceptedCountForHandleOnDate("bob", "2024-01-05")).isEqualTo(1);
    }

    @Test
    void warehouseSqlTasksRefreshBatchDateIntervalWithoutLosingGlobalFirstAccepted() throws Exception {
        CodeforcesCollectBatch initialBatch = new CodeforcesCollectBatch(
                "batch-initial",
                Instant.parse("2026-01-01T00:00:00Z")
        );
        writer.upsertBatch(initialBatch, List.of(
                submission(1L, "alice", "A", "2024-01-01T10:00:00", "OK"),
                submission(2L, "alice", "B", "2024-01-05T10:00:00", "OK"),
                submission(3L, "alice", "C", "2024-01-10T10:00:00", "OK")
        ));
        runWarehouseTasks(initialBatch.batchId());

        insertStaleDwdSubmission(999L, "2024-01-05T08:00:00");

        CodeforcesCollectBatch targetBatch = new CodeforcesCollectBatch(
                "batch-target",
                Instant.parse("2026-01-02T00:00:00Z")
        );
        writer.upsertBatch(targetBatch, List.of(
                submission(4L, "alice", "A", "2024-01-05T12:00:00", "OK"),
                submission(5L, "alice", "D", "2024-01-05T13:00:00", "OK")
        ));

        runWarehouseTasks(targetBatch.batchId());

        assertThat(submissionIdsOnDate("2024-01-05"))
                .containsExactlyInAnyOrder("2", "4", "5");
        assertThat(firstAcceptedSubmissionId("1000:A")).isEqualTo("1");
        assertThat(firstAcceptedProblemKeysOnDate("2024-01-05"))
                .containsExactlyInAnyOrder("1000:B", "1000:D");
        assertThat(rating800AcceptedCountOnDate("2024-01-01")).isEqualTo(1);
        assertThat(rating800AcceptedCountOnDate("2024-01-05")).isEqualTo(2);
        assertThat(rating800AcceptedCountOnDate("2024-01-10")).isEqualTo(1);
    }

    @Test
    void warehouseRefreshServiceRefreshesCollectedRecentWindowWhenFirstAcceptedDateMoves() {
        CodeforcesCollectBatch initialBatch = new CodeforcesCollectBatch(
                "batch-first-accepted-initial",
                Instant.parse("2026-01-01T00:00:00Z")
        );
        writer.upsertBatch(initialBatch, List.of(
                submission(10L, "alice", "A", "2024-01-10T10:00:00", "OK")
        ));
        OjWarehouseRefreshService service = refreshService();

        assertThat(service.refresh(initialBatch.batchId(), null).status()).isEqualTo(SqlTaskRunStatus.SUCCESS);
        assertThat(firstAcceptedDate("1000:A")).isEqualTo(LocalDate.parse("2024-01-10"));
        assertThat(rating800AcceptedCountOnDate("2024-01-10")).isEqualTo(1);

        CodeforcesCollectBatch targetBatch = new CodeforcesCollectBatch(
                "batch-first-accepted-target",
                Instant.parse("2026-01-02T00:00:00Z")
        );
        writer.upsertBatch(targetBatch, List.of(
                submission(10L, "alice", "A", "2024-01-10T10:00:00", "OK"),
                submission(11L, "alice", "A", "2024-01-05T10:00:00", "OK")
        ));

        assertThat(service.refresh(targetBatch.batchId(), null).status()).isEqualTo(SqlTaskRunStatus.SUCCESS);

        assertThat(firstAcceptedDate("1000:A")).isEqualTo(LocalDate.parse("2024-01-05"));
        assertThat(rating800AcceptedCountOnDate("2024-01-05")).isEqualTo(1);
        assertThat(rating800AcceptedCountOnDate("2024-01-10")).isZero();
    }

    private void runWarehouseTasks(String batchId) throws Exception {
        Optional<OjWarehouseRefreshInterval> interval = refreshIntervalRepository().findBatchDateInterval(batchId);
        execute(DWD_SQL, batchId, interval);
        execute(FIRST_ACCEPTED_SQL, batchId, interval);
        execute(DAILY_SUMMARY_SQL, batchId, interval);
    }

    private void execute(
            String location,
            String batchId,
            Optional<OjWarehouseRefreshInterval> interval
    ) throws Exception {
        String sql = new ClassPathResource(location).getContentAsString(StandardCharsets.UTF_8);
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("batchId", batchId)
                .addValue(
                        "refreshFromDateUtcPlus8",
                        interval.map(OjWarehouseRefreshInterval::fromDateUtcPlus8)
                                .map(Date::valueOf)
                                .orElse(null)
                )
                .addValue(
                        "refreshToDateUtcPlus8",
                        interval.map(OjWarehouseRefreshInterval::toDateUtcPlus8)
                                .map(Date::valueOf)
                                .orElse(null)
                );
        for (String statement : sql.split(";")) {
            if (!statement.isBlank()) {
                namedJdbcTemplate.update(statement, parameters);
            }
        }
    }

    private OjWarehouseRefreshService refreshService() {
        return new OjWarehouseRefreshService(
                new SqlTaskRunner(
                        namedJdbcTemplate,
                        new DataSourceTransactionManager(dataSource),
                        new DefaultResourceLoader()
                ),
                refreshIntervalRepository(),
                "classpath:sql/tasks/codeforces-warehouse-refresh.yml",
                "batchId has no Codeforces submissions with creationTimeSeconds"
        );
    }

    private JdbcCodeforcesWarehouseRefreshIntervalRepository refreshIntervalRepository() {
        return new JdbcCodeforcesWarehouseRefreshIntervalRepository(namedJdbcTemplate);
    }

    private CodeforcesOdsSubmission submission(
            long submissionId,
            String handle,
            String problemIndex,
            String submittedAtUtcPlus8,
            String verdict
    ) {
        LocalDateTime submittedAt = LocalDateTime.parse(submittedAtUtcPlus8);
        return new CodeforcesOdsSubmission(
                submissionId,
                1000L,
                submittedAt.toEpochSecond(ZoneOffset.ofHours(8)),
                null,
                1000L,
                problemIndex,
                "Problem " + problemIndex,
                "PROGRAMMING",
                null,
                800,
                "[]",
                handle,
                "PRACTICE",
                "{\"members\":[{\"handle\":\"" + handle + "\"}]}",
                "C++23",
                verdict,
                "TESTS",
                1,
                46,
                1024L,
                "unused-batch-id",
                Instant.parse("2026-01-01T00:00:00Z"),
                "{\"id\":" + submissionId + "}",
                "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
        );
    }

    private CodeforcesOdsSubmission teamSubmission(long submissionId, String handle) {
        LocalDateTime submittedAt = LocalDateTime.parse("2024-01-05T10:00:00");
        return new CodeforcesOdsSubmission(
                submissionId,
                1000L,
                submittedAt.toEpochSecond(ZoneOffset.ofHours(8)),
                null,
                1000L,
                "A",
                "Problem A",
                "PROGRAMMING",
                null,
                800,
                "[]",
                handle,
                "PRACTICE",
                "{\"members\":[{\"handle\":\"alice\"},{\"handle\":\"bob\"}]}",
                "C++23",
                "OK",
                "TESTS",
                1,
                46,
                1024L,
                "unused-batch-id",
                Instant.parse("2026-01-01T00:00:00Z"),
                "{\"id\":" + submissionId + "}",
                "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
        );
    }

    private void insertStaleDwdSubmission(long submissionId, String submittedAtUtcPlus8) {
        LocalDateTime submittedAt = LocalDateTime.parse(submittedAtUtcPlus8);
        jdbcTemplate.update("""
                insert into dwd_codeforces__submission (
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
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                submissionId,
                String.valueOf(submissionId),
                "alice",
                Timestamp.valueOf(submittedAt),
                Date.valueOf(submittedAt.toLocalDate()),
                "1000:STALE",
                "STALE",
                "Stale Problem",
                "800",
                "C++23",
                "OK",
                1,
                46,
                "https://codeforces.com/contest/1000/submission/" + submissionId,
                "stale-batch",
                Timestamp.from(Instant.parse("2026-01-03T00:00:00Z")),
                "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
    }

    private List<String> submissionIdsOnDate(String date) {
        return jdbcTemplate.queryForList("""
                select submission_id
                from dwd_codeforces__submission
                where submitted_date_utc_plus8 = ?
                """, String.class, Date.valueOf(LocalDate.parse(date)));
    }

    private List<String> handlesForDwdSubmission(String submissionId) {
        return jdbcTemplate.queryForList("""
                select handle
                from dwd_codeforces__submission
                where submission_id = ?
                order by handle
                """, String.class, submissionId);
    }

    private List<String> handlesForFirstAcceptedProblem(String problemKey) {
        return jdbcTemplate.queryForList("""
                select handle
                from dwm_codeforces__handle_problem_first_accepted
                where problem_key = ?
                order by handle
                """, String.class, problemKey);
    }

    private String firstAcceptedSubmissionId(String problemKey) {
        return jdbcTemplate.queryForObject("""
                select first_accepted_submission_id
                from dwm_codeforces__handle_problem_first_accepted
                where handle = 'alice'
                  and problem_key = ?
                """, String.class, problemKey);
    }

    private LocalDate firstAcceptedDate(String problemKey) {
        Date date = jdbcTemplate.queryForObject("""
                select first_accepted_date_utc_plus8
                from dwm_codeforces__handle_problem_first_accepted
                where handle = 'alice'
                  and problem_key = ?
                """, Date.class, problemKey);
        return date == null ? null : date.toLocalDate();
    }

    private List<String> firstAcceptedProblemKeysOnDate(String date) {
        return jdbcTemplate.queryForList("""
                select problem_key
                from dwm_codeforces__handle_problem_first_accepted
                where handle = 'alice'
                  and first_accepted_date_utc_plus8 = ?
                """, String.class, Date.valueOf(LocalDate.parse(date)));
    }

    private int rating800AcceptedCountOnDate(String date) {
        return rating800AcceptedCountForHandleOnDate("alice", date);
    }

    private int rating800AcceptedCountForHandleOnDate(String handle, String date) {
        List<Integer> counts = jdbcTemplate.queryForList("""
                select accepted_problem_count
                from dws_codeforces__handle_daily_rating_accepted_summary
                where handle = ?
                  and accepted_date_utc_plus8 = ?
                  and difficulty = '800'
                """, Integer.class, handle, Date.valueOf(LocalDate.parse(date)));
        return counts.isEmpty() ? 0 : counts.get(0);
    }

    private int count(String tableName) {
        Integer count = jdbcTemplate.queryForObject("select count(*) from " + tableName, Integer.class);
        return count == null ? 0 : count;
    }

    private int expectedFirstAcceptedRows() {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*)
                from (
                    select handle, problem_key
                    from dwd_codeforces__submission
                    where is_accepted = 1
                      and problem_key is not null
                      and problem_index is not null
                      and submitted_at_utc_plus8 is not null
                      and submitted_date_utc_plus8 is not null
                    group by handle, problem_key
                ) grouped
                """, Integer.class);
        return count == null ? 0 : count;
    }

    private int expectedDailySummaryRows() {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*)
                from (
                    select
                        handle,
                        first_accepted_date_utc_plus8,
                        coalesce(difficulty, 'UNRATED')
                    from dwm_codeforces__handle_problem_first_accepted
                    group by handle, first_accepted_date_utc_plus8, coalesce(difficulty, 'UNRATED')
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
