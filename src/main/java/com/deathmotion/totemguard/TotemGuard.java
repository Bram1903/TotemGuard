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

package com.deathmotion.totemguard;

import com.deathmotion.totemguard.commands.TotemGuardCommand;
import com.deathmotion.totemguard.config.ConfigManager;
import com.deathmotion.totemguard.listeners.UserTracker;
import com.deathmotion.totemguard.manager.AlertManager;
import com.deathmotion.totemguard.manager.CheckManager;
import com.deathmotion.totemguard.manager.DiscordManager;
import com.deathmotion.totemguard.manager.PunishmentManager;
import com.deathmotion.totemguard.util.TGVersion;
import com.deathmotion.totemguard.util.UpdateChecker;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import io.github.retrooper.packetevents.bstats.Metrics;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

public final class TotemGuard extends JavaPlugin {

    @Getter
    private static TotemGuard instance;

    @Getter
    private TGVersion version;

    @Getter
    private ConfigManager configManager;
    @Getter
    private AlertManager alertManager;
    @Getter
    private UserTracker userTracker;
    @Getter
    private DiscordManager discordManager;
    @Getter
    private PunishmentManager punishmentManager;
    @Getter
    private CheckManager checkManager;

    @Override
    public void onEnable() {
        instance = this;
        version = TGVersion.createFromPackageVersion();

        configManager = new ConfigManager(this);

        if (!loadConfig()) {
            instance.getServer().getPluginManager().disablePlugin(instance);
            return;
        }

        alertManager = new AlertManager(this);
        userTracker = new UserTracker(this);
        discordManager = new DiscordManager(this);
        punishmentManager = new PunishmentManager(this);
        checkManager = new CheckManager(this);

        PacketEvents.getAPI().getEventManager().registerListener(userTracker, PacketListenerPriority.LOW);

        new TotemGuardCommand(this);
        new UpdateChecker(this);
        enableBStats();
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabling TotemGuard...");
    }

    /**
     * Loads the plugin configuration.
     *
     * @return true if the configuration was loaded successfully, false otherwise.
     */
    private boolean loadConfig() {
        final Optional<Throwable> error = configManager.loadConfig();
        if (error.isPresent()) {
            instance.getLogger().log(java.util.logging.Level.SEVERE, "Failed to load configuration", error.get());
            return false;
        }
        return true;
    }

    public int getTps() {
        return (int) Math.round(Bukkit.getTPS()[0]);
    }

    public void debug(String message) {
        if (configManager.getSettings().isDebug()) {
            getLogger().info("[DEBUG] " + message);
        }
    }

    private void enableBStats() {
        try {
            new Metrics(this, 23179);
        } catch (Exception e) {
            this.getLogger().warning("Something went wrong while enabling bStats.\n" + e.getMessage());
        }
    }
}
