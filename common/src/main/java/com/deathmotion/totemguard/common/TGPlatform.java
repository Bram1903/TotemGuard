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
import com.deathmotion.totemguard.api.event.EventSubscription;
import com.deathmotion.totemguard.common.cache.CacheRepositoryImpl;
import com.deathmotion.totemguard.common.commands.CommandManagerImpl;
import com.deathmotion.totemguard.common.config.ConfigRepositoryImpl;
import com.deathmotion.totemguard.common.database.DatabaseRepositoryImpl;
import com.deathmotion.totemguard.common.event.EventRepositoryImpl;
import com.deathmotion.totemguard.common.event.internal.InternalPlayerEvent;
import com.deathmotion.totemguard.common.event.internal.impl.InventoryChangedEvent;
import com.deathmotion.totemguard.common.event.internal.listeners.EventCheckManagerListener;
import com.deathmotion.totemguard.common.event.internal.listeners.TotemReplenishedListener;
import com.deathmotion.totemguard.common.event.packet.PacketCheckManagerListener;
import com.deathmotion.totemguard.common.event.packet.PacketPlayerJoinQuit;
import com.deathmotion.totemguard.common.features.alert.AlertRepositoryImpl;
import com.deathmotion.totemguard.common.features.check.CheckService;
import com.deathmotion.totemguard.common.features.discord.DiscordWebhookService;
import com.deathmotion.totemguard.common.features.history.HistoryRepositoryImpl;
import com.deathmotion.totemguard.common.features.integration.IntegrationRegistrar;
import com.deathmotion.totemguard.common.features.mods.*;
import com.deathmotion.totemguard.common.features.monitor.MonitorRepository;
import com.deathmotion.totemguard.common.features.punishment.PunishmentRepositoryImpl;
import com.deathmotion.totemguard.common.features.session.SessionViolationStore;
import com.deathmotion.totemguard.common.features.stats.StatsRepositoryImpl;
import com.deathmotion.totemguard.common.features.teleport.TeleportService;
import com.deathmotion.totemguard.common.features.update.UpdateCheckerRepositoryImpl;
import com.deathmotion.totemguard.common.gui.GuiManager;
import com.deathmotion.totemguard.common.gui.GuiPacketListener;
import com.deathmotion.totemguard.common.message.MessageService;
import com.deathmotion.totemguard.common.network.NetworkPresenceRepository;
import com.deathmotion.totemguard.common.network.ProxyTopologyService;
import com.deathmotion.totemguard.common.network.ServerIdentity;
import com.deathmotion.totemguard.common.network.bridge.BackendAnnouncer;
import com.deathmotion.totemguard.common.network.bridge.ProxyBridgeSubscriber;
import com.deathmotion.totemguard.common.network.follow.FollowRepository;
import com.deathmotion.totemguard.common.placeholder.PlaceholderRepositoryImpl;
import com.deathmotion.totemguard.common.platform.Platform;
import com.deathmotion.totemguard.common.platform.player.PlatformPlayerFactory;
import com.deathmotion.totemguard.common.platform.sender.Sender;
import com.deathmotion.totemguard.common.player.PlayerRepositoryImpl;
import com.deathmotion.totemguard.common.redis.RedisRepositoryImpl;
import com.deathmotion.totemguard.common.redis.broker.packets.impl.SyncCheckRequestPacket;
import com.deathmotion.totemguard.common.redis.broker.packets.impl.SyncCheckResultPacket;
import com.deathmotion.totemguard.common.redis.broker.packets.impl.SyncTeleportRequestPacket;
import com.deathmotion.totemguard.common.reload.ReloadService;
import com.deathmotion.totemguard.common.util.CompatibilityUtil;
import com.deathmotion.totemguard.common.util.ConsoleBanner;
import com.deathmotion.totemguard.common.util.LoggerSuppressor;
import com.deathmotion.totemguard.common.util.Scheduler;
import com.deathmotion.totemguard.integrity.JarIntegrityChecker;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.google.common.base.Stopwatch;
import lombok.Getter;
import lombok.Setter;
import org.incendo.cloud.CommandManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

@Getter
public abstract class TGPlatform {

    @Getter
    private static final int bStatsId = 23179;
    @Getter
    private static TGPlatform instance;
    private final Platform platform;
    private final Logger logger;
    private final List<PacketListenerAbstract> packetListeners = new ArrayList<>();
    private final List<EventSubscription> internalSubscriptions = new ArrayList<>();
    @Setter
    private boolean enabled = true;
    private ReloadService reloadService;
    private ConfigRepositoryImpl configRepository;
    private PlaceholderRepositoryImpl placeholderRepository;
    private RedisRepositoryImpl redisRepository;
    private DatabaseRepositoryImpl databaseRepository;
    private CacheRepositoryImpl cacheRepository;
    private MessageService messageService;
    private EventRepositoryImpl eventRepository;
    private PunishmentRepositoryImpl punishmentRepository;
    private AlertRepositoryImpl alertRepository;
    private DiscordWebhookService discordWebhookService;
    private PlayerRepositoryImpl playerRepository;
    private CommandManagerImpl commandManager;
    private UpdateCheckerRepositoryImpl updateCheckerRepository;
    private HistoryRepositoryImpl historyRepository;
    private StatsRepositoryImpl statsRepository;
    private GuiManager guiManager;
    private IntegrationRegistrar integrationRegistrar;
    private NetworkPresenceRepository networkPresenceRepository;
    private FollowRepository followRepository;
    private ProxyTopologyService proxyTopologyService;
    private ProxyBridgeSubscriber proxyBridgeSubscriber;
    private BackendAnnouncer backendAnnouncer;
    private MonitorRepository monitorRepository;
    private ModDetectionService modDetectionService;
    private CheckService checkService;
    private SessionViolationStore sessionViolationStore;
    private ModSessionStore modSessionStore;
    private TeleportService teleportService;
    private TGPlatformAPI api;

    public TGPlatform(Platform platform) {
        instance = this;
        this.platform = platform;
        logger = Logger.getLogger("TotemGuard");
    }

    public void commonOnInitialize() {
        if (!new JarIntegrityChecker(logger, "TotemGuard").verifyCurrentJar()) {
            setEnabled(false);
        }
    }

    public void commonOnEnable() {
        if (!enabled) {
            disablePlugin();
            return;
        }

        LoggerSuppressor.suppressDefaultNoise();

        Stopwatch stopwatch = Stopwatch.createStarted();
        if (!CompatibilityUtil.isCompatible()) {
            setEnabled(false);
            disablePlugin();
            return;
        }

        // Fail fast on incomplete subclass wiring rather than NPE'ing on first login.
        if (getPlatformPlayerFactory() == null) {
            logger.severe("Platform " + platform + " did not provide a PlatformPlayerFactory — disabling.");
            setEnabled(false);
            disablePlugin();
            return;
        }
        if (getScheduler() == null) {
            logger.severe("Platform " + platform + " did not provide a Scheduler — disabling.");
            setEnabled(false);
            disablePlugin();
            return;
        }

        ConsoleBanner.print();

        reloadService = new ReloadService();
        configRepository = new ConfigRepositoryImpl();
        ModRegistry.load();
        placeholderRepository = new PlaceholderRepositoryImpl();
        ServerIdentity serverIdentity = ServerIdentity.create();
        redisRepository = new RedisRepositoryImpl(serverIdentity.instanceId());
        databaseRepository = new DatabaseRepositoryImpl();
        cacheRepository = new CacheRepositoryImpl();
        messageService = new MessageService();
        eventRepository = new EventRepositoryImpl();
        punishmentRepository = new PunishmentRepositoryImpl();
        alertRepository = new AlertRepositoryImpl();
        discordWebhookService = new DiscordWebhookService();
        playerRepository = new PlayerRepositoryImpl();
        integrationRegistrar = new IntegrationRegistrar();
        guiManager = new GuiManager();
        historyRepository = new HistoryRepositoryImpl();
        statsRepository = new StatsRepositoryImpl();
        checkService = new com.deathmotion.totemguard.common.features.check.CheckService();
        sessionViolationStore = new SessionViolationStore(redisRepository, logger);
        modSessionStore = new ModSessionStore(redisRepository, logger);
        teleportService = new TeleportService(this);
        commandManager = new CommandManagerImpl();
        updateCheckerRepository = new UpdateCheckerRepositoryImpl();
        networkPresenceRepository = new NetworkPresenceRepository(this, serverIdentity);
        networkPresenceRepository.addListener(alertRepository);
        redisRepository.addStateListener(alertRepository);
        networkPresenceRepository.addServerNameResolvedListener(name -> {
            DatabaseRepositoryImpl db = this.databaseRepository;
            if (db != null && db.isConnected()) db.assignThisServerName(name);
        });
        networkPresenceRepository.start();

        monitorRepository = new MonitorRepository(this);
        networkPresenceRepository.addListener(monitorRepository);
        monitorRepository.start();

        followRepository = new FollowRepository(this);
        networkPresenceRepository.addListener(followRepository);
        followRepository.start();

        integrationRegistrar.enableAll();

        proxyTopologyService = new ProxyTopologyService(this);
        proxyBridgeSubscriber = new ProxyBridgeSubscriber(this);
        redisRepository.addStateListener(proxyBridgeSubscriber);

        backendAnnouncer = new BackendAnnouncer(this, serverIdentity.instanceId(),
                networkPresenceRepository.getLocalServerName());
        redisRepository.addStateListener(backendAnnouncer);

        ModKickThenBanTracker kickThenBanTracker = new ModKickThenBanTracker(cacheRepository);
        ModLogAlertTracker logAlertTracker = new ModLogAlertTracker(cacheRepository);
        networkPresenceRepository.addListener(logAlertTracker);
        ModResolver modResolver = new ModResolver(this, kickThenBanTracker, logAlertTracker);
        modDetectionService = new ModDetectionService(this, modResolver, kickThenBanTracker, modSessionStore);

        registerPacketListener(new PacketPlayerJoinQuit());
        registerPacketListener(new PacketCheckManagerListener(playerRepository));
        registerPacketListener(new GuiPacketListener());
        registerPacketListener(new ModPacketObserver(modDetectionService, playerRepository));

        internalSubscriptions.add(eventRepository.subscribeInternal(InventoryChangedEvent.class, new TotemReplenishedListener()));
        internalSubscriptions.add(eventRepository.subscribeInternal(InternalPlayerEvent.class, new EventCheckManagerListener()));

        api = new TGPlatformAPI();
        TotemGuard.init(api);

        enableBStats();
        logger.info("Enabled TotemGuard in " + stopwatch.stop().elapsed().toMillis() + "ms");
    }

    public void commonOnDisable() {
        for (PacketListenerAbstract listener : packetListeners) {
            try {
                PacketEvents.getAPI().getEventManager().unregisterListener(listener);
            } catch (Exception ex) {
                logger.warning("Failed to unregister packet listener " + listener.getClass().getName() + ": " + ex.getMessage());
            }
        }
        packetListeners.clear();

        for (EventSubscription subscription : internalSubscriptions) {
            try {
                subscription.close();
            } catch (Exception ex) {
                logger.warning("Failed to close internal event subscription: " + ex.getMessage());
            }
        }
        internalSubscriptions.clear();

        if (integrationRegistrar != null) integrationRegistrar.disableAll();
        if (monitorRepository != null) monitorRepository.stop();
        if (followRepository != null) followRepository.stop();
        if (networkPresenceRepository != null) networkPresenceRepository.stop();
        if (playerRepository != null) playerRepository.shutdown();
        if (updateCheckerRepository != null) updateCheckerRepository.shutdown();
        if (alertRepository != null) redisRepository.removeStateListener(alertRepository);
        if (proxyBridgeSubscriber != null) redisRepository.removeStateListener(proxyBridgeSubscriber);
        if (backendAnnouncer != null) {
            redisRepository.removeStateListener(backendAnnouncer);
            backendAnnouncer.stop();
        }
        if (redisRepository != null) redisRepository.stop();
        if (databaseRepository != null) databaseRepository.stop();
        if (guiManager != null) guiManager.shutdown();
        if (discordWebhookService != null) discordWebhookService.shutdown();
    }

    private void registerPacketListener(PacketListenerAbstract listener) {
        PacketEvents.getAPI().getEventManager().registerListener(listener);
        packetListeners.add(listener);
    }

    public abstract Scheduler getScheduler();

    public abstract void dispatchCommand(String command);

    /**
     * Resolves the given player UUID to a {@link Sender} for command execution as that player.
     * Returns {@code null} if no player with that UUID is currently online on this server.
     * Used by features (e.g. GUI teleport buttons) that need to execute commands on behalf of
     * the viewer rather than the console.
     */
    public abstract @Nullable Sender createSender(@NotNull UUID playerUuid);

    public abstract CommandManager<Sender> getCommandManager();

    public abstract void enableBStats();

    public abstract PlatformPlayerFactory getPlatformPlayerFactory();

    public abstract String getPluginDirectory();

    public abstract String getPlatformVersion();

    public abstract boolean isPluginEnabled(String plugin);

    public abstract void disablePlugin();

    public abstract boolean checkPlatformCompatibility();

    public abstract int getServerPort();

    public @org.jetbrains.annotations.NotNull String getProxyFacingHost() {
        return "";
    }

    public void handleIncomingTeleportRequest(SyncTeleportRequestPacket.Payload payload) {
    }

    public void handleIncomingCheckRequest(SyncCheckRequestPacket.Payload payload) {
        com.deathmotion.totemguard.common.features.check.CheckService service = this.checkService;
        if (service != null) service.acceptRemoteCheckRequest(payload);
    }

    public void handleIncomingCheckResult(SyncCheckResultPacket.Payload payload) {
        com.deathmotion.totemguard.common.features.check.CheckService service = this.checkService;
        if (service != null) service.acceptRemoteCheckResult(payload);
    }

    public boolean canRouteToInstance(UUID targetInstanceId) {
        return proxyTopologyService.canRouteToInstance(targetInstanceId);
    }
}
