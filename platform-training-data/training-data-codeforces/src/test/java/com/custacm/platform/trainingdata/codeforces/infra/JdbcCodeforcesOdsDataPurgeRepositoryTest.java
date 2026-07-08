package com.custacm.platform.trainingdata.codeforces.infra;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcCodeforcesOdsDataPurgeRepositoryTest {
    private JdbcTemplate jdbcTemplate;
    private JdbcCodeforcesOdsDataPurgeRepository repository;

    @BeforeEach
    void setUp() throws Exception {
        DataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:cf_ods_data_purge_" + UUID.randomUUID()
                        + ";MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        jdbcTemplate = new JdbcTemplate(dataSource);
        repository = new JdbcCodeforcesOdsDataPurgeRepository(new NamedParameterJdbcTemplate(dataSource));
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V010__create_ods_codeforces_submission.sql"));
        }
    }

    @Test
    void purgesOnlyOdsRowsForHandle() {
        insertOdsRow("tourist", 380000001);
        insertOdsRow("Benq", 380000002);

        int deletedRows = repository.purgeAllByHandle("tourist");

        assertThat(deletedRows).isEqualTo(1);
        assertThat(count("ods_codeforces__submission", "author_handle = 'tourist'")).isZero();
        assertThat(count("ods_codeforces__submission", "author_handle = 'Benq'")).isEqualTo(1);
    }

    private void insertOdsRow(String handle, long submissionId) {
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
    }

    private int count(String tableName, String predicate) {
        return jdbcTemplate.queryForObject("select count(*) from " + tableName + " where " + predicate, Integer.class);
    }

    private static String hash(long submissionId) {
        return String.format("%064d", submissionId);
    }
}
