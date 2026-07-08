delete from dws_atcoder__handle_daily_rating_accepted_summary
where accepted_date_utc_plus8 between :refreshFromDateUtcPlus8 and :refreshToDateUtcPlus8;

insert into dws_atcoder__handle_daily_rating_accepted_summary (
    handle,
    accepted_date_utc_plus8,
    difficulty,
    accepted_problem_count
)
select
    first_accepted.handle,
    first_accepted.first_accepted_date_utc_plus8,
    coalesce(first_accepted.difficulty, 'UNRATED') as difficulty,
    count(*) as accepted_problem_count
from dwm_atcoder__handle_problem_first_accepted first_accepted
where first_accepted.first_accepted_date_utc_plus8 between :refreshFromDateUtcPlus8 and :refreshToDateUtcPlus8
group by
    first_accepted.handle,
    first_accepted.first_accepted_date_utc_plus8,
    coalesce(first_accepted.difficulty, 'UNRATED')
on duplicate key update
    accepted_problem_count = values(accepted_problem_count),
    updated_at = current_timestamp(6);
