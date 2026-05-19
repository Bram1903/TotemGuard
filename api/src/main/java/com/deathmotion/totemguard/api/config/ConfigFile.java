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

package com.deathmotion.totemguard.api.config;

import org.jetbrains.annotations.NotNull;

/**
 * A configuration file managed by TotemGuard. Filenames are both the bundled resource
 * name and the on-disk name.
 */
public enum ConfigFile {

    /**
     * Main plugin configuration, the {@code config.yml} that owns server-wide toggles and integrations.
     */
    CONFIG("config.yml"),

    /**
     * Per-check tuning ({@code checks.yml}), enable flag, thresholds, punishment commands.
     */
    CHECKS("checks.yml"),

    /**
     * User-facing strings ({@code messages.yml}), the source for every chat and GUI line.
     */
    MESSAGES("messages.yml"),

    /**
     * Mod-detection ruleset ({@code mods.yml}), severities, payload patterns, translation keys.
     */
    MODS("mods.yml"),

    /**
     * Discord webhook routing ({@code discord.yml}), endpoint URLs and template overrides.
     */
    DISCORD("discord.yml");

    private final String fileName;

    ConfigFile(String fileName) {
        this.fileName = fileName;
    }

    /**
     * Bundled-resource and on-disk filename for this config. Identical strings so the
     * default resource can be copied to disk unchanged.
     */
    @NotNull
    public String fileName() {
        return fileName;
    }
}
