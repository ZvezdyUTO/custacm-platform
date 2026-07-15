-- Author: huangbingrui.awa

ALTER TABLE competition
    DROP CHECK chk_competition_year,
    MODIFY COLUMN competition_year smallint unsigned NULL,
    ADD COLUMN competition_date date NULL AFTER competition_year,
    ADD CONSTRAINT chk_competition_year
        CHECK (competition_year IS NULL OR competition_year BETWEEN 1900 AND 9999),
    ADD CONSTRAINT chk_competition_date_year
        CHECK (
            competition_date IS NULL
            OR (competition_year IS NOT NULL AND YEAR(competition_date) = competition_year)
        );
