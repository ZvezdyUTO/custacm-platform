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
        // Include both ends of a first-accepted move so DWM and daily counts stay consistent.
        // Later repeat acceptances cannot move the first acceptance and keep the batch interval narrow.
        return jdbcTemplate.queryForObject("""
                with touched_problems as (
                    select distinct batch.author_handle as handle,
                           concat(batch.problem_contest_id, ':', batch.problem_index) as problem_key
                    from ods_codeforces__submission batch
                    left join dwm_codeforces__handle_problem_first_accepted existing
                      on existing.handle = batch.author_handle
                     and existing.problem_key = concat(batch.problem_contest_id, ':', batch.problem_index)
                    where batch.batch_id = :batchId
                      and batch.creation_time_seconds is not null
                      and batch.problem_contest_id is not null
                      and batch.problem_index is not null
                      and trim(batch.problem_index) <> ''
                      and (
                          (batch.verdict = 'OK' and (
                              existing.handle is null
                              or timestampadd(HOUR, 8, timestampadd(
                                  SECOND, batch.creation_time_seconds, timestamp '1970-01-01 00:00:00'
                              )) <= existing.first_accepted_at_utc_plus8
                          ))
                          or existing.first_accepted_submission_id = concat('', batch.codeforces_submission_id)
                      )
                )
                select min(refresh_date) as from_date, max(refresh_date) as to_date
                from (
                    select cast(timestampadd(
                        HOUR, 8, timestampadd(SECOND, creation_time_seconds, timestamp '1970-01-01 00:00:00')
                    ) as date) as refresh_date
                    from ods_codeforces__submission
                    where batch_id = :batchId
                      and creation_time_seconds is not null

                    union all

                    select existing.first_accepted_date_utc_plus8 as refresh_date
                    from dwm_codeforces__handle_problem_first_accepted existing
                    join touched_problems touched
                      on existing.handle = touched.handle
                     and existing.problem_key = touched.problem_key

                    union all

                    select cast(timestampadd(
                        HOUR, 8, timestampadd(SECOND, min(candidate.creation_time_seconds), timestamp '1970-01-01 00:00:00')
                    ) as date) as refresh_date
                    from ods_codeforces__submission candidate
                    join touched_problems touched
                      on candidate.author_handle = touched.handle
                     and concat(candidate.problem_contest_id, ':', candidate.problem_index) = touched.problem_key
                    where candidate.verdict = 'OK'
                      and candidate.creation_time_seconds is not null
                    group by touched.handle, touched.problem_key
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
