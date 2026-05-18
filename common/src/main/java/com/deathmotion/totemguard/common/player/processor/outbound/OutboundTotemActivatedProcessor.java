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

package com.deathmotion.totemguard.common.player.processor.outbound;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.inventory.InventoryConstants;
import com.deathmotion.totemguard.common.player.latency.PacketLatencyHandler;
import com.deathmotion.totemguard.common.player.processor.ProcessorOutbound;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityStatus;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;

public final class OutboundTotemActivatedProcessor extends ProcessorOutbound {

    private static final int TOTEM_OF_UNDYING_STATUS = 35;

    private final PacketLatencyHandler latencyHandler;

    private boolean pendingTotemActivation;

    public OutboundTotemActivatedProcessor(TGPlayer player) {
        super(player);
        this.latencyHandler = player.getLatencyHandler();
    }

    @Override
    public void handleOutbound(PacketSendEvent event) {
        if (event.isCancelled()) return;

        final PacketTypeCommon type = event.getPacketType();
        if (type == PacketType.Play.Server.ENTITY_STATUS) {
            handleEntityStatus(event);
        } else if (type == PacketType.Play.Server.SET_SLOT) {
            handleSetSlot(event);
        }
    }

    private void handleEntityStatus(PacketSendEvent event) {
        final WrapperPlayServerEntityStatus packet = new WrapperPlayServerEntityStatus(event);
        if (packet.getEntityId() != player.getUser().getEntityId()) return;
        if (packet.getStatus() != TOTEM_OF_UNDYING_STATUS) return;

        pendingTotemActivation = true;
    }

    private void handleSetSlot(PacketSendEvent event) {
        if (!pendingTotemActivation) return;

        final WrapperPlayServerSetSlot packet = new WrapperPlayServerSetSlot(event);
        if (packet.getWindowId() != InventoryConstants.PLAYER_WINDOW_ID) return;

        final int slot = packet.getSlot();
        if (!player.getInventory().isHandSlot(slot)) return;

        final ItemStack itemStack = packet.getItem();
        if (!itemStack.isEmpty()) return;
        if (!player.getInventory().isTotemInSlot(slot)) return;

        pendingTotemActivation = false;

        latencyHandler.compensate(event, timestamp -> {
            player.setLastTotemUse(timestamp);
            player.setLastTotemPickup(null);
            player.getDebugOverlayManager().refresh();
            TGPlatform.getInstance().getInternalEventBus().getTotemActivated().fire(player, timestamp);
        });
    }
}
