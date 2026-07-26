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

package com.deathmotion.totemguard.common.replay.viewer;

import com.github.retrooper.packetevents.event.*;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientHeldItemChange;

import java.util.Set;

public final class ReplayViewerListener extends PacketListenerAbstract {

    private static final Set<PacketTypeCommon> ALWAYS_OUT = Set.of(
            PacketType.Play.Server.KEEP_ALIVE,
            PacketType.Play.Server.PING,
            PacketType.Play.Server.DISCONNECT,
            PacketType.Play.Server.PLUGIN_MESSAGE,
            PacketType.Play.Server.RESOURCE_PACK_SEND,
            PacketType.Play.Server.CHAT_MESSAGE,
            PacketType.Play.Server.SYSTEM_CHAT_MESSAGE,
            PacketType.Play.Server.DISGUISED_CHAT,
            PacketType.Play.Server.PLAYER_CHAT_HEADER,
            PacketType.Play.Server.SERVER_DATA,
            PacketType.Play.Server.DECLARE_COMMANDS,
            PacketType.Play.Server.TAB_COMPLETE
    );

    private static final Set<PacketTypeCommon> ALWAYS_IN = Set.of(
            PacketType.Play.Client.KEEP_ALIVE,
            PacketType.Play.Client.PONG,
            PacketType.Play.Client.PLUGIN_MESSAGE,
            PacketType.Play.Client.CLIENT_SETTINGS,
            PacketType.Play.Client.CHAT_MESSAGE,
            PacketType.Play.Client.CHAT_COMMAND,
            PacketType.Play.Client.CHAT_COMMAND_UNSIGNED,
            PacketType.Play.Client.RESOURCE_PACK_STATUS,
            PacketType.Play.Client.TAB_COMPLETE
    );

    private final ReplayViewerService viewers;

    public ReplayViewerListener(ReplayViewerService viewers) {
        super(PacketListenerPriority.LOWEST);
        this.viewers = viewers;
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (!viewers.any()) return;
        if (event.getConnectionState() != ConnectionState.PLAY) return;
        if (!viewers.watching(event.getUser())) return;
        if (ALWAYS_OUT.contains(event.getPacketType())) return;
        event.setCancelled(true);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!viewers.any()) return;
        if (event.getConnectionState() != ConnectionState.PLAY) return;
        ReplayViewerSession session = viewers.session(event.getUser());
        if (session == null) return;

        PacketTypeCommon type = event.getPacketType();
        if (ALWAYS_IN.contains(type)) return;

        if (type == PacketType.Play.Client.HELD_ITEM_CHANGE) {
            session.onSelect(new WrapperPlayClientHeldItemChange(event).getSlot());
        } else if (type == PacketType.Play.Client.ANIMATION) {
            session.onSwing();
        } else if (type == PacketType.Play.Client.USE_ITEM
                || type == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT
                || type == PacketType.Play.Client.INTERACT_ENTITY) {
            session.onUse();
        }

        event.setCancelled(true);
    }

    @Override
    public void onUserDisconnect(UserDisconnectEvent event) {
        if (!viewers.any()) return;
        if (event.getUser().getUUID() == null) return;
        viewers.onDisconnect(event.getUser().getUUID());
    }
}
