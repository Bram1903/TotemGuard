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

package com.deathmotion.totemguard.proxybridge.velocity;

import com.deathmotion.totemguard.proxybridge.common.ProxyConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

final class VelocityConfigLoader {

    private static final String FILE_NAME = "config.toml";

    private VelocityConfigLoader() {
    }

    static ProxyConfig load(Path dataFolder, Logger logger) {
        Path file = dataFolder.resolve(FILE_NAME);
        try {
            if (!Files.exists(file)) {
                Files.createDirectories(dataFolder);
                Files.writeString(file, defaultToml(), StandardCharsets.UTF_8);
                logger.info("Wrote default config to " + file);
                return ProxyConfig.defaults();
            }

            Map<String, String> root = new HashMap<>();
            Map<String, String> redis = new HashMap<>();
            Map<String, String> bucket = root;
            for (String raw : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String line = stripComment(raw).trim();
                if (line.isEmpty()) continue;
                if (line.startsWith("[") && line.endsWith("]")) {
                    String section = line.substring(1, line.length() - 1).trim();
                    bucket = "redis".equals(section) ? redis : new HashMap<>();
                    continue;
                }
                int eq = line.indexOf('=');
                if (eq < 0) continue;
                String key = line.substring(0, eq).trim();
                String value = unquote(line.substring(eq + 1).trim());
                bucket.put(key, value);
            }

            ProxyConfig.Redis r = new ProxyConfig.Redis(
                    redis.getOrDefault("host", "127.0.0.1"),
                    parseInt(redis.get("port"), 6379),
                    redis.getOrDefault("username", ""),
                    redis.getOrDefault("password", ""),
                    parseInt(redis.get("database"), 0),
                    Boolean.parseBoolean(redis.getOrDefault("tls", "false"))
            );
            return new ProxyConfig(
                    Boolean.parseBoolean(root.getOrDefault("enabled", "false")),
                    root.getOrDefault("display-name", "proxy"),
                    r
            );
        } catch (IOException ex) {
            logger.warning("Failed to load " + file + ": " + ex.getMessage() + ", using defaults");
            return ProxyConfig.defaults();
        }
    }

    private static String defaultToml() {
        return """
                #    ___________     __                   ________                       .___
                #    \\__    ___/____/  |_  ____   _____  /  _____/ __ _______ _______  __| _/
                #      |    | /  _ \\   __\\/ __ \\ /     \\/   \\  ___|  |  \\__  \\\\_  __ \\/ __ |
                #      |    |(  <_> )  | \\  ___/|  Y Y  \\    \\_\\  \\  |  // __ \\|  | \\/ /_/ |
                #      |____| \\____/|__|  \\___  >__|_|  /\\______  /____/(____  /__|  \\____ |
                #                             \\/      \\/        \\/           \\/           \\/
                #                          ProxyBridge - optional proxy companion
                
                # =====================================================================
                # FIRST-RUN SETUP
                # ---------------------------------------------------------------------
                #   1. Edit the [redis] section below so it points at the same Redis
                #      instance your TotemGuard backend plugins are using.
                #   2. Flip "enabled" to true.
                #   3. Run /tgbridge reload (or restart the proxy).
                # The bridge will NOT attempt any Redis connection while this is false,
                # so a fresh install won't spam connect errors.
                # =====================================================================
                enabled = false
                
                # Operator-friendly name for this proxy. Surfaces in TotemGuard logs and
                # APIs when a backend reports which proxy it sits behind.
                display-name = "proxy"
                
                # Redis connection. Must point at the same Redis instance the TotemGuard
                # backend plugins are using: that's the whole channel the bridge talks over.
                [redis]
                host = "127.0.0.1"
                port = 6379
                username = ""
                password = ""
                database = 0
                tls = false
                """;
    }

    private static String stripComment(String line) {
        int hash = -1;
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') inQuotes = !inQuotes;
            else if (c == '#' && !inQuotes) {
                hash = i;
                break;
            }
        }
        return hash < 0 ? line : line.substring(0, hash);
    }

    private static String unquote(String value) {
        if (value.length() >= 2 && value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"') {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static int parseInt(String value, int fallback) {
        if (value == null) return fallback;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}
