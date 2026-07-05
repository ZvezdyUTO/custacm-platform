create table auth_user_account (
    student_identity varchar(128) not null,
    password_hash varchar(100) not null,
    role varchar(32) not null,
    status varchar(32) not null,
    created_at timestamp(6) not null,
    updated_at timestamp(6) not null,
    primary key (student_identity),
    constraint ck_auth_user_account_role check (role in ('admin', 'player')),
    constraint ck_auth_user_account_status check (status in ('active', 'disabled'))
);
