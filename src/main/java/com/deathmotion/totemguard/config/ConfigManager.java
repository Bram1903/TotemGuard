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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.deathmotion.totemguard.config;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.api.interfaces.IConfigManager;
import com.deathmotion.totemguard.messaging.AlertMessengerRegistry;
import de.exlll.configlib.ConfigLib;
import de.exlll.configlib.YamlConfigurationProperties;
import de.exlll.configlib.YamlConfigurations;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import lombok.Getter;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

@Getter
public class ConfigManager implements IConfigManager {

    private final TotemGuard plugin;
    private Settings settings;

    public ConfigManager(TotemGuard plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        File settingsFile = getSettingsFile();
        YamlConfigurationProperties properties = createYamlProperties();

        try {
            settings = YamlConfigurations.update(settingsFile.toPath(), Settings.class, properties);
        } catch (Exception e) {
            logAndDisable("Failed to create default config file during load", e);
        }
    }

    public void reload() {
        FoliaScheduler.getAsyncScheduler().runNow(plugin, (o) -> {
            File settingsFile = getSettingsFile();
            YamlConfigurationProperties properties = createYamlProperties();

            if (!settingsFile.exists()) {
                try {
                    plugin.getLogger().info("Recreating config file...");
                    settings = YamlConfigurations.update(settingsFile.toPath(), Settings.class, properties);
                } catch (Exception e) {
                    logAndDisable("Failed to create default config file during reload", e);
                }
            }

            plugin.getProxyMessenger().stop();

            try {
                settings = YamlConfigurations.load(settingsFile.toPath(), Settings.class, properties);
            } catch (Exception e) {
                logAndDisable("Failed to load config file during reload", e);
            }

            configureProxyMessenger();
        });
    }

    private void configureProxyMessenger() {
        plugin.setProxyMessenger(AlertMessengerRegistry.getMessenger(settings.getProxyAlerts().getMethod(), plugin).orElseThrow(() -> new RuntimeException("Unknown proxy messaging method in config.yml!")));
        plugin.getProxyMessenger().start();
    }

    private File getSettingsFile() {
        return new File(plugin.getDataFolder(), "config.yml");
    }

    private YamlConfigurationProperties createYamlProperties() {
        return ConfigLib.BUKKIT_DEFAULT_PROPERTIES.toBuilder()
                .charset(StandardCharsets.UTF_8)
                .outputNulls(true)
                .inputNulls(false)
                .header(createHeader())
                .build();
    }

    private void logAndDisable(String message, Exception e) {
        plugin.getLogger().log(Level.SEVERE, message, e);
        plugin.getServer().getPluginManager().disablePlugin(plugin);
    }

    private String createHeader() {
        return """
                  ___________     __                   ________                       .___
                  \\__    ___/____/  |_  ____   _____  /  _____/ __ _______ _______  __| _/
                    |    | /  _ \\   __\\/ __ \\ /     \\/   \\  ___|  |  \\__  \\\\_  __ \\/ __ |
                    |    |(  <_> )  | \\  ___/|  Y Y  \\    \\_\\  \\  |  // __ \\|  | \\/ /_/ |
                    |____| \\____/|__|  \\___  >__|_|  /\\______  /____/(____  /__|  \\____ |
                                           \\/      \\/        \\/           \\/           \\/\
                """;
    }
}
