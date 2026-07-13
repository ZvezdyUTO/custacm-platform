-- Author: huangbingrui.awa
ALTER TABLE blog
    ADD COLUMN deleted_at DATETIME(0) NULL COMMENT '进入文章回收站的时间' AFTER update_time,
    ADD INDEX idx_blog_recycle_bin (deleted_at);
