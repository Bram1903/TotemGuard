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
import com.deathmotion.totemguard.common.alert.AlertRepositoryImpl;
import com.deathmotion.totemguard.common.antivpn.AntiVPNRepositoryImpl;
import com.deathmotion.totemguard.common.cache.CacheRepositoryImpl;
import com.deathmotion.totemguard.common.check.impl.mods.ModRegistry;
import com.deathmotion.totemguard.common.commands.CommandManagerImpl;
import com.deathmotion.totemguard.common.config.ConfigRepositoryImpl;
import com.deathmotion.totemguard.common.database.DatabaseRepositoryImpl;
import com.deathmotion.totemguard.common.discord.DiscordWebhookService;
import com.deathmotion.totemguard.common.event.EventRepositoryImpl;
import com.deathmotion.totemguard.common.event.internal.InternalPlayerEvent;
import com.deathmotion.totemguard.common.event.internal.impl.InventoryChangedEvent;
import com.deathmotion.totemguard.common.event.internal.listeners.EventCheckManagerListener;
import com.deathmotion.totemguard.common.event.internal.listeners.TotemReplenishedListener;
import com.deathmotion.totemguard.common.event.packet.PacketCheckManagerListener;
import com.deathmotion.totemguard.common.event.packet.PacketPlayerJoinQuit;
import com.deathmotion.totemguard.common.gui.GuiManager;
import com.deathmotion.totemguard.common.gui.GuiPacketListener;
import com.deathmotion.totemguard.common.history.HistoryRepositoryImpl;
import com.deathmotion.totemguard.common.integration.IntegrationRegistrar;
import com.deathmotion.totemguard.common.message.MessageService;
import com.deathmotion.totemguard.common.monitor.MonitorRepository;
import com.deathmotion.totemguard.common.network.BungeeChannelManager;
import com.deathmotion.totemguard.common.network.NetworkPresenceRepository;
import com.deathmotion.totemguard.common.network.ServerIdentity;
import com.deathmotion.totemguard.common.placeholder.PlaceholderRepositoryImpl;
import com.deathmotion.totemguard.common.platform.Platform;
import com.deathmotion.totemguard.common.platform.player.PlatformPlayerFactory;
import com.deathmotion.totemguard.common.platform.sender.Sender;
import com.deathmotion.totemguard.common.player.PlayerRepositoryImpl;
import com.deathmotion.totemguard.common.punishment.PunishmentRepositoryImpl;
import com.deathmotion.totemguard.common.redis.RedisRepositoryImpl;
import com.deathmotion.totemguard.common.redis.broker.packets.impl.SyncTeleportRequestPacket;
import com.deathmotion.totemguard.common.reload.ReloadService;
import com.deathmotion.totemguard.common.stats.StatsRepositoryImpl;
import com.deathmotion.totemguard.common.update.UpdateCheckerRepositoryImpl;
import com.deathmotion.totemguard.common.util.*;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.google.common.base.Stopwatch;
import lombok.Getter;
import lombok.Setter;
import org.incendo.cloud.CommandManager;

import java.util.ArrayList;
import java.util.List;
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
    private AntiVPNRepositoryImpl antiVPNRepository;
    private UpdateCheckerRepositoryImpl updateCheckerRepository;
    private HistoryRepositoryImpl historyRepository;
    private StatsRepositoryImpl statsRepository;
    private GuiManager guiManager;
    private IntegrationRegistrar integrationRegistrar;
    private NetworkPresenceRepository networkPresenceRepository;
    private BungeeChannelManager bungeeChannelManager;
    private MonitorRepository monitorRepository;
    private TGPlatformAPI api;

    public TGPlatform(Platform platform) {
        instance = this;
        this.platform = platform;
        logger = Logger.getLogger("TotemGuard");
    }

    public void commonOnInitialize() {
        if (!JarIntegrityChecker.verifyCurrentJar()) {
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
        // ServerIdentity is shared between RedisRepository (broker sender id + unicast
        // channel suffix) and NetworkPresenceRepository (publisher of the same instance
        // id in subscribe packets). They MUST be the same UUID, otherwise per-instance
        // unicast channels don't line up.
        ServerIdentity serverIdentity = ServerIdentity.create(configRepository.configView().server());
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
        commandManager = new CommandManagerImpl();
        antiVPNRepository = new AntiVPNRepositoryImpl();
        updateCheckerRepository = new UpdateCheckerRepositoryImpl();
        networkPresenceRepository = new NetworkPresenceRepository(this, serverIdentity);
        networkPresenceRepository.addListener(alertRepository);
        networkPresenceRepository.start();

        monitorRepository = new MonitorRepository(this);
        networkPresenceRepository.addListener(monitorRepository);
        monitorRepository.start();

        integrationRegistrar.enableAll();

        bungeeChannelManager = new BungeeChannelManager(this);

        registerPacketListener(new PacketPlayerJoinQuit());
        registerPacketListener(new PacketCheckManagerListener(playerRepository));
        registerPacketListener(new GuiPacketListener());
        registerPacketListener(bungeeChannelManager);

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
        if (networkPresenceRepository != null) networkPresenceRepository.stop();
        if (playerRepository != null) playerRepository.shutdown();
        if (updateCheckerRepository != null) updateCheckerRepository.shutdown();
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

    public abstract CommandManager<Sender> getCommandManager();

    public abstract void enableBStats();

    public abstract PlatformPlayerFactory getPlatformPlayerFactory();

    public abstract String getPluginDirectory();

    public abstract String getPlatformVersion();

    public abstract boolean isPluginEnabled(String plugin);

    public abstract void disablePlugin();

    /**
     * Validates platform-specific runtime requirements (server software, server
     * version, mod loader, etc.). Implementations are expected to log their
     * own user-facing errors via {@link com.deathmotion.totemguard.common.util.CompatibilityLogger}
     * before returning {@code false}.
     */
    public abstract boolean checkPlatformCompatibility();

    public void handleIncomingTeleportRequest(SyncTeleportRequestPacket.Payload payload) {
    }

    public boolean canRouteToServer(String serverName) {
        return bungeeChannelManager.isServerOnThisProxy(serverName);
    }

    public String resolveServerName(String serverName) {
        return bungeeChannelManager.resolveServerName(serverName);
    }

    public @org.jetbrains.annotations.Nullable String resolveProxyServerId(String serverName) {
        return bungeeChannelManager.resolveProxyServerId(serverName);
    }

    public void refreshProxyTopology() {
        bungeeChannelManager.refresh();
    }
}
