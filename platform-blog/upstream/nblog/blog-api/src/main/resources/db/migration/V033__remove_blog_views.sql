-- Author: huangbingrui.awa
DELETE FROM schedule_job
WHERE bean_name = 'redisSyncScheduleTask'
  AND method_name = 'syncBlogViewsToDatabase';

ALTER TABLE blog DROP COLUMN views;
