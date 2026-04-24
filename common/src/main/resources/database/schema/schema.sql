-- TotemGuardV3 schema — MySQL / MariaDB.
-- Timestamps are BIGINT epoch milliseconds.

CREATE TABLE IF NOT EXISTS tg_servers (
    id           SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name         VARCHAR(64) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tg_servers_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin;

CREATE TABLE IF NOT EXISTS tg_checks (
    id           SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name         VARCHAR(64) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tg_checks_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin;

CREATE TABLE IF NOT EXISTS tg_players (
    id           INT UNSIGNED NOT NULL AUTO_INCREMENT,
    uuid         BINARY(16)   NOT NULL,
    last_name    VARCHAR(16)  NOT NULL,
    first_seen   BIGINT NOT NULL,
    last_seen    BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tg_players_uuid (uuid),
    KEY idx_tg_players_last_name (last_name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin;

-- A profile is the <player, server, client brand, client version> tuple.
-- Reused across rejoins so the same connection context only stores one row.
CREATE TABLE IF NOT EXISTS tg_profiles (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    player_id       INT UNSIGNED NOT NULL,
    server_id       SMALLINT UNSIGNED NOT NULL,
    client_brand    VARCHAR(64),
    client_version  SMALLINT,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tg_profiles_identity (player_id, server_id, client_brand, client_version),
    CONSTRAINT fk_tg_profiles_player FOREIGN KEY (player_id) REFERENCES tg_players(id) ON DELETE CASCADE,
    CONSTRAINT fk_tg_profiles_server FOREIGN KEY (server_id) REFERENCES tg_servers(id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin;

CREATE TABLE IF NOT EXISTS tg_alerts (
    id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    profile_id        BIGINT UNSIGNED,
    player_id         INT UNSIGNED NOT NULL,
    server_id         SMALLINT UNSIGNED NOT NULL,
    check_id          SMALLINT UNSIGNED NOT NULL,
    violations        INT UNSIGNED NOT NULL,
    debug             VARCHAR(128),
    keepalive_ping    SMALLINT UNSIGNED,
    transaction_ping  SMALLINT UNSIGNED,
    created_at        BIGINT NOT NULL,
    PRIMARY KEY (id),
    KEY idx_tg_alerts_player_created (player_id, created_at),
    KEY idx_tg_alerts_check_created  (check_id, created_at),
    KEY idx_tg_alerts_server_created (server_id, created_at),
    KEY idx_tg_alerts_profile        (profile_id),
    KEY idx_tg_alerts_created        (created_at),
    CONSTRAINT fk_tg_alerts_profile FOREIGN KEY (profile_id) REFERENCES tg_profiles(id) ON DELETE SET NULL,
    CONSTRAINT fk_tg_alerts_player  FOREIGN KEY (player_id)  REFERENCES tg_players(id)  ON DELETE CASCADE,
    CONSTRAINT fk_tg_alerts_check   FOREIGN KEY (check_id)   REFERENCES tg_checks(id),
    CONSTRAINT fk_tg_alerts_server  FOREIGN KEY (server_id)  REFERENCES tg_servers(id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin;

-- type: 0=GENERIC (not persisted), 1=KICK, 2=BAN. Never swept.
CREATE TABLE IF NOT EXISTS tg_punishments (
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    profile_id   BIGINT UNSIGNED,
    player_id    INT UNSIGNED NOT NULL,
    server_id    SMALLINT UNSIGNED NOT NULL,
    check_id     SMALLINT UNSIGNED NOT NULL,
    type         TINYINT UNSIGNED NOT NULL,
    command      VARCHAR(256) NOT NULL,
    debug        VARCHAR(128),
    created_at   BIGINT NOT NULL,
    PRIMARY KEY (id),
    KEY idx_tg_punishments_player_created (player_id, created_at),
    KEY idx_tg_punishments_check_created  (check_id, created_at),
    KEY idx_tg_punishments_server_created (server_id, created_at),
    KEY idx_tg_punishments_profile        (profile_id),
    CONSTRAINT fk_tg_punishments_profile FOREIGN KEY (profile_id) REFERENCES tg_profiles(id) ON DELETE SET NULL,
    CONSTRAINT fk_tg_punishments_player  FOREIGN KEY (player_id)  REFERENCES tg_players(id)  ON DELETE CASCADE,
    CONSTRAINT fk_tg_punishments_check   FOREIGN KEY (check_id)   REFERENCES tg_checks(id),
    CONSTRAINT fk_tg_punishments_server  FOREIGN KEY (server_id)  REFERENCES tg_servers(id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin;

-- Decoupled from tg_players so staff prefs survive DB resets.
CREATE TABLE IF NOT EXISTS tg_staff_alert_prefs (
    player_uuid     BINARY(16) NOT NULL,
    alerts_enabled  TINYINT UNSIGNED NOT NULL,
    updated_at      BIGINT NOT NULL,
    PRIMARY KEY (player_uuid)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin;

CREATE TABLE IF NOT EXISTS tg_vpn_cache (
    ip_hash       BINARY(32) NOT NULL,
    is_vpn        TINYINT UNSIGNED NOT NULL,
    provider      VARCHAR(64),
    response      VARCHAR(512),
    cached_at     BIGINT NOT NULL,
    expires_at    BIGINT NOT NULL,
    PRIMARY KEY (ip_hash),
    KEY idx_tg_vpn_cache_expires (expires_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin;
