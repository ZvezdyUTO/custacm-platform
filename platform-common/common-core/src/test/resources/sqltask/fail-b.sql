insert into sql_task_test_log(step_name, marker, note)
values ('B', :marker, 'rolled back before failing');

insert into table_that_does_not_exist(id)
values (1);
