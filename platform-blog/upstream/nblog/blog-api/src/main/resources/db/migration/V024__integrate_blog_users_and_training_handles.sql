ALTER TABLE `user`
    MODIFY COLUMN username varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
	ADD INDEX idx_user_role (role),
    ADD CONSTRAINT chk_user_role CHECK (role IN ('ROLE_admin', 'ROLE_player'));

ALTER TABLE oj_handle_account
    RENAME COLUMN student_identity TO username;

ALTER TABLE oj_handle_account
    MODIFY COLUMN username varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    ADD CONSTRAINT fk_oj_handle_account_user
        FOREIGN KEY (username) REFERENCES `user` (username)
        ON UPDATE CASCADE ON DELETE CASCADE;

ALTER TABLE blog
    ADD CONSTRAINT fk_blog_user
        FOREIGN KEY (user_id) REFERENCES `user` (id)
        ON DELETE SET NULL;

ALTER TABLE comment
    MODIFY COLUMN user_id bigint NULL,
    ADD CONSTRAINT fk_comment_user
        FOREIGN KEY (user_id) REFERENCES `user` (id)
        ON DELETE SET NULL;
