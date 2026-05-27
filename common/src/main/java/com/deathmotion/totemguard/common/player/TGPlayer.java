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

package com.deathmotion.totemguard.common.player;

import com.deathmotion.totemguard.api.history.HistoryView;
import com.deathmotion.totemguard.api.user.BanAnimation;
import com.deathmotion.totemguard.api.user.InventoryStatus;
import com.deathmotion.totemguard.api.user.TGUser;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.cache.CacheCodecs;
import com.deathmotion.totemguard.common.cache.CacheKeys;
import com.deathmotion.totemguard.common.cache.CacheRepositoryImpl;
import com.deathmotion.totemguard.common.cache.data.CheckSnapshot;
import com.deathmotion.totemguard.common.check.CheckManagerImpl;
import com.deathmotion.totemguard.common.features.mods.ModSession;
import com.deathmotion.totemguard.common.features.punishment.BanAnimationImpl;
import com.deathmotion.totemguard.common.platform.player.PlatformPlayer;
import com.deathmotion.totemguard.common.player.data.*;
import com.deathmotion.totemguard.common.player.data.ping.PingData;
import com.deathmotion.totemguard.common.player.debug.DebugOverlayManager;
import com.deathmotion.totemguard.common.player.debug.provider.TotemDebugProvider;
import com.deathmotion.totemguard.common.player.debug.provider.TransactionDebugProvider;
import com.deathmotion.totemguard.common.player.inventory.InventoryRecipeTracker;
import com.deathmotion.totemguard.common.player.inventory.PacketInventory;
import com.deathmotion.totemguard.common.player.inventory.enums.Issuer;
import com.deathmotion.totemguard.common.player.inventory.slot.CarriedItem;
import com.deathmotion.totemguard.common.player.latency.PacketLatencyHandler;
import com.deathmotion.totemguard.common.player.processor.ProcessorInbound;
import com.deathmotion.totemguard.common.player.processor.ProcessorOutbound;
import com.deathmotion.totemguard.common.player.processor.inbound.*;
import com.deathmotion.totemguard.common.player.processor.outbound.*;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDisconnect;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter
public class TGPlayer implements TGUser {

    private static final Duration CHECK_SNAPSHOT_TTL = Duration.ofMinutes(5);
    private final TGPlatform platform;
    private final UUID uuid;
    private final User user;
    private final Instant sessionStart = Instant.now();
    private final PacketInventory inventory;
    private final CheckManagerImpl checkManager;
    private final Data data;
    private final TotemData totemData;
    private final ClickData clickData;
    private final TickData tickData;
    private final PingData pingData;
    private final CombatTracker combatTracker;
    private final InventoryRecipeTracker inventoryRecipeTracker;
    private final DebugOverlayManager debugOverlayManager;
    private final PacketLatencyHandler latencyHandler;
    private final BanAnimation banAnimation;
    private final ProcessorInbound[] processorInbounds;
    private final ProcessorOutbound[] processorOutbounds;

    private final AtomicBoolean hasDisconnected = new AtomicBoolean();
    private final int modDetectionWindowId = -ThreadLocalRandom.current().nextInt(10_000, Integer.MAX_VALUE);
    private boolean hasLoggedIn;
    private @Nullable PlatformPlayer platformPlayer;

    @Setter()
    private String clientBrand = "Unknown";
    @Setter
    private volatile int databasePlayerId;

    @Setter
    @Nullable
    private volatile Long databaseProfileId;

    @Setter
    private boolean marlowOptimizer;

    @Setter
    @Nullable
    private Long lastTotemUse;

    @Setter
    @Nullable
    private Long lastTotemPickup;

    @Setter
    private volatile boolean manualCheckActive;

    private volatile Boolean supportsEndTickCache;

    @Setter
    @Nullable
    private volatile ModSession modSession;

    public TGPlayer(@NotNull User user) {
        this.platform = TGPlatform.getInstance();
        this.uuid = user.getUUID();
        this.user = user;

        this.inventory = new PacketInventory();
        this.data = new Data(this);
        this.totemData = new TotemData();
        this.clickData = new ClickData();
        this.tickData = new TickData();
        this.pingData = new PingData();
        this.combatTracker = new CombatTracker();
        this.inventoryRecipeTracker = new InventoryRecipeTracker(this);
        this.debugOverlayManager = new DebugOverlayManager(this);
        this.debugOverlayManager.register(new TransactionDebugProvider());
        this.debugOverlayManager.register(new TotemDebugProvider());
        this.latencyHandler = new PacketLatencyHandler(this);
        this.banAnimation = new BanAnimationImpl(this);
        this.checkManager = new CheckManagerImpl(this);

        this.processorInbounds = new ProcessorInbound[]{
                new InboundPingProcessor(this),
                new InboundInventoryProcessor(this),
                new InboundClientProcessor(this),
                new InboundActionProcessor(this),
                new InboundTeleportProcessor(this),
                new InboundMovementProcessor(this),
        };

        this.processorOutbounds = new ProcessorOutbound[]{
                new OutboundPingProcessor(this),
                new OutboundBundleProcessor(this),
                new OutboundSpawnProcessor(this),
                new OutboundTotemActivatedProcessor(this),
                new OutboundInventoryProcessor(this),
                new OutboundTeleportProcessor(this),
                new OutboundMovementProcessor(this),
                new OutboundEntityProcessor(this),
                new OutboundCameraProcessor(this),
                new OutboundMetadataProcessor(this),
        };
    }

    public void onLogin() {
        TGPlatform platform = TGPlatform.getInstance();
        PlayerRepositoryImpl playerRepository = platform.getPlayerRepository();

        platformPlayer = platform.getPlatformPlayerFactory().create(uuid);
        if (platformPlayer == null) {
            playerRepository.removeUser(user);
            return;
        }

        if (!playerRepository.shouldCheck(user, platformPlayer)) {
            playerRepository.removeUser(user);
            return;
        }

        supportsEndTickCache = computeSupportsEndTick();

        platform.getEventBus().getUserJoin().fire(this);

        platform.getScheduler().runAsyncTask(() -> {
            applyCachedData();
            hasLoggedIn = true;
            platform.getModDetectionService().onPlayerLogin(this);
            resolveDatabaseProfile();
        });
    }

    public void onLogout() {
        platform.getModDetectionService().onPlayerLogout(uuid);
        platform.getScheduler().runAsyncTask(this::cacheData);
    }

    public void resyncFromPlatform() {
        PlatformPlayer current = this.platformPlayer;
        if (current == null) return;
        String brand = current.clientBrandName();
        if (brand != null && !brand.isBlank()) {
            this.clientBrand = brand;
        }
        current.resyncInventoryToClient();
    }

    @Blocking
    public void persistCacheOnShutdown() {
        cacheData();
    }

    public void timedOut() {
        disconnect("[TotemGuard] Timed out");
    }

    public void disconnect(String reason) {
        if (!hasDisconnected.compareAndSet(false, true)) return;
        platform.getLogger().info("Disconnecting " + user.getName() + ": " + reason);
        try {
            user.sendPacket(new WrapperPlayServerDisconnect(Component.text(reason)));
        } catch (Exception ignored) {
        }
        user.closeConnection();
    }

    public void disconnect(Component screen, String logReason) {
        if (!hasDisconnected.compareAndSet(false, true)) return;
        platform.getLogger().info("Disconnecting " + user.getName() + ": " + logReason);
        try {
            user.sendPacket(new WrapperPlayServerDisconnect(screen));
        } catch (Exception ignored) {
        }
        user.closeConnection();
    }

    void resolveDatabaseProfile() {
        if (!platform.getDatabaseRepository().isConnected()) return;
        try {
            long[] result = platform.getDatabaseRepository().resolveProfile(
                    uuid,
                    user.getName(),
                    clientBrand,
                    getClientVersion().getProtocolVersion(),
                    System.currentTimeMillis()
            );
            this.databasePlayerId = (int) result[0];
            this.databaseProfileId = result[1];
        } catch (Exception ex) {
            platform.getLogger().warning(
                    "Failed to resolve database profile for " + user.getName() + ": " + ex.getMessage());
        }
    }

    public boolean isModDetectionWindow(int windowId) {
        return windowId == modDetectionWindowId;
    }

    public void onServerTick() {
        data.flushNetherPortalContact();
    }

    public void triggerInventoryEvent() {
        CarriedItem carriedItem = inventory.getCarriedItem();
        boolean carriedItemUpdated = carriedItem != null && carriedItem.isUpdated();

        if (!carriedItemUpdated && inventory.getUpdatedSlots().isEmpty()) {
            return;
        }

        var updatedSlotsSnapshot = new ArrayList<>(inventory.getUpdatedSlots());
        var issuer = inventory.getLastIssuer();
        var carriedSnapshot = carriedItemUpdated ? carriedItem : null;

        if (carriedItemUpdated) {
            carriedItem.hasUpdated();
        }
        inventory.getUpdatedSlots().clear();

        platform.getInternalEventBus().getInventoryChanged().fire(this, carriedSnapshot, updatedSlotsSnapshot, issuer);
        debugOverlayManager.refresh();
    }

    @Override
    public @NotNull String getName() {
        return user.getName();
    }

    @Override
    public boolean hasAlertsEnabled() {
        return TGPlatform.getInstance().getAlertRepository().hasAlertsEnabled(uuid);
    }

    @Override
    public boolean toggleAlerts() {
        return TGPlatform.getInstance().getAlertRepository().toggleAlerts(uuid);
    }

    @Override
    public @NotNull HistoryView getHistory() {
        return TGPlatform.getInstance().getHistoryRepository().of(uuid);
    }

    @Override
    public @NotNull BanAnimation getBanAnimation() {
        return banAnimation;
    }

    @Override
    public @NotNull InventoryStatus getInventoryStatus() {
        return new InventoryStatus(data.isOpenInventory(), data.getLastInventoryIssuer() == Issuer.SERVER);
    }

    public ClientVersion getClientVersion() {
        return Objects.requireNonNullElseGet(user.getClientVersion(), () -> PacketEvents.getAPI().getServerManager().getVersion().toClientVersion());
    }

    public boolean supportsEndTick() {
        Boolean cached = supportsEndTickCache;
        if (cached != null) return cached;
        return computeSupportsEndTick();
    }

    private boolean computeSupportsEndTick() {
        return getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21_2) && PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_21_2);
    }

    @Blocking
    private void applyCachedData() {
        CacheRepositoryImpl cacheRepository = platform.getCacheRepository();
        List<CheckSnapshot> checkSnapshots = cacheRepository.getAndRefresh(
                CacheKeys.checkSnapshots(uuid), CacheCodecs.CHECK_SNAPSHOTS, CHECK_SNAPSHOT_TTL);
        if (checkSnapshots != null) checkManager.applySnapshot(checkSnapshots);
    }

    @Blocking
    private void cacheData() {
        CacheRepositoryImpl cacheRepository = platform.getCacheRepository();
        cacheRepository.put(CacheKeys.checkSnapshots(uuid),
                checkManager.getSnapshot(), CacheCodecs.CHECK_SNAPSHOTS, CHECK_SNAPSHOT_TTL);
    }
}
