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

import com.deathmotion.totemguard.api.event.events.TGPluginShutdownEvent;
import com.deathmotion.totemguard.common.cache.CacheRepositoryImpl;
import com.deathmotion.totemguard.common.commands.CommandManagerImpl;
import com.deathmotion.totemguard.common.config.ConfigRepositoryImpl;
import com.deathmotion.totemguard.common.database.DatabaseRepositoryImpl;
import com.deathmotion.totemguard.common.event.EventBusImpl;
import com.deathmotion.totemguard.common.event.internal.InternalEventBus;
import com.deathmotion.totemguard.common.features.alert.AlertRepositoryImpl;
import com.deathmotion.totemguard.common.features.check.CheckService;
import com.deathmotion.totemguard.common.features.discord.DiscordWebhookService;
import com.deathmotion.totemguard.common.features.follow.FollowRepository;
import com.deathmotion.totemguard.common.features.history.HistoryRepositoryImpl;
import com.deathmotion.totemguard.common.features.integration.IntegrationRegistrar;
import com.deathmotion.totemguard.common.features.mods.ModDetectionService;
import com.deathmotion.totemguard.common.features.mods.ModSessionStore;
import com.deathmotion.totemguard.common.features.monitor.MonitorRepository;
import com.deathmotion.totemguard.common.features.punishment.PunishmentRepositoryImpl;
import com.deathmotion.totemguard.common.features.session.SessionViolationStore;
import com.deathmotion.totemguard.common.features.stats.StatsRepositoryImpl;
import com.deathmotion.totemguard.common.features.teleport.TeleportService;
import com.deathmotion.totemguard.common.features.update.UpdateCheckerRepositoryImpl;
import com.deathmotion.totemguard.common.fleet.FleetCacheLifecycle;
import com.deathmotion.totemguard.common.gui.GuiManager;
import com.deathmotion.totemguard.common.message.MessageService;
import com.deathmotion.totemguard.common.network.NetworkPresenceRepository;
import com.deathmotion.totemguard.common.network.ProxyTopologyService;
import com.deathmotion.totemguard.common.network.bridge.BridgeManager;
import com.deathmotion.totemguard.common.placeholder.PlaceholderRepositoryImpl;
import com.deathmotion.totemguard.common.platform.Platform;
import com.deathmotion.totemguard.common.platform.player.PlatformPlayerFactory;
import com.deathmotion.totemguard.common.platform.sender.Sender;
import com.deathmotion.totemguard.common.player.PlayerRepositoryImpl;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.redis.RedisRepositoryImpl;
import com.deathmotion.totemguard.common.redis.broker.packets.impl.SyncCheckRequestPacket;
import com.deathmotion.totemguard.common.redis.broker.packets.impl.SyncCheckResultPacket;
import com.deathmotion.totemguard.common.redis.broker.packets.impl.SyncTeleportRequestPacket;
import com.deathmotion.totemguard.common.reload.ReloadService;
import com.deathmotion.totemguard.common.util.Scheduler;
import com.deathmotion.totemguard.host.TGPluginHost;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
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

    final List<PacketListenerAbstract> packetListeners = new ArrayList<>();

    private final Platform platform;
    private final Logger logger;

    ReloadService reloadService;
    ConfigRepositoryImpl configRepository;
    PlaceholderRepositoryImpl placeholderRepository;
    RedisRepositoryImpl redisRepository;
    DatabaseRepositoryImpl databaseRepository;
    CacheRepositoryImpl cacheRepository;
    MessageService messageService;
    EventBusImpl eventBus;
    InternalEventBus internalEventBus;
    PunishmentRepositoryImpl punishmentRepository;
    AlertRepositoryImpl alertRepository;
    DiscordWebhookService discordWebhookService;
    PlayerRepositoryImpl playerRepository;
    CommandManagerImpl commandManager;
    UpdateCheckerRepositoryImpl updateCheckerRepository;
    HistoryRepositoryImpl historyRepository;
    StatsRepositoryImpl statsRepository;
    GuiManager guiManager;
    IntegrationRegistrar integrationRegistrar;
    NetworkPresenceRepository networkPresenceRepository;
    FollowRepository followRepository;
    ProxyTopologyService proxyTopologyService;
    BridgeManager bridgeManager;
    MonitorRepository monitorRepository;
    ModDetectionService modDetectionService;
    CheckService checkService;
    SessionViolationStore sessionViolationStore;
    ModSessionStore modSessionStore;
    TeleportService teleportService;
    FleetCacheLifecycle fleetCacheLifecycle;
    TGPlatformAPI api;

    @Setter
    private boolean enabled = true;

    @Setter
    private TGPluginShutdownEvent.Reason shutdownReason = TGPluginShutdownEvent.Reason.SERVER_STOP;

    @Setter
    private boolean managedByLoader = false;

    @Setter
    private TGPluginHost pluginHost;

    public TGPlatform(Platform platform) {
        instance = this;
        this.platform = platform;
        logger = Logger.getLogger("TotemGuard");
    }

    static void clearInstance() {
        instance = null;
    }

    protected void afterCommandsRegistered() {
    }

    public void tickPlayers() {
        for (TGPlayer player : playerRepository.getPlayers()) {
            player.onServerTick();
        }
    }

    public void commonOnEnable() {
        TotemGuardLifecycle.enable(this);
    }

    public void commonOnDisable() {
        TotemGuardLifecycle.disable(this);
    }

    void registerPacketListenerInternal(PacketListenerAbstract listener) {
        PacketEvents.getAPI().getEventManager().registerListener(listener);
        packetListeners.add(listener);
    }

    public abstract Scheduler getScheduler();

    public abstract void dispatchCommand(String command);

    public abstract @Nullable Sender createSender(@NotNull UUID playerUuid);

    public abstract CommandManager<Sender> getCommandManager();

    public abstract void enableBStats();

    public abstract PlatformPlayerFactory getPlatformPlayerFactory();

    public abstract String getPluginDirectory();

    public abstract String getPlatformVersion();

    public abstract boolean isPluginEnabled(String plugin);

    public abstract void disablePlugin();

    public abstract boolean checkPlatformCompatibility();

    public boolean shouldVerifyJarIntegrity() {
        return true;
    }

    public void handleIncomingTeleportRequest(SyncTeleportRequestPacket.Payload payload) {
    }

    public void handleIncomingCheckRequest(SyncCheckRequestPacket.Payload payload) {
        CheckService service = this.checkService;
        if (service != null) service.acceptRemoteCheckRequest(payload);
    }

    public void handleIncomingCheckResult(SyncCheckResultPacket.Payload payload) {
        CheckService service = this.checkService;
        if (service != null) service.acceptRemoteCheckResult(payload);
    }

    public ProxyTopologyService.RouteStatus checkRoute(UUID targetInstanceId) {
        return proxyTopologyService.checkRoute(targetInstanceId);
    }
}
