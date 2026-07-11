create table if not exists codeforces_handle_account (
    username varchar(128) not null,
    codeforces_handle varchar(128) not null,
    created_at datetime(6) not null default current_timestamp(6),
    updated_at datetime(6) not null default current_timestamp(6),
    primary key (username),
    unique key uk_codeforces_handle_account_handle (codeforces_handle)
);
