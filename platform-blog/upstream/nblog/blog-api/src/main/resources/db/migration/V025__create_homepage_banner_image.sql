CREATE TABLE `homepage_banner_image` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `image_url` varchar(1024) NOT NULL,
  `sort_order` int NOT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_homepage_banner_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO `homepage_banner_image` (`image_url`, `sort_order`) VALUES
  ('https://cdn.naccl.top/blog/img/bg1.jpg', 0),
  ('https://cdn.naccl.top/blog/img/bg2.jpg', 1),
  ('https://cdn.naccl.top/blog/img/bg3.jpg', 2);
