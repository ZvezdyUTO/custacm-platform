alter table ods_codeforces__submission
    drop index uk_ods_codeforces_submission_source;

alter table ods_codeforces__submission
    add unique key uk_ods_codeforces_submission_handle (
        codeforces_submission_id,
        author_handle
    );

alter table dwd_codeforces__submission
    drop index uk_dwd_codeforces_submission_id;

alter table dwd_codeforces__submission
    add unique key uk_dwd_codeforces_submission_handle (
        submission_id,
        handle
    );
