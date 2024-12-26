/*
 *  This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 *  Copyright (C) 2024 Bram and contributors
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.deathmotion.totemguard.manager;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.config.Checks;
import com.deathmotion.totemguard.config.Messages;
import com.deathmotion.totemguard.config.Settings;
import com.deathmotion.totemguard.config.Webhooks;
import com.deathmotion.totemguard.messaging.AlertMessengerRegistry;
import com.deathmotion.totemguard.models.TotemPlayer;
import com.github.retrooper.packetevents.netty.channel.ChannelHelper;
import de.exlll.configlib.NameFormatters;
import de.exlll.configlib.YamlConfigurationProperties;
import de.exlll.configlib.YamlConfigurations;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import lombok.Getter;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

@Getter
public class ConfigManager {
    private final TotemGuard plugin;

    private Settings settings;
    private Checks checks;
    private Messages messages;
    private Webhooks webhooks;

    public ConfigManager(TotemGuard plugin) {
        this.plugin = plugin;
        initializeConfig();
    }

    private void initializeConfig() {
        loadConfigurations();
    }

    public void reload() {
        FoliaScheduler.getAsyncScheduler().runNow(plugin, (o) -> {
            loadConfigurations();
            setupProxyMessenger();

            // Reload checks for all players
            for (TotemPlayer totemPlayer : TotemGuard.getInstance().getPlayerDataManager().getEntries()) {
                ChannelHelper.runInEventLoop(totemPlayer.user.getChannel(), totemPlayer::reload);
            }
        });
    }

    private void loadConfigurations() {
        YamlConfigurationProperties properties = createYamlProperties();
        settings = loadConfigFile(getSettingsFile(), Settings.class, properties, "Failed to load config file");
        checks = loadConfigFile(getChecksFile(), Checks.class, properties, "Failed to load checks file");
        messages = loadConfigFile(getMessagesFile(), Messages.class, properties, "Failed to load messages file");
        webhooks = loadConfigFile(getWebhookFile(), Webhooks.class, properties, "Failed to load webhooks file");
    }

    private void setupProxyMessenger() {
        plugin.setProxyMessenger(AlertMessengerRegistry.getMessenger(
                settings.getProxy().getMethod(),
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

    private File getMessagesFile() {
        return new File(plugin.getDataFolder(), "messages.yml");
    }

    private File getWebhookFile() {
        return new File(plugin.getDataFolder(), "webhooks.yml");
    }

    private YamlConfigurationProperties createYamlProperties() {
        return YamlConfigurationProperties.newBuilder()
                .charset(StandardCharsets.UTF_8)
                .setNameFormatter(NameFormatters.LOWER_KEBAB_CASE)
                .outputNulls(false)
                .inputNulls(true)
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
