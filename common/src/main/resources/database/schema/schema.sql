CREATE TABLE IF NOT EXISTS tg_servers (
    id   SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name VARCHAR(64)       NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tg_servers_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin;

CREATE TABLE IF NOT EXISTS tg_checks (
    id   SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name VARCHAR(64)       NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tg_checks_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin;

CREATE TABLE IF NOT EXISTS tg_client_brands (
    id   INT UNSIGNED NOT NULL AUTO_INCREMENT,
    name VARCHAR(64)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tg_client_brands_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin;

CREATE TABLE IF NOT EXISTS tg_debug_messages (
    id       INT UNSIGNED NOT NULL AUTO_INCREMENT,
    template VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tg_debug_messages_template (template)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin;

CREATE TABLE IF NOT EXISTS tg_punishment_commands (
    id      INT UNSIGNED NOT NULL AUTO_INCREMENT,
    command VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tg_punishment_commands_command (command)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin;

CREATE TABLE IF NOT EXISTS tg_players (
    id              INT UNSIGNED NOT NULL AUTO_INCREMENT,
    uuid            BINARY(16)   NOT NULL,
    last_name       VARCHAR(16)  NOT NULL,
    last_name_lower VARCHAR(16) AS (LOWER(last_name)) STORED,
    first_seen      INT UNSIGNED NOT NULL,
    last_seen       INT UNSIGNED NOT NULL,
    last_flagged_at INT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tg_players_uuid (uuid),
    KEY idx_tg_players_last_name_lower (last_name_lower),
    KEY idx_tg_players_last_seen       (last_seen),
    KEY idx_tg_players_last_flagged_at (last_flagged_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin;

CREATE TABLE IF NOT EXISTS tg_profiles (
    id             BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT,
    player_id      INT UNSIGNED      NOT NULL,
    server_id      SMALLINT UNSIGNED NOT NULL,
    brand_id       INT UNSIGNED      NOT NULL,
    client_version SMALLINT          NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tg_profiles_identity (player_id, server_id, brand_id, client_version),
    CONSTRAINT fk_tg_profiles_player FOREIGN KEY (player_id) REFERENCES tg_players(id)       ON DELETE CASCADE,
    CONSTRAINT fk_tg_profiles_server FOREIGN KEY (server_id) REFERENCES tg_servers(id),
    CONSTRAINT fk_tg_profiles_brand  FOREIGN KEY (brand_id)  REFERENCES tg_client_brands(id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin;

CREATE TABLE IF NOT EXISTS tg_alerts (
    id          BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT,
    profile_id  BIGINT UNSIGNED   NOT NULL,
    player_id   INT UNSIGNED      NOT NULL,
    check_id    SMALLINT UNSIGNED NOT NULL,
    debug_id    INT UNSIGNED      NULL,
    debug_args  VARCHAR(255)      NULL,
    created_at  INT UNSIGNED      NOT NULL,
    PRIMARY KEY (id),
    KEY idx_tg_alerts_player_created       (player_id, created_at),
    KEY idx_tg_alerts_player_check_created (player_id, check_id, created_at),
    KEY idx_tg_alerts_created_at           (created_at),
    CONSTRAINT fk_tg_alerts_profile FOREIGN KEY (profile_id) REFERENCES tg_profiles(id)         ON DELETE CASCADE,
    CONSTRAINT fk_tg_alerts_player  FOREIGN KEY (player_id)  REFERENCES tg_players(id)          ON DELETE CASCADE,
    CONSTRAINT fk_tg_alerts_check   FOREIGN KEY (check_id)   REFERENCES tg_checks(id),
    CONSTRAINT fk_tg_alerts_debug   FOREIGN KEY (debug_id)   REFERENCES tg_debug_messages(id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin;

CREATE TABLE IF NOT EXISTS tg_punishments (
    id           BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT,
    profile_id   BIGINT UNSIGNED   NOT NULL,
    player_id    INT UNSIGNED      NOT NULL,
    check_id     SMALLINT UNSIGNED NOT NULL,
    type         TINYINT UNSIGNED  NOT NULL,
    command_id   INT UNSIGNED      NOT NULL,
    command_args VARCHAR(255)      NULL,
    debug_id     INT UNSIGNED      NULL,
    debug_args   VARCHAR(255)      NULL,
    created_at   INT UNSIGNED      NOT NULL,
    PRIMARY KEY (id),
    KEY idx_tg_punishments_player_created (player_id, created_at),
    CONSTRAINT fk_tg_punishments_profile FOREIGN KEY (profile_id) REFERENCES tg_profiles(id)            ON DELETE CASCADE,
    CONSTRAINT fk_tg_punishments_player  FOREIGN KEY (player_id)  REFERENCES tg_players(id)             ON DELETE CASCADE,
    CONSTRAINT fk_tg_punishments_check   FOREIGN KEY (check_id)   REFERENCES tg_checks(id),
    CONSTRAINT fk_tg_punishments_command FOREIGN KEY (command_id) REFERENCES tg_punishment_commands(id),
    CONSTRAINT fk_tg_punishments_debug   FOREIGN KEY (debug_id)   REFERENCES tg_debug_messages(id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin;

CREATE TABLE IF NOT EXISTS tg_stats_daily (
    day_epoch    INT UNSIGNED NOT NULL,
    alerts       INT UNSIGNED NOT NULL DEFAULT 0,
    punishments  INT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (day_epoch)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin;

CREATE TABLE IF NOT EXISTS tg_staff_alert_prefs (
    player_uuid    BINARY(16)       NOT NULL,
    alerts_enabled TINYINT UNSIGNED NOT NULL,
    local_only     TINYINT UNSIGNED NOT NULL DEFAULT 0,
    updated_at     INT UNSIGNED     NOT NULL,
    PRIMARY KEY (player_uuid)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin;

CREATE TABLE IF NOT EXISTS tg_vpn_cache (
    ip_hash   BINARY(32)       NOT NULL,
    is_vpn    TINYINT UNSIGNED NOT NULL,
    cached_at INT UNSIGNED     NOT NULL,
    PRIMARY KEY (ip_hash),
    KEY idx_tg_vpn_cache_cached_at (cached_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin;
