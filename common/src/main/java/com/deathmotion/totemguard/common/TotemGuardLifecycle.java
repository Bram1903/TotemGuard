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

package com.deathmotion.totemguard.common;

import com.deathmotion.totemguard.api.TotemGuard;
import com.deathmotion.totemguard.api.event.impl.TGPluginShutdownEvent;
import com.deathmotion.totemguard.common.cache.CacheRepositoryImpl;
import com.deathmotion.totemguard.common.commands.CommandManagerImpl;
import com.deathmotion.totemguard.common.config.ConfigRepositoryImpl;
import com.deathmotion.totemguard.common.database.DatabaseRepositoryImpl;
import com.deathmotion.totemguard.common.event.EventRepositoryImpl;
import com.deathmotion.totemguard.common.event.api.impl.TGPluginShutdownEventImpl;
import com.deathmotion.totemguard.common.event.internal.InternalPlayerEvent;
import com.deathmotion.totemguard.common.event.internal.impl.InventoryChangedEvent;
import com.deathmotion.totemguard.common.event.internal.listeners.EventCheckManagerListener;
import com.deathmotion.totemguard.common.event.internal.listeners.TotemReplenishedListener;
import com.deathmotion.totemguard.common.event.packet.PacketCheckManagerListener;
import com.deathmotion.totemguard.common.event.packet.PacketPlayerJoinQuit;
import com.deathmotion.totemguard.common.features.alert.AlertRepositoryImpl;
import com.deathmotion.totemguard.common.features.check.CheckService;
import com.deathmotion.totemguard.common.features.discord.DiscordWebhookService;
import com.deathmotion.totemguard.common.features.follow.FollowRepository;
import com.deathmotion.totemguard.common.features.follow.FollowerPacketListener;
import com.deathmotion.totemguard.common.features.history.HistoryRepositoryImpl;
import com.deathmotion.totemguard.common.features.integration.IntegrationRegistrar;
import com.deathmotion.totemguard.common.features.mods.*;
import com.deathmotion.totemguard.common.features.monitor.MonitorRepository;
import com.deathmotion.totemguard.common.features.punishment.PunishmentRepositoryImpl;
import com.deathmotion.totemguard.common.features.session.SessionViolationStore;
import com.deathmotion.totemguard.common.features.stats.StatsRepositoryImpl;
import com.deathmotion.totemguard.common.features.teleport.TeleportService;
import com.deathmotion.totemguard.common.features.update.UpdateCheckerRepositoryImpl;
import com.deathmotion.totemguard.common.fleet.FleetCacheLifecycle;
import com.deathmotion.totemguard.common.gui.GuiManager;
import com.deathmotion.totemguard.common.gui.GuiPacketListener;
import com.deathmotion.totemguard.common.message.MessageService;
import com.deathmotion.totemguard.common.network.NetworkPresenceRepository;
import com.deathmotion.totemguard.common.network.ProxyTopologyService;
import com.deathmotion.totemguard.common.network.ServerIdentity;
import com.deathmotion.totemguard.common.network.bridge.BridgeManager;
import com.deathmotion.totemguard.common.network.bridge.BridgePacketListener;
import com.deathmotion.totemguard.common.placeholder.PlaceholderRepositoryImpl;
import com.deathmotion.totemguard.common.player.PlayerRepositoryImpl;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.redis.RedisRepositoryImpl;
import com.deathmotion.totemguard.common.reload.ReloadService;
import com.deathmotion.totemguard.common.util.CompatibilityUtil;
import com.deathmotion.totemguard.common.util.ConsoleBanner;
import com.deathmotion.totemguard.common.util.LoggerSuppressor;
import com.deathmotion.totemguard.common.util.TGVersions;
import com.deathmotion.totemguard.integrity.JarIntegrityChecker;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.player.User;
import com.google.common.base.Stopwatch;

import java.util.Collection;
import java.util.UUID;

final class TotemGuardLifecycle {

    private TotemGuardLifecycle() {
    }

    static void enable(TGPlatform p) {
        LoggerSuppressor.suppressDefaultNoise();

        Stopwatch stopwatch = Stopwatch.createStarted();
        if (!CompatibilityUtil.isCompatible()) {
            p.setEnabled(false);
            p.disablePlugin();
            return;
        }

        ConsoleBanner.print();

        // The loader already runs this exact check against the same jar before injecting
        // the api classes, so skip it here to avoid double-logging when loader-driven.
        if (!p.isManagedByLoader() && !new JarIntegrityChecker(p.getLogger(), "TotemGuard").verifyCurrentJar()) {
            p.setEnabled(false);
            p.disablePlugin();
            return;
        }

        p.reloadService = new ReloadService();
        p.configRepository = new ConfigRepositoryImpl();
        ModRegistry.load();
        p.placeholderRepository = new PlaceholderRepositoryImpl();
        ServerIdentity serverIdentity = ServerIdentity.create();
        p.redisRepository = new RedisRepositoryImpl(serverIdentity.instanceId());
        p.databaseRepository = new DatabaseRepositoryImpl();
        p.cacheRepository = new CacheRepositoryImpl();
        p.messageService = new MessageService();
        p.eventRepository = new EventRepositoryImpl();
        p.punishmentRepository = new PunishmentRepositoryImpl();
        p.alertRepository = new AlertRepositoryImpl();
        p.discordWebhookService = new DiscordWebhookService();
        p.playerRepository = new PlayerRepositoryImpl();
        p.integrationRegistrar = new IntegrationRegistrar();
        p.guiManager = new GuiManager();
        p.historyRepository = new HistoryRepositoryImpl();
        p.statsRepository = new StatsRepositoryImpl();
        p.checkService = new CheckService();
        p.sessionViolationStore = new SessionViolationStore(p.redisRepository, p.getLogger());
        p.modSessionStore = new ModSessionStore(p.redisRepository, p.getLogger());
        p.teleportService = new TeleportService(p);
        p.commandManager = new CommandManagerImpl();
        p.afterCommandsRegistered();
        p.updateCheckerRepository = new UpdateCheckerRepositoryImpl();
        p.networkPresenceRepository = new NetworkPresenceRepository(p, serverIdentity);
        p.networkPresenceRepository.addListener(p.alertRepository);
        p.redisRepository.addStateListener(p.alertRepository);
        p.networkPresenceRepository.addServerNameResolvedListener(name -> {
            DatabaseRepositoryImpl db = p.databaseRepository;
            if (db != null && db.isConnected()) db.assignThisServerName(name);
        });
        p.networkPresenceRepository.start();

        p.monitorRepository = new MonitorRepository(p);
        p.networkPresenceRepository.addListener(p.monitorRepository);
        p.monitorRepository.start();

        p.followRepository = new FollowRepository(p);
        p.networkPresenceRepository.addListener(p.followRepository);
        p.followRepository.start();

        p.integrationRegistrar.enableAll();

        p.proxyTopologyService = new ProxyTopologyService(p);
        p.bridgeManager = new BridgeManager(p, serverIdentity.instanceId());
        p.bridgeManager.start();

        ModKickThenBanTracker kickThenBanTracker = new ModKickThenBanTracker(p.cacheRepository);
        ModLogAlertTracker logAlertTracker = new ModLogAlertTracker(p.cacheRepository);
        p.networkPresenceRepository.addListener(logAlertTracker);
        ModResolver modResolver = new ModResolver(p, kickThenBanTracker, logAlertTracker);
        p.modDetectionService = new ModDetectionService(p, modResolver, kickThenBanTracker, p.modSessionStore);

        p.registerPacketListenerInternal(new PacketPlayerJoinQuit());
        p.registerPacketListenerInternal(new PacketCheckManagerListener(p.playerRepository));
        p.registerPacketListenerInternal(new GuiPacketListener());
        p.registerPacketListenerInternal(new ModPacketObserver(p.modDetectionService, p.playerRepository));
        p.registerPacketListenerInternal(new BridgePacketListener());
        p.registerPacketListenerInternal(new FollowerPacketListener(p.followRepository));

        p.internalSubscriptions.add(p.eventRepository.subscribeInternal(InventoryChangedEvent.class, new TotemReplenishedListener()));
        p.internalSubscriptions.add(p.eventRepository.subscribeInternal(InternalPlayerEvent.class, new EventCheckManagerListener()));

        p.fleetCacheLifecycle = new FleetCacheLifecycle(p, p.redisRepository, serverIdentity.instanceId(), p.getLogger());
        p.fleetCacheLifecycle.register();

        p.api = new TGPlatformAPI();
        TotemGuard.replace(p.api);

        p.enableBStats();
        replayOnlinePlayers(p);
        p.getLogger().info("Enabled TotemGuard in " + stopwatch.stop().elapsed().toMillis() + "ms");
    }

    private static void replayOnlinePlayers(TGPlatform p) {
        try {
            Collection<User> users = PacketEvents.getAPI().getProtocolManager().getUsers();
            if (users == null || users.isEmpty()) return;

            int replayed = 0;
            for (User user : users) {
                try {
                    if (user.getConnectionState() != ConnectionState.PLAY) continue;
                    UUID uuid = user.getUUID();
                    if (uuid == null) continue;
                    p.playerRepository.onLoginPacket(user);
                    p.playerRepository.onLogin(user);
                    TGPlayer player = p.playerRepository.getPlayer(user);
                    if (player != null) {
                        player.resyncFromPlatform();
                    }
                    replayed++;
                } catch (Throwable t) {
                    p.getLogger().warning("Failed to replay online player " + user.getName() + ": " + t.getMessage());
                }
            }
            if (replayed > 0) {
                p.getLogger().info("Initialized " + replayed + " already-connected player" + (replayed == 1 ? "" : "s") + " on enable.");
            }
        } catch (Throwable t) {
            p.getLogger().warning("Online-player replay failed: " + t.getMessage());
        }
    }

    static void disable(TGPlatform p) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        TGPluginShutdownEvent.Reason reason = p.getShutdownReason() != null
                ? p.getShutdownReason()
                : TGPluginShutdownEvent.Reason.SERVER_STOP;

        TotemGuard.shutdown();

        if (p.fleetCacheLifecycle != null) {
            try {
                p.fleetCacheLifecycle.unregister();
            } catch (Throwable t) {
                p.getLogger().warning("FleetCacheLifecycle.unregister threw: " + t.getMessage());
            }
        }

        if (p.eventRepository != null) {
            try {
                p.eventRepository.post(new TGPluginShutdownEventImpl(reason, TGVersions.CURRENT.toString()));
            } catch (Throwable t) {
                p.getLogger().warning("TGPluginShutdownEvent dispatch threw: " + t.getMessage());
            }
        }

        if (p.playerRepository != null) {
            try {
                p.playerRepository.persistAllOnShutdown();
            } catch (Throwable t) {
                p.getLogger().warning("persistAllOnShutdown failed: " + t.getMessage());
            }
        }

        for (PacketListenerAbstract listener : p.packetListeners) {
            try {
                PacketEvents.getAPI().getEventManager().unregisterListener(listener);
            } catch (Exception ex) {
                p.getLogger().warning("Failed to unregister packet listener " + listener.getClass().getName() + ": " + ex.getMessage());
            }
        }
        p.packetListeners.clear();

        p.internalSubscriptions.forEach(sub -> {
            try {
                sub.close();
            } catch (Exception ex) {
                p.getLogger().warning("Failed to close internal event subscription: " + ex.getMessage());
            }
        });
        p.internalSubscriptions.clear();

        if (p.commandManager != null) {
            try {
                p.commandManager.unregisterAll();
            } catch (Exception ex) {
                p.getLogger().warning("Failed to unregister commands: " + ex.getMessage());
            }
        }
        if (p.integrationRegistrar != null) p.integrationRegistrar.disableAll();
        if (p.monitorRepository != null) p.monitorRepository.stop();
        if (p.followRepository != null) p.followRepository.stop();
        if (p.networkPresenceRepository != null) p.networkPresenceRepository.stop();
        if (p.playerRepository != null) p.playerRepository.shutdown();
        if (p.updateCheckerRepository != null) p.updateCheckerRepository.shutdown();
        if (p.alertRepository != null && p.redisRepository != null)
            p.redisRepository.removeStateListener(p.alertRepository);
        if (p.bridgeManager != null) p.bridgeManager.shutdown();
        if (p.redisRepository != null) p.redisRepository.stop();
        if (p.databaseRepository != null) p.databaseRepository.stop();
        if (p.guiManager != null) p.guiManager.shutdown();
        if (p.discordWebhookService != null) p.discordWebhookService.shutdown();

        if (TGPlatform.getInstance() == p) {
            TGPlatform.clearInstance();
        }

        p.getLogger().info("Disabled TotemGuard in " + stopwatch.stop().elapsed().toMillis() + "ms (reason: " + reason + ")");
    }
}
