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

package com.deathmotion.totemguard.common.check.impl.tick;

import com.deathmotion.totemguard.api.check.CheckType;
import com.deathmotion.totemguard.common.check.CheckImpl;
import com.deathmotion.totemguard.common.check.annotations.CheckData;
import com.deathmotion.totemguard.common.check.type.PacketCheck;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.data.MovementData;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(description = "Action packets after the movement packet of their tick", type = CheckType.TICK,
        experimental = true)
public class TickE extends CheckImpl implements PacketCheck {

    private static final int RELIABLE_TICKS = 3;
    private static final double SKIP_THRESHOLD = 0.03;
    private static final double BUFFER_GAIN = 1.0;
    private static final double BUFFER_DECAY = 0.05;
    private static final double BUFFER_THRESHOLD = 3.0;
    private static final double BUFFER_RETAIN = 1.0;

    private final boolean vehicleActionExempt;

    private boolean armed;
    private boolean flyingThisTick;
    private String queuedName;
    private int queuedCount;
    private String candidateName;
    private int candidateAge;
    private int reliableStreak;
    private boolean lastInVehicle;

    public TickE(TGPlayer player) {
        super(player);
        this.vehicleActionExempt = player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_19_3);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        PacketTypeCommon packetType = event.getPacketType();

        if (countsAsClientTick(packetType)) {
            onCountedTick();
            return;
        }

        if (packetType == PacketType.Play.Client.PONG) {
            if (!player.getPingData().isLastTransactionReplyValid()) return;
            if (armed && queuedName != null && candidateName == null) {
                candidateName = queuedName;
                candidateAge = 0;
            }
            queuedName = null;
            queuedCount = 0;
            armed = false;
            return;
        }

        if (!armed) return;
        String watched = watchedName(event, packetType);
        if (watched == null) return;
        if (queuedName == null) queuedName = watched;
        queuedCount++;
    }

    private void onCountedTick() {
        queuedName = null;
        queuedCount = 0;
        armed = true;

        boolean inVehicle = data.isInVehicle();
        boolean switchedVehicle = inVehicle != lastInVehicle;
        lastInVehicle = inVehicle;
        boolean reliable = tickReliable(inVehicle) && !switchedVehicle;
        reliableStreak = reliable ? reliableStreak + 1 : 0;

        if (candidateName != null) {
            candidateAge++;
            if (!reliable) {
                candidateName = null;
            } else if (candidateAge >= 2 && reliableStreak >= RELIABLE_TICKS) {
                if (buffer.increase(BUFFER_GAIN) >= BUFFER_THRESHOLD) {
                    buffer.set(BUFFER_RETAIN);
                    fail("packet={0}", candidateName);
                }
                candidateName = null;
            }
        } else {
            buffer.decrease(BUFFER_DECAY);
        }
    }

    private boolean tickReliable(boolean inVehicle) {
        if (player.supportsEndTick()) return true;
        if (inVehicle) return true;
        MovementData movement = data.getMovementData();
        if (!movement.isLastFlyingPositionChanged()) return false;
        double dx = movement.getCurrent().getX() - movement.getPrevious().getX();
        double dy = movement.getCurrent().getY() - movement.getPrevious().getY();
        double dz = movement.getCurrent().getZ() - movement.getPrevious().getZ();
        return Math.abs(dx) > SKIP_THRESHOLD || Math.abs(dy) > SKIP_THRESHOLD
                || Math.abs(dz) > SKIP_THRESHOLD;
    }

    private String watchedName(PacketReceiveEvent event, PacketTypeCommon packetType) {
        if (packetType == PacketType.Play.Client.PLAYER_ABILITIES) return "abilities";
        if (packetType == PacketType.Play.Client.INTERACT_ENTITY) return "interact";
        if (packetType == PacketType.Play.Client.ATTACK) return "attack";
        if (packetType == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) return "place";
        if (packetType == PacketType.Play.Client.USE_ITEM) return "use";
        if (packetType == PacketType.Play.Client.PLAYER_DIGGING) return "digging";
        if (packetType == PacketType.Play.Client.HELD_ITEM_CHANGE) return "held slot";
        if (packetType == PacketType.Play.Client.SPECTATE) return "spectate";
        if (packetType == PacketType.Play.Client.ENTITY_ACTION) {
            if (vehicleActionExempt && data.isInVehicle()) return null;
            WrapperPlayClientEntityAction packet = new WrapperPlayClientEntityAction(event);
            if (packet.getAction() == WrapperPlayClientEntityAction.Action.LEAVE_BED) return null;
            return "entity action";
        }
        return null;
    }

    private boolean countsAsClientTick(PacketTypeCommon packetType) {
        if (WrapperPlayClientPlayerFlying.isFlying(packetType)) {
            boolean counted = !data.getTeleportData().lastPacketWasTeleport()
                    && !data.getMovementData().isLastFlyingWasDuplicate();
            flyingThisTick = true;
            return counted;
        }

        if (packetType == PacketType.Play.Client.CLIENT_TICK_END && player.supportsEndTick()) {
            boolean hadFlying = flyingThisTick;
            flyingThisTick = false;
            return !hadFlying;
        }

        return false;
    }
}
