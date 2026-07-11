ALTER TABLE `user`
    ADD COLUMN `signature` varchar(160) NOT NULL DEFAULT '' COMMENT '用户个性签名' AFTER `avatar`;

CREATE TABLE `user_profile_link` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `user_id` bigint NOT NULL,
    `label` varchar(30) NOT NULL COMMENT '链接显示名称',
    `url` varchar(2048) NOT NULL COMMENT '链接地址',
    `sort_order` smallint NOT NULL DEFAULT 0,
    `create_time` datetime NOT NULL,
    `update_time` datetime NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_user_profile_link_user_sort` (`user_id`, `sort_order`, `id`),
    CONSTRAINT `fk_user_profile_link_user`
        FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
