-- TotemGuardV3 schema — MySQL / MariaDB dialect.
--
-- Player, server and check names are stored once in their respective catalog
-- tables and referenced by small surrogate integer ids everywhere else. This
-- keeps the hot tables (tg_alerts, tg_sessions) narrow — 4-byte ints instead
-- of 16-byte BINARY UUIDs per row — which matters when millions of alert rows
-- accumulate across a fleet of servers.
--
-- Timestamps are BIGINT epoch milliseconds so range scans and retention
-- sweeps are index-friendly without timezone ambiguity.

CREATE TABLE IF NOT EXISTS tg_schema_version (
    id           TINYINT UNSIGNED NOT NULL,
    version      INT UNSIGNED NOT NULL,
    applied_at   BIGINT NOT NULL,
    PRIMARY KEY (id),
    CHECK (id = 1)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin;

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

CREATE TABLE IF NOT EXISTS tg_sessions (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    player_id       INT UNSIGNED NOT NULL,
    server_id       SMALLINT UNSIGNED NOT NULL,
    name            VARCHAR(16) NOT NULL,
    client_brand    VARCHAR(128),
    client_version  SMALLINT,
    started_at      BIGINT NOT NULL,
    ended_at        BIGINT,
    PRIMARY KEY (id),
    KEY idx_tg_sessions_player_started (player_id, started_at),
    KEY idx_tg_sessions_server_started (server_id, started_at),
    CONSTRAINT fk_tg_sessions_player FOREIGN KEY (player_id) REFERENCES tg_players(id) ON DELETE CASCADE,
    CONSTRAINT fk_tg_sessions_server FOREIGN KEY (server_id) REFERENCES tg_servers(id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin;

CREATE TABLE IF NOT EXISTS tg_alerts (
    id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    session_id        BIGINT UNSIGNED,
    player_id         INT UNSIGNED NOT NULL,
    server_id         SMALLINT UNSIGNED NOT NULL,
    check_id          SMALLINT UNSIGNED NOT NULL,
    violations        INT UNSIGNED NOT NULL,
    debug             VARCHAR(512),
    -- Per-alert context captured from the player at the moment of the flag.
    -- Nullable so early-login flags that fire before the ping sampler has
    -- warmed up don't fabricate misleading numbers.
    keepalive_ping    SMALLINT UNSIGNED,
    transaction_ping  SMALLINT UNSIGNED,
    created_at        BIGINT NOT NULL,
    PRIMARY KEY (id),
    KEY idx_tg_alerts_player_created (player_id, created_at),
    KEY idx_tg_alerts_check_created  (check_id, created_at),
    KEY idx_tg_alerts_server_created (server_id, created_at),
    KEY idx_tg_alerts_session        (session_id),
    KEY idx_tg_alerts_created        (created_at),
    CONSTRAINT fk_tg_alerts_session FOREIGN KEY (session_id) REFERENCES tg_sessions(id) ON DELETE SET NULL,
    CONSTRAINT fk_tg_alerts_player  FOREIGN KEY (player_id)  REFERENCES tg_players(id)  ON DELETE CASCADE,
    CONSTRAINT fk_tg_alerts_check   FOREIGN KEY (check_id)   REFERENCES tg_checks(id),
    CONSTRAINT fk_tg_alerts_server  FOREIGN KEY (server_id)  REFERENCES tg_servers(id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin;

-- Punishments executed by TotemGuard. Never swept by retention — this is
-- permanent moderation history used by /tg profile and mod-kick tooling.
-- Each row captures the *intent* (type: 0=GENERIC, 1=KICK, 2=BAN)
-- alongside the expanded command string that was actually dispatched.
CREATE TABLE IF NOT EXISTS tg_punishments (
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    session_id   BIGINT UNSIGNED,
    player_id    INT UNSIGNED NOT NULL,
    server_id    SMALLINT UNSIGNED NOT NULL,
    check_id     SMALLINT UNSIGNED NOT NULL,
    type         TINYINT UNSIGNED NOT NULL,
    command      VARCHAR(512) NOT NULL,
    debug        VARCHAR(512),
    created_at   BIGINT NOT NULL,
    PRIMARY KEY (id),
    KEY idx_tg_punishments_player_created (player_id, created_at),
    KEY idx_tg_punishments_check_created  (check_id, created_at),
    KEY idx_tg_punishments_server_created (server_id, created_at),
    KEY idx_tg_punishments_session        (session_id),
    CONSTRAINT fk_tg_punishments_session FOREIGN KEY (session_id) REFERENCES tg_sessions(id) ON DELETE SET NULL,
    CONSTRAINT fk_tg_punishments_player  FOREIGN KEY (player_id)  REFERENCES tg_players(id)  ON DELETE CASCADE,
    CONSTRAINT fk_tg_punishments_check   FOREIGN KEY (check_id)   REFERENCES tg_checks(id),
    CONSTRAINT fk_tg_punishments_server  FOREIGN KEY (server_id)  REFERENCES tg_servers(id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin;

-- Staff toggle preferences are intentionally decoupled from tg_players so
-- staff can opt in on a fresh install before they've ever been seen here.
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
