-- Author: huangbingrui.awa

CREATE TABLE competition (
    id bigint NOT NULL AUTO_INCREMENT,
    full_name varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    active_full_name varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
    competition_year smallint unsigned NOT NULL,
    participation_mode varchar(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    create_time datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at datetime(0) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_competition_active_full_name (active_full_name),
    KEY idx_competition_public_filter (deleted_at, competition_year, id),
    CONSTRAINT chk_competition_year
        CHECK (competition_year BETWEEN 1900 AND 9999),
    CONSTRAINT chk_competition_participation_mode
        CHECK (participation_mode IN ('INDIVIDUAL', 'TEAM', 'MIXED')),
    CONSTRAINT chk_competition_active_full_name
        CHECK (
            (deleted_at IS NULL AND active_full_name IS NOT NULL AND active_full_name = full_name)
            OR (deleted_at IS NOT NULL AND active_full_name IS NULL)
        )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE competition_type_tag (
    competition_id bigint NOT NULL,
    type varchar(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    PRIMARY KEY (competition_id, type),
    KEY idx_competition_type_tag_type (type, competition_id),
    CONSTRAINT chk_competition_type_tag_type CHECK (type IN (
        'ICPC',
        'CCPC',
        'PROVINCIAL',
        'INVITATIONAL',
        'NATIONAL_SITE',
        'ASIA_REGIONAL',
        'ASIA_EAST_CONTINENT_FINAL',
        'NATIONAL_FINAL',
        'WORLD_FINAL',
        'LANQIAO_CUP',
        'BAIDU_STAR',
        'GPLT',
        'OTHER'
    )),
    CONSTRAINT fk_competition_type_tag_competition
        FOREIGN KEY (competition_id) REFERENCES competition (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE competition_participant (
    id bigint NOT NULL AUTO_INCREMENT,
    competition_id bigint NOT NULL,
    username varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
    display_name_snapshot varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    create_time datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_competition_participant_id_competition (id, competition_id),
    UNIQUE KEY uk_competition_participant_competition_username (competition_id, username),
    KEY idx_competition_participant_username (username, competition_id),
    CONSTRAINT fk_competition_participant_competition
        FOREIGN KEY (competition_id) REFERENCES competition (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_competition_participant_user
        FOREIGN KEY (username) REFERENCES `user` (username)
        ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE competition_award (
    id bigint NOT NULL AUTO_INCREMENT,
    competition_id bigint NOT NULL,
    award_mode varchar(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    team_name varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
    award_scope varchar(16) CHARACTER SET ascii COLLATE ascii_bin NULL,
    award_level tinyint unsigned NOT NULL,
    rank_position int unsigned NOT NULL,
    rank_total int unsigned NOT NULL,
    award_name varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
    create_time datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_competition_award_id_competition (id, competition_id),
    KEY idx_competition_award_competition (competition_id, id),
    CONSTRAINT chk_competition_award_mode
        CHECK (award_mode IN ('INDIVIDUAL', 'TEAM')),
    CONSTRAINT chk_competition_award_scope
        CHECK (award_scope IS NULL OR award_scope IN ('PROVINCIAL', 'NATIONAL')),
    CONSTRAINT chk_competition_award_level
        CHECK (award_level BETWEEN 1 AND 4),
    CONSTRAINT chk_competition_award_rank
        CHECK (rank_position > 0 AND rank_total > 0 AND rank_position <= rank_total),
    CONSTRAINT fk_competition_award_competition
        FOREIGN KEY (competition_id) REFERENCES competition (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE competition_award_recipient (
    competition_id bigint NOT NULL,
    award_id bigint NOT NULL,
    participant_id bigint NOT NULL,
    profile_visible boolean NOT NULL DEFAULT false,
    PRIMARY KEY (award_id, participant_id),
    KEY idx_competition_award_recipient_participant (participant_id, award_id),
    KEY idx_competition_award_recipient_competition (competition_id, award_id),
    CONSTRAINT chk_competition_award_recipient_profile_visible
        CHECK (profile_visible IN (false, true)),
    CONSTRAINT fk_competition_award_recipient_award
        FOREIGN KEY (award_id, competition_id) REFERENCES competition_award (id, competition_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_competition_award_recipient_participant
        FOREIGN KEY (participant_id, competition_id) REFERENCES competition_participant (id, competition_id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE competition_article (
    participant_id bigint NOT NULL,
    blog_id bigint NOT NULL,
    create_time datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (participant_id, blog_id),
    KEY idx_competition_article_blog (blog_id, participant_id),
    CONSTRAINT fk_competition_article_participant
        FOREIGN KEY (participant_id) REFERENCES competition_participant (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_competition_article_blog
        FOREIGN KEY (blog_id) REFERENCES blog (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
