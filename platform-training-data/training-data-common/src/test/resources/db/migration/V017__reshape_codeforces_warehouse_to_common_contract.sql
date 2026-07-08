drop table if exists dws_codeforces__handle_daily_rating_accepted_summary;
drop table if exists dwm_codeforces__handle_problem_first_accepted;
drop table if exists dwd_codeforces__submission;

create table dwd_codeforces__submission (
    id bigint primary key auto_increment,
    ods_submission_id bigint not null,
    submission_id varchar(128) not null,
    handle varchar(128) not null,
    submitted_at_utc_plus8 datetime(6) null,
    submitted_date_utc_plus8 date null,
    problem_key varchar(128) null,
    problem_index varchar(32) null,
    problem_name varchar(255) null,
    difficulty varchar(64) null,
    language varchar(255) null,
    verdict varchar(128) null,
    is_accepted tinyint(1) not null,
    time_consumed_millis int null,
    source_url varchar(512) null,
    ods_batch_id varchar(128) not null,
    ods_fetched_at datetime(6) not null,
    ods_payload_hash char(64) not null,
    created_at datetime(6) not null default current_timestamp(6),
    updated_at datetime(6) not null default current_timestamp(6),
    unique key uk_dwd_codeforces_submission_handle (submission_id, handle),
    index idx_dwd_codeforces_submission_ods (ods_submission_id),
    index idx_dwd_codeforces_handle_time (handle, submitted_at_utc_plus8),
    index idx_dwd_codeforces_problem_time (problem_key, submitted_at_utc_plus8, submission_id),
    index idx_dwd_codeforces_handle_problem (handle, problem_key),
    index idx_dwd_codeforces_batch (ods_batch_id)
);

create table dwm_codeforces__handle_problem_first_accepted (
    id bigint primary key auto_increment,
    handle varchar(128) not null,
    problem_key varchar(128) not null,
    problem_index varchar(32) null,
    problem_name varchar(255) null,
    difficulty varchar(64) null,
    first_accepted_submission_id varchar(128) not null,
    first_accepted_at_utc_plus8 datetime(6) not null,
    first_accepted_date_utc_plus8 date not null,
    first_accepted_language varchar(255) null,
    first_accepted_source_url varchar(512) null,
    created_at datetime(6) not null default current_timestamp(6),
    updated_at datetime(6) not null default current_timestamp(6),
    unique key uk_dwm_codeforces_handle_problem (handle, problem_key),
    index idx_dwm_codeforces_problem (problem_key),
    index idx_dwm_codeforces_handle_date (handle, first_accepted_date_utc_plus8)
);

create table dws_codeforces__handle_daily_rating_accepted_summary (
    id bigint primary key auto_increment,
    handle varchar(128) not null,
    accepted_date_utc_plus8 date not null,
    difficulty varchar(64) not null,
    accepted_problem_count int not null,
    created_at datetime(6) not null default current_timestamp(6),
    updated_at datetime(6) not null default current_timestamp(6),
    unique key uk_dws_codeforces_handle_date_difficulty (
        handle,
        accepted_date_utc_plus8,
        difficulty
    ),
    index idx_dws_codeforces_date_difficulty (
        accepted_date_utc_plus8,
        difficulty
    )
);
