-- Author: huangbingrui.awa

ALTER TABLE competition_award
    DROP CHECK chk_competition_award_rank,
    MODIFY COLUMN rank_position int unsigned NULL,
    MODIFY COLUMN rank_total int unsigned NULL,
    ADD CONSTRAINT chk_competition_award_rank CHECK (
        (rank_position IS NULL AND rank_total IS NULL)
        OR (
            rank_position IS NOT NULL
            AND rank_total IS NOT NULL
            AND rank_position > 0
            AND rank_total > 0
            AND rank_position <= rank_total
        )
    );

ALTER TABLE competition_award_recipient
    ADD COLUMN profile_sort_order bigint unsigned NULL AFTER profile_visible;

UPDATE competition_award_recipient
SET profile_sort_order = award_id
WHERE profile_visible = true;

ALTER TABLE competition_award_recipient
    ADD CONSTRAINT chk_competition_award_recipient_profile_order CHECK (
        (profile_visible = false AND profile_sort_order IS NULL)
        OR (profile_visible = true AND profile_sort_order IS NOT NULL)
    );

INSERT INTO competition_type_tag (competition_id, type)
SELECT c.id, 'ASIA_REGIONAL'
FROM competition c
WHERE c.full_name = '第 50 届 ICPC 亚洲区域赛（西安）'
  AND NOT EXISTS (
      SELECT 1 FROM competition_type_tag ctt
      WHERE ctt.competition_id = c.id AND ctt.type = 'ASIA_REGIONAL'
  );
