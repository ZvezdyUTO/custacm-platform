create table if not exists oj_handle_account_v2 (
    username varchar(128) not null,
    handles_json longtext not null,
    need_collect boolean not null default true,
    created_at datetime(6) not null default current_timestamp(6),
    updated_at datetime(6) not null default current_timestamp(6),
    primary key (username)
);

insert into oj_handle_account_v2 (
    username,
    handles_json,
    need_collect,
    created_at,
    updated_at
)
select username,
       concat('{"CODEFORCES":"', codeforces_handle, '"}'),
       need_collect,
       created_at,
       updated_at
from codeforces_handle_account;

drop table codeforces_handle_account;

alter table oj_handle_account_v2
    rename to oj_handle_account;
