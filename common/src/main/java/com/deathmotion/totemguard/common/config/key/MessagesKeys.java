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

package com.deathmotion.totemguard.common.config.key;

import com.deathmotion.totemguard.api.config.key.ConfigKey;

/**
 * Typed paths into {@code messages.yml}.
 * <p>
 * Defaults live in the bundled {@code messages.yml} resource. These constants are an
 * internal implementation detail of TotemGuard's bundled YAML — they are intentionally
 * not part of the published API so that the layout can evolve without breaking
 * downstream consumers. External code that needs a value should call
 * {@link com.deathmotion.totemguard.api.config.Config#getString(String)} (or one of
 * its sibling path-based accessors) directly with the dotted path.
 */
public final class MessagesKeys {

    public static final ConfigKey<String> PREFIX = ConfigKey.string("prefix");

    public static final ConfigKey<String> ALERTS_MESSAGE = ConfigKey.string("alerts.message");
    public static final ConfigKey<String> ALERTS_HOVER = ConfigKey.string("alerts.hover");
    public static final ConfigKey<String> ALERTS_COMMAND = ConfigKey.string("alerts.command");

    public static final ConfigKey<String> MOD_ALERT_MESSAGE = ConfigKey.string("mod-detection.alert.message");

    public static final ConfigKey<String> ALERTS_ENABLED = ConfigKey.string("alerts.enabled");
    public static final ConfigKey<String> ALERTS_ENABLED_LOCAL_ONLY = ConfigKey.string("alerts.enabled-local-only");
    public static final ConfigKey<String> ALERTS_DISABLED = ConfigKey.string("alerts.disabled");
    public static final ConfigKey<String> ALERTS_LOCAL_ONLY_ENABLED = ConfigKey.string("alerts.local-only-enabled");
    public static final ConfigKey<String> ALERTS_LOCAL_ONLY_DISABLED = ConfigKey.string("alerts.local-only-disabled");
    public static final ConfigKey<String> TESTER_ENABLED = ConfigKey.string("alerts.tester.enabled");
    public static final ConfigKey<String> TESTER_DISABLED = ConfigKey.string("alerts.tester.disabled");
    public static final ConfigKey<String> TESTER_BYPASS = ConfigKey.string("alerts.tester.bypass");

    public static final ConfigKey<String> FOCUS_ENABLED = ConfigKey.string("alerts.focus.enabled");
    public static final ConfigKey<String> FOCUS_DISABLED = ConfigKey.string("alerts.focus.disabled");
    public static final ConfigKey<String> FOCUS_NONE_ACTIVE = ConfigKey.string("alerts.focus.none-active");
    public static final ConfigKey<String> FOCUS_TARGET_OFFLINE = ConfigKey.string("alerts.focus.target-offline");
    public static final ConfigKey<String> FOCUS_NOT_FOUND = ConfigKey.string("alerts.focus.not-found");
    public static final ConfigKey<String> FOCUS_SELF = ConfigKey.string("alerts.focus.self-not-allowed");

    public static final ConfigKey<String> TELEPORT_NOT_FOUND = ConfigKey.string("commands.teleport.not-found");
    public static final ConfigKey<String> TELEPORT_SELF = ConfigKey.string("commands.teleport.self-not-allowed");
    public static final ConfigKey<String> TELEPORT_SAME_SERVER = ConfigKey.string("commands.teleport.same-server");
    public static final ConfigKey<String> TELEPORT_CROSS_SERVER = ConfigKey.string("commands.teleport.cross-server");
    public static final ConfigKey<String> TELEPORT_NO_REDIS = ConfigKey.string("commands.teleport.no-redis");
    public static final ConfigKey<String> TELEPORT_DIFFERENT_PROXY = ConfigKey.string("commands.teleport.different-proxy");

    public static final ConfigKey<String> RELOAD = ConfigKey.string("reload");

    public static final ConfigKey<String> UPDATE_AVAILABLE = ConfigKey.string("update-checker.available");

    public static final ConfigKey<String> GENERAL_PLAYER_ONLY = ConfigKey.string("general.player-only");
    public static final ConfigKey<String> GENERAL_PLAYER_NOT_FOUND = ConfigKey.string("general.player-not-found");
    public static final ConfigKey<String> GENERAL_PLAYER_DATA_MISSING = ConfigKey.string("general.player-data-missing");
    public static final ConfigKey<String> GENERAL_DATABASE_UNAVAILABLE = ConfigKey.string("general.database-unavailable");
    public static final ConfigKey<String> GENERAL_NO_RECORDS = ConfigKey.string("general.no-records");
    public static final ConfigKey<String> GENERAL_LOOKUP_FAILED = ConfigKey.string("general.lookup-failed");

    public static final ConfigKey<String> ROOT_VERSION = ConfigKey.string("commands.root.version");
    public static final ConfigKey<String> ROOT_GUI_OPEN_FAILED = ConfigKey.string("commands.root.gui-open-failed");

    public static final ConfigKey<String> CHECK_ALREADY_CHECKING = ConfigKey.string("commands.check.already-checking");
    public static final ConfigKey<String> CHECK_ON_COOLDOWN = ConfigKey.string("commands.check.on-cooldown");
    public static final ConfigKey<String> CHECK_WRONG_GAMEMODE = ConfigKey.string("commands.check.wrong-gamemode");
    public static final ConfigKey<String> CHECK_INVULNERABLE = ConfigKey.string("commands.check.invulnerable");
    public static final ConfigKey<String> CHECK_NO_TOTEM = ConfigKey.string("commands.check.no-totem");
    public static final ConfigKey<String> CHECK_NO_BACKUP_TOTEM = ConfigKey.string("commands.check.no-backup-totem");
    public static final ConfigKey<String> CHECK_DAMAGE_FAILED = ConfigKey.string("commands.check.damage-failed");
    public static final ConfigKey<String> CHECK_FLAGGED = ConfigKey.string("commands.check.flagged");
    public static final ConfigKey<String> CHECK_PASSED = ConfigKey.string("commands.check.passed");
    public static final ConfigKey<String> CHECK_NO_REDIS = ConfigKey.string("commands.check.no-redis");
    public static final ConfigKey<String> CHECK_TIMEOUT = ConfigKey.string("commands.check.timeout");
    public static final ConfigKey<String> CHECK_DISPATCHED = ConfigKey.string("commands.check.dispatched");
    public static final ConfigKey<String> CHECK_STAFF_NOTICE = ConfigKey.string("commands.check.staff-notice");
    public static final ConfigKey<String> CHECK_STAFF_NOTICE_LOCAL = ConfigKey.string("commands.check.staff-notice-local");

    public static final ConfigKey<String> MONITOR_SELF = ConfigKey.string("commands.monitor.self-monitor");
    public static final ConfigKey<String> MONITOR_BLOCKED = ConfigKey.string("commands.monitor.blocked");
    public static final ConfigKey<String> MONITOR_OPEN_FAILED = ConfigKey.string("commands.monitor.open-failed");
    public static final ConfigKey<String> MONITOR_TARGET_OFFLINE = ConfigKey.string("commands.monitor.target-offline");

    public static final ConfigKey<String> HISTORY_OPEN_FAILED = ConfigKey.string("commands.history.open-failed");

    public static final ConfigKey<String> PROFILE_OPEN_FAILED = ConfigKey.string("commands.profile.open-failed");

    public static final ConfigKey<String> CLEARHISTORY_CLEARING = ConfigKey.string("commands.clearhistory.clearing");
    public static final ConfigKey<String> CLEARHISTORY_CLEARED = ConfigKey.string("commands.clearhistory.cleared");
    public static final ConfigKey<String> CLEARHISTORY_CLEAR_FAILED = ConfigKey.string("commands.clearhistory.clear-failed");

    // Note: /tg debug, /tg inventory and /tg placeholder are snapshot-only developer
    // tools. Their output is hardcoded against the Palette and intentionally not exposed
    // here.

    public static final ConfigKey<String> GUI_BTN_CLOSE_TITLE = ConfigKey.string("gui.button.close.title");
    public static final ConfigKey<String> GUI_BTN_CLOSE_LORE = ConfigKey.string("gui.button.close.lore");
    public static final ConfigKey<String> GUI_BTN_BACK_TITLE = ConfigKey.string("gui.button.back.title");
    public static final ConfigKey<String> GUI_BTN_BACK_LORE = ConfigKey.string("gui.button.back.lore");
    public static final ConfigKey<String> GUI_BTN_BACK_TO_PROFILE_TITLE = ConfigKey.string("gui.button.back-to-profile.title");
    public static final ConfigKey<String> GUI_BTN_BACK_TO_PROFILE_LORE = ConfigKey.string("gui.button.back-to-profile.lore");
    public static final ConfigKey<String> GUI_BTN_BACK_TO_HISTORY_TITLE = ConfigKey.string("gui.button.back-to-history.title");
    public static final ConfigKey<String> GUI_BTN_BACK_TO_HISTORY_LORE = ConfigKey.string("gui.button.back-to-history.lore");
    public static final ConfigKey<String> GUI_BTN_BACK_TO_OVERVIEW_TITLE = ConfigKey.string("gui.button.back-to-overview.title");
    public static final ConfigKey<String> GUI_BTN_BACK_TO_OVERVIEW_LORE = ConfigKey.string("gui.button.back-to-overview.lore");
    public static final ConfigKey<String> GUI_BTN_BACK_TO_ALERTS_TITLE = ConfigKey.string("gui.button.back-to-alerts.title");
    public static final ConfigKey<String> GUI_BTN_BACK_TO_ALERTS_LORE = ConfigKey.string("gui.button.back-to-alerts.lore");
    public static final ConfigKey<String> GUI_BTN_RETURN_TITLE = ConfigKey.string("gui.button.return.title");
    public static final ConfigKey<String> GUI_BTN_RETURN_LORE = ConfigKey.string("gui.button.return.lore");
    public static final ConfigKey<String> GUI_BTN_NEXT_PAGE_TITLE = ConfigKey.string("gui.button.next-page.title");
    public static final ConfigKey<String> GUI_BTN_PREVIOUS_PAGE_TITLE = ConfigKey.string("gui.button.previous-page.title");

    public static final ConfigKey<String> GUI_STATUS_YES = ConfigKey.string("gui.status.yes");
    public static final ConfigKey<String> GUI_STATUS_NO = ConfigKey.string("gui.status.no");
    public static final ConfigKey<String> GUI_STATUS_EMPTY = ConfigKey.string("gui.status.empty");
    public static final ConfigKey<String> GUI_STATUS_ENABLED = ConfigKey.string("gui.status.enabled");
    public static final ConfigKey<String> GUI_STATUS_DISABLED = ConfigKey.string("gui.status.disabled");
    public static final ConfigKey<String> GUI_STATUS_CONNECTED = ConfigKey.string("gui.status.connected");
    public static final ConfigKey<String> GUI_STATUS_DISCONNECTED = ConfigKey.string("gui.status.disconnected");
    public static final ConfigKey<String> GUI_STATUS_CURRENTLY_SELECTED = ConfigKey.string("gui.status.currently-selected");
    public static final ConfigKey<String> GUI_STATUS_CLICK_TO_SWITCH = ConfigKey.string("gui.status.click-to-switch");
    public static final ConfigKey<String> GUI_STATUS_CLICK_TO_BROWSE = ConfigKey.string("gui.status.click-to-browse");

    public static final ConfigKey<String> GUI_ERR_NO_PERMISSION = ConfigKey.string("gui.error.no-permission");
    public static final ConfigKey<String> GUI_ERR_MOD_DETECTION_RUNNING = ConfigKey.string("gui.error.mod-detection-running");
    public static final ConfigKey<String> GUI_ERR_CANNOT_MONITOR_SELF = ConfigKey.string("gui.error.cannot-monitor-self");
    public static final ConfigKey<String> GUI_ERR_MONITOR_BLOCKED = ConfigKey.string("gui.error.monitor-blocked");
    public static final ConfigKey<String> GUI_ERR_DATABASE_OFFLINE = ConfigKey.string("gui.error.database-offline");
    public static final ConfigKey<String> GUI_ERR_DB_UNREACHABLE = ConfigKey.string("gui.error.db-unreachable");
    public static final ConfigKey<String> GUI_ERR_CHECK_SERVER_LOG = ConfigKey.string("gui.error.check-server-log");
    public static final ConfigKey<String> GUI_ERR_FAILED_LOAD_ALERTS = ConfigKey.string("gui.error.failed-load-alerts");
    public static final ConfigKey<String> GUI_ERR_FAILED_LOAD_CHECKS = ConfigKey.string("gui.error.failed-load-checks");
    public static final ConfigKey<String> GUI_ERR_FAILED_LOAD_STATS = ConfigKey.string("gui.error.failed-load-stats");

    public static final ConfigKey<String> GUI_LOADING_GENERIC = ConfigKey.string("gui.loading.generic");
    public static final ConfigKey<String> GUI_LOADING_QUERYING_DATABASE = ConfigKey.string("gui.loading.querying-database");
    public static final ConfigKey<String> GUI_LOADING_JOIN_TIMES = ConfigKey.string("gui.loading.join-times");

    public static final ConfigKey<String> GUI_INFO_TITLE = ConfigKey.string("gui.info.title");
    public static final ConfigKey<String> GUI_INFO_SERVICES_TITLE = ConfigKey.string("gui.info.services-title");
    public static final ConfigKey<String> GUI_INFO_SERVICES_LORE_1 = ConfigKey.string("gui.info.services-lore-1");
    public static final ConfigKey<String> GUI_INFO_INFORMATION_TITLE = ConfigKey.string("gui.info.information-title");
    public static final ConfigKey<String> GUI_INFO_SECTION_VERSION = ConfigKey.string("gui.info.section-version");
    public static final ConfigKey<String> GUI_INFO_SECTION_PLATFORM = ConfigKey.string("gui.info.section-platform");
    public static final ConfigKey<String> GUI_INFO_SECTION_DEV_BUILD = ConfigKey.string("gui.info.section-dev-build");
    public static final ConfigKey<String> GUI_INFO_STATISTICS_TITLE = ConfigKey.string("gui.info.statistics-title");
    public static final ConfigKey<String> GUI_INFO_STATISTICS_DISABLED = ConfigKey.string("gui.info.statistics-disabled");
    public static final ConfigKey<String> GUI_INFO_STATISTICS_LORE_1 = ConfigKey.string("gui.info.statistics-lore-1");
    public static final ConfigKey<String> GUI_INFO_STATISTICS_LORE_2 = ConfigKey.string("gui.info.statistics-lore-2");
    public static final ConfigKey<String> GUI_INFO_STATISTICS_OFFLINE_1 = ConfigKey.string("gui.info.statistics-offline-1");
    public static final ConfigKey<String> GUI_INFO_STATISTICS_OFFLINE_2 = ConfigKey.string("gui.info.statistics-offline-2");
    public static final ConfigKey<String> GUI_INFO_NETWORK_BACKENDS = ConfigKey.string("gui.info.network-backends");
    public static final ConfigKey<String> GUI_INFO_NETWORK_PLAYERS = ConfigKey.string("gui.info.network-players");

    public static final ConfigKey<String> GUI_PROFILE_TITLE = ConfigKey.string("gui.profile.title");
    public static final ConfigKey<String> GUI_PROFILE_UNTRACKED_TITLE = ConfigKey.string("gui.profile.untracked-title");
    public static final ConfigKey<String> GUI_PROFILE_UNTRACKED_LORE = ConfigKey.string("gui.profile.untracked-lore");
    public static final ConfigKey<String> GUI_PROFILE_NO_VIOLATIONS = ConfigKey.string("gui.profile.no-violations");
    public static final ConfigKey<String> GUI_PROFILE_FIRST_JOINED_LOADING = ConfigKey.string("gui.profile.first-joined-loading");
    public static final ConfigKey<String> GUI_PROFILE_MONITOR_SELF_TITLE = ConfigKey.string("gui.profile.monitor-self-title");
    public static final ConfigKey<String> GUI_PROFILE_MONITOR_SELF_LORE = ConfigKey.string("gui.profile.monitor-self-lore");
    public static final ConfigKey<String> GUI_PROFILE_MONITOR_OPEN_TITLE = ConfigKey.string("gui.profile.monitor-open-title");
    public static final ConfigKey<String> GUI_PROFILE_MONITOR_OPEN_LORE_1 = ConfigKey.string("gui.profile.monitor-open-lore-1");
    public static final ConfigKey<String> GUI_PROFILE_MONITOR_OPEN_LORE_2 = ConfigKey.string("gui.profile.monitor-open-lore-2");
    public static final ConfigKey<String> GUI_PROFILE_HISTORY_TITLE = ConfigKey.string("gui.profile.history-title");
    public static final ConfigKey<String> GUI_PROFILE_HISTORY_LORE_1 = ConfigKey.string("gui.profile.history-lore-1");
    public static final ConfigKey<String> GUI_PROFILE_HISTORY_LORE_2 = ConfigKey.string("gui.profile.history-lore-2");
    public static final ConfigKey<String> GUI_PROFILE_HEAD_UUID_LABEL = ConfigKey.string("gui.profile.head.uuid-label");
    public static final ConfigKey<String> GUI_PROFILE_HEAD_CLIENT_VERSION_LABEL = ConfigKey.string("gui.profile.head.client-version-label");
    public static final ConfigKey<String> GUI_PROFILE_HEAD_CLIENT_BRAND_LABEL = ConfigKey.string("gui.profile.head.client-brand-label");
    public static final ConfigKey<String> GUI_PROFILE_HEAD_KEEPALIVE_PING_LABEL = ConfigKey.string("gui.profile.head.keepalive-ping-label");
    public static final ConfigKey<String> GUI_PROFILE_HEAD_TRANSACTION_PING_LABEL = ConfigKey.string("gui.profile.head.transaction-ping-label");
    public static final ConfigKey<String> GUI_PROFILE_HEAD_FIRST_JOINED_LABEL = ConfigKey.string("gui.profile.head.first-joined-label");
    public static final ConfigKey<String> GUI_PROFILE_HEAD_VIOLATIONS_LABEL = ConfigKey.string("gui.profile.head.violations-label");
    public static final ConfigKey<String> GUI_PROFILE_HEAD_VIOLATIONS_SUMMARY = ConfigKey.string("gui.profile.head.violations-summary");
    public static final ConfigKey<String> GUI_PROFILE_HEAD_VIOLATIONS_ENTRY = ConfigKey.string("gui.profile.head.violations-entry");
    public static final ConfigKey<String> GUI_PROFILE_HEAD_VIOLATIONS_OVERFLOW = ConfigKey.string("gui.profile.head.violations-overflow");

    public static final ConfigKey<String> GUI_MONITOR_TITLE = ConfigKey.string("gui.monitor.title");
    public static final ConfigKey<String> GUI_MONITOR_UNTRACKED_TITLE = ConfigKey.string("gui.monitor.untracked-title");
    public static final ConfigKey<String> GUI_MONITOR_UNTRACKED_LORE = ConfigKey.string("gui.monitor.untracked-lore");
    public static final ConfigKey<String> GUI_MONITOR_SELF_DISABLED_TITLE = ConfigKey.string("gui.monitor.self-disabled-title");
    public static final ConfigKey<String> GUI_MONITOR_SELF_DISABLED_LORE = ConfigKey.string("gui.monitor.self-disabled-lore");
    public static final ConfigKey<String> GUI_MONITOR_HEAD_TOOLTIP = ConfigKey.string("gui.monitor.head-tooltip");
    public static final ConfigKey<String> GUI_MONITOR_PACKET_STATE_TITLE = ConfigKey.string("gui.monitor.packet-state-title");
    public static final ConfigKey<String> GUI_MONITOR_LATENCY_TITLE = ConfigKey.string("gui.monitor.latency-title");
    public static final ConfigKey<String> GUI_MONITOR_CLIENT_TITLE = ConfigKey.string("gui.monitor.client-title");

    public static final ConfigKey<String> GUI_HISTORY_HUB_TITLE = ConfigKey.string("gui.history-hub.title");
    public static final ConfigKey<String> GUI_HISTORY_HUB_DB_LORE_1 = ConfigKey.string("gui.history-hub.db-unavailable-lore-1");
    public static final ConfigKey<String> GUI_HISTORY_HUB_ALERTS_TITLE = ConfigKey.string("gui.history-hub.alerts-title");
    public static final ConfigKey<String> GUI_HISTORY_HUB_ALERTS_LORE_1 = ConfigKey.string("gui.history-hub.alerts-lore-1");
    public static final ConfigKey<String> GUI_HISTORY_HUB_ALERTS_LORE_2 = ConfigKey.string("gui.history-hub.alerts-lore-2");
    public static final ConfigKey<String> GUI_HISTORY_HUB_PUNISHMENTS_TITLE = ConfigKey.string("gui.history-hub.punishments-title");
    public static final ConfigKey<String> GUI_HISTORY_HUB_PUNISHMENTS_LORE_1 = ConfigKey.string("gui.history-hub.punishments-lore-1");
    public static final ConfigKey<String> GUI_HISTORY_HUB_PUNISHMENTS_LORE_2 = ConfigKey.string("gui.history-hub.punishments-lore-2");

    public static final ConfigKey<String> GUI_ALERTS_TITLE = ConfigKey.string("gui.alerts.title");
    public static final ConfigKey<String> GUI_ALERTS_DB_LORE_1 = ConfigKey.string("gui.alerts.db-unavailable-lore-1");
    public static final ConfigKey<String> GUI_ALERTS_DEBUG_LABEL = ConfigKey.string("gui.alerts.debug-label");
    public static final ConfigKey<String> GUI_ALERTS_EMPTY_CLEAN_TITLE = ConfigKey.string("gui.alerts.empty-clean-title");
    public static final ConfigKey<String> GUI_ALERTS_EMPTY_FILTER_TITLE = ConfigKey.string("gui.alerts.empty-filter-title");
    public static final ConfigKey<String> GUI_ALERTS_EMPTY_CLEAN_LORE = ConfigKey.string("gui.alerts.empty-clean-lore");
    public static final ConfigKey<String> GUI_ALERTS_EMPTY_FILTER_LORE_1 = ConfigKey.string("gui.alerts.empty-filter-lore-1");
    public static final ConfigKey<String> GUI_ALERTS_EMPTY_FILTER_LORE_2 = ConfigKey.string("gui.alerts.empty-filter-lore-2");
    public static final ConfigKey<String> GUI_ALERTS_FILTER_PICK_TITLE = ConfigKey.string("gui.alerts.filter-pick-title");
    public static final ConfigKey<String> GUI_ALERTS_FILTER_CHANGE_TITLE = ConfigKey.string("gui.alerts.filter-change-title");
    public static final ConfigKey<String> GUI_ALERTS_FILTER_CLEAR_TITLE = ConfigKey.string("gui.alerts.filter-clear-title");
    public static final ConfigKey<String> GUI_ALERTS_FILTER_PICK_LORE_1 = ConfigKey.string("gui.alerts.filter-pick-lore-1");
    public static final ConfigKey<String> GUI_ALERTS_FILTER_PICK_LORE_2 = ConfigKey.string("gui.alerts.filter-pick-lore-2");
    public static final ConfigKey<String> GUI_ALERTS_FILTER_CHANGE_LORE = ConfigKey.string("gui.alerts.filter-change-lore");
    public static final ConfigKey<String> GUI_ALERTS_FILTER_CLEAR_LORE = ConfigKey.string("gui.alerts.filter-clear-lore");

    public static final ConfigKey<String> GUI_ALERT_CHECKS_TITLE = ConfigKey.string("gui.alert-checks.title");
    public static final ConfigKey<String> GUI_ALERT_CHECKS_EMPTY_TITLE = ConfigKey.string("gui.alert-checks.empty-title");
    public static final ConfigKey<String> GUI_ALERT_CHECKS_EMPTY_LORE = ConfigKey.string("gui.alert-checks.empty-lore");
    public static final ConfigKey<String> GUI_ALERT_CHECKS_FILTER_UNAVAILABLE = ConfigKey.string("gui.alert-checks.filter-unavailable");
    public static final ConfigKey<String> GUI_ALERT_CHECKS_VIEW_FILTER_HINT = ConfigKey.string("gui.alert-checks.view-filter-hint");

    public static final ConfigKey<String> GUI_PUNISHMENTS_TITLE = ConfigKey.string("gui.punishments.title");
    public static final ConfigKey<String> GUI_PUNISHMENTS_DB_LORE_1 = ConfigKey.string("gui.punishments.db-unavailable-lore-1");
    public static final ConfigKey<String> GUI_PUNISHMENTS_COMMAND_LABEL = ConfigKey.string("gui.punishments.command-label");
    public static final ConfigKey<String> GUI_PUNISHMENTS_DEBUG_LABEL = ConfigKey.string("gui.punishments.debug-label");
    public static final ConfigKey<String> GUI_PUNISHMENTS_EMPTY_CLEAN_TITLE = ConfigKey.string("gui.punishments.empty-clean-title");
    public static final ConfigKey<String> GUI_PUNISHMENTS_EMPTY_CLEAN_LORE = ConfigKey.string("gui.punishments.empty-clean-lore");

    public static final ConfigKey<String> GUI_STATISTICS_TITLE = ConfigKey.string("gui.statistics.title");
    public static final ConfigKey<String> GUI_STATISTICS_DB_LORE_1 = ConfigKey.string("gui.statistics.db-unavailable-lore-1");
    public static final ConfigKey<String> GUI_STATISTICS_PICK_WINDOW_LORE = ConfigKey.string("gui.statistics.pick-window-lore");
    public static final ConfigKey<String> GUI_STATISTICS_CURRENT_WINDOW_TITLE = ConfigKey.string("gui.statistics.current-window-title");

    private MessagesKeys() {
    }
}
