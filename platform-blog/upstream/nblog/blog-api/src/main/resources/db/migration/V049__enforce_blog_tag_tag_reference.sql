ALTER TABLE blog_tag
    ADD CONSTRAINT fk_blog_tag_tag
        FOREIGN KEY (tag_id) REFERENCES tag (id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT;
