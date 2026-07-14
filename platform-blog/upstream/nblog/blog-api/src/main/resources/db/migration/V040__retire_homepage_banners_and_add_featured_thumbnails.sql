-- Author: huangbingrui.awa
DROP TABLE `homepage_banner_image`;

ALTER TABLE `homepage_featured_image`
  ADD COLUMN `thumbnail_url` varchar(1024) NULL AFTER `image_url`;
