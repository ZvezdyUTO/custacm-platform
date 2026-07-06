package com.custacm.platform.trainingdata.codeforces.infra.repo;

import com.custacm.platform.trainingdata.codeforces.domain.model.CodeforcesStudentDataPurgeResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcCodeforcesStudentDataPurgeRepositoryTest {
    private JdbcTemplate jdbcTemplate;
    private JdbcCodeforcesStudentDataPurgeRepository repository;

    @BeforeEach
    void setUp() throws Exception {
        DataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:cf_student_data_purge_" + UUID.randomUUID()
                        + ";MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        jdbcTemplate = new JdbcTemplate(dataSource);
        repository = new JdbcCodeforcesStudentDataPurgeRepository(
                new NamedParameterJdbcTemplate(dataSource),
                new DataSourceTransactionManager(dataSource)
        );
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V010__create_ods_codeforces_submission.sql"));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V011__create_codeforces_dwd_dwm_dws_tables.sql"));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V012__rename_codeforces_warehouse_time_columns_to_utc_plus8.sql"));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V013__reshape_codeforces_dws_daily_rating_summary.sql"));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V014__create_codeforces_handle_account.sql"));
        }
    }

    @Test
    void purgesOnlyRowsForCurrentIdentityHandle() {
        insertHandleAccount("112487张三", "tourist");
        insertHandleAccount("112488李四", "Benq");
        insertCodeforcesRows("tourist", 380000001);
        insertCodeforcesRows("Benq", 380000002);

        CodeforcesStudentDataPurgeResult result = repository.purgeByStudentIdentity("112487张三");

        assertThat(result.studentIdentity()).isEqualTo("112487张三");
        assertThat(result.handle()).isEqualTo("tourist");
        assertThat(result.handleAccountRows()).isEqualTo(1);
        assertThat(result.odsSubmissionRows()).isEqualTo(1);
        assertThat(result.dwdSubmissionRows()).isEqualTo(1);
        assertThat(result.dwmFirstAcceptedRows()).isEqualTo(1);
        assertThat(result.dwsAcceptedSummaryRows()).isEqualTo(1);
        assertThat(result.totalDeletedRows()).isEqualTo(5);
        assertThat(count("codeforces_handle_account", "student_identity = '112487张三'")).isZero();
        assertThat(count("codeforces_handle_account", "student_identity = '112488李四'")).isEqualTo(1);
        assertThat(count("ods_codeforces__submission", "author_handle = 'tourist'")).isZero();
        assertThat(count("dwd_codeforces__submission", "author_handle = 'tourist'")).isZero();
        assertThat(count("dwm_codeforces__handle_problem_first_accepted", "author_handle = 'tourist'")).isZero();
        assertThat(count("dws_codeforces__handle_daily_rating_accepted_summary", "author_handle = 'tourist'")).isZero();
        assertThat(count("ods_codeforces__submission", "author_handle = 'Benq'")).isEqualTo(1);
        assertThat(count("dwd_codeforces__submission", "author_handle = 'Benq'")).isEqualTo(1);
        assertThat(count("dwm_codeforces__handle_problem_first_accepted", "author_handle = 'Benq'")).isEqualTo(1);
        assertThat(count("dws_codeforces__handle_daily_rating_accepted_summary", "author_handle = 'Benq'")).isEqualTo(1);
    }

    @Test
    void returnsZeroCountsWhenIdentityHasNoHandleBinding() {
        insertHandleAccount("112488李四", "Benq");
        insertCodeforcesRows("Benq", 380000002);

        CodeforcesStudentDataPurgeResult result = repository.purgeByStudentIdentity("missing");

        assertThat(result.studentIdentity()).isEqualTo("missing");
        assertThat(result.handle()).isNull();
        assertThat(result.totalDeletedRows()).isZero();
        assertThat(count("codeforces_handle_account", "student_identity = '112488李四'")).isEqualTo(1);
        assertThat(count("ods_codeforces__submission", "author_handle = 'Benq'")).isEqualTo(1);
    }

    private void insertHandleAccount(String studentIdentity, String handle) {
        jdbcTemplate.update("""
                insert into codeforces_handle_account (student_identity, codeforces_handle)
                values (?, ?)
                """, studentIdentity, handle);
    }

    private void insertCodeforcesRows(String handle, long submissionId) {
        jdbcTemplate.update("""
                insert into ods_codeforces__submission (
                    codeforces_submission_id,
                    author_handle,
                    batch_id,
                    fetched_at,
                    raw_payload,
                    payload_hash
                ) values (?, ?, 'batch-test', timestamp '2026-07-06 00:00:00', '{}', ?)
                """, submissionId, handle, hash(submissionId));
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
                    is_accepted,
                    ods_batch_id,
                    ods_fetched_at,
                    ods_payload_hash
                ) values (?, ?, ?, timestamp '2026-07-06 08:00:00', date '2026-07-06',
                    '1000:A', 1000, 'A', 1, 'batch-test', timestamp '2026-07-06 00:00:00', ?)
                """, submissionId, submissionId, handle, hash(submissionId));
        jdbcTemplate.update("""
                insert into dwm_codeforces__handle_problem_first_accepted (
                    author_handle,
                    problem_key,
                    problem_contest_id,
                    problem_index,
                    first_accepted_submission_id,
                    first_accepted_at_utc_plus8,
                    first_accepted_date_utc_plus8
                ) values (?, '1000:A', 1000, 'A', ?, timestamp '2026-07-06 08:00:00', date '2026-07-06')
                """, handle, submissionId);
        jdbcTemplate.update("""
                insert into dws_codeforces__handle_daily_rating_accepted_summary (
                    author_handle,
                    accepted_date_utc_plus8,
                    rating_800_accepted_problem_count
                ) values (?, date '2026-07-06', 1)
                """, handle);
    }

    private int count(String tableName, String predicate) {
        return jdbcTemplate.queryForObject("select count(*) from " + tableName + " where " + predicate, Integer.class);
    }

    private static String hash(long submissionId) {
        return String.format("%064d", submissionId);
    }
}
