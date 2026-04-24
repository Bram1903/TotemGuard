/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2026 Bram and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.deathmotion.totemguard.common.database;

/**
 * Central home for the non-trivial MySQL query strings used across DAOs.
 *
 * <p>Ad-hoc single-line SELECTs stay inline with their DAO; upserts and the
 * alert batch insert live here so the exact SQL is easy to find and tune.</p>
 */
public final class Sql {

    public static final String SCHEMA_RESOURCE_PATH = "database/schema/schema.sql";

    public static final String UPSERT_SERVER =
            "INSERT INTO tg_servers (name) VALUES (?) " +
                    "ON DUPLICATE KEY UPDATE name = VALUES(name)";

    public static final String UPSERT_CHECK =
            "INSERT INTO tg_checks (name) VALUES (?) " +
                    "ON DUPLICATE KEY UPDATE name = VALUES(name)";

    public static final String UPSERT_PLAYER =
            "INSERT INTO tg_players (uuid, last_name, first_seen, last_seen) " +
                    "VALUES (?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "  last_name = VALUES(last_name), " +
                    "  last_seen = VALUES(last_seen)";

    public static final String UPSERT_STAFF_ALERT_PREF =
            "INSERT INTO tg_staff_alert_prefs (player_uuid, alerts_enabled, updated_at) " +
                    "VALUES (?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "  alerts_enabled = VALUES(alerts_enabled), " +
                    "  updated_at = VALUES(updated_at)";

    public static final String SELECT_STAFF_ALERT_PREF =
            "SELECT alerts_enabled FROM tg_staff_alert_prefs WHERE player_uuid = ?";

    public static final String UPSERT_VPN_CACHE =
            "INSERT INTO tg_vpn_cache (ip_hash, is_vpn, provider, response, cached_at, expires_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "  is_vpn     = VALUES(is_vpn), " +
                    "  provider   = VALUES(provider), " +
                    "  response   = VALUES(response), " +
                    "  cached_at  = VALUES(cached_at), " +
                    "  expires_at = VALUES(expires_at)";

    public static final String INSERT_ALERT =
            "INSERT INTO tg_alerts " +
                    "(session_id, player_id, server_id, check_id, violations, debug, " +
                    " keepalive_ping, transaction_ping, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

    public static final String INSERT_PUNISHMENT =
            "INSERT INTO tg_punishments " +
                    "(session_id, player_id, server_id, check_id, type, command, debug, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    public static final String INSERT_SESSION =
            "INSERT INTO tg_sessions " +
                    "(player_id, server_id, name, client_brand, client_version, started_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";

    public static final String DELETE_OLD_ALERTS =
            "DELETE FROM tg_alerts WHERE created_at < ? LIMIT ?";

    public static final String DELETE_OLD_VPN_CACHE =
            "DELETE FROM tg_vpn_cache WHERE cached_at < ? LIMIT ?";

    // History views — joined against tg_checks/tg_servers to surface names
    // alongside ids. Filter by uuid so callers don't need to resolve the
    // surrogate player id first.
    public static final String SELECT_ALERTS_BY_UUID =
            "SELECT a.id, c.name AS check_name, s.name AS server_name, " +
                    "       a.violations, a.debug, " +
                    "       a.keepalive_ping, a.transaction_ping, " +
                    "       sess.client_brand, sess.client_version, " +
                    "       a.created_at " +
                    "FROM tg_alerts a " +
                    "JOIN tg_players p ON a.player_id = p.id " +
                    "JOIN tg_checks  c ON a.check_id  = c.id " +
                    "JOIN tg_servers s ON a.server_id = s.id " +
                    "LEFT JOIN tg_sessions sess ON a.session_id = sess.id " +
                    "WHERE p.uuid = ? " +
                    "ORDER BY a.created_at DESC " +
                    "LIMIT ? OFFSET ?";

    public static final String COUNT_ALERTS_BY_UUID =
            "SELECT COUNT(*) FROM tg_alerts a " +
                    "JOIN tg_players p ON a.player_id = p.id " +
                    "WHERE p.uuid = ?";

    public static final String SELECT_ALERT_CHECK_SUMMARIES_BY_UUID =
            "SELECT c.name AS check_name, COUNT(*) AS alert_count " +
                    "FROM tg_alerts a " +
                    "JOIN tg_players p ON a.player_id = p.id " +
                    "JOIN tg_checks  c ON a.check_id  = c.id " +
                    "WHERE p.uuid = ? " +
                    "GROUP BY c.name " +
                    "ORDER BY c.name ASC";

    public static final String SELECT_ALERTS_BY_UUID_CHECK =
            "SELECT a.id, c.name AS check_name, s.name AS server_name, " +
                    "       a.violations, a.debug, " +
                    "       a.keepalive_ping, a.transaction_ping, " +
                    "       sess.client_brand, sess.client_version, " +
                    "       a.created_at " +
                    "FROM tg_alerts a " +
                    "JOIN tg_players p ON a.player_id = p.id " +
                    "JOIN tg_checks  c ON a.check_id  = c.id " +
                    "JOIN tg_servers s ON a.server_id = s.id " +
                    "LEFT JOIN tg_sessions sess ON a.session_id = sess.id " +
                    "WHERE p.uuid = ? AND c.name = ? " +
                    "ORDER BY a.created_at DESC " +
                    "LIMIT ? OFFSET ?";

    public static final String COUNT_ALERTS_BY_UUID_CHECK =
            "SELECT COUNT(*) FROM tg_alerts a " +
                    "JOIN tg_players p ON a.player_id = p.id " +
                    "JOIN tg_checks  c ON a.check_id  = c.id " +
                    "WHERE p.uuid = ? AND c.name = ?";

    public static final String SELECT_PUNISHMENTS_BY_UUID =
            "SELECT pu.id, c.name AS check_name, s.name AS server_name, " +
                    "       pu.type, pu.command, pu.debug, pu.created_at " +
                    "FROM tg_punishments pu " +
                    "JOIN tg_players p ON pu.player_id = p.id " +
                    "JOIN tg_checks  c ON pu.check_id  = c.id " +
                    "JOIN tg_servers s ON pu.server_id = s.id " +
                    "WHERE p.uuid = ? " +
                    "ORDER BY pu.created_at DESC " +
                    "LIMIT ? OFFSET ?";

    public static final String COUNT_PUNISHMENTS_BY_UUID =
            "SELECT COUNT(*) FROM tg_punishments pu " +
                    "JOIN tg_players p ON pu.player_id = p.id " +
                    "WHERE p.uuid = ?";

    private Sql() {
    }
}
