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

package com.deathmotion.totemguard.proxybridge.bungee;

import com.deathmotion.totemguard.proxybridge.common.ProxyConfig;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

final class BungeeConfigLoader {

    private static final String FILE_NAME = "config.yml";

    private BungeeConfigLoader() {
    }

    static ProxyConfig load(Path dataFolder, Logger logger) {
        Path file = dataFolder.resolve(FILE_NAME);
        try {
            if (!Files.exists(file)) {
                Files.createDirectories(dataFolder);
                Files.writeString(file, defaultYaml(), StandardCharsets.UTF_8);
                logger.info("Wrote default config to " + file);
                return ProxyConfig.defaults();
            }

            Configuration cfg = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file.toFile());
            Configuration redis = cfg.getSection("redis");
            ProxyConfig.Redis r = new ProxyConfig.Redis(
                    redis.getString("host", "127.0.0.1"),
                    redis.getInt("port", 6379),
                    redis.getString("username", ""),
                    redis.getString("password", ""),
                    redis.getInt("database", 0),
                    redis.getBoolean("tls", false)
            );
            return new ProxyConfig(
                    cfg.getBoolean("enabled", false),
                    cfg.getString("display-name", "proxy"),
                    r
            );
        } catch (IOException ex) {
            logger.warning("Failed to load " + file + ": " + ex.getMessage() + ", using defaults");
            return ProxyConfig.defaults();
        }
    }

    private static String defaultYaml() {
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
                #   1. Edit the 'redis' section below so it points at the same Redis
                #      instance your TotemGuard backend plugins are using.
                #   2. Flip 'enabled' to true.
                #   3. Run /tgbridge reload (or restart the proxy).
                # The bridge will NOT attempt any Redis connection while this is false,
                # so a fresh install won't spam connect errors.
                # =====================================================================
                enabled: false
                
                # Operator-friendly name for this proxy. Surfaces in TotemGuard logs and
                # APIs when a backend reports which proxy it sits behind.
                display-name: proxy
                
                # Redis connection. Must point at the same Redis instance the TotemGuard
                # backend plugins are using: that's the whole channel the bridge talks over.
                redis:
                  host: 127.0.0.1
                  port: 6379
                  username: ""
                  password: ""
                  database: 0
                  tls: false
                """;
    }
}
