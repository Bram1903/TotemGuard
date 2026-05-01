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
import com.deathmotion.totemguard.api.user.TGUser;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.cache.CacheCodecs;
import com.deathmotion.totemguard.common.cache.CacheKeys;
import com.deathmotion.totemguard.common.cache.CacheRepositoryImpl;
import com.deathmotion.totemguard.common.cache.data.CheckSnapshot;
import com.deathmotion.totemguard.common.check.CheckManagerImpl;
import com.deathmotion.totemguard.common.check.impl.mods.Mod;
import com.deathmotion.totemguard.common.event.api.impl.TGUserJoinEventImpl;
import com.deathmotion.totemguard.common.event.internal.impl.InventoryChangedEvent;
import com.deathmotion.totemguard.common.platform.player.PlatformPlayer;
import com.deathmotion.totemguard.common.platform.player.PlatformUser;
import com.deathmotion.totemguard.common.platform.player.PlatformUserCreation;
import com.deathmotion.totemguard.common.player.data.ClickData;
import com.deathmotion.totemguard.common.player.data.Data;
import com.deathmotion.totemguard.common.player.data.TickData;
import com.deathmotion.totemguard.common.player.data.TotemData;
import com.deathmotion.totemguard.common.player.data.ping.PingData;
import com.deathmotion.totemguard.common.player.debug.DebugOverlayManager;
import com.deathmotion.totemguard.common.player.debug.provider.TotemDebugProvider;
import com.deathmotion.totemguard.common.player.debug.provider.TransactionDebugProvider;
import com.deathmotion.totemguard.common.player.inventory.InventoryRecipeTracker;
import com.deathmotion.totemguard.common.player.inventory.PacketInventory;
import com.deathmotion.totemguard.common.player.inventory.slot.CarriedItem;
import com.deathmotion.totemguard.common.player.latency.PacketLatencyHandler;
import com.deathmotion.totemguard.common.player.processor.ProcessorInbound;
import com.deathmotion.totemguard.common.player.processor.ProcessorOutbound;
import com.deathmotion.totemguard.common.player.processor.inbound.*;
import com.deathmotion.totemguard.common.player.processor.outbound.*;
import com.deathmotion.totemguard.common.punishment.BanAnimationImpl;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Represents a player in TotemGuard. This object is bound to a single player and gets removed once the player leaves the server / proxy.
 */
@Getter
public class TGPlayer implements TGUser {

    private static final Duration CHECK_SNAPSHOT_TTL = Duration.ofMinutes(5);
    private final TGPlatform platform;
    private final UUID uuid;
    private final User user;
    private final PacketInventory inventory;
    private final CheckManagerImpl checkManager;
    private final Data data;
    private final TotemData totemData;
    private final ClickData clickData;
    private final TickData tickData;
    private final PingData pingData;
    private final InventoryRecipeTracker inventoryRecipeTracker;
    private final DebugOverlayManager debugOverlayManager;
    private final PacketLatencyHandler latencyHandler;
    private final BanAnimation banAnimation;
    private final List<ProcessorInbound> processorInbounds;
    private final List<ProcessorOutbound> processorOutbounds;

    private final AtomicBoolean hasDisconnected = new AtomicBoolean();
    private final int modDetectionWindowId = -ThreadLocalRandom.current().nextInt(10_000, Integer.MAX_VALUE);
    private boolean hasLoggedIn;
    private PlatformUser platformUser;
    private @Nullable PlatformPlayer platformPlayer;
    @Setter()
    private String clientBrand = "Unknown";
    /**
     * 0 while unresolved or when the database is disabled.
     */
    @Setter
    private volatile int databasePlayerId;

    /**
     * null while unresolved or when the database is disabled.
     */
    @Setter
    @Nullable
    private volatile Long databaseProfileId;

    @Setter
    @Nullable
    private Long lastTotemUse;

    @Setter
    @Nullable
    private Long lastTotemPickup;

    @Getter
    @Setter
    private boolean vpn;

    /**
     * True while a staff-initiated {@code /tg check} is running against this player.
     * Server-originated inventory mutations performed by the command (clearing the
     * offhand, restoring contents) must not feed the auto-totem detection pipeline,
     * so processors consult this flag and skip their own bookkeeping while it's set.
     */
    @Setter
    private volatile boolean manualCheckActive;

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
        this.inventoryRecipeTracker = new InventoryRecipeTracker(this);
        this.debugOverlayManager = new DebugOverlayManager(this);
        this.debugOverlayManager.register(new TransactionDebugProvider());
        this.debugOverlayManager.register(new TotemDebugProvider());
        this.latencyHandler = new PacketLatencyHandler(this);
        this.banAnimation = new BanAnimationImpl(this);
        this.checkManager = new CheckManagerImpl(this);

        this.processorInbounds = new ArrayList<>() {{
            add(new InboundPingProcessor(TGPlayer.this));
            add(new InboundInventoryProcessor(TGPlayer.this));
            add(new InboundClientBrandProcessor(TGPlayer.this));
            add(new InboundActionProcessor(TGPlayer.this));
            add(new InboundTeleportProcessor(TGPlayer.this));
            add(new InboundMovementProcessor(TGPlayer.this));
        }};

        this.processorOutbounds = new ArrayList<>() {{
            add(new OutboundPingProcessor(TGPlayer.this));
            add(new OutboundBundleProcessor(TGPlayer.this));
            add(new OutboundSpawnProcessor(TGPlayer.this));
            add(new OutboundTotemActivatedProcessor(TGPlayer.this));
            add(new OutboundInventoryProcessor(TGPlayer.this));
            add(new OutboundTeleportProcessor(TGPlayer.this));
            add(new OutboundMovementProcessor(TGPlayer.this));
            add(new OutboundEntityProcessor(TGPlayer.this));
            add(new OutboundCameraProcessor(TGPlayer.this));
            add(new OutboundMetadataProcessor(TGPlayer.this));
        }};
    }

    public void onLogin() {
        TGPlatform platform = TGPlatform.getInstance();
        PlayerRepositoryImpl playerRepository = platform.getPlayerRepository();

        PlatformUserCreation platformUserCreation = platform.getPlatformUserFactory().create(uuid);
        if (platformUserCreation == null) {
            playerRepository.removeUser(user);
            return;
        }

        platformUser = platformUserCreation.getPlatformUser();
        platformPlayer = platformUserCreation.getPlatformPlayer();

        if (!playerRepository.shouldCheck(user, platformUser)) {
            playerRepository.removeUser(user);
            return;
        }

        platform.getEventRepository().post(new TGUserJoinEventImpl(this));

        platform.getScheduler().runAsyncTask(() -> {
            applyCachedData();
            hasLoggedIn = true;
            checkManager.getPacketCheck(Mod.class).handle();
            platform.getAntiVPNRepository().validateConnection(this);
            resolveDatabaseProfile();
        });
    }

    public void onLogout() {
        platform.getScheduler().runAsyncTask(this::cacheData);
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

    public boolean isModDetectionActive() {
        Mod mod = checkManager.getPacketCheck(Mod.class);
        return mod != null && mod.isDetectionActive();
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

        InventoryChangedEvent event = new InventoryChangedEvent(
                this,
                carriedSnapshot,
                updatedSlotsSnapshot,
                issuer
        );

        platform.getEventRepository().post(event);
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

    public ClientVersion getClientVersion() {
        return Objects.requireNonNullElseGet(user.getClientVersion(), () -> PacketEvents.getAPI().getServerManager().getVersion().toClientVersion());
    }

    public boolean supportsEndTick() {
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
