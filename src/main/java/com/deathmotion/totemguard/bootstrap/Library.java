/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2024 Bram and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.deathmotion.totemguard.bootstrap;

import com.deathmotion.totemguard.api.versioning.TGVersion;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Getter
public enum Library {
    CONFIGLIB("de.exlll", "configlib-yaml", "4.5.0"),
    DISCORD_WEBHOOK("club.minnced", "discord-webhooks", "0.8.0"),
    LETTUCE("io.lettuce", "lettuce-core", "6.5.1.RELEASE"),
    EXPIRINGMAP("net.jodah", "expiringmap", "0.5.11"),
    COMMANDAPI_MOJANG_MAPPED("dev.jorel", "commandapi-bukkit-shade-mojang-mapped", "9.7.0"),
    COMMANDAPI("dev.jorel", "commandapi-bukkit-shade", "9.7.0");

    private final String group;
    private final String name;
    private final String version;

    Library(String group, String name, String version) {
        this.group = group;
        this.name = name;
        this.version = version;
    }

    /**
     * Retrieves the appropriate libraries for the current plugin version.
     *
     * @return An array of filtered libraries.
     */
    public static Library[] getFilteredLibraries() {
        Library commandApiLibrary = getServerVersion().isNewerThan(TGVersion.fromString("1.20.4"))
                ? COMMANDAPI_MOJANG_MAPPED
                : COMMANDAPI;
        return new Library[]{CONFIGLIB, DISCORD_WEBHOOK, LETTUCE, EXPIRINGMAP, commandApiLibrary};
    }

    /**
     * Reads and extracts the plugin version from the version.json file inside the JAR.
     *
     * @return The plugin version.
     */
    private static TGVersion getServerVersion() {
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("version.json")) {
            if (inputStream == null) {
                throw new RuntimeException("version.json resource not found in the server JAR");
            }
            JsonObject jsonObject = JsonParser.parseReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).getAsJsonObject();
            String version = jsonObject.get("id").getAsString();
            return TGVersion.fromString(version);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read or parse version from server JAR: " + e.getMessage(), e);
        }
    }

    /**
     * Formats the Maven dependency string.
     *
     * @return The formatted Maven dependency string.
     */
    public String getMavenDependency() {
        return String.format("%s:%s:%s", group, name, version);
    }
}
