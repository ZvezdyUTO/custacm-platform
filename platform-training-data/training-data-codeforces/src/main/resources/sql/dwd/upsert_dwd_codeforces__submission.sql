delete from dwd_codeforces__submission
where submitted_date_utc_plus8 between :refreshFromDateUtcPlus8 and :refreshToDateUtcPlus8;

insert into dwd_codeforces__submission (
    ods_submission_id,
    codeforces_submission_id,
    author_handle,
    contest_id,
    submitted_at_utc_plus8,
    submitted_date_utc_plus8,
    relative_time_seconds,
    problem_key,
    problem_contest_id,
    problem_index,
    problem_name,
    problem_type,
    problem_points,
    problem_rating,
    problem_tags_json,
    author_participant_type,
    programming_language,
    verdict,
    is_accepted,
    testset,
    passed_test_count,
    time_consumed_millis,
    memory_consumed_bytes,
    ods_batch_id,
    ods_fetched_at,
    ods_payload_hash
)
select
    ods.id,
    ods.codeforces_submission_id,
    ods.author_handle,
    ods.contest_id,
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
    ods.relative_time_seconds,
    case
        when ods.problem_contest_id is null or ods.problem_index is null or trim(ods.problem_index) = '' then null
        else concat(ods.problem_contest_id, ':', ods.problem_index)
    end,
    ods.problem_contest_id,
    ods.problem_index,
    ods.problem_name,
    ods.problem_type,
    ods.problem_points,
    ods.problem_rating,
    ods.problem_tags_json,
    ods.author_participant_type,
    ods.programming_language,
    ods.verdict,
    case when ods.verdict = 'OK' then 1 else 0 end,
    ods.testset,
    ods.passed_test_count,
    ods.time_consumed_millis,
    ods.memory_consumed_bytes,
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
    author_handle = values(author_handle),
    contest_id = values(contest_id),
    submitted_at_utc_plus8 = values(submitted_at_utc_plus8),
    submitted_date_utc_plus8 = values(submitted_date_utc_plus8),
    relative_time_seconds = values(relative_time_seconds),
    problem_key = values(problem_key),
    problem_contest_id = values(problem_contest_id),
    problem_index = values(problem_index),
    problem_name = values(problem_name),
    problem_type = values(problem_type),
    problem_points = values(problem_points),
    problem_rating = values(problem_rating),
    problem_tags_json = values(problem_tags_json),
    author_participant_type = values(author_participant_type),
    programming_language = values(programming_language),
    verdict = values(verdict),
    is_accepted = values(is_accepted),
    testset = values(testset),
    passed_test_count = values(passed_test_count),
    time_consumed_millis = values(time_consumed_millis),
    memory_consumed_bytes = values(memory_consumed_bytes),
    ods_batch_id = values(ods_batch_id),
    ods_fetched_at = values(ods_fetched_at),
    ods_payload_hash = values(ods_payload_hash),
    updated_at = current_timestamp(6);
