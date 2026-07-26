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

    public static final ConfigKey<String> FOLLOW_ENABLED = ConfigKey.string("commands.follow.enabled");
    public static final ConfigKey<String> FOLLOW_DISABLED = ConfigKey.string("commands.follow.disabled");
    public static final ConfigKey<String> FOLLOW_NONE_ACTIVE = ConfigKey.string("commands.follow.none-active");
    public static final ConfigKey<String> FOLLOW_NOT_FOUND = ConfigKey.string("commands.follow.not-found");
    public static final ConfigKey<String> FOLLOW_SELF = ConfigKey.string("commands.follow.self-not-allowed");
    public static final ConfigKey<String> FOLLOW_TARGET_OFFLINE = ConfigKey.string("commands.follow.target-offline");
    public static final ConfigKey<String> FOLLOW_DIFFERENT_PROXY = ConfigKey.string("commands.follow.different-proxy");
    public static final ConfigKey<String> FOLLOW_NO_BRIDGE = ConfigKey.string("commands.follow.no-bridge");
    public static final ConfigKey<String> FOLLOW_NO_REDIS = ConfigKey.string("commands.follow.no-redis");
    public static final ConfigKey<String> FOLLOW_TARGET_BYPASSED = ConfigKey.string("commands.follow.target-bypassed");
    public static final ConfigKey<String> FOLLOW_ACTION_BAR = ConfigKey.string("commands.follow.action-bar");

    public static final ConfigKey<String> TELEPORT_NOT_FOUND = ConfigKey.string("commands.teleport.not-found");
    public static final ConfigKey<String> TELEPORT_SELF = ConfigKey.string("commands.teleport.self-not-allowed");
    public static final ConfigKey<String> TELEPORT_SAME_SERVER = ConfigKey.string("commands.teleport.same-server");
    public static final ConfigKey<String> TELEPORT_CROSS_SERVER = ConfigKey.string("commands.teleport.cross-server");
    public static final ConfigKey<String> TELEPORT_NO_REDIS = ConfigKey.string("commands.teleport.no-redis");
    public static final ConfigKey<String> TELEPORT_DIFFERENT_PROXY = ConfigKey.string("commands.teleport.different-proxy");
    public static final ConfigKey<String> TELEPORT_NO_BRIDGE = ConfigKey.string("commands.teleport.no-bridge");
    public static final ConfigKey<String> TELEPORT_TARGET_BYPASSED = ConfigKey.string("commands.teleport.target-bypassed");

    public static final ConfigKey<String> REPLAY_DISABLED = ConfigKey.string("commands.replay.disabled");
    public static final ConfigKey<String> REPLAY_ARMED = ConfigKey.string("commands.replay.armed");
    public static final ConfigKey<String> REPLAY_ARM_REPLACED = ConfigKey.string("commands.replay.arm-replaced");
    public static final ConfigKey<String> REPLAY_ARM_REQUESTED = ConfigKey.string("commands.replay.arm-requested");
    public static final ConfigKey<String> REPLAY_SHADOW_ARMED = ConfigKey.string("commands.replay.shadow-armed");
    public static final ConfigKey<String> REPLAY_SHADOW_QUEUED = ConfigKey.string("commands.replay.shadow-queued");
    public static final ConfigKey<String> REPLAY_ARM_KICK_SCREEN = ConfigKey.string("commands.replay.arm-kick-screen");
    public static final ConfigKey<String> REPLAY_ARM_CANCELLED = ConfigKey.string("commands.replay.arm-cancelled");
    public static final ConfigKey<String> REPLAY_ARM_NONE = ConfigKey.string("commands.replay.arm-none");
    public static final ConfigKey<String> REPLAY_ARM_EXPIRED = ConfigKey.string("commands.replay.arm-expired");
    public static final ConfigKey<String> REPLAY_STARTED = ConfigKey.string("commands.replay.started");
    public static final ConfigKey<String> REPLAY_START_FAILED = ConfigKey.string("commands.replay.start-failed");
    public static final ConfigKey<String> REPLAY_STOPPED = ConfigKey.string("commands.replay.stopped");
    public static final ConfigKey<String> REPLAY_STOPPED_REASON = ConfigKey.string("commands.replay.stopped-reason");
    public static final ConfigKey<String> REPLAY_NOT_RECORDING = ConfigKey.string("commands.replay.not-recording");
    public static final ConfigKey<String> REPLAY_DEGRADED = ConfigKey.string("commands.replay.degraded");
    public static final ConfigKey<String> REPLAY_MARKED = ConfigKey.string("commands.replay.marked");
    public static final ConfigKey<String> REPLAY_STATUS = ConfigKey.string("commands.replay.status");
    public static final ConfigKey<String> REPLAY_STATUS_ARMED = ConfigKey.string("commands.replay.status-armed");
    public static final ConfigKey<String> REPLAY_STATUS_IDLE = ConfigKey.string("commands.replay.status-idle");
    public static final ConfigKey<String> REPLAY_STATUS_RETAINED = ConfigKey.string("commands.replay.status-retained");
    public static final ConfigKey<String> REPLAY_DUMP_STARTED = ConfigKey.string("commands.replay.dump-started");
    public static final ConfigKey<String> REPLAY_DUMP_UNAVAILABLE = ConfigKey.string("commands.replay.dump-unavailable");
    public static final ConfigKey<String> REPLAY_SCENARIO_INVALID = ConfigKey.string("commands.replay.scenario-invalid");
    public static final ConfigKey<String> REPLAY_STOP_REQUESTED = ConfigKey.string("commands.replay.stop-requested");
    public static final ConfigKey<String> REPLAY_HUD_ENABLED = ConfigKey.string("commands.replay.hud-enabled");
    public static final ConfigKey<String> REPLAY_HUD_DISABLED = ConfigKey.string("commands.replay.hud-disabled");
    public static final ConfigKey<String> REPLAY_HUD_FOLLOWING = ConfigKey.string("commands.replay.hud-following");
    public static final ConfigKey<String> REPLAY_HUD_UNFOLLOWED = ConfigKey.string("commands.replay.hud-unfollowed");
    public static final ConfigKey<String> REPLAY_INSPECT_UNREADABLE = ConfigKey.string("commands.replay.inspect-unreadable");
    public static final ConfigKey<String> REPLAY_ACTION_BAR = ConfigKey.string("commands.replay.action-bar");
    public static final ConfigKey<String> REPLAY_ACTION_BAR_FLAGS = ConfigKey.string("commands.replay.action-bar-flags");
    public static final ConfigKey<String> REPLAY_ACTION_BAR_FLAGGED = ConfigKey.string("commands.replay.action-bar-flagged");
    public static final ConfigKey<String> REPLAY_ACTION_BAR_DROPPED = ConfigKey.string("commands.replay.action-bar-dropped");
    public static final ConfigKey<String> REPLAY_MAX_LENGTH = ConfigKey.string("commands.replay.max-length");
    public static final ConfigKey<String> REPLAY_RUN_STARTED = ConfigKey.string("commands.replay.run-started");
    public static final ConfigKey<String> REPLAY_RUN_FINISHED = ConfigKey.string("commands.replay.run-finished");
    public static final ConfigKey<String> REPLAY_RUN_FAILED = ConfigKey.string("commands.replay.run-failed");
    public static final ConfigKey<String> REPLAY_FILE_NOT_FOUND = ConfigKey.string("commands.replay.file-not-found");
    public static final ConfigKey<String> REPLAY_RUN_BUSY = ConfigKey.string("commands.replay.run-busy");
    public static final ConfigKey<String> REPLAY_RUN_TRUNCATED = ConfigKey.string("commands.replay.run-truncated");
    public static final ConfigKey<String> REPLAY_VACUOUS = ConfigKey.string("commands.replay.vacuous");
    public static final ConfigKey<String> REPLAY_VERIFY_MATCHED = ConfigKey.string("commands.replay.verify-matched");
    public static final ConfigKey<String> REPLAY_VERIFY_DIVERGED = ConfigKey.string("commands.replay.verify-diverged");
    public static final ConfigKey<String> REPLAY_VERIFY_SKIPPED = ConfigKey.string("commands.replay.verify-skipped");
    public static final ConfigKey<String> REPLAY_FLAGS_MATCHED = ConfigKey.string("commands.replay.flags-matched");
    public static final ConfigKey<String> REPLAY_FLAGS_DIVERGED = ConfigKey.string("commands.replay.flags-diverged");
    public static final ConfigKey<String> REPLAY_WORLD_MATCHED = ConfigKey.string("commands.replay.world-matched");
    public static final ConfigKey<String> REPLAY_WORLD_DIVERGED = ConfigKey.string("commands.replay.world-diverged");
    public static final ConfigKey<String> REPLAY_WORLD_UNSETTLED = ConfigKey.string("commands.replay.world-unsettled");
    public static final ConfigKey<String> REPLAY_WORLD_VACUOUS = ConfigKey.string("commands.replay.world-vacuous");

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
    public static final ConfigKey<String> MONITOR_TARGET_BYPASSED = ConfigKey.string("commands.monitor.target-bypassed");

    public static final ConfigKey<String> HISTORY_OPEN_FAILED = ConfigKey.string("commands.history.open-failed");

    public static final ConfigKey<String> PROFILE_OPEN_FAILED = ConfigKey.string("commands.profile.open-failed");

    public static final ConfigKey<String> STATS_OPEN_FAILED = ConfigKey.string("commands.stats.open-failed");

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
    public static final ConfigKey<String> GUI_STATUS_NOT_INSTALLED = ConfigKey.string("gui.status.not-installed");
    public static final ConfigKey<String> GUI_STATUS_CURRENTLY_SELECTED = ConfigKey.string("gui.status.currently-selected");
    public static final ConfigKey<String> GUI_STATUS_CLICK_TO_SWITCH = ConfigKey.string("gui.status.click-to-switch");
    public static final ConfigKey<String> GUI_STATUS_CLICK_TO_BROWSE = ConfigKey.string("gui.status.click-to-browse");

    public static final ConfigKey<String> GUI_STATUS_CLICK_TO_OPEN = ConfigKey.string("gui.status.click-to-open");
    public static final ConfigKey<String> GUI_STATUS_CLICK_TO_RUN = ConfigKey.string("gui.status.click-to-run");
    public static final ConfigKey<String> GUI_STATUS_CLICK_TO_TYPE = ConfigKey.string("gui.status.click-to-type");

    public static final ConfigKey<String> GUI_ERR_NO_PERMISSION = ConfigKey.string("gui.error.no-permission");
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
    public static final ConfigKey<String> GUI_INFO_SECTION_LOADER = ConfigKey.string("gui.info.section-loader");
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
    public static final ConfigKey<String> GUI_PROFILE_MODS_CLEAN_TITLE = ConfigKey.string("gui.profile.mods.clean-title");
    public static final ConfigKey<String> GUI_PROFILE_MODS_CLEAN_LORE = ConfigKey.string("gui.profile.mods.clean-lore");
    public static final ConfigKey<String> GUI_PROFILE_MODS_DETECTED_TITLE = ConfigKey.string("gui.profile.mods.detected-title");
    public static final ConfigKey<String> GUI_PROFILE_MODS_DETECTED_SUMMARY = ConfigKey.string("gui.profile.mods.detected-summary");
    public static final ConfigKey<String> GUI_PROFILE_MODS_DETECTED_ENTRY = ConfigKey.string("gui.profile.mods.detected-entry");
    public static final ConfigKey<String> GUI_PROFILE_MODS_DETECTED_OVERFLOW = ConfigKey.string("gui.profile.mods.detected-overflow");
    public static final ConfigKey<String> GUI_PROFILE_MODS_CLICK_HINT = ConfigKey.string("gui.profile.mods.click-hint");
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

    public static final ConfigKey<String> GUI_MOD_SESSION_TITLE = ConfigKey.string("gui.mod-session.title");
    public static final ConfigKey<String> GUI_MOD_SESSION_EMPTY_TITLE = ConfigKey.string("gui.mod-session.empty-title");
    public static final ConfigKey<String> GUI_MOD_SESSION_EMPTY_LORE = ConfigKey.string("gui.mod-session.empty-lore");
    public static final ConfigKey<String> GUI_MOD_SESSION_SEVERITY_LABEL = ConfigKey.string("gui.mod-session.severity-label");
    public static final ConfigKey<String> GUI_MOD_SESSION_METHOD_LABEL = ConfigKey.string("gui.mod-session.method-label");
    public static final ConfigKey<String> GUI_MOD_SESSION_FOOTER_TITLE = ConfigKey.string("gui.mod-session.footer-title");
    public static final ConfigKey<String> GUI_MOD_SESSION_FOOTER_LABEL = ConfigKey.string("gui.mod-session.footer-label");

    public static final ConfigKey<String> GUI_MOD_SEVERITY_LOG = ConfigKey.string("gui.mod.severity.log");
    public static final ConfigKey<String> GUI_MOD_SEVERITY_KICK = ConfigKey.string("gui.mod.severity.kick");
    public static final ConfigKey<String> GUI_MOD_SEVERITY_KICK_THEN_BAN = ConfigKey.string("gui.mod.severity.kick-then-ban");
    public static final ConfigKey<String> GUI_MOD_SEVERITY_BAN = ConfigKey.string("gui.mod.severity.ban");

    public static final ConfigKey<String> GUI_MOD_METHOD_PLUGIN_CHANNEL = ConfigKey.string("gui.mod.method.plugin-channel");
    public static final ConfigKey<String> GUI_MOD_METHOD_PLUGIN_MESSAGE = ConfigKey.string("gui.mod.method.plugin-message");
    public static final ConfigKey<String> GUI_MOD_METHOD_TRANSLATION = ConfigKey.string("gui.mod.method.translation");

    public static final ConfigKey<String> GUI_TOP_TITLE = ConfigKey.string("gui.top.title");
    public static final ConfigKey<String> GUI_TOP_TITLE_FILTERED = ConfigKey.string("gui.top.title-filtered");
    public static final ConfigKey<String> GUI_TOP_LOCAL_ONLY_TITLE = ConfigKey.string("gui.top.local-only-title");
    public static final ConfigKey<String> GUI_TOP_LOCAL_ONLY_LORE_1 = ConfigKey.string("gui.top.local-only-lore-1");
    public static final ConfigKey<String> GUI_TOP_LOCAL_ONLY_LORE_2 = ConfigKey.string("gui.top.local-only-lore-2");
    public static final ConfigKey<String> GUI_TOP_LOADING_LORE = ConfigKey.string("gui.top.loading-lore");
    public static final ConfigKey<String> GUI_TOP_EMPTY_TITLE = ConfigKey.string("gui.top.empty-title");
    public static final ConfigKey<String> GUI_TOP_EMPTY_FILTER_TITLE = ConfigKey.string("gui.top.empty-filter-title");
    public static final ConfigKey<String> GUI_TOP_EMPTY_LORE = ConfigKey.string("gui.top.empty-lore");
    public static final ConfigKey<String> GUI_TOP_EMPTY_FILTER_LORE = ConfigKey.string("gui.top.empty-filter-lore");
    public static final ConfigKey<String> GUI_TOP_ERR_SELF_TELEPORT = ConfigKey.string("gui.top.err-self-teleport");
    public static final ConfigKey<String> GUI_TOP_ERR_TELEPORT_UNAVAILABLE = ConfigKey.string("gui.top.err-teleport-unavailable");
    public static final ConfigKey<String> GUI_TOP_PAGE_NUMBER = ConfigKey.string("gui.top.page-number");
    public static final ConfigKey<String> GUI_TOP_PAGE_SUMMARY = ConfigKey.string("gui.top.page-summary");
    public static final ConfigKey<String> GUI_TOP_FOOTER_TRACKED_LABEL = ConfigKey.string("gui.top.footer-tracked-label");
    public static final ConfigKey<String> GUI_TOP_FOOTER_MATCHING_LABEL = ConfigKey.string("gui.top.footer-matching-label");
    public static final ConfigKey<String> GUI_TOP_FOOTER_FILTER_LABEL = ConfigKey.string("gui.top.footer-filter-label");
    public static final ConfigKey<String> GUI_TOP_FOOTER_HINT_LEFT_CLICK = ConfigKey.string("gui.top.footer-hint-left-click");
    public static final ConfigKey<String> GUI_TOP_FOOTER_HINT_RIGHT_CLICK = ConfigKey.string("gui.top.footer-hint-right-click");
    public static final ConfigKey<String> GUI_TOP_FILTER_PICK_TITLE = ConfigKey.string("gui.top.filter-pick-title");
    public static final ConfigKey<String> GUI_TOP_FILTER_PICK_LORE_1 = ConfigKey.string("gui.top.filter-pick-lore-1");
    public static final ConfigKey<String> GUI_TOP_FILTER_PICK_LORE_2 = ConfigKey.string("gui.top.filter-pick-lore-2");
    public static final ConfigKey<String> GUI_TOP_FILTER_CHANGE_TITLE = ConfigKey.string("gui.top.filter-change-title");
    public static final ConfigKey<String> GUI_TOP_FILTER_CHANGE_LORE = ConfigKey.string("gui.top.filter-change-lore");
    public static final ConfigKey<String> GUI_TOP_FILTER_CLEAR_TITLE = ConfigKey.string("gui.top.filter-clear-title");
    public static final ConfigKey<String> GUI_TOP_FILTER_CLEAR_LORE = ConfigKey.string("gui.top.filter-clear-lore");
    public static final ConfigKey<String> GUI_TOP_HEAD_SERVER_LABEL = ConfigKey.string("gui.top.head.server-label");
    public static final ConfigKey<String> GUI_TOP_HEAD_SESSION_LABEL = ConfigKey.string("gui.top.head.session-label");
    public static final ConfigKey<String> GUI_TOP_HEAD_FILTER_VL_LABEL = ConfigKey.string("gui.top.head.filter-vl-label");
    public static final ConfigKey<String> GUI_TOP_HEAD_TOTAL_VL_LABEL = ConfigKey.string("gui.top.head.total-vl-label");
    public static final ConfigKey<String> GUI_TOP_HEAD_TOP_CHECKS_LABEL = ConfigKey.string("gui.top.head.top-checks-label");
    public static final ConfigKey<String> GUI_TOP_HEAD_TOP_CHECKS_ENTRY = ConfigKey.string("gui.top.head.top-checks-entry");
    public static final ConfigKey<String> GUI_TOP_HEAD_TOP_CHECKS_OVERFLOW = ConfigKey.string("gui.top.head.top-checks-overflow");
    public static final ConfigKey<String> GUI_TOP_HEAD_LEFT_CLICK_ACTION = ConfigKey.string("gui.top.head.left-click-action");
    public static final ConfigKey<String> GUI_TOP_HEAD_RIGHT_CLICK_ACTION = ConfigKey.string("gui.top.head.right-click-action");

    public static final ConfigKey<String> GUI_TOP_CHECKS_TITLE = ConfigKey.string("gui.top-checks.title");
    public static final ConfigKey<String> GUI_TOP_CHECKS_LOCAL_ONLY_LORE = ConfigKey.string("gui.top-checks.local-only-lore");
    public static final ConfigKey<String> GUI_TOP_CHECKS_LOADING_LORE = ConfigKey.string("gui.top-checks.loading-lore");
    public static final ConfigKey<String> GUI_TOP_CHECKS_EMPTY_TITLE = ConfigKey.string("gui.top-checks.empty-title");
    public static final ConfigKey<String> GUI_TOP_CHECKS_EMPTY_LORE = ConfigKey.string("gui.top-checks.empty-lore");
    public static final ConfigKey<String> GUI_TOP_CHECKS_ENTRY_TOTAL_VL_LABEL = ConfigKey.string("gui.top-checks.entry-total-vl-label");
    public static final ConfigKey<String> GUI_TOP_CHECKS_ENTRY_VIOLATORS_LABEL = ConfigKey.string("gui.top-checks.entry-violators-label");
    public static final ConfigKey<String> GUI_TOP_CHECKS_ENTRY_ACTION = ConfigKey.string("gui.top-checks.entry-action");
    public static final ConfigKey<String> GUI_TOP_CHECKS_FOOTER_DISTINCT_LABEL = ConfigKey.string("gui.top-checks.footer-distinct-label");

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
    public static final ConfigKey<String> GUI_STATISTICS_SECTION_ACTIVITY = ConfigKey.string("gui.statistics.section-activity");
    public static final ConfigKey<String> GUI_STATISTICS_SECTION_PLAYERS = ConfigKey.string("gui.statistics.section-players");
    public static final ConfigKey<String> GUI_STATISTICS_SECTION_STORAGE = ConfigKey.string("gui.statistics.section-storage");
    public static final ConfigKey<String> GUI_CHAT_INPUT_PROMPT = ConfigKey.string("gui.chat-input.prompt");
    public static final ConfigKey<String> GUI_CHAT_INPUT_CANCELLED = ConfigKey.string("gui.chat-input.cancelled");
    public static final ConfigKey<String> GUI_CHAT_INPUT_EXPIRED = ConfigKey.string("gui.chat-input.expired");
    public static final ConfigKey<String> GUI_REPLAY_HUB_TITLE = ConfigKey.string("gui.replay.hub-title");
    public static final ConfigKey<String> GUI_REPLAY_UNAVAILABLE_TITLE = ConfigKey.string("gui.replay.unavailable-title");
    public static final ConfigKey<String> GUI_REPLAY_UNAVAILABLE_LORE = ConfigKey.string("gui.replay.unavailable-lore");
    public static final ConfigKey<String> GUI_REPLAY_START_TITLE = ConfigKey.string("gui.replay.start-title");
    public static final ConfigKey<String> GUI_REPLAY_START_LORE_1 = ConfigKey.string("gui.replay.start-lore-1");
    public static final ConfigKey<String> GUI_REPLAY_START_LORE_2 = ConfigKey.string("gui.replay.start-lore-2");
    public static final ConfigKey<String> GUI_REPLAY_LIBRARY_TITLE = ConfigKey.string("gui.replay.library-title");
    public static final ConfigKey<String> GUI_REPLAY_LIBRARY_LORE_1 = ConfigKey.string("gui.replay.library-lore-1");
    public static final ConfigKey<String> GUI_REPLAY_LIVE_TITLE = ConfigKey.string("gui.replay.live-title");
    public static final ConfigKey<String> GUI_REPLAY_LIVE_LORE_1 = ConfigKey.string("gui.replay.live-lore-1");
    public static final ConfigKey<String> GUI_REPLAY_LIBRARY_SCREEN_TITLE = ConfigKey.string("gui.replay.library-screen-title");
    public static final ConfigKey<String> GUI_REPLAY_LOADING_LIBRARY = ConfigKey.string("gui.replay.loading-library");
    public static final ConfigKey<String> GUI_REPLAY_READING_DETAILS = ConfigKey.string("gui.replay.reading-details");
    public static final ConfigKey<String> GUI_REPLAY_LIBRARY_EMPTY_TITLE = ConfigKey.string("gui.replay.library-empty-title");
    public static final ConfigKey<String> GUI_REPLAY_LIBRARY_EMPTY_LORE = ConfigKey.string("gui.replay.library-empty-lore");
    public static final ConfigKey<String> GUI_REPLAY_LIBRARY_NO_MATCH_TITLE = ConfigKey.string("gui.replay.library-no-match-title");
    public static final ConfigKey<String> GUI_REPLAY_LIBRARY_NO_MATCH_LORE = ConfigKey.string("gui.replay.library-no-match-lore");
    public static final ConfigKey<String> GUI_REPLAY_FILTER_TITLE = ConfigKey.string("gui.replay.filter-title");
    public static final ConfigKey<String> GUI_REPLAY_FILTER_LORE_1 = ConfigKey.string("gui.replay.filter-lore-1");
    public static final ConfigKey<String> GUI_REPLAY_FILTER_CLEAR_TITLE = ConfigKey.string("gui.replay.filter-clear-title");
    public static final ConfigKey<String> GUI_REPLAY_FILTER_CLEAR_LORE = ConfigKey.string("gui.replay.filter-clear-lore");
    public static final ConfigKey<String> GUI_REPLAY_ROW_HINT_OPEN = ConfigKey.string("gui.replay.row-hint-open");
    public static final ConfigKey<String> GUI_REPLAY_ROW_HINT_EDIT = ConfigKey.string("gui.replay.row-hint-edit");
    public static final ConfigKey<String> GUI_REPLAY_UNREADABLE_TITLE = ConfigKey.string("gui.replay.unreadable-title");
    public static final ConfigKey<String> GUI_REPLAY_UNREADABLE_LORE = ConfigKey.string("gui.replay.unreadable-lore");
    public static final ConfigKey<String> GUI_REPLAY_RECORDING_TITLE = ConfigKey.string("gui.replay.recording-title");
    public static final ConfigKey<String> GUI_REPLAY_RUN_TITLE = ConfigKey.string("gui.replay.run-title");
    public static final ConfigKey<String> GUI_REPLAY_RUN_LORE_1 = ConfigKey.string("gui.replay.run-lore-1");
    public static final ConfigKey<String> GUI_REPLAY_RUN_LORE_2 = ConfigKey.string("gui.replay.run-lore-2");
    public static final ConfigKey<String> GUI_REPLAY_RUN_BUSY_LORE = ConfigKey.string("gui.replay.run-busy-lore");
    public static final ConfigKey<String> GUI_REPLAY_EDIT_TITLE = ConfigKey.string("gui.replay.edit-title");
    public static final ConfigKey<String> GUI_REPLAY_EDIT_LORE_1 = ConfigKey.string("gui.replay.edit-lore-1");
    public static final ConfigKey<String> GUI_REPLAY_EDIT_SCREEN_TITLE = ConfigKey.string("gui.replay.edit-screen-title");
    public static final ConfigKey<String> GUI_REPLAY_EDIT_PREVIEW_LORE = ConfigKey.string("gui.replay.edit-preview-lore");
    public static final ConfigKey<String> GUI_REPLAY_RENAME_TITLE = ConfigKey.string("gui.replay.rename-title");
    public static final ConfigKey<String> GUI_REPLAY_RENAME_LORE_1 = ConfigKey.string("gui.replay.rename-lore-1");
    public static final ConfigKey<String> GUI_REPLAY_LABEL_TITLE = ConfigKey.string("gui.replay.label-title");
    public static final ConfigKey<String> GUI_REPLAY_LABEL_LORE_1 = ConfigKey.string("gui.replay.label-lore-1");
    public static final ConfigKey<String> GUI_REPLAY_TAGS_TITLE = ConfigKey.string("gui.replay.tags-title");
    public static final ConfigKey<String> GUI_REPLAY_TAGS_LORE_1 = ConfigKey.string("gui.replay.tags-lore-1");
    public static final ConfigKey<String> GUI_REPLAY_NOTE_TITLE = ConfigKey.string("gui.replay.note-title");
    public static final ConfigKey<String> GUI_REPLAY_NOTE_LORE_1 = ConfigKey.string("gui.replay.note-lore-1");
    public static final ConfigKey<String> GUI_REPLAY_NOTE_LORE_2 = ConfigKey.string("gui.replay.note-lore-2");
    public static final ConfigKey<String> GUI_REPLAY_SAVE_TITLE = ConfigKey.string("gui.replay.save-title");
    public static final ConfigKey<String> GUI_REPLAY_SAVE_LORE = ConfigKey.string("gui.replay.save-lore");
    public static final ConfigKey<String> GUI_REPLAY_SAVE_UNCHANGED_LORE = ConfigKey.string("gui.replay.save-unchanged-lore");
    public static final ConfigKey<String> GUI_REPLAY_SAVED = ConfigKey.string("gui.replay.saved");
    public static final ConfigKey<String> GUI_REPLAY_SAVE_FAILED = ConfigKey.string("gui.replay.save-failed");
    public static final ConfigKey<String> GUI_REPLAY_SAVE_MISSING = ConfigKey.string("gui.replay.save-missing");
    public static final ConfigKey<String> GUI_REPLAY_SAVE_RUNNING = ConfigKey.string("gui.replay.save-running");
    public static final ConfigKey<String> GUI_REPLAY_TAGS_SCREEN_TITLE = ConfigKey.string("gui.replay.tags-screen-title");
    public static final ConfigKey<String> GUI_REPLAY_TAG_ON = ConfigKey.string("gui.replay.tag-on");
    public static final ConfigKey<String> GUI_REPLAY_TAG_OFF = ConfigKey.string("gui.replay.tag-off");
    public static final ConfigKey<String> GUI_REPLAY_TAG_TRACE = ConfigKey.string("gui.replay.tag-trace");
    public static final ConfigKey<String> GUI_REPLAY_TAG_NO_TRACE = ConfigKey.string("gui.replay.tag-no-trace");
    public static final ConfigKey<String> GUI_REPLAY_TAG_CUSTOM_TITLE = ConfigKey.string("gui.replay.tag-custom-title");
    public static final ConfigKey<String> GUI_REPLAY_TAG_CUSTOM_LORE_1 = ConfigKey.string("gui.replay.tag-custom-lore-1");
    public static final ConfigKey<String> GUI_REPLAY_TAG_CLEAR_TITLE = ConfigKey.string("gui.replay.tag-clear-title");
    public static final ConfigKey<String> GUI_REPLAY_TAG_CLEAR_LORE = ConfigKey.string("gui.replay.tag-clear-lore");
    public static final ConfigKey<String> GUI_REPLAY_TAG_DONE_TITLE = ConfigKey.string("gui.replay.tag-done-title");
    public static final ConfigKey<String> GUI_REPLAY_TAG_DONE_LORE = ConfigKey.string("gui.replay.tag-done-lore");
    public static final ConfigKey<String> GUI_REPLAY_SETUP_TITLE = ConfigKey.string("gui.replay.setup-title");
    public static final ConfigKey<String> GUI_REPLAY_SETUP_TARGET_TITLE = ConfigKey.string("gui.replay.setup-target-title");
    public static final ConfigKey<String> GUI_REPLAY_SETUP_TARGET_LORE_1 = ConfigKey.string("gui.replay.setup-target-lore-1");
    public static final ConfigKey<String> GUI_REPLAY_SETUP_TARGET_LORE_2 = ConfigKey.string("gui.replay.setup-target-lore-2");
    public static final ConfigKey<String> GUI_REPLAY_SETUP_MODE_RECORD_TITLE = ConfigKey.string("gui.replay.setup-mode-record-title");
    public static final ConfigKey<String> GUI_REPLAY_SETUP_MODE_RECORD_LORE_1 = ConfigKey.string("gui.replay.setup-mode-record-lore-1");
    public static final ConfigKey<String> GUI_REPLAY_SETUP_MODE_RECORD_LORE_2 = ConfigKey.string("gui.replay.setup-mode-record-lore-2");
    public static final ConfigKey<String> GUI_REPLAY_SETUP_MODE_SHADOW_TITLE = ConfigKey.string("gui.replay.setup-mode-shadow-title");
    public static final ConfigKey<String> GUI_REPLAY_SETUP_MODE_SHADOW_LORE_1 = ConfigKey.string("gui.replay.setup-mode-shadow-lore-1");
    public static final ConfigKey<String> GUI_REPLAY_SETUP_MODE_SHADOW_LORE_2 = ConfigKey.string("gui.replay.setup-mode-shadow-lore-2");
    public static final ConfigKey<String> GUI_REPLAY_SETUP_SCENARIO_TITLE = ConfigKey.string("gui.replay.setup-scenario-title");
    public static final ConfigKey<String> GUI_REPLAY_SETUP_SCENARIO_LORE_1 = ConfigKey.string("gui.replay.setup-scenario-lore-1");
    public static final ConfigKey<String> GUI_REPLAY_SETUP_START_TITLE = ConfigKey.string("gui.replay.setup-start-title");
    public static final ConfigKey<String> GUI_REPLAY_SETUP_START_RECORD_LORE = ConfigKey.string("gui.replay.setup-start-record-lore");
    public static final ConfigKey<String> GUI_REPLAY_SETUP_START_SHADOW_LORE = ConfigKey.string("gui.replay.setup-start-shadow-lore");
    public static final ConfigKey<String> GUI_REPLAY_SETUP_START_NO_TARGET_LORE = ConfigKey.string("gui.replay.setup-start-no-target-lore");
    public static final ConfigKey<String> GUI_REPLAY_SETUP_START_NO_NAME_LORE = ConfigKey.string("gui.replay.setup-start-no-name-lore");
    public static final ConfigKey<String> GUI_REPLAY_ACTIVE_TITLE = ConfigKey.string("gui.replay.active-title");
    public static final ConfigKey<String> GUI_REPLAY_ACTIVE_IDLE_TITLE = ConfigKey.string("gui.replay.active-idle-title");
    public static final ConfigKey<String> GUI_REPLAY_ACTIVE_IDLE_LORE = ConfigKey.string("gui.replay.active-idle-lore");
    public static final ConfigKey<String> GUI_REPLAY_ACTIVE_STOP_HINT = ConfigKey.string("gui.replay.active-stop-hint");
    public static final ConfigKey<String> GUI_REPLAY_ACTIVE_HUD_ON_HINT = ConfigKey.string("gui.replay.active-hud-on-hint");
    public static final ConfigKey<String> GUI_REPLAY_ACTIVE_HUD_OFF_HINT = ConfigKey.string("gui.replay.active-hud-off-hint");
    public static final ConfigKey<String> GUI_REPLAY_ACTIVE_CANCEL_HINT = ConfigKey.string("gui.replay.active-cancel-hint");
    public static final ConfigKey<String> GUI_REPLAY_OPEN_FAILED = ConfigKey.string("gui.replay.open-failed");
    public static final ConfigKey<String> GUI_REPLAY_LABELS_TITLE = ConfigKey.string("gui.replay.labels-title");
    public static final ConfigKey<String> GUI_REPLAY_LABEL_LEGIT_LORE = ConfigKey.string("gui.replay.label-legit-lore");
    public static final ConfigKey<String> GUI_REPLAY_LABEL_CHEAT_LORE = ConfigKey.string("gui.replay.label-cheat-lore");
    public static final ConfigKey<String> GUI_REPLAY_LABEL_SCRATCH_LORE = ConfigKey.string("gui.replay.label-scratch-lore");
    public static final ConfigKey<String> GUI_REPLAY_LABEL_AUTO_LORE = ConfigKey.string("gui.replay.label-auto-lore");
    public static final ConfigKey<String> GUI_REPLAY_LABEL_ALL_TITLE = ConfigKey.string("gui.replay.label-all-title");
    public static final ConfigKey<String> GUI_REPLAY_LABEL_ALL_LORE = ConfigKey.string("gui.replay.label-all-lore");
    public static final ConfigKey<String> GUI_REPLAY_SETUP_BUSY_TITLE = ConfigKey.string("gui.replay.setup-busy-title");
    public static final ConfigKey<String> GUI_REPLAY_SETUP_BUSY_LORE_1 = ConfigKey.string("gui.replay.setup-busy-lore-1");
    public static final ConfigKey<String> GUI_REPLAY_SETUP_STOP_TITLE = ConfigKey.string("gui.replay.setup-stop-title");
    public static final ConfigKey<String> GUI_REPLAY_SETUP_STOP_LORE = ConfigKey.string("gui.replay.setup-stop-lore");
    public static final ConfigKey<String> GUI_REPLAY_SETUP_ARMED_TITLE = ConfigKey.string("gui.replay.setup-armed-title");
    public static final ConfigKey<String> GUI_REPLAY_SETUP_ARMED_LORE_1 = ConfigKey.string("gui.replay.setup-armed-lore-1");
    public static final ConfigKey<String> GUI_REPLAY_SETUP_CANCEL_TITLE = ConfigKey.string("gui.replay.setup-cancel-title");
    public static final ConfigKey<String> GUI_REPLAY_SETUP_CANCEL_LORE = ConfigKey.string("gui.replay.setup-cancel-lore");
    public static final ConfigKey<String> GUI_REPLAY_MENU_NEEDS_PLAYER = ConfigKey.string("gui.replay.menu-needs-player");

    private MessagesKeys() {
    }
}
