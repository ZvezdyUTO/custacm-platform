ALTER TABLE blog_tag
    ADD CONSTRAINT fk_blog_tag_blog
        FOREIGN KEY (blog_id) REFERENCES blog (id)
        ON UPDATE CASCADE
        ON DELETE CASCADE;
