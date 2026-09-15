ALTER TABLE category
    ADD COLUMN description varchar(200) NOT NULL DEFAULT '' AFTER color;
