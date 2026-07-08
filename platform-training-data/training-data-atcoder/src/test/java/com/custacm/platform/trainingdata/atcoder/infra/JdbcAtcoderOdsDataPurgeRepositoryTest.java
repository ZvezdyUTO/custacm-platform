package com.custacm.platform.trainingdata.atcoder.infra;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcAtcoderOdsDataPurgeRepositoryTest {
    private JdbcTemplate jdbcTemplate;
    private JdbcAtcoderOdsDataPurgeRepository repository;

    @BeforeEach
    void setUp() {
        DataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:atcoder_ods_data_purge_" + UUID.randomUUID()
                        + ";MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        jdbcTemplate = new JdbcTemplate(dataSource);
        repository = new JdbcAtcoderOdsDataPurgeRepository(new NamedParameterJdbcTemplate(dataSource));
        jdbcTemplate.execute("""
                create table ods_atcoder__submission (
                    id bigint primary key auto_increment,
                    atcoder_submission_id bigint not null,
                    user_id varchar(128) not null,
                    batch_id varchar(128) not null,
                    fetched_at datetime(6) not null,
                    raw_payload longtext not null,
                    payload_hash char(64) not null
                )
                """);
    }

    @Test
    void purgesOnlyAtcoderOdsRowsForHandle() {
        insertOdsRow("tourist_atcoder", 390000001);
        insertOdsRow("benq_atcoder", 390000002);

        int deletedRows = repository.purgeAllByHandle("tourist_atcoder");

        assertThat(deletedRows).isEqualTo(1);
        assertThat(count("user_id = 'tourist_atcoder'")).isZero();
        assertThat(count("user_id = 'benq_atcoder'")).isEqualTo(1);
    }

    private void insertOdsRow(String handle, long submissionId) {
        jdbcTemplate.update("""
                insert into ods_atcoder__submission (
                    atcoder_submission_id,
                    user_id,
                    batch_id,
                    fetched_at,
                    raw_payload,
                    payload_hash
                ) values (?, ?, 'batch-test', timestamp '2026-07-06 00:00:00', '{}', ?)
                """, submissionId, handle, hash(submissionId));
    }

    private int count(String predicate) {
        return jdbcTemplate.queryForObject(
                "select count(*) from ods_atcoder__submission where " + predicate,
                Integer.class
        );
    }

    private static String hash(long submissionId) {
        return String.format("%064d", submissionId);
    }
}
