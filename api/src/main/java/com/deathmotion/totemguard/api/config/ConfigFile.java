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

/**
 * Identifies a configuration file managed by TotemGuard.
 * <p>
 * The filenames are the resource names inside the plugin JAR and the persisted names
 * inside the server's configuration directory.
 */
public enum ConfigFile {

    /**
     * Main plugin configuration.
     */
    CONFIG("config.yml"),

    /**
     * Check configuration.
     */
    CHECKS("checks.yml"),

    /**
     * User-facing messages (prefixes, alerts, etc.).
     */
    MESSAGES("messages.yml"),

    /**
     * Client mod detection configuration.
     */
    MODS("mods.yml");

    private final String fileName;

    ConfigFile(String fileName) {
        this.fileName = fileName;
    }

    /**
     * Returns the file name for this config (resource name and persisted name).
     *
     * @return the config file name
     */
    public String fileName() {
        return fileName;
    }
}
