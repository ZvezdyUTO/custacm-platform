ALTER TABLE tag
    ADD CONSTRAINT uk_tag_name UNIQUE (tag_name);
