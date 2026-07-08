delete from dwm_atcoder__handle_problem_first_accepted
where first_accepted_date_utc_plus8 between :refreshFromDateUtcPlus8 and :refreshToDateUtcPlus8;

insert into dwm_atcoder__handle_problem_first_accepted (
    handle,
    problem_key,
    problem_index,
    problem_name,
    difficulty,
    first_accepted_submission_id,
    first_accepted_at_utc_plus8,
    first_accepted_date_utc_plus8,
    first_accepted_language,
    first_accepted_source_url
)
select
    ranked.handle,
    ranked.problem_key,
    ranked.problem_index,
    ranked.problem_name,
    ranked.difficulty,
    ranked.submission_id,
    ranked.submitted_at_utc_plus8,
    ranked.submitted_date_utc_plus8,
    ranked.language,
    ranked.source_url
from (
    select
        dwd.*,
        row_number() over (
            partition by dwd.handle, dwd.problem_key
            order by dwd.submitted_at_utc_plus8, length(dwd.submission_id), dwd.submission_id
        ) as accepted_rank
    from dwd_atcoder__submission dwd
    where dwd.is_accepted = 1
      and dwd.problem_key is not null
      and trim(dwd.problem_key) <> ''
      and dwd.submitted_at_utc_plus8 is not null
      and dwd.submitted_date_utc_plus8 is not null
) ranked
where ranked.accepted_rank = 1
  and ranked.submitted_date_utc_plus8 between :refreshFromDateUtcPlus8 and :refreshToDateUtcPlus8
on duplicate key update
    problem_index = values(problem_index),
    problem_name = values(problem_name),
    difficulty = values(difficulty),
    first_accepted_submission_id = values(first_accepted_submission_id),
    first_accepted_at_utc_plus8 = values(first_accepted_at_utc_plus8),
    first_accepted_date_utc_plus8 = values(first_accepted_date_utc_plus8),
    first_accepted_language = values(first_accepted_language),
    first_accepted_source_url = values(first_accepted_source_url),
    updated_at = current_timestamp(6);
