ALTER TABLE blog
    ADD COLUMN is_internal bit(1) NOT NULL DEFAULT b'0' COMMENT '仅登录用户可见' AFTER is_published,
    DROP COLUMN password;
