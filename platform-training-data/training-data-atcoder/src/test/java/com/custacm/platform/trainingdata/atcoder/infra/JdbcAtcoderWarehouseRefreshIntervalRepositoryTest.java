package com.custacm.platform.trainingdata.atcoder.infra;

import com.custacm.platform.trainingdata.common.domain.oj.model.OjWarehouseRefreshInterval;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcAtcoderWarehouseRefreshIntervalRepositoryTest {
    private JdbcTemplate jdbcTemplate;
    private JdbcAtcoderWarehouseRefreshIntervalRepository repository;

    @BeforeEach
    void setUp() throws Exception {
        DataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:atcoder_interval_" + UUID.randomUUID()
                        + ";MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        jdbcTemplate = new JdbcTemplate(dataSource);
        repository = new JdbcAtcoderWarehouseRefreshIntervalRepository(new NamedParameterJdbcTemplate(dataSource));
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V019__create_atcoder_ods_tables.sql"));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V020__create_atcoder_warehouse_tables.sql"));
        }
    }

    @Test
    void returnsBaseBatchSubmittedDateInterval() {
        insertOds("batch-1", 1L, "tourist", "abc100_a", "2026-07-01T23:30:00Z", "AC");
        insertOds("batch-1", 2L, "tourist", "abc100_b", "2026-07-02T00:30:00Z", "WA");

        Optional<OjWarehouseRefreshInterval> interval = repository.findBatchDateInterval("batch-1");

        assertThat(interval).contains(new OjWarehouseRefreshInterval(
                LocalDate.parse("2026-07-02"),
                LocalDate.parse("2026-07-02")
        ));
    }

    @Test
    void includesExistingFirstAcceptedDateForTouchedAcceptedProblems() {
        insertExistingDwm("tourist", "abc100_a", "2026-07-10");
        insertOds("batch-1", 1L, "tourist", "abc100_a", "2026-07-01T00:00:00Z", "AC");

        Optional<OjWarehouseRefreshInterval> interval = repository.findBatchDateInterval("batch-1");

        assertThat(interval).contains(new OjWarehouseRefreshInterval(
                LocalDate.parse("2026-07-01"),
                LocalDate.parse("2026-07-10")
        ));
    }

    @Test
    void ignoresExistingFirstAcceptedDateForUnacceptedTouchedProblems() {
        insertExistingDwm("tourist", "abc100_a", "2026-07-10");
        insertOds("batch-1", 1L, "tourist", "abc100_a", "2026-07-01T00:00:00Z", "WA");

        Optional<OjWarehouseRefreshInterval> interval = repository.findBatchDateInterval("batch-1");

        assertThat(interval).contains(new OjWarehouseRefreshInterval(
                LocalDate.parse("2026-07-01"),
                LocalDate.parse("2026-07-01")
        ));
    }

    @Test
    void returnsEmptyForMissingBatch() {
        assertThat(repository.findBatchDateInterval("missing")).isEmpty();
    }

    @Test
    void returnsLatestValidBatchByFetchedAtThenId() {
        insertOds("batch-old", 10L, "tourist", "abc100_a", "2026-07-01T00:00:00Z", "AC",
                "2026-07-08 00:00:00");
        insertOds("batch-tie-first", 11L, "tourist", "abc100_b", "2026-07-02T00:00:00Z", "AC",
                "2026-07-09 00:00:00");
        insertOds("batch-tie-last", 12L, "tourist", "abc100_c", "2026-07-03T00:00:00Z", "AC",
                "2026-07-09 00:00:00");

        assertThat(repository.findLatestBatchId()).contains("batch-tie-last");
    }

    @Test
    void ignoresBlankLatestBatchAndReturnsEmptyWithoutValidRows() {
        insertOds(" ", 20L, "tourist", "abc100_a", "2026-07-01T00:00:00Z", "AC",
                "2026-07-10 00:00:00");

        assertThat(repository.findLatestBatchId()).isEmpty();
    }

    private void insertOds(
            String batchId,
            long submissionId,
            String handle,
            String problemId,
            String submittedAtUtc,
            String result
    ) {
        insertOds(batchId, submissionId, handle, problemId, submittedAtUtc, result,
                "2026-07-08 00:00:00");
    }

    private void insertOds(
            String batchId,
            long submissionId,
            String handle,
            String problemId,
            String submittedAtUtc,
            String result,
            String fetchedAt
    ) {
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
                ) values (?, ?, ?, 'abc100', ?, 'C++23', 100.0, 1024, ?, 46, ?, ?, '{}', ?)
                """,
                submissionId,
                Instant.parse(submittedAtUtc).getEpochSecond(),
                problemId,
                handle,
                result,
                batchId,
                Timestamp.valueOf(fetchedAt),
                hash(submissionId));
    }

    private void insertExistingDwm(String handle, String problemKey, String firstAcceptedDate) {
        jdbcTemplate.update("""
                insert into dwm_atcoder__handle_problem_first_accepted (
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
                ) values (?, ?, 'A', 'Problem A', null, '999',
                    ?, ?, 'C++23', 'https://atcoder.jp/contests/abc100/submissions/999')
                """,
                handle,
                problemKey,
                Timestamp.valueOf(firstAcceptedDate + " 08:00:00"),
                Date.valueOf(firstAcceptedDate));
    }

    private static String hash(long submissionId) {
        return String.format("%064d", submissionId);
    }
}
