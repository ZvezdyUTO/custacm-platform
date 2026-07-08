delete from dwd_codeforces__submission
where submitted_date_utc_plus8 between :refreshFromDateUtcPlus8 and :refreshToDateUtcPlus8;

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
)
select
    ods.id,
    concat('', ods.codeforces_submission_id),
    ods.author_handle,
    timestampadd(
        HOUR,
        8,
        timestampadd(SECOND, ods.creation_time_seconds, timestamp '1970-01-01 00:00:00')
    ),
    cast(timestampadd(
        HOUR,
        8,
        timestampadd(SECOND, ods.creation_time_seconds, timestamp '1970-01-01 00:00:00')
    ) as date),
    case
        when ods.problem_contest_id is null or ods.problem_index is null or trim(ods.problem_index) = '' then null
        else concat(ods.problem_contest_id, ':', ods.problem_index)
    end,
    ods.problem_index,
    ods.problem_name,
    case when ods.problem_rating is null then null else concat('', ods.problem_rating) end,
    ods.programming_language,
    ods.verdict,
    case when ods.verdict = 'OK' then 1 else 0 end,
    ods.time_consumed_millis,
    case
        when ods.contest_id is null then null
        else concat('https://codeforces.com/contest/', ods.contest_id, '/submission/', ods.codeforces_submission_id)
    end,
    ods.batch_id,
    ods.fetched_at,
    ods.payload_hash
from ods_codeforces__submission ods
where ods.creation_time_seconds is not null
      and cast(timestampadd(
            HOUR,
            8,
            timestampadd(SECOND, ods.creation_time_seconds, timestamp '1970-01-01 00:00:00')
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
