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

package com.deathmotion.totemguard.util;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.config.Settings;
import com.deathmotion.totemguard.data.Constants;
import com.deathmotion.totemguard.packetlisteners.UpdateNotifier;
import com.github.retrooper.packetevents.PacketEvents;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.CompletableFuture;

public class UpdateChecker {
    private final TotemGuard plugin;
    private final Settings settings;

    public UpdateChecker(TotemGuard plugin) {
        this.plugin = plugin;
        this.settings = plugin.getConfigManager().getSettings();

        if (settings.getUpdateChecker().isEnabled()) {
            checkForUpdate();
        }
    }

    public void checkForUpdate() {
        CompletableFuture.runAsync(() -> {
            try {
                TGVersion localVersion = TGVersions.CURRENT;
                TGVersion latestVersion = fetchLatestGitHubVersion();

                if (latestVersion != null) {
                    handleVersionComparison(localVersion, latestVersion);
                } else {
                    plugin.getLogger().warning("Unable to fetch the latest version from GitHub.");
                }
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to check for updates: " + ex.getMessage());
            }
        });
    }

    private TGVersion fetchLatestGitHubVersion() {
        try {
            URLConnection connection = new URL(Constants.GITHUB_API_URL).openConnection();
            connection.addRequestProperty("User-Agent", "Mozilla/4.0");
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String jsonResponse = reader.readLine();
            reader.close();
            JsonObject jsonObject = new Gson().fromJson(jsonResponse, JsonObject.class);
            return TGVersion.fromString(jsonObject.get("tag_name").getAsString().replaceFirst("^[vV]", ""));
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to parse TotemGuard version! Version API: " + e.getMessage());
            return null;
        }
    }

    private void handleVersionComparison(TGVersion localVersion, TGVersion latestVersion) {
        if (localVersion.isOlderThan(latestVersion)) {
            notifyUpdateAvailable(localVersion, latestVersion);
        } else if (localVersion.isNewerThan(latestVersion)) {
            notifyOnDevBuild(localVersion, latestVersion);
        }
    }

    private void notifyUpdateAvailable(TGVersion currentVersion, TGVersion newVersion) {
        if (settings.getUpdateChecker().isPrintToConsole()) {
            plugin.getServer().getConsoleSender().sendMessage(Component.text("[TotemGuard] ", NamedTextColor.DARK_GREEN)
                    .append(Component.text("Update available! ", NamedTextColor.BLUE))
                    .append(Component.text("Current version: ", NamedTextColor.WHITE))
                    .append(Component.text(currentVersion.toString(), NamedTextColor.GOLD))
                    .append(Component.text(" | New version: ", NamedTextColor.WHITE))
                    .append(Component.text(newVersion.toString(), NamedTextColor.DARK_PURPLE)));
        }
        if (settings.getUpdateChecker().isNotifyInGame()) {
            PacketEvents.getAPI().getEventManager().registerListener(new UpdateNotifier(plugin, newVersion));
        }
    }

    private void notifyOnDevBuild(TGVersion currentVersion, TGVersion newVersion) {
        if (settings.getUpdateChecker().isPrintToConsole()) {
            plugin.getServer().getConsoleSender().sendMessage(Component.text("[TotemGuard] ", NamedTextColor.DARK_GREEN)
                    .append(Component.text("Development build detected. ", NamedTextColor.WHITE))
                    .append(Component.text("Current version: ", NamedTextColor.WHITE))
                    .append(Component.text(currentVersion.toString(), NamedTextColor.AQUA))
                    .append(Component.text(" | Latest stable version: ", NamedTextColor.WHITE))
                    .append(Component.text(newVersion.toString(), NamedTextColor.DARK_AQUA)));
        }
    }
}
