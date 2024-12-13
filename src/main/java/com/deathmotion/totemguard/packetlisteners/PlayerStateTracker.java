/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2024 Bram and contributors
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

package com.deathmotion.totemguard.packetlisteners;

import com.deathmotion.totemguard.models.PlayerState;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;

import java.util.UUID;

public class PlayerStateTracker implements PacketListener {
    private final UserTracker userTracker;

    public PlayerStateTracker(UserTracker userTracker) {
        this.userTracker = userTracker;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType().equals(PacketType.Play.Client.ENTITY_ACTION)) {
            handleEntityAction(event);
        }
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        PacketTypeCommon packetType = event.getPacketType();
        if (packetType.equals(PacketType.Play.Server.RESPAWN) || packetType.equals(PacketType.Play.Server.JOIN_GAME) || packetType.equals(PacketType.Play.Server.CONFIGURATION_START)) {
            resetPlayerState(event.getUser());
        }
    }

    private void handleEntityAction(PacketReceiveEvent event) {
        User user = event.getUser();
        UUID uuid = user.getUUID();

        userTracker.getTotemPlayer(uuid).ifPresent(totemPlayer -> {
            PlayerState playerState = totemPlayer.playerState();
            WrapperPlayClientEntityAction actionPacket = new WrapperPlayClientEntityAction(event);

            switch (actionPacket.getAction()) {
                case START_SNEAKING -> playerState.setSneaking(true);
                case STOP_SNEAKING -> playerState.setSneaking(false);
                case START_SPRINTING -> playerState.setSprinting(true);
                case STOP_SPRINTING -> playerState.setSprinting(false);
            }
        });
    }

    private void resetPlayerState(User user) {
        UUID uuid = user.getUUID();

        userTracker.getTotemPlayer(uuid).ifPresent(totemPlayer -> {
            PlayerState playerState = totemPlayer.playerState();
            playerState.setSneaking(false);
            playerState.setSprinting(false);
        });
    }
}