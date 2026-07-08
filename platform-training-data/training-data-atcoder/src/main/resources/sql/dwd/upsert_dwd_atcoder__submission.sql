delete from dwd_atcoder__submission
where submitted_date_utc_plus8 between :refreshFromDateUtcPlus8 and :refreshToDateUtcPlus8;

insert into dwd_atcoder__submission (
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
select
    ods.id,
    concat('', ods.atcoder_submission_id),
    ods.user_id,
    timestampadd(
        HOUR,
        8,
        timestampadd(SECOND, ods.epoch_second, timestamp '1970-01-01 00:00:00')
    ),
    cast(timestampadd(
        HOUR,
        8,
        timestampadd(SECOND, ods.epoch_second, timestamp '1970-01-01 00:00:00')
    ) as date),
    ods.problem_id,
    problem.problem_index,
    coalesce(problem.title, problem.problem_name, ods.problem_id),
    case
        when model.clipped_difficulty is null then null
        when model.is_experimental = 1 then null
        when lower(trim(coalesce(ods.contest_id, problem.contest_id, ''))) not like 'abc%'
                and lower(trim(coalesce(ods.contest_id, problem.contest_id, ''))) not like 'arc%'
                and lower(trim(coalesce(ods.contest_id, problem.contest_id, ''))) not like 'agc%' then null
        when model.clipped_difficulty >= 2800 then '2800+'
        else concat('', floor(model.clipped_difficulty / 400) * 400)
    end,
    ods.language,
    ods.result,
    case when ods.result = 'AC' then 1 else 0 end,
    ods.execution_time_millis,
    case
        when coalesce(ods.contest_id, problem.contest_id) is null
                or trim(coalesce(ods.contest_id, problem.contest_id)) = '' then null
        else concat(
                'https://atcoder.jp/contests/',
                coalesce(ods.contest_id, problem.contest_id),
                '/submissions/',
                ods.atcoder_submission_id
        )
    end,
    ods.batch_id,
    ods.fetched_at,
    ods.payload_hash
from ods_atcoder__submission ods
left join ods_atcoder__problem problem
       on problem.problem_id = ods.problem_id
left join ods_atcoder__problem_model model
       on model.problem_id = ods.problem_id
where cast(timestampadd(
        HOUR,
        8,
        timestampadd(SECOND, ods.epoch_second, timestamp '1970-01-01 00:00:00')
    ) as date) between :refreshFromDateUtcPlus8 and :refreshToDateUtcPlus8
on duplicate key update
    ods_submission_id = values(ods_submission_id),
    handle = values(handle),
    submitted_at_utc_plus8 = values(submitted_at_utc_plus8),
    submitted_date_utc_plus8 = values(submitted_date_utc_plus8),
    problem_key = values(problem_key),
    problem_index = values(problem_index),
    problem_name = values(problem_name),
    difficulty = values(difficulty),
    language = values(language),
    verdict = values(verdict),
    is_accepted = values(is_accepted),
    time_consumed_millis = values(time_consumed_millis),
    source_url = values(source_url),
    ods_batch_id = values(ods_batch_id),
    ods_fetched_at = values(ods_fetched_at),
    ods_payload_hash = values(ods_payload_hash),
    updated_at = current_timestamp(6);
