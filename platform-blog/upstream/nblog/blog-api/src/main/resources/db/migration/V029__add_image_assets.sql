CREATE TABLE image_asset (
    id BIGINT NOT NULL AUTO_INCREMENT,
    public_id CHAR(36) NOT NULL,
    owner_user_id BIGINT NULL,
    purpose VARCHAR(32) NOT NULL,
    original_path VARCHAR(512) NOT NULL,
    thumbnail_path VARCHAR(512) NOT NULL,
    original_url VARCHAR(512) NOT NULL,
    thumbnail_url VARCHAR(512) NOT NULL,
    mime_type VARCHAR(32) NOT NULL,
    width INT NOT NULL,
    height INT NOT NULL,
    original_bytes BIGINT NOT NULL,
    thumbnail_bytes BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_image_asset_public_id (public_id),
    KEY idx_image_asset_owner (owner_user_id),
    KEY idx_image_asset_cleanup (status, create_time),
    CONSTRAINT fk_image_asset_owner FOREIGN KEY (owner_user_id) REFERENCES `user` (id) ON DELETE SET NULL
);

CREATE TABLE blog_image_reference (
    blog_id BIGINT NOT NULL,
    image_asset_id BIGINT NOT NULL,
    role VARCHAR(16) NOT NULL,
    PRIMARY KEY (blog_id, image_asset_id),
    UNIQUE KEY uk_blog_image_reference_asset (image_asset_id),
    CONSTRAINT fk_blog_image_reference_blog FOREIGN KEY (blog_id) REFERENCES blog (id) ON DELETE CASCADE,
    CONSTRAINT fk_blog_image_reference_asset FOREIGN KEY (image_asset_id) REFERENCES image_asset (id) ON DELETE CASCADE
);

ALTER TABLE blog ADD COLUMN first_picture_asset_id BIGINT NULL AFTER first_picture;
ALTER TABLE blog ADD CONSTRAINT fk_blog_first_picture_asset
    FOREIGN KEY (first_picture_asset_id) REFERENCES image_asset (id) ON DELETE SET NULL;

ALTER TABLE `user` ADD COLUMN avatar_asset_id BIGINT NULL AFTER avatar;
ALTER TABLE `user` ADD CONSTRAINT fk_user_avatar_asset
    FOREIGN KEY (avatar_asset_id) REFERENCES image_asset (id) ON DELETE SET NULL;
