delete from dwm_codeforces__handle_problem_first_accepted
where not exists (
    select 1
    from (
        select
            dwd.author_handle,
            dwd.problem_key
        from dwd_codeforces__submission dwd
        where dwd.is_accepted = 1
          and dwd.problem_key is not null
          and dwd.problem_contest_id is not null
          and dwd.problem_index is not null
          and dwd.submitted_at is not null
          and dwd.submitted_date_utc is not null
        group by
            dwd.author_handle,
            dwd.problem_key
    ) current_first_accepted
    where current_first_accepted.author_handle = dwm_codeforces__handle_problem_first_accepted.author_handle
      and current_first_accepted.problem_key = dwm_codeforces__handle_problem_first_accepted.problem_key
);

insert into dwm_codeforces__handle_problem_first_accepted (
    author_handle,
    problem_key,
    problem_contest_id,
    problem_index,
    problem_name,
    problem_type,
    problem_points,
    problem_rating,
    problem_tags_json,
    first_accepted_submission_id,
    first_accepted_at,
    first_accepted_date_utc,
    first_accepted_language
)
select
    ranked.author_handle,
    ranked.problem_key,
    ranked.problem_contest_id,
    ranked.problem_index,
    ranked.problem_name,
    ranked.problem_type,
    ranked.problem_points,
    ranked.problem_rating,
    ranked.problem_tags_json,
    ranked.codeforces_submission_id,
    ranked.submitted_at,
    ranked.submitted_date_utc,
    ranked.programming_language
from (
    select
        dwd.*,
        row_number() over (
            partition by dwd.author_handle, dwd.problem_key
            order by dwd.submitted_at, dwd.codeforces_submission_id
        ) as accepted_rank
    from dwd_codeforces__submission dwd
    where dwd.is_accepted = 1
      and dwd.problem_key is not null
      and dwd.problem_contest_id is not null
      and dwd.problem_index is not null
      and dwd.submitted_at is not null
      and dwd.submitted_date_utc is not null
) ranked
where ranked.accepted_rank = 1
on duplicate key update
    problem_contest_id = values(problem_contest_id),
    problem_index = values(problem_index),
    problem_name = values(problem_name),
    problem_type = values(problem_type),
    problem_points = values(problem_points),
    problem_rating = values(problem_rating),
    problem_tags_json = values(problem_tags_json),
    first_accepted_submission_id = values(first_accepted_submission_id),
    first_accepted_at = values(first_accepted_at),
    first_accepted_date_utc = values(first_accepted_date_utc),
    first_accepted_language = values(first_accepted_language),
    updated_at = current_timestamp(6);
