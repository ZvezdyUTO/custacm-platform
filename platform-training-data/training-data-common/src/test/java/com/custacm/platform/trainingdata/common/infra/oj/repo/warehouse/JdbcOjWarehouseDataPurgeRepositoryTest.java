package com.custacm.platform.trainingdata.common.infra.oj.repo.warehouse;

import com.custacm.platform.trainingdata.common.domain.oj.repo.OjWarehouseDataPurgeRepository;
import com.custacm.platform.trainingdata.common.domain.oj.value.OjNames;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcOjWarehouseDataPurgeRepositoryTest {
    private JdbcTemplate jdbcTemplate;
    private JdbcOjWarehouseDataPurgeRepository repository;

    @BeforeEach
    void setUp() throws Exception {
        DataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:cf_warehouse_data_purge_" + UUID.randomUUID()
                        + ";MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        jdbcTemplate = new JdbcTemplate(dataSource);
        repository = new JdbcOjWarehouseDataPurgeRepository(
                new NamedParameterJdbcTemplate(dataSource),
                new DataSourceTransactionManager(dataSource)
        );
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V010__create_ods_codeforces_submission.sql"));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V011__create_codeforces_dwd_dwm_dws_tables.sql"));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V012__rename_codeforces_warehouse_time_columns_to_utc_plus8.sql"));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V013__reshape_codeforces_dws_daily_rating_summary.sql"));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V017__reshape_codeforces_warehouse_to_common_contract.sql"));
        }
    }

    @Test
    void purgesOnlyWarehouseRowsForHandle() {
        insertWarehouseRows("tourist", 380000001);
        insertWarehouseRows("Benq", 380000002);

        OjWarehouseDataPurgeRepository.OjWarehouseDataPurgeCounts deletedRows =
                repository.purgeAllByHandle(OjNames.CODEFORCES, "tourist");

        assertThat(deletedRows.dwdSubmissionRows()).isEqualTo(1);
        assertThat(deletedRows.dwmFirstAcceptedRows()).isEqualTo(1);
        assertThat(deletedRows.dwsAcceptedSummaryRows()).isEqualTo(1);
        assertThat(count("dwd_codeforces__submission", "handle = 'tourist'")).isZero();
        assertThat(count("dwm_codeforces__handle_problem_first_accepted", "handle = 'tourist'")).isZero();
        assertThat(count("dws_codeforces__handle_daily_rating_accepted_summary", "handle = 'tourist'")).isZero();
        assertThat(count("dwd_codeforces__submission", "handle = 'Benq'")).isEqualTo(1);
        assertThat(count("dwm_codeforces__handle_problem_first_accepted", "handle = 'Benq'")).isEqualTo(1);
        assertThat(count("dws_codeforces__handle_daily_rating_accepted_summary", "handle = 'Benq'")).isEqualTo(1);
    }

    private void insertWarehouseRows(String handle, long submissionId) {
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
                ) values (?, ?, ?, timestamp '2026-07-06 08:00:00', date '2026-07-06',
                    '1000:A', 'A', 'Problem A', '800', 'C++23', 'OK', 1, 46,
                    ?, 'batch-test', timestamp '2026-07-06 00:00:00', ?)
                """, submissionId, String.valueOf(submissionId), handle,
                "https://codeforces.com/contest/1000/submission/" + submissionId,
                hash(submissionId));
        jdbcTemplate.update("""
                insert into dwm_codeforces__handle_problem_first_accepted (
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
                ) values (?, '1000:A', 'A', 'Problem A', '800', ?, timestamp '2026-07-06 08:00:00',
                    date '2026-07-06', 'C++23', ?)
                """, handle, String.valueOf(submissionId),
                "https://codeforces.com/contest/1000/submission/" + submissionId);
        jdbcTemplate.update("""
                insert into dws_codeforces__handle_daily_rating_accepted_summary (
                    handle,
                    accepted_date_utc_plus8,
                    difficulty,
                    accepted_problem_count
                ) values (?, date '2026-07-06', '800', 1)
                """, handle);
    }

    private int count(String tableName, String predicate) {
        return jdbcTemplate.queryForObject("select count(*) from " + tableName + " where " + predicate, Integer.class);
    }

    private static String hash(long submissionId) {
        return String.format("%064d", submissionId);
    }
}
