/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2025 Bram and contributors
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

import com.deathmotion.totemguard.api.TotemGuardProvider;
import com.deathmotion.totemguard.commands.commandapi.TotemGuardCommand;
import com.deathmotion.totemguard.database.DatabaseProvider;
import com.deathmotion.totemguard.events.bukkit.CheckManagerBukkitListener;
import com.deathmotion.totemguard.events.lunarclient.ApolloPlayerListener;
import com.deathmotion.totemguard.events.packets.CheckManagerPacketListener;
import com.deathmotion.totemguard.events.packets.PacketPingListener;
import com.deathmotion.totemguard.events.packets.PacketPlayerJoinQuit;
import com.deathmotion.totemguard.manager.*;
import com.deathmotion.totemguard.messenger.MessengerService;
import com.deathmotion.totemguard.redis.RedisService;
import com.deathmotion.totemguard.util.UpdateChecker;
import com.deathmotion.totemguard.util.VersionResolver;
import com.github.retrooper.packetevents.PacketEvents;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import dev.jorel.commandapi.CommandAPILogger;
import io.github.retrooper.packetevents.bstats.bukkit.Metrics;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class TotemGuard extends JavaPlugin {

    @Getter
    private static TotemGuard instance;

    private boolean isSupportedVersion = true;

    private ConfigManager configManager;
    private CloudCommandManager cloudCommandManager;
    private DatabaseProvider databaseProvider;
    private MessengerService messengerService;
    private AlertManagerImpl alertManager;
    private PunishmentManager punishmentManager;
    private DiscordManager discordManager;
    private PlayerDataManager playerDataManager;
    private RedisService redisService;
    private UpdateChecker updateChecker;

    public TotemGuard() {
        instance = this;
    }

    @Override
    public void onLoad() {
        if (!new VersionResolver().isSupportedVersion()) {
            isSupportedVersion = false;
            return;
        }

        configManager = new ConfigManager(this);
        CommandAPI.setLogger(CommandAPILogger.fromJavaLogger(getLogger()));
        CommandAPIBukkitConfig config = new CommandAPIBukkitConfig(this);
        config.setNamespace(configManager.getSettings().getCommand());
        CommandAPI.onLoad(config);
    }

    @Override
    public void onEnable() {
        if (!isSupportedVersion) {
            getLogger().severe("Minecraft version " + Bukkit.getMinecraftVersion() + " is not supported by TotemGuard.");
            getLogger().severe("We highly recommend updating your server to at least " + VersionResolver.getMinimumSupportedVersion().getReleaseName() + " or later.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        CommandAPI.onEnable();

        databaseProvider = new DatabaseProvider(this);
        messengerService = new MessengerService(this);
        alertManager = new AlertManagerImpl(this);
        punishmentManager = new PunishmentManager(this);
        discordManager = new DiscordManager(this);
        playerDataManager = new PlayerDataManager(this);
        redisService = new RedisService(this);
        updateChecker = new UpdateChecker(this);

        TotemGuardProvider.setAPI(new TotemGuardAPIImpl(this));

        PacketEvents.getAPI().getEventManager().registerListener(new PacketPlayerJoinQuit(this));
        PacketEvents.getAPI().getEventManager().registerListener(new PacketPingListener());
        PacketEvents.getAPI().getEventManager().registerListener(new CheckManagerPacketListener());

        getServer().getPluginManager().registerEvents(new CheckManagerBukkitListener(), this);

        // Needed for the Lunar Client specific check
        new ApolloPlayerListener(this);

        cloudCommandManager = new CloudCommandManager(this);
        new TotemGuardCommand(this);

        enableBStats();
    }

    @Override
    public void onDisable() {
        if (redisService != null) redisService.stop();
        if (databaseProvider != null) databaseProvider.close();

        CommandAPI.onDisable();
    }

    public void debug(String message) {
        if (configManager.getSettings().isDebug()) {
            String debugMessage = "[TG DEBUG] " + message;
            getLogger().info(debugMessage);
            Bukkit.broadcast(debugMessage, "TotemGuard.Debug");
        }
    }

    private void enableBStats() {
        new Metrics(this, 23179);
    }
}
