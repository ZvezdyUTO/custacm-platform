-- Author: huangbingrui.awa
DELETE banner
FROM `homepage_banner_image` banner
JOIN (
  SELECT MIN(`id`) AS `keep_id`
  FROM `homepage_banner_image`
) retained ON banner.`id` <> retained.`keep_id`;

UPDATE `homepage_banner_image`
SET `image_url` = '/img/homepage-banner-default.png',
    `sort_order` = 0;

INSERT INTO `homepage_banner_image` (`image_url`, `sort_order`)
SELECT '/img/homepage-banner-default.png', 0
WHERE NOT EXISTS (SELECT 1 FROM `homepage_banner_image`);
