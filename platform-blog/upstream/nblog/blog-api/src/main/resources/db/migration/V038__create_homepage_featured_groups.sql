-- Author: huangbingrui.awa

CREATE TABLE homepage_featured_group (
    id bigint NOT NULL AUTO_INCREMENT,
    title varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    sort_order smallint unsigned NOT NULL,
    create_time datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_homepage_featured_group_sort_order (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE homepage_featured_group_article (
    group_id bigint NOT NULL,
    blog_id bigint NOT NULL,
    sort_order tinyint unsigned NOT NULL,
    PRIMARY KEY (group_id, blog_id),
    UNIQUE KEY uk_homepage_featured_group_article_blog (blog_id),
    UNIQUE KEY uk_homepage_featured_group_article_sort_order (group_id, sort_order),
    CONSTRAINT chk_homepage_featured_group_article_sort_order
        CHECK (sort_order BETWEEN 0 AND 2),
    CONSTRAINT fk_homepage_featured_group_article_group
        FOREIGN KEY (group_id) REFERENCES homepage_featured_group (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_homepage_featured_group_article_blog
        FOREIGN KEY (blog_id) REFERENCES blog (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO homepage_featured_group (title, sort_order)
SELECT '精选文章', 0
FROM DUAL
WHERE (
    SELECT COUNT(*)
    FROM blog
    WHERE is_published = true
      AND is_internal = false
      AND deleted_at IS NULL
) >= 3;

INSERT INTO homepage_featured_group_article (group_id, blog_id, sort_order)
SELECT featured_group.id, ranked_blog.id, ranked_blog.sort_order
FROM homepage_featured_group AS featured_group
JOIN (
    SELECT id,
           ROW_NUMBER() OVER (
               ORDER BY is_top DESC, is_recommend DESC, update_time DESC, id DESC
           ) - 1 AS sort_order
    FROM blog
    WHERE is_published = true
      AND is_internal = false
      AND deleted_at IS NULL
) AS ranked_blog ON ranked_blog.sort_order BETWEEN 0 AND 2
WHERE featured_group.sort_order = 0;
