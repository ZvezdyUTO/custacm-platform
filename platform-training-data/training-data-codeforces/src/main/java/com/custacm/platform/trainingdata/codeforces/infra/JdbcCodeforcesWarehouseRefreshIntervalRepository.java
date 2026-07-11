package com.custacm.platform.trainingdata.codeforces.infra;

import com.custacm.platform.trainingdata.common.domain.oj.model.OjWarehouseRefreshInterval;
import com.custacm.platform.trainingdata.common.domain.oj.repo.OjWarehouseRefreshIntervalRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.Date;
import java.util.Optional;

public class JdbcCodeforcesWarehouseRefreshIntervalRepository implements OjWarehouseRefreshIntervalRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcCodeforcesWarehouseRefreshIntervalRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<String> findLatestBatchId() {
        return jdbcTemplate.query("""
                select batch_id
                from ods_codeforces__submission
                where batch_id is not null
                  and trim(batch_id) <> ''
                  and fetched_at is not null
                  and creation_time_seconds is not null
                order by fetched_at desc, id desc
                limit 1
                """, new MapSqlParameterSource(), (rs, rowNum) -> rs.getString("batch_id"))
                .stream()
                .findFirst();
    }

    @Override
    public Optional<OjWarehouseRefreshInterval> findBatchDateInterval(String batchId) {
        return jdbcTemplate.queryForObject("""
                select
                    min(cast(timestampadd(
                        HOUR,
                        8,
                        timestampadd(SECOND, creation_time_seconds, timestamp '1970-01-01 00:00:00')
                    ) as date)) as from_date,
                    max(cast(timestampadd(
                        HOUR,
                        8,
                        timestampadd(SECOND, creation_time_seconds, timestamp '1970-01-01 00:00:00')
                    ) as date)) as to_date
                from ods_codeforces__submission
                where batch_id = :batchId
                  and creation_time_seconds is not null
                """, new MapSqlParameterSource("batchId", batchId), (rs, rowNum) -> {
            Date fromDate = rs.getDate("from_date");
            Date toDate = rs.getDate("to_date");
            if (fromDate == null || toDate == null) {
                return Optional.empty();
            }
            return Optional.of(new OjWarehouseRefreshInterval(
                    fromDate.toLocalDate(),
                    toDate.toLocalDate()
            ));
        });
    }
}
