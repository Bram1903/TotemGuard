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

import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.data.Data;
import com.deathmotion.totemguard.common.player.data.MovementData;
import com.deathmotion.totemguard.common.player.data.RotationCredits;
import com.deathmotion.totemguard.common.player.processor.ProcessorOutbound;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityPositionSync;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerPositionAndLook;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerRotation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPassengers;

public class OutboundMovementProcessor extends ProcessorOutbound {

    private final Data data;
    private final MovementData movementData;
    private final RotationCredits rotationCredits;

    public OutboundMovementProcessor(TGPlayer player) {
        super(player);
        this.data = player.getData();
        this.movementData = data.getMovementData();
        this.rotationCredits = data.getRotationCredits();
    }

    @Override
    public void handleOutbound(PacketSendEvent event) {
        if (event.isCancelled()) return;
        final PacketTypeCommon type = event.getPacketType();
        if (type == PacketType.Play.Server.PLAYER_POSITION_AND_LOOK) {
            movementData.handleServerSync(new WrapperPlayServerPlayerPositionAndLook(event));
        } else if (type == PacketType.Play.Server.PLAYER_ROTATION) {
            movementData.handleServerSync(new WrapperPlayServerPlayerRotation(event));
        }

        if (writesRotation(type, event)) {
            rotationCredits.serverWrote(event);
        }
    }

    // A horse snaps its rider's rotation from 1.21.9 and a boat clamps it, so any mount of ours is a rotation write
    private boolean writesRotation(PacketTypeCommon type, PacketSendEvent event) {
        if (type == PacketType.Play.Server.PLAYER_POSITION_AND_LOOK
                || type == PacketType.Play.Server.PLAYER_ROTATION
                || type == PacketType.Play.Server.FACE_PLAYER
                || type == PacketType.Play.Server.CAMERA
                || type == PacketType.Play.Server.JOIN_GAME
                || type == PacketType.Play.Server.RESPAWN) {
            return true;
        }
        if (type == PacketType.Play.Server.ENTITY_TELEPORT) {
            return movesUsOrOurVehicle(new WrapperPlayServerEntityTeleport(event).getEntityId());
        }
        // handleEntityPositionSync skips the entity the client is authoritative for, so only a vehicle's sync turns us
        if (type == PacketType.Play.Server.ENTITY_POSITION_SYNC) {
            return data.isInVehicle() && new WrapperPlayServerEntityPositionSync(event).getId() == data.getVehicleId();
        }
        if (type == PacketType.Play.Server.SET_PASSENGERS) {
            WrapperPlayServerSetPassengers packet = new WrapperPlayServerSetPassengers(event);
            if (packet.getEntityId() == data.getVehicleId()) return true;
            int self = player.getUser().getEntityId();
            for (int passenger : packet.getPassengers()) {
                if (passenger == self) return true;
            }
            return false;
        }
        if (type == PacketType.Play.Server.ENTITY_ANIMATION) {
            WrapperPlayServerEntityAnimation packet = new WrapperPlayServerEntityAnimation(event);
            return packet.getType() == WrapperPlayServerEntityAnimation.EntityAnimationType.WAKE_UP
                    && packet.getEntityId() == player.getUser().getEntityId();
        }
        return false;
    }

    private boolean movesUsOrOurVehicle(int entityId) {
        return entityId == player.getUser().getEntityId() || (data.isInVehicle() && entityId == data.getVehicleId());
    }
}
