package com.custacm.platform.trainingdata.codeforces.infra.repo;

import com.custacm.platform.trainingdata.codeforces.domain.model.CodeforcesWarehouseRefreshInterval;
import com.custacm.platform.trainingdata.codeforces.domain.repo.CodeforcesWarehouseRefreshIntervalRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.Date;
import java.util.Optional;

public class JdbcCodeforcesWarehouseRefreshIntervalRepository implements CodeforcesWarehouseRefreshIntervalRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcCodeforcesWarehouseRefreshIntervalRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<CodeforcesWarehouseRefreshInterval> findBatchDateInterval(String batchId) {
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
            return Optional.of(new CodeforcesWarehouseRefreshInterval(
                    fromDate.toLocalDate(),
                    toDate.toLocalDate()
            ));
        });
    }
}
