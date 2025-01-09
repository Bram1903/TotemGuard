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
import com.deathmotion.totemguard.database.DatabaseService;
import com.deathmotion.totemguard.events.bukkit.CheckManagerBukkitListener;
import com.deathmotion.totemguard.events.packets.CheckManagerPacketListener;
import com.deathmotion.totemguard.events.packets.PacketConfigurationListener;
import com.deathmotion.totemguard.events.packets.PacketPlayerJoinQuit;
import com.deathmotion.totemguard.manager.*;
import com.deathmotion.totemguard.messaging.AlertMessengerRegistry;
import com.deathmotion.totemguard.messaging.ProxyAlertMessenger;
import com.deathmotion.totemguard.messenger.MessengerService;
import com.deathmotion.totemguard.util.UpdateChecker;
import com.github.retrooper.packetevents.PacketEvents;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import dev.jorel.commandapi.CommandAPILogger;
import io.github.retrooper.packetevents.bstats.bukkit.Metrics;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class TotemGuard extends JavaPlugin {

    @Getter
    private static TotemGuard instance;

    private ConfigManager configManager;
    private MessengerService messengerService;
    private AlertManagerImpl alertManager;
    private PunishmentManager punishmentManager;
    private DiscordManager discordManager;
    @Setter
    private ProxyAlertMessenger proxyMessenger;
    private PlayerDataManager playerDataManager;
    private DatabaseManager databaseManager;
    private DatabaseService databaseService;
    private UpdateChecker updateChecker;
    private TotemGuardAPIImpl totemGuardAPI;

    @Override
    public void onLoad() {
        CommandAPI.setLogger(CommandAPILogger.fromJavaLogger(getLogger()));
        CommandAPIBukkitConfig config = new CommandAPIBukkitConfig(this);
        config.usePluginNamespace();
        CommandAPI.onLoad(config);
    }

    @Override
    public void onEnable() {
        instance = this;

        configManager = new ConfigManager(this);
        messengerService = new MessengerService(this);
        alertManager = new AlertManagerImpl(this);
        punishmentManager = new PunishmentManager(this);
        discordManager = new DiscordManager(this);
        proxyMessenger = AlertMessengerRegistry.getMessenger(configManager.getSettings().getProxy().getMethod(), this).orElseThrow(() -> new RuntimeException("Unknown proxy messaging method in config.yml!"));
        playerDataManager = new PlayerDataManager(this);
        databaseManager = new DatabaseManager(this);
        databaseService = new DatabaseService(this);
        totemGuardAPI = new TotemGuardAPIImpl(this);
        updateChecker = new UpdateChecker(this);

        PacketEvents.getAPI().getEventManager().registerListener(new PacketConfigurationListener());
        PacketEvents.getAPI().getEventManager().registerListener(new PacketPlayerJoinQuit(this));
        PacketEvents.getAPI().getEventManager().registerListener(new CheckManagerPacketListener());
        getServer().getPluginManager().registerEvents(new CheckManagerBukkitListener(), this);

        CommandAPI.onEnable();
        new TotemGuardCommand(this);
        enableBStats();
    }

    @Override
    public void onDisable() {
        if (proxyMessenger != null) proxyMessenger.stop();
        if (databaseManager != null) databaseManager.close();

        CommandAPI.onDisable();
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
