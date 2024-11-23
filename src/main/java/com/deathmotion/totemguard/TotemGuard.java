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
import com.deathmotion.totemguard.database.DatabaseService;
import com.deathmotion.totemguard.listeners.ReloadListener;
import com.deathmotion.totemguard.manager.*;
import com.deathmotion.totemguard.mojang.MojangService;
import com.deathmotion.totemguard.packetlisteners.ProxyMessenger;
import com.deathmotion.totemguard.packetlisteners.UserTracker;
import com.deathmotion.totemguard.util.MessageService;
import com.deathmotion.totemguard.util.TGVersion;
import com.deathmotion.totemguard.util.UpdateChecker;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import io.github.retrooper.packetevents.bstats.bukkit.Metrics;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

@Getter
public final class TotemGuard extends JavaPlugin {

    @Getter
    private static TotemGuard instance;

    private TGVersion version;
    private ConfigManager configManager;
    private MessageService messageService;

    private DatabaseManager databaseManager;
    private DatabaseService databaseService;
    private MojangService mojangService;
    private AlertManager alertManager;
    private UserTracker userTracker;
    private DiscordManager discordManager;
    private PunishmentManager punishmentManager;
    private CheckManager checkManager;
    private TrackerManager trackerManager;
    private ProxyMessenger proxyMessenger;

    @Override
    public void onEnable() {
        instance = this;
        version = TGVersion.createFromPackageVersion();

        configManager = new ConfigManager(this);

        if (!loadConfig()) {
            instance.getServer().getPluginManager().disablePlugin(instance);
            return;
        }

        messageService = new MessageService(this);

        databaseManager = new DatabaseManager(this);
        databaseService = new DatabaseService(this);
        mojangService = new MojangService(this);

        userTracker = new UserTracker(this);
        alertManager = new AlertManager(this);
        discordManager = new DiscordManager(this);
        punishmentManager = new PunishmentManager(this);
        checkManager = new CheckManager(this);
        trackerManager = new TrackerManager(this);
        proxyMessenger = new ProxyMessenger(this);

        PacketEvents.getAPI().getEventManager().registerListener(userTracker, PacketListenerPriority.LOW);

        if (Bukkit.getPluginManager().getPlugin("BetterReload") != null)
            Bukkit.getPluginManager().registerEvents(new ReloadListener(this), this);

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
            String debugMessage = "[TG DEBUG] " + message;
            getLogger().info(debugMessage);
            Bukkit.broadcast(Component.text(debugMessage), "TotemGuard.Debug");
        }
    }

    private void enableBStats() {
        new Metrics(this, 23179);
    }
}
