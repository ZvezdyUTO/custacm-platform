ALTER TABLE blog
    ADD CONSTRAINT fk_blog_category
        FOREIGN KEY (category_id) REFERENCES category (id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT;
