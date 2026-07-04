delete from dws_codeforces__handle_daily_rating_accepted_summary
where not exists (
    select 1
    from (
        select
            first_accepted.author_handle,
            first_accepted.first_accepted_date_utc as accepted_date_utc,
            case
                when first_accepted.problem_rating is null then 'UNRATED'
                else concat('', first_accepted.problem_rating)
            end as problem_rating_key
        from dwm_codeforces__handle_problem_first_accepted first_accepted
        group by
            first_accepted.author_handle,
            first_accepted.first_accepted_date_utc,
            case
                when first_accepted.problem_rating is null then 'UNRATED'
                else concat('', first_accepted.problem_rating)
            end
    ) current_summary
    where current_summary.author_handle = dws_codeforces__handle_daily_rating_accepted_summary.author_handle
      and current_summary.accepted_date_utc = dws_codeforces__handle_daily_rating_accepted_summary.accepted_date_utc
      and current_summary.problem_rating_key = dws_codeforces__handle_daily_rating_accepted_summary.problem_rating_key
);

insert into dws_codeforces__handle_daily_rating_accepted_summary (
    author_handle,
    accepted_date_utc,
    problem_rating_key,
    problem_rating,
    accepted_problem_count
)
select
    first_accepted.author_handle,
    first_accepted.first_accepted_date_utc,
    case
        when first_accepted.problem_rating is null then 'UNRATED'
        else concat('', first_accepted.problem_rating)
    end as problem_rating_key,
    first_accepted.problem_rating,
    count(*) as accepted_problem_count
from dwm_codeforces__handle_problem_first_accepted first_accepted
group by
    first_accepted.author_handle,
    first_accepted.first_accepted_date_utc,
    case
        when first_accepted.problem_rating is null then 'UNRATED'
        else concat('', first_accepted.problem_rating)
    end,
    first_accepted.problem_rating
on duplicate key update
    problem_rating = values(problem_rating),
    accepted_problem_count = values(accepted_problem_count),
    updated_at = current_timestamp(6);
