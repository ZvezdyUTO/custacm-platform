ALTER TABLE blog_tag
    ADD INDEX idx_blog_tag_tag_id_blog_id (tag_id, blog_id);
