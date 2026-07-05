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

import com.deathmotion.totemguard.common.check.impl.inventory.InventoryD;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.data.CombatTracker;
import com.deathmotion.totemguard.common.player.data.Data;
import com.deathmotion.totemguard.common.player.data.InputData;
import com.deathmotion.totemguard.common.player.inventory.enums.Issuer;
import com.deathmotion.totemguard.common.player.latency.PacketLatencyHandler;
import com.deathmotion.totemguard.common.player.processor.ProcessorOutbound;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChangeGameState;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerJoinGame;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerAbilities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerRespawn;

public class OutboundSpawnProcessor extends ProcessorOutbound {

    private final Data data;
    private final InputData inputData;
    private final PacketLatencyHandler latencyHandler;
    private final CombatTracker combatTracker;
    private final InventoryD inventoryD;

    public OutboundSpawnProcessor(TGPlayer player) {
        super(player);
        this.data = player.getData();
        this.inputData = player.getData().getInputData();
        this.latencyHandler = player.getLatencyHandler();
        this.combatTracker = player.getCombatTracker();
        this.inventoryD = player.getCheckManager().allChecks.getInstance(InventoryD.class);
    }

    @Override
    public void handleOutbound(PacketSendEvent event) {
        if (event.isCancelled()) return;
        final PacketTypeCommon packetType = event.getPacketType();

        if (packetType == PacketType.Play.Server.JOIN_GAME) {
            WrapperPlayServerJoinGame packet = new WrapperPlayServerJoinGame(event);
            data.setGameMode(packet.getGameMode());
        } else if (packetType == PacketType.Play.Server.CHANGE_GAME_STATE) {
            WrapperPlayServerChangeGameState packet = new WrapperPlayServerChangeGameState(event);
            if (packet.getReason() == WrapperPlayServerChangeGameState.Reason.CHANGE_GAME_MODE) {
                int ordinal = (int) packet.getValue();
                if (ordinal >= 0 && ordinal < GameMode.values().length) {
                    latencyHandler.compensate(event, () -> data.setGameMode(GameMode.values()[ordinal]));
                }
            }
        } else if (packetType == PacketType.Play.Server.PLAYER_ABILITIES) {
            WrapperPlayServerPlayerAbilities packet = new WrapperPlayServerPlayerAbilities(event);
            boolean flightAllowed = packet.isFlightAllowed();
            boolean flying = packet.isFlying();
            latencyHandler.compensate(event, () -> {
                data.setCanFly(flightAllowed);
                data.setFlying(flying);
            });
        } else if (packetType == PacketType.Play.Server.RESPAWN) {
            WrapperPlayServerRespawn packet = new WrapperPlayServerRespawn(event);
            boolean resetSwimming = (packet.getKeptData() & WrapperPlayServerRespawn.KEEP_ENTITY_DATA) == 0;

            latencyHandler.compensate(event, timestamp -> {
                if (player.getClientVersion().isOlderThan(ClientVersion.V_1_16) || player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_20)) {
                    data.setSneaking(false);
                }

                data.setGameMode(packet.getGameMode());
                data.setOpenInventory(false, Issuer.SERVER);
                data.setDead(false);
                data.setSprinting(false);
                data.resetSprintTracking();
                data.setVehicleId(-1);
                data.setSleeping(false);
                if (resetSwimming) data.setSwimming(false);
                inputData.reset();
                data.getMovementData().reset();
                player.getPhysics().reset();
                data.getAttributeData().reset();
                data.getEffectData().reset();
                combatTracker.reset();
                if (inventoryD != null) inventoryD.resetSession();
            });
        }
    }
}
