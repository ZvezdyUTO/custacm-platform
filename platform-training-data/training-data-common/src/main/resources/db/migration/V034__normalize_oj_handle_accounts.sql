-- Author: huangbingrui.awa

-- MySQL DDL is non-transactional. A repaired retry first removes only the two
-- V034-owned tables, then validates all legacy JSON and handle uniqueness in a
-- temporary table before creating persistent structures.
DROP TABLE IF EXISTS oj_handle_binding;
DROP TABLE IF EXISTS training_member;
DROP TEMPORARY TABLE IF EXISTS tmp_oj_handle_binding_v034;

CREATE TEMPORARY TABLE tmp_oj_handle_binding_v034 (
    username varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    oj_name varchar(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    handle varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    last_collected_at datetime(6) NULL,
    created_at datetime(6) NOT NULL,
    updated_at datetime(6) NOT NULL,
    PRIMARY KEY (username, oj_name),
    UNIQUE KEY uk_tmp_oj_handle_binding_oj_name_handle (oj_name, handle)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci;

INSERT INTO tmp_oj_handle_binding_v034 (
    username,
    oj_name,
    handle,
    last_collected_at,
    created_at,
    updated_at
)
SELECT legacy.username,
       'CODEFORCES',
       trim(legacy.handle),
       CASE
           WHEN legacy.last_collected_at IS NULL OR legacy.last_collected_at = '' THEN NULL
           ELSE CONVERT_TZ(
                   CAST(REPLACE(REPLACE(legacy.last_collected_at, 'T', ' '), 'Z', '') AS datetime(6)),
                   '+00:00',
                   '+08:00'
                )
       END,
       legacy.created_at,
       legacy.updated_at
FROM (
    SELECT username,
           NULLIF(JSON_UNQUOTE(JSON_EXTRACT(handles_json, '$.CODEFORCES')), 'null') AS handle,
           NULLIF(JSON_UNQUOTE(JSON_EXTRACT(
                   NULLIF(collection_states_json, ''),
                   '$.CODEFORCES.lastCollectedAt'
           )), 'null') AS last_collected_at,
           created_at,
           updated_at
    FROM oj_handle_account
) legacy
WHERE legacy.handle IS NOT NULL AND trim(legacy.handle) <> '';

INSERT INTO tmp_oj_handle_binding_v034 (
    username,
    oj_name,
    handle,
    last_collected_at,
    created_at,
    updated_at
)
SELECT legacy.username,
       'ATCODER',
       trim(legacy.handle),
       CASE
           WHEN legacy.last_collected_at IS NULL OR legacy.last_collected_at = '' THEN NULL
           ELSE CONVERT_TZ(
                   CAST(REPLACE(REPLACE(legacy.last_collected_at, 'T', ' '), 'Z', '') AS datetime(6)),
                   '+00:00',
                   '+08:00'
                )
       END,
       legacy.created_at,
       legacy.updated_at
FROM (
    SELECT username,
           NULLIF(JSON_UNQUOTE(JSON_EXTRACT(handles_json, '$.ATCODER')), 'null') AS handle,
           NULLIF(JSON_UNQUOTE(JSON_EXTRACT(
                   NULLIF(collection_states_json, ''),
                   '$.ATCODER.lastCollectedAt'
           )), 'null') AS last_collected_at,
           created_at,
           updated_at
    FROM oj_handle_account
) legacy
WHERE legacy.handle IS NOT NULL AND trim(legacy.handle) <> '';

CREATE TABLE training_member (
    username varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    need_collect boolean NOT NULL DEFAULT true,
    created_at datetime(6) NOT NULL DEFAULT current_timestamp(6),
    updated_at datetime(6) NOT NULL DEFAULT current_timestamp(6),
    PRIMARY KEY (username),
    CONSTRAINT fk_training_member_user
        FOREIGN KEY (username) REFERENCES `user` (username)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci;

INSERT INTO training_member (username, need_collect, created_at, updated_at)
SELECT username, need_collect, created_at, updated_at
FROM oj_handle_account;

CREATE TABLE oj_handle_binding (
    username varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    oj_name varchar(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    handle varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    last_collected_at datetime(6) NULL,
    created_at datetime(6) NOT NULL DEFAULT current_timestamp(6),
    updated_at datetime(6) NOT NULL DEFAULT current_timestamp(6),
    PRIMARY KEY (username, oj_name),
    UNIQUE KEY uk_oj_handle_binding_oj_name_handle (oj_name, handle),
    CONSTRAINT chk_oj_handle_binding_oj_name CHECK (oj_name IN ('CODEFORCES', 'ATCODER')),
    CONSTRAINT fk_oj_handle_binding_training_member
        FOREIGN KEY (username) REFERENCES training_member (username)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci;

INSERT INTO oj_handle_binding (
    username,
    oj_name,
    handle,
    last_collected_at,
    created_at,
    updated_at
)
SELECT username,
       oj_name,
       handle,
       last_collected_at,
       created_at,
       updated_at
FROM tmp_oj_handle_binding_v034;

DROP TEMPORARY TABLE tmp_oj_handle_binding_v034;
