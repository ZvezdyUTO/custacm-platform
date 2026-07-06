create index idx_dwd_codeforces_problem_time_submission
    on dwd_codeforces__submission (
        problem_key,
        submitted_at_utc_plus8,
        codeforces_submission_id
    );
