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
import com.deathmotion.totemguard.api.events.ApiDisabledEvent;
import com.deathmotion.totemguard.api.events.ApiEnabledEvent;
import com.deathmotion.totemguard.api.interfaces.IConfigManager;
import com.deathmotion.totemguard.config.impl.Checks;
import com.deathmotion.totemguard.config.impl.Settings;
import com.deathmotion.totemguard.config.impl.Webhook;
import com.deathmotion.totemguard.messaging.AlertMessengerRegistry;
import de.exlll.configlib.ConfigLib;
import de.exlll.configlib.YamlConfigurationProperties;
import de.exlll.configlib.YamlConfigurations;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import lombok.Getter;
import org.bukkit.Bukkit;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

@Getter
public class ConfigManager implements IConfigManager {

    private final TotemGuard plugin;
    private Settings settings;
    private Checks checks;
    private Webhook webhook;

    private boolean apiEnabled;

    public ConfigManager(TotemGuard plugin) {
        this.plugin = plugin;
        initializeConfig();
    }

    private void initializeConfig() {
        loadConfigurations();
        scheduleApiStateCheck();
    }

    public void reload() {
        FoliaScheduler.getAsyncScheduler().runNow(plugin, (o) -> {
            plugin.getProxyMessenger().stop();
            loadConfigurations();
            setupProxyMessenger();
            handleApiState(settings.isApi());
        });
    }

    private void loadConfigurations() {
        YamlConfigurationProperties properties = createYamlProperties();
        settings = loadConfigFile(getSettingsFile(), Settings.class, properties, "Failed to load config file");
        checks = loadConfigFile(getChecksFile(), Checks.class, properties, "Failed to load checks file");
        webhook = loadConfigFile(getWebhooksFile(), Webhook.class, properties, "Failed to load webhook file");
    }

    private void scheduleApiStateCheck() {
        FoliaScheduler.getAsyncScheduler().runNow(plugin, (o) -> handleApiState(settings.isApi()));
    }

    private void handleApiState(boolean newApiState) {
        if (apiEnabled != newApiState) {
            apiEnabled = newApiState;
            Bukkit.getPluginManager().callEvent(apiEnabled ? new ApiEnabledEvent() : new ApiDisabledEvent());
        }
    }

    private void setupProxyMessenger() {
        plugin.setProxyMessenger(AlertMessengerRegistry.getMessenger(
                settings.getProxyAlerts().getMethod(),
                plugin
        ).orElseThrow(() -> new RuntimeException("Unknown proxy messaging method in config.yml!")));
        plugin.getProxyMessenger().start();
    }

    private File getSettingsFile() {
        return new File(plugin.getDataFolder(), "config.yml");
    }

    private File getChecksFile() {
        return new File(plugin.getDataFolder(), "checks.yml");
    }

    private File getWebhooksFile() {
        return new File(plugin.getDataFolder(), "webhooks.yml");
    }

    private YamlConfigurationProperties createYamlProperties() {
        return ConfigLib.BUKKIT_DEFAULT_PROPERTIES.toBuilder()
                .charset(StandardCharsets.UTF_8)
                .outputNulls(true)
                .inputNulls(false)
                .header(createConfigHeader())
                .build();
    }

    private <T> T loadConfigFile(File file, Class<T> configClass, YamlConfigurationProperties properties, String errorMessage) {
        try {
            return YamlConfigurations.update(file.toPath(), configClass, properties);
        } catch (Exception e) {
            logAndDisable(errorMessage, e);
            return null;
        }
    }

    private String createConfigHeader() {
        return """
                  ___________     __                   ________                       .___
                  \\__    ___/____/  |_  ____   _____  /  _____/ __ _______ _______  __| _/
                    |    | /  _ \\   __\\/ __ \\ /     \\/   \\  ___|  |  \\__  \\\\_  __ \\/ __ |
                    |    |(  <_> )  | \\  ___/|  Y Y  \\    \\_\\  \\  |  // __ \\|  | \\/ /_/ |
                    |____| \\____/|__|  \\___  >__|_|  /\\______  /____/(____  /__|  \\____ |
                                           \\/      \\/        \\/           \\/           \\/
                """;
    }

    private void logAndDisable(String message, Exception e) {
        plugin.getLogger().log(Level.SEVERE, message, e);
        plugin.getServer().getPluginManager().disablePlugin(plugin);
    }
}
