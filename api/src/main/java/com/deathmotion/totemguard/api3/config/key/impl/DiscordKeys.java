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

package com.deathmotion.totemguard.api3.config.key.impl;

import com.deathmotion.totemguard.api3.config.key.ConfigValueKey;
import org.jetbrains.annotations.NotNull;

/**
 * Typed keys for {@code discord.yml}.
 * <p>
 * Per-webhook settings are accessed via the prefix-based factory helpers (e.g. {@link #url(String)}),
 * passing {@link #ALERTS_PREFIX} or {@link #PUNISHMENTS_PREFIX} so the same schema is shared
 * between the two webhook channels.
 */
public final class DiscordKeys {

    public static final String ALERTS_PREFIX = "alerts";
    public static final String PUNISHMENTS_PREFIX = "punishments";

    public static final ConfigValueKey<String> BACKTICK_REPLACEMENT =
            ConfigValueKey.required("backtick-replacement", "ʼ");

    private DiscordKeys() {
    }

    public static @NotNull ConfigValueKey<Boolean> enabled(@NotNull String prefix) {
        return ConfigValueKey.required(prefix + ".enabled", false);
    }

    public static @NotNull ConfigValueKey<String> url(@NotNull String prefix) {
        return ConfigValueKey.required(prefix + ".url", "");
    }

    public static @NotNull ConfigValueKey<String> username(@NotNull String prefix) {
        return ConfigValueKey.required(prefix + ".username", "TotemGuard");
    }

    public static @NotNull ConfigValueKey<String> avatar(@NotNull String prefix) {
        return ConfigValueKey.required(prefix + ".avatar", "");
    }

    public static @NotNull ConfigValueKey<String> title(@NotNull String prefix) {
        return ConfigValueKey.required(prefix + ".title", "TotemGuard");
    }

    public static @NotNull ConfigValueKey<String> color(@NotNull String prefix) {
        return ConfigValueKey.required(prefix + ".color", "#d9b61a");
    }

    public static @NotNull ConfigValueKey<Boolean> timestamp(@NotNull String prefix) {
        return ConfigValueKey.required(prefix + ".timestamp", true);
    }

    public static @NotNull ConfigValueKey<String> thumbnail(@NotNull String prefix) {
        return ConfigValueKey.required(prefix + ".thumbnail", "");
    }

    public static @NotNull ConfigValueKey<String> footer(@NotNull String prefix) {
        return ConfigValueKey.required(prefix + ".footer", "");
    }

    public static @NotNull String fieldsPath(@NotNull String prefix) {
        return prefix + ".fields";
    }
}
