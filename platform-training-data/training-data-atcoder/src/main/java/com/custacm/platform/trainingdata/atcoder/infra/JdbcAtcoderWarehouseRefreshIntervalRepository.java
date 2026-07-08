package com.custacm.platform.trainingdata.atcoder.infra;

import com.custacm.platform.trainingdata.common.domain.oj.model.OjWarehouseRefreshInterval;
import com.custacm.platform.trainingdata.common.domain.oj.repo.OjWarehouseRefreshIntervalRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.Date;
import java.util.Optional;

public class JdbcAtcoderWarehouseRefreshIntervalRepository implements OjWarehouseRefreshIntervalRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcAtcoderWarehouseRefreshIntervalRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<OjWarehouseRefreshInterval> findBatchDateInterval(String batchId) {
        return jdbcTemplate.queryForObject("""
                select
                    min(refresh_date) as from_date,
                    max(refresh_date) as to_date
                from (
                    select cast(timestampadd(
                        HOUR,
                        8,
                        timestampadd(SECOND, epoch_second, timestamp '1970-01-01 00:00:00')
                    ) as date) as refresh_date
                    from ods_atcoder__submission
                    where batch_id = :batchId

                    union all

                    select existing.first_accepted_date_utc_plus8 as refresh_date
                    from dwm_atcoder__handle_problem_first_accepted existing
                    join (
                        select distinct user_id, problem_id
                        from ods_atcoder__submission
                        where batch_id = :batchId
                          and result = 'AC'
                          and problem_id is not null
                          and trim(problem_id) <> ''
                    ) touched
                      on existing.handle = touched.user_id
                     and existing.problem_key = touched.problem_id
                ) refresh_dates
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
