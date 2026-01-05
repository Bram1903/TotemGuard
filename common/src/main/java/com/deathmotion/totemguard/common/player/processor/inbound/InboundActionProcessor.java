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

package com.deathmotion.totemguard.common.player.processor.inbound;

import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.data.ClickData;
import com.deathmotion.totemguard.common.player.data.Data;
import com.deathmotion.totemguard.common.player.processor.ProcessorInbound;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.*;

public class InboundActionProcessor extends ProcessorInbound {

    private final Data data;
    private final ClickData clickData;

    public InboundActionProcessor(TGPlayer player) {
        super(player);
        this.data = player.getData();
        this.clickData = player.getClickData();
    }

    @Override
    public void handleInbound(PacketReceiveEvent event) {
        final PacketTypeCommon packetType = event.getPacketType();

        if (packetType == PacketType.Play.Client.ANIMATION) {
            // This misses breaking blocks (needs fixing)
            if (data.isInvalidLeftClick()) return;
            clickData.recordLeftClick();
        } else if (packetType == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
            data.setPlacing(true);
            clickData.recordRightClick();
        } else if (packetType == PacketType.Play.Client.INTERACT_ENTITY) {
            if (new WrapperPlayClientInteractEntity(event).getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                data.setAttacking(true);
            } else {
                data.setInteracting(true);
            }
        } else if (packetType == PacketType.Play.Client.USE_ITEM) {
            data.setUsing(true);
            clickData.recordRightClick();
        } else if (packetType == PacketType.Play.Client.ENTITY_ACTION) {
            WrapperPlayClientEntityAction packet = new WrapperPlayClientEntityAction(event);
            if (packet.getEntityId() != player.getUser().getEntityId()) return;

            switch (packet.getAction()) {
                case START_SNEAKING -> data.setSneaking(true);
                case STOP_SNEAKING -> data.setSneaking(false);
                case START_SPRINTING -> data.setSprinting(true);
                case STOP_SPRINTING -> data.setSprinting(false);
            }
        } else if (packetType == PacketType.Play.Client.PLAYER_DIGGING) {
            WrapperPlayClientPlayerDigging packet = new WrapperPlayClientPlayerDigging(event);

            switch (packet.getAction()) {
                case START_DIGGING -> data.setDigging(true);
                case CANCELLED_DIGGING, FINISHED_DIGGING -> data.setDigging(false);
                case RELEASE_USE_ITEM -> {
                    data.setUsing(false);
                }
            }
        } else if (packetType == PacketType.Play.Client.PLAYER_INPUT) {
            if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21_6)) {
                WrapperPlayClientPlayerInput packet = new WrapperPlayClientPlayerInput(event);
                data.setSneaking(packet.isShift());
            }
        } else if (packetType == PacketType.Play.Client.PLAYER_ABILITIES) {
            WrapperPlayClientPlayerAbilities packet = new WrapperPlayClientPlayerAbilities(event);
            data.setFlying(packet.isFlying() && data.isCanFly());
        } else if (player.isTickEndPacket(packetType)) {
            if (packetType == PacketType.Play.Client.CLIENT_TICK_END) {
                // Autoclicker checks will only work on 1.21.2 because I cba to deal with timing issues
                // Older clients don't send flying packets when not moving, so I don't know when the tick ended
                clickData.tick();
            }
        }
    }

    @Override
    public void handleInboundPost(PacketReceiveEvent event) {
        final PacketTypeCommon packetType = event.getPacketType();

        if (player.isTickEndPacket(packetType)) {
            data.setPlacing(false);
            data.setDigging(false);
            data.setInteracting(false);
            data.setAttacking(false);
            data.setUsing(false);
        }

        clickData.checkPost();
    }
}
