/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2025 Bram and contributors
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

package com.deathmotion.totemguard.common.event.packet;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.manager.PlayerManager;
import com.github.retrooper.packetevents.event.*;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;

public class PacketPlayerJoinQuit extends PacketListenerAbstract {

    private final PlayerManager playerManager;

    public PacketPlayerJoinQuit(TGPlatform platform) {
        this.playerManager = platform.getPlayerManager();
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Login.Server.LOGIN_SUCCESS) {
            event.getTasksAfterSend().add(() -> playerManager.onLoginPacket(event.getUser()));
        }
    }

    @Override
    public void onUserConnect(UserConnectEvent event) {
        if (event.getUser().getConnectionState() == ConnectionState.PLAY && !playerManager.isExempt(event.getUser())) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onUserLogin(UserLoginEvent event) {
        playerManager.onLogin(event.getUser());
    }

    @Override
    public void onUserDisconnect(UserDisconnectEvent event) {
        playerManager.onPlayerDisconnect(event.getUser());
    }
}