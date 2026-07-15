-- Author: huangbingrui.awa

ALTER TABLE competition_award
    ADD COLUMN requires_login boolean NOT NULL DEFAULT false AFTER award_name,
    ADD CONSTRAINT chk_competition_award_requires_login
        CHECK (requires_login IN (false, true));
