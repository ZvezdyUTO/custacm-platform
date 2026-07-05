create table auth_user_account_v021 (
    student_identity varchar(128) not null,
    password_hash varchar(100) not null,
    role varchar(32) not null,
    created_at timestamp(6) not null,
    updated_at timestamp(6) not null,
    primary key (student_identity),
    constraint ck_auth_user_account_role_v021 check (role in ('admin', 'player', 'disable'))
);

insert into auth_user_account_v021 (
    student_identity, password_hash, role, created_at, updated_at
)
select
    student_identity,
    password_hash,
    case
        when status = 'disabled' then 'disable'
        else role
    end,
    created_at,
    updated_at
from auth_user_account;

drop table auth_user_account;

alter table auth_user_account_v021 rename to auth_user_account;
