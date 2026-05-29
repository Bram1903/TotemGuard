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

package com.deathmotion.totemguard.discord.config;

import com.deathmotion.totemguard.api.event.events.TGDiagnosticEvent.Severity;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record BotConfig(
        @NotNull String token,
        @NotNull StatusConfig status,
        @NotNull CommandsConfig commands,
        @NotNull ChannelConfig alerts,
        @NotNull ChannelConfig punishments,
        @NotNull DiagnosticsConfig diagnostics
) {
    public static final List<String> KNOWN_COMMANDS =
            List.of("history", "status", "version", "stats", "reload", "restart", "update");

    private static final int DEFAULT_ALERT_COLOR = 0xFBAF00;
    private static final int DEFAULT_PUNISHMENT_COLOR = 0xD4452C;

    public static @NotNull BotConfig fromMap(Object rawRoot) {
        YamlAccess root = YamlAccess.of(rawRoot);

        YamlAccess bot = root.section("bot");
        String token = bot.string("token", "").trim();

        YamlAccess statusYaml = bot.section("status");
        StatusConfig status = new StatusConfig(
                statusYaml.bool("enabled", true),
                statusYaml.string("activity", "WATCHING").trim().toUpperCase(),
                statusYaml.string("text", "%players% players"),
                Math.max(15, statusYaml.integer("update-interval-seconds", 60))
        );

        YamlAccess cmds = root.section("commands");
        YamlAccess enabledYaml = cmds.section("enabled");
        Map<String, Boolean> enabled = new LinkedHashMap<>();
        for (String name : KNOWN_COMMANDS) {
            enabled.put(name, enabledYaml.bool(name, true));
        }

        YamlAccess permsYaml = cmds.section("permissions");
        YamlAccess overridesYaml = permsYaml.section("overrides");
        Map<String, CommandPermission> overrides = new LinkedHashMap<>();
        for (String name : KNOWN_COMMANDS) {
            CommandPermission override = permission(overridesYaml.section(name));
            if (!override.isEmpty()) overrides.put(name, override);
        }

        CommandsConfig commands = new CommandsConfig(
                sanitizeBase(cmds.string("base", "totemguard")),
                List.copyOf(cmds.longList("guild-ids")),
                Map.copyOf(enabled),
                permission(permsYaml.section("regular")),
                permission(permsYaml.section("admin")),
                Map.copyOf(overrides)
        );

        ChannelConfig alerts = channel(root.section("alerts"), "TotemGuard Alert", DEFAULT_ALERT_COLOR);
        ChannelConfig punishments = channel(root.section("punishments"), "TotemGuard Punishment", DEFAULT_PUNISHMENT_COLOR);
        DiagnosticsConfig diagnostics = diagnostics(root.section("diagnostics"));

        return new BotConfig(token, status, commands, alerts, punishments, diagnostics);
    }

    private static DiagnosticsConfig diagnostics(YamlAccess yaml) {
        return new DiagnosticsConfig(
                yaml.bool("enabled", false),
                yaml.longValue("channel-id", 0L),
                parseSeverity(yaml.string("min-severity", "INFO"))
        );
    }

    private static Severity parseSeverity(String raw) {
        try {
            return Severity.valueOf(raw.trim().toUpperCase());
        } catch (RuntimeException e) {
            return Severity.INFO;
        }
    }

    private static CommandPermission permission(YamlAccess yaml) {
        return new CommandPermission(
                Set.copyOf(yaml.longList("role-ids")),
                Set.copyOf(yaml.longList("user-ids")));
    }

    private static ChannelConfig channel(YamlAccess yaml, String defaultTitle, int defaultColor) {
        return new ChannelConfig(
                yaml.bool("enabled", false),
                yaml.longValue("channel-id", 0L),
                parseColor(yaml.string("color", ""), defaultColor),
                yaml.string("title", defaultTitle)
        );
    }

    private static String sanitizeBase(String raw) {
        String cleaned = raw.trim().toLowerCase().replaceAll("[^a-z0-9_-]", "");
        if (cleaned.isEmpty()) return "totemguard";
        return cleaned.length() > 32 ? cleaned.substring(0, 32) : cleaned;
    }

    private static int parseColor(String hex, int fallback) {
        if (hex == null || hex.isBlank()) return fallback;
        String trimmed = hex.trim();
        if (trimmed.startsWith("#")) trimmed = trimmed.substring(1);
        try {
            return Integer.parseInt(trimmed, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public boolean botEnabled() {
        return !token.isBlank();
    }

    public record StatusConfig(boolean enabled, @NotNull String activity, @NotNull String text,
                               int updateIntervalSeconds) {
    }

    public record CommandsConfig(
            @NotNull String base,
            @NotNull List<Long> guildIds,
            @NotNull Map<String, Boolean> enabled,
            @NotNull CommandPermission regular,
            @NotNull CommandPermission admin,
            @NotNull Map<String, CommandPermission> overrides
    ) {
        public boolean isEnabled(@NotNull String name) {
            return enabled.getOrDefault(name, true);
        }

        public @NotNull CommandPermission permission(@NotNull String name, boolean control) {
            CommandPermission override = overrides.get(name);
            if (override != null) return override;
            return control ? admin : regular;
        }
    }

    public record CommandPermission(@NotNull Set<Long> roleIds, @NotNull Set<Long> userIds) {
        public boolean isEmpty() {
            return roleIds.isEmpty() && userIds.isEmpty();
        }
    }

    public record ChannelConfig(boolean enabled, long channelId, int color, @NotNull String title) {
        public boolean usable() {
            return enabled && channelId != 0L;
        }
    }

    public record DiagnosticsConfig(boolean enabled, long channelId, @NotNull Severity minSeverity) {
        public boolean usable() {
            return enabled && channelId != 0L;
        }

        public boolean passes(@NotNull Severity severity) {
            return severity.ordinal() >= minSeverity.ordinal();
        }
    }
}
