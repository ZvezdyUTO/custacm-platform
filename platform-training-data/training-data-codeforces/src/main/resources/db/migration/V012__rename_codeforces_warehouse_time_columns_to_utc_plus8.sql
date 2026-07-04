alter table dwd_codeforces__submission
    rename column submitted_at to submitted_at_utc_plus8;

alter table dwd_codeforces__submission
    rename column submitted_date_utc to submitted_date_utc_plus8;

alter table dwm_codeforces__handle_problem_first_accepted
    rename column first_accepted_at to first_accepted_at_utc_plus8;

alter table dwm_codeforces__handle_problem_first_accepted
    rename column first_accepted_date_utc to first_accepted_date_utc_plus8;

alter table dws_codeforces__handle_daily_rating_accepted_summary
    rename column accepted_date_utc to accepted_date_utc_plus8;
