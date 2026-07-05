create table if not exists dws_codeforces__handle_daily_rating_accepted_summary_v2 (
    id bigint primary key auto_increment,
    author_handle varchar(128) not null,
    accepted_date_utc_plus8 date not null,
    rating_800_accepted_problem_count int not null default 0,
    rating_900_accepted_problem_count int not null default 0,
    rating_1000_accepted_problem_count int not null default 0,
    rating_1100_accepted_problem_count int not null default 0,
    rating_1200_accepted_problem_count int not null default 0,
    rating_1300_accepted_problem_count int not null default 0,
    rating_1400_accepted_problem_count int not null default 0,
    rating_1500_accepted_problem_count int not null default 0,
    rating_1600_accepted_problem_count int not null default 0,
    rating_1700_accepted_problem_count int not null default 0,
    rating_1800_accepted_problem_count int not null default 0,
    rating_1900_accepted_problem_count int not null default 0,
    rating_2000_accepted_problem_count int not null default 0,
    rating_2100_accepted_problem_count int not null default 0,
    rating_2200_accepted_problem_count int not null default 0,
    rating_2300_accepted_problem_count int not null default 0,
    rating_2400_accepted_problem_count int not null default 0,
    rating_2500_accepted_problem_count int not null default 0,
    rating_2600_accepted_problem_count int not null default 0,
    rating_2700_accepted_problem_count int not null default 0,
    rating_2800_accepted_problem_count int not null default 0,
    rating_2900_accepted_problem_count int not null default 0,
    rating_3000_accepted_problem_count int not null default 0,
    rating_3100_accepted_problem_count int not null default 0,
    rating_3200_accepted_problem_count int not null default 0,
    rating_3300_accepted_problem_count int not null default 0,
    rating_3400_accepted_problem_count int not null default 0,
    rating_3500_accepted_problem_count int not null default 0,
    unrated_accepted_problem_count int not null default 0,
    created_at datetime(6) not null default current_timestamp(6),
    updated_at datetime(6) not null default current_timestamp(6)
);

insert into dws_codeforces__handle_daily_rating_accepted_summary_v2 (
    author_handle,
    accepted_date_utc_plus8,
    rating_800_accepted_problem_count,
    rating_900_accepted_problem_count,
    rating_1000_accepted_problem_count,
    rating_1100_accepted_problem_count,
    rating_1200_accepted_problem_count,
    rating_1300_accepted_problem_count,
    rating_1400_accepted_problem_count,
    rating_1500_accepted_problem_count,
    rating_1600_accepted_problem_count,
    rating_1700_accepted_problem_count,
    rating_1800_accepted_problem_count,
    rating_1900_accepted_problem_count,
    rating_2000_accepted_problem_count,
    rating_2100_accepted_problem_count,
    rating_2200_accepted_problem_count,
    rating_2300_accepted_problem_count,
    rating_2400_accepted_problem_count,
    rating_2500_accepted_problem_count,
    rating_2600_accepted_problem_count,
    rating_2700_accepted_problem_count,
    rating_2800_accepted_problem_count,
    rating_2900_accepted_problem_count,
    rating_3000_accepted_problem_count,
    rating_3100_accepted_problem_count,
    rating_3200_accepted_problem_count,
    rating_3300_accepted_problem_count,
    rating_3400_accepted_problem_count,
    rating_3500_accepted_problem_count,
    unrated_accepted_problem_count,
    created_at,
    updated_at
)
select
    author_handle,
    accepted_date_utc_plus8,
    coalesce(sum(case when problem_rating = 800 then accepted_problem_count else 0 end), 0),
    coalesce(sum(case when problem_rating = 900 then accepted_problem_count else 0 end), 0),
    coalesce(sum(case when problem_rating = 1000 then accepted_problem_count else 0 end), 0),
    coalesce(sum(case when problem_rating = 1100 then accepted_problem_count else 0 end), 0),
    coalesce(sum(case when problem_rating = 1200 then accepted_problem_count else 0 end), 0),
    coalesce(sum(case when problem_rating = 1300 then accepted_problem_count else 0 end), 0),
    coalesce(sum(case when problem_rating = 1400 then accepted_problem_count else 0 end), 0),
    coalesce(sum(case when problem_rating = 1500 then accepted_problem_count else 0 end), 0),
    coalesce(sum(case when problem_rating = 1600 then accepted_problem_count else 0 end), 0),
    coalesce(sum(case when problem_rating = 1700 then accepted_problem_count else 0 end), 0),
    coalesce(sum(case when problem_rating = 1800 then accepted_problem_count else 0 end), 0),
    coalesce(sum(case when problem_rating = 1900 then accepted_problem_count else 0 end), 0),
    coalesce(sum(case when problem_rating = 2000 then accepted_problem_count else 0 end), 0),
    coalesce(sum(case when problem_rating = 2100 then accepted_problem_count else 0 end), 0),
    coalesce(sum(case when problem_rating = 2200 then accepted_problem_count else 0 end), 0),
    coalesce(sum(case when problem_rating = 2300 then accepted_problem_count else 0 end), 0),
    coalesce(sum(case when problem_rating = 2400 then accepted_problem_count else 0 end), 0),
    coalesce(sum(case when problem_rating = 2500 then accepted_problem_count else 0 end), 0),
    coalesce(sum(case when problem_rating = 2600 then accepted_problem_count else 0 end), 0),
    coalesce(sum(case when problem_rating = 2700 then accepted_problem_count else 0 end), 0),
    coalesce(sum(case when problem_rating = 2800 then accepted_problem_count else 0 end), 0),
    coalesce(sum(case when problem_rating = 2900 then accepted_problem_count else 0 end), 0),
    coalesce(sum(case when problem_rating = 3000 then accepted_problem_count else 0 end), 0),
    coalesce(sum(case when problem_rating = 3100 then accepted_problem_count else 0 end), 0),
    coalesce(sum(case when problem_rating = 3200 then accepted_problem_count else 0 end), 0),
    coalesce(sum(case when problem_rating = 3300 then accepted_problem_count else 0 end), 0),
    coalesce(sum(case when problem_rating = 3400 then accepted_problem_count else 0 end), 0),
    coalesce(sum(case when problem_rating = 3500 then accepted_problem_count else 0 end), 0),
    coalesce(sum(case when problem_rating is null then accepted_problem_count else 0 end), 0),
    min(created_at),
    max(updated_at)
from dws_codeforces__handle_daily_rating_accepted_summary
group by
    author_handle,
    accepted_date_utc_plus8;

drop table dws_codeforces__handle_daily_rating_accepted_summary;

alter table dws_codeforces__handle_daily_rating_accepted_summary_v2
    rename to dws_codeforces__handle_daily_rating_accepted_summary;

alter table dws_codeforces__handle_daily_rating_accepted_summary
    add unique key uk_dws_codeforces_handle_date (
        author_handle,
        accepted_date_utc_plus8
    );

create index idx_dws_codeforces_daily_rating_date
    on dws_codeforces__handle_daily_rating_accepted_summary (accepted_date_utc_plus8);
