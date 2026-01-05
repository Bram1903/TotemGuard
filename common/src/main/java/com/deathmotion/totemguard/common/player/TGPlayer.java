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

import com.deathmotion.totemguard.api.event.impl.TGUserJoinEvent;
import com.deathmotion.totemguard.api.user.TGUser;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.check.CheckManagerImpl;
import com.deathmotion.totemguard.common.check.impl.mods.Mod;
import com.deathmotion.totemguard.common.event.internal.impl.InventoryChangedEvent;
import com.deathmotion.totemguard.common.platform.player.PlatformPlayer;
import com.deathmotion.totemguard.common.platform.player.PlatformUser;
import com.deathmotion.totemguard.common.platform.player.PlatformUserCreation;
import com.deathmotion.totemguard.common.player.data.ClickData;
import com.deathmotion.totemguard.common.player.data.Data;
import com.deathmotion.totemguard.common.player.data.TotemData;
import com.deathmotion.totemguard.common.player.inventory.PacketInventory;
import com.deathmotion.totemguard.common.player.inventory.slot.CarriedItem;
import com.deathmotion.totemguard.common.player.latency.LatencyHandler;
import com.deathmotion.totemguard.common.player.processor.ProcessorInbound;
import com.deathmotion.totemguard.common.player.processor.ProcessorOutbound;
import com.deathmotion.totemguard.common.player.processor.inbound.InboundActionProcessor;
import com.deathmotion.totemguard.common.player.processor.inbound.InboundClientBrandProcessor;
import com.deathmotion.totemguard.common.player.processor.inbound.InboundInventoryProcessor;
import com.deathmotion.totemguard.common.player.processor.outbound.*;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.User;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying.isFlying;

/**
 * Represents a player in TotemGuard. This object is bound to a single player and gets removed once the player leaves the server / proxy.
 */
@Getter
public class TGPlayer implements TGUser {

    private final TGPlatform platform;
    private final UUID uuid;
    private final User user;
    private final LatencyHandler latencyHandler;
    private final PacketInventory inventory;
    private final CheckManagerImpl checkManager;

    private final Data data;
    private final TotemData totemData;
    private final ClickData clickData;

    private final List<ProcessorInbound> processorInbounds;
    private final List<ProcessorOutbound> processorOutbounds;

    private boolean hasLoggedIn;
    private PlatformUser platformUser;
    private @Nullable PlatformPlayer platformPlayer;

    @Setter()
    private String clientBrand;

    @Setter
    @Nullable
    private Long lastTotemUse;

    @Setter
    @Nullable
    private Long lastTotemUseCompensated;

    public TGPlayer(@NotNull User user) {
        this.platform = TGPlatform.getInstance();
        this.uuid = user.getUUID();
        this.user = user;
        this.latencyHandler = new LatencyHandler(this);
        this.inventory = new PacketInventory();
        this.checkManager = new CheckManagerImpl(this);
        this.data = new Data();
        this.totemData = new TotemData();
        this.clickData = new ClickData();

        this.processorInbounds = new ArrayList<>() {{
            add(new InboundInventoryProcessor(TGPlayer.this));
            add(new InboundClientBrandProcessor(TGPlayer.this));
            add(new InboundActionProcessor(TGPlayer.this));
        }};

        this.processorOutbounds = new ArrayList<>() {{
            add(new OutboundBundleProcessor(TGPlayer.this));
            add(new OutboundSpawnProcessor(TGPlayer.this));
            add(new OutboundHealthProcessor(TGPlayer.this));
            add(new OutboundTotemActivatedProcessor(TGPlayer.this));
            add(new OutboundInventoryProcessor(TGPlayer.this));
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

        hasLoggedIn = true;
        platform.getEventRepository().post(new TGUserJoinEvent(this));

        checkManager.getPacketCheck(Mod.class).handle();
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
    }


    @Override
    public @NotNull String getName() {
        return user.getName();
    }

    @Override
    public boolean hasAlertsEnabled() {
        return false;
    }

    @Override
    public boolean setAlertsEnabled(boolean alertsEnabled) {
        return false;
    }

    public ClientVersion getClientVersion() {
        // If temporarily null, assume server version...
        return Objects.requireNonNullElseGet(user.getClientVersion(), () -> ClientVersion.getById(PacketEvents.getAPI().getServerManager().getVersion().getProtocolVersion()));
    }

    public boolean isTickEndPacket(PacketTypeCommon packetType) {
        if (getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21_2) && packetType == PacketType.Play.Client.CLIENT_TICK_END) {
            return true;
        }

        return isFlying(packetType);
    }
}
