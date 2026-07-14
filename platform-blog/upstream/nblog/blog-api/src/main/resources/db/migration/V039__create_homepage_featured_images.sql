-- Author: huangbingrui.awa
CREATE TABLE `homepage_featured_image` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `image_url` varchar(1024) NOT NULL,
  `sort_order` int NOT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_homepage_featured_image_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
