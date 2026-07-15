ALTER TABLE comment
    ADD INDEX idx_comment_scope_parent_published_created
        (page, blog_id, parent_comment_id, is_published, create_time DESC, id DESC);
