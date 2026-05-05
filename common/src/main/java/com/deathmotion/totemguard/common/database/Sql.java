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

public final class Sql {

    public static final String SCHEMA_RESOURCE_PATH = "database/schema/schema.sql";

    public static final String UPSERT_SERVER =
            "INSERT INTO tg_servers (name) VALUES (?) " +
                    "ON DUPLICATE KEY UPDATE name = VALUES(name)";
    public static final String SELECT_SERVER_ID = "SELECT id FROM tg_servers WHERE name = ?";
    public static final String UPSERT_CHECK =
            "INSERT INTO tg_checks (name) VALUES (?) " +
                    "ON DUPLICATE KEY UPDATE name = VALUES(name)";
    public static final String SELECT_CHECK_ID = "SELECT id FROM tg_checks WHERE name = ?";
    public static final String UPSERT_BRAND =
            "INSERT INTO tg_client_brands (name) VALUES (?) " +
                    "ON DUPLICATE KEY UPDATE name = VALUES(name)";
    public static final String SELECT_BRAND_ID = "SELECT id FROM tg_client_brands WHERE name = ?";
    public static final String UPSERT_DEBUG_TEMPLATE =
            "INSERT INTO tg_debug_messages (template) VALUES (?) " +
                    "ON DUPLICATE KEY UPDATE template = VALUES(template)";
    public static final String SELECT_DEBUG_TEMPLATE_ID =
            "SELECT id FROM tg_debug_messages WHERE template = ?";
    public static final String UPSERT_PUNISHMENT_COMMAND =
            "INSERT INTO tg_punishment_commands (command) VALUES (?) " +
                    "ON DUPLICATE KEY UPDATE command = VALUES(command)";
    public static final String SELECT_PUNISHMENT_COMMAND_ID =
            "SELECT id FROM tg_punishment_commands WHERE command = ?";
    public static final String UPSERT_PLAYER =
            "INSERT INTO tg_players (uuid, last_name, first_seen, last_seen) " +
                    "VALUES (?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE last_name = VALUES(last_name), last_seen = VALUES(last_seen)";
    public static final String SELECT_PLAYER_BY_NAME =
            "SELECT id, uuid, last_name, first_seen, last_seen FROM tg_players " +
                    "WHERE last_name_lower = LOWER(?) " +
                    "ORDER BY last_seen DESC LIMIT 1";
    public static final String SELECT_PLAYER_BY_UUID =
            "SELECT id, uuid, last_name, first_seen, last_seen FROM tg_players WHERE uuid = ? LIMIT 1";
    public static final String UPDATE_PLAYER_LAST_FLAGGED_AT =
            "UPDATE tg_players SET last_flagged_at = ? WHERE id = ? AND last_flagged_at < ?";
    public static final String SELECT_PROFILE_ID =
            "SELECT id FROM tg_profiles " +
                    "WHERE player_id = ? AND server_id = ? AND brand_id = ? AND client_version = ? LIMIT 1";
    public static final String INSERT_PROFILE =
            "INSERT INTO tg_profiles (player_id, server_id, brand_id, client_version) VALUES (?, ?, ?, ?)";
    public static final String UPSERT_STAFF_ALERT_PREF =
            "INSERT INTO tg_staff_alert_prefs (player_uuid, alerts_enabled, local_only, updated_at) " +
                    "VALUES (?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE alerts_enabled = VALUES(alerts_enabled), " +
                    "  local_only = VALUES(local_only), updated_at = VALUES(updated_at)";
    public static final String SELECT_STAFF_ALERT_PREF =
            "SELECT alerts_enabled, local_only FROM tg_staff_alert_prefs WHERE player_uuid = ?";
    public static final String UPSERT_VPN_CACHE =
            "INSERT INTO tg_vpn_cache (ip_hash, is_vpn, cached_at) VALUES (?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE is_vpn = VALUES(is_vpn), cached_at = VALUES(cached_at)";
    public static final String SELECT_VPN_CACHE =
            "SELECT is_vpn FROM tg_vpn_cache WHERE ip_hash = ? AND cached_at > ? LIMIT 1";
    public static final String DELETE_OLD_VPN_CACHE =
            "DELETE FROM tg_vpn_cache WHERE cached_at < ? LIMIT ?";
    public static final String INSERT_ALERT =
            "INSERT INTO tg_alerts " +
                    "(profile_id, player_id, check_id, debug_id, debug_args, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";
    public static final String DELETE_OLD_ALERTS =
            "DELETE FROM tg_alerts WHERE created_at < ? LIMIT ?";
    public static final String INSERT_PUNISHMENT =
            "INSERT INTO tg_punishments " +
                    "(profile_id, player_id, check_id, type, command_id, command_args, debug_id, debug_args, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    public static final String UPSERT_STATS_DAILY_ALERTS =
            "INSERT INTO tg_stats_daily (day_epoch, alerts) VALUES (?, ?) " +
                    "ON DUPLICATE KEY UPDATE alerts = alerts + VALUES(alerts)";
    public static final String UPSERT_STATS_DAILY_PUNISHMENTS =
            "INSERT INTO tg_stats_daily (day_epoch, punishments) VALUES (?, ?) " +
                    "ON DUPLICATE KEY UPDATE punishments = punishments + VALUES(punishments)";
    public static final String SUM_STATS_DAILY_ALL_TIME =
            "SELECT COALESCE(SUM(alerts), 0), COALESCE(SUM(punishments), 0) FROM tg_stats_daily";
    public static final String SUM_STATS_DAILY_SINCE =
            "SELECT COALESCE(SUM(alerts), 0), COALESCE(SUM(punishments), 0) FROM tg_stats_daily WHERE day_epoch >= ?";
    public static final String COUNT_PLAYERS_TOTAL = "SELECT COUNT(*) FROM tg_players";
    public static final String COUNT_PLAYERS_ACTIVE_SINCE =
            "SELECT COUNT(*) FROM tg_players WHERE last_seen >= ?";
    public static final String COUNT_PLAYERS_FLAGGED_TOTAL =
            "SELECT COUNT(*) FROM tg_players WHERE last_flagged_at > 0";
    public static final String COUNT_PLAYERS_FLAGGED_SINCE =
            "SELECT COUNT(*) FROM tg_players WHERE last_flagged_at >= ?";
    public static final String SELECT_TG_TABLE_SIZES =
            "SELECT table_name, table_rows, data_length, index_length " +
                    "FROM information_schema.tables " +
                    "WHERE table_schema = DATABASE() AND table_name LIKE 'tg\\_%'";
    private static final String PLAYER_ID_BY_UUID = "(SELECT id FROM tg_players WHERE uuid = ?)";
    public static final String DELETE_ALERTS_BY_UUID =
            "DELETE FROM tg_alerts WHERE player_id = " + PLAYER_ID_BY_UUID + " LIMIT ?";
    public static final String COUNT_ALERTS_BY_UUID =
            "SELECT COUNT(*) FROM tg_alerts WHERE player_id = " + PLAYER_ID_BY_UUID;
    public static final String SELECT_ALERT_CHECK_SUMMARIES_BY_UUID =
            "SELECT c.name AS check_name, COUNT(*) AS alert_count " +
                    "FROM tg_alerts a " +
                    "JOIN tg_checks c ON a.check_id = c.id " +
                    "WHERE a.player_id = " + PLAYER_ID_BY_UUID + " " +
                    "GROUP BY c.name ORDER BY c.name ASC";
    public static final String COUNT_ALERTS_BY_UUID_SINCE =
            "SELECT COUNT(*) FROM tg_alerts " +
                    "WHERE player_id = " + PLAYER_ID_BY_UUID + " AND created_at >= ?";
    public static final String DELETE_PUNISHMENTS_BY_UUID =
            "DELETE FROM tg_punishments WHERE player_id = " + PLAYER_ID_BY_UUID + " LIMIT ?";
    public static final String COUNT_PUNISHMENTS_BY_UUID =
            "SELECT COUNT(*) FROM tg_punishments WHERE player_id = " + PLAYER_ID_BY_UUID;
    public static final String COUNT_PUNISHMENTS_BY_UUID_SINCE =
            "SELECT COUNT(*) FROM tg_punishments " +
                    "WHERE player_id = " + PLAYER_ID_BY_UUID + " AND created_at >= ?";
    private static final String CHECK_ID_BY_NAME = "(SELECT id FROM tg_checks  WHERE name = ?)";
    public static final String COUNT_ALERTS_BY_UUID_CHECK =
            "SELECT COUNT(*) FROM tg_alerts " +
                    "WHERE player_id = " + PLAYER_ID_BY_UUID + " AND check_id = " + CHECK_ID_BY_NAME;
    public static final String COUNT_ALERTS_BY_UUID_CHECK_SINCE =
            "SELECT COUNT(*) FROM tg_alerts " +
                    "WHERE player_id = " + PLAYER_ID_BY_UUID +
                    "  AND check_id = " + CHECK_ID_BY_NAME +
                    "  AND created_at >= ?";
    private static final String ALERT_SELECT_COLUMNS =
            "a.id, c.name AS check_name, s.name AS server_name, " +
                    "d.template AS debug_template, a.debug_args, " +
                    "b.name AS client_brand, prof.client_version, a.created_at";
    private static final String ALERT_OUTER_JOIN =
            "JOIN tg_alerts a            ON a.id = page.id " +
                    "JOIN tg_profiles prof       ON a.profile_id = prof.id " +
                    "JOIN tg_servers s           ON prof.server_id = s.id " +
                    "JOIN tg_client_brands b     ON prof.brand_id  = b.id " +
                    "JOIN tg_checks c            ON a.check_id   = c.id " +
                    "LEFT JOIN tg_debug_messages d ON a.debug_id  = d.id ";
    public static final String SELECT_ALERTS_BY_UUID =
            "SELECT " + ALERT_SELECT_COLUMNS + " FROM (" +
                    "  SELECT id FROM tg_alerts " +
                    "  WHERE player_id = " + PLAYER_ID_BY_UUID + " " +
                    "  ORDER BY created_at DESC LIMIT ? OFFSET ?" +
                    ") page " + ALERT_OUTER_JOIN +
                    "ORDER BY a.created_at DESC";
    public static final String SELECT_ALERTS_BY_UUID_CHECK =
            "SELECT " + ALERT_SELECT_COLUMNS + " FROM (" +
                    "  SELECT id FROM tg_alerts " +
                    "  WHERE player_id = " + PLAYER_ID_BY_UUID +
                    "    AND check_id = " + CHECK_ID_BY_NAME + " " +
                    "  ORDER BY created_at DESC LIMIT ? OFFSET ?" +
                    ") page " + ALERT_OUTER_JOIN +
                    "ORDER BY a.created_at DESC";
    public static final String SELECT_ALERTS_BY_UUID_SINCE =
            "SELECT " + ALERT_SELECT_COLUMNS + " FROM (" +
                    "  SELECT id FROM tg_alerts " +
                    "  WHERE player_id = " + PLAYER_ID_BY_UUID +
                    "    AND created_at >= ? " +
                    "  ORDER BY created_at DESC LIMIT ? OFFSET ?" +
                    ") page " + ALERT_OUTER_JOIN +
                    "ORDER BY a.created_at DESC";
    public static final String SELECT_ALERTS_BY_UUID_CHECK_SINCE =
            "SELECT " + ALERT_SELECT_COLUMNS + " FROM (" +
                    "  SELECT id FROM tg_alerts " +
                    "  WHERE player_id = " + PLAYER_ID_BY_UUID +
                    "    AND check_id = " + CHECK_ID_BY_NAME +
                    "    AND created_at >= ? " +
                    "  ORDER BY created_at DESC LIMIT ? OFFSET ?" +
                    ") page " + ALERT_OUTER_JOIN +
                    "ORDER BY a.created_at DESC";

    private static final String PUNISHMENT_SELECT_COLUMNS =
            "pu.id, c.name AS check_name, s.name AS server_name, pu.type, " +
                    "cmd.command AS command_template, pu.command_args, " +
                    "d.template AS debug_template, pu.debug_args, pu.created_at";
    private static final String PUNISHMENT_OUTER_JOIN =
            "JOIN tg_punishments pu          ON pu.id = page.id " +
                    "JOIN tg_profiles prof           ON pu.profile_id = prof.id " +
                    "JOIN tg_servers s               ON prof.server_id = s.id " +
                    "JOIN tg_checks c                ON pu.check_id   = c.id " +
                    "JOIN tg_punishment_commands cmd ON pu.command_id = cmd.id " +
                    "LEFT JOIN tg_debug_messages d   ON pu.debug_id   = d.id ";
    public static final String SELECT_PUNISHMENTS_BY_UUID =
            "SELECT " + PUNISHMENT_SELECT_COLUMNS + " FROM (" +
                    "  SELECT id FROM tg_punishments " +
                    "  WHERE player_id = " + PLAYER_ID_BY_UUID + " " +
                    "  ORDER BY created_at DESC LIMIT ? OFFSET ?" +
                    ") page " + PUNISHMENT_OUTER_JOIN +
                    "ORDER BY pu.created_at DESC";
    public static final String SELECT_PUNISHMENTS_BY_UUID_SINCE =
            "SELECT " + PUNISHMENT_SELECT_COLUMNS + " FROM (" +
                    "  SELECT id FROM tg_punishments " +
                    "  WHERE player_id = " + PLAYER_ID_BY_UUID +
                    "    AND created_at >= ? " +
                    "  ORDER BY created_at DESC LIMIT ? OFFSET ?" +
                    ") page " + PUNISHMENT_OUTER_JOIN +
                    "ORDER BY pu.created_at DESC";

    private Sql() {
    }
}
