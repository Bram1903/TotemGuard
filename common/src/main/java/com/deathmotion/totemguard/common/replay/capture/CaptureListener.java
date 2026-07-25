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

package com.deathmotion.totemguard.common.replay.capture;

import com.deathmotion.totemguard.common.player.PlayerLookup;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.replay.ReplayService;
import com.github.retrooper.packetevents.event.*;
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.login.client.WrapperLoginClientLoginStart;
import org.jetbrains.annotations.Nullable;

public final class CaptureListener extends PacketListenerAbstract {

    private final ReplayService service;
    private final PlayerLookup players;

    public CaptureListener(ReplayService service, PlayerLookup players) {
        super(PacketListenerPriority.LOW);
        this.service = service;
        this.players = players;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Login.Client.LOGIN_START) {
            service.onLoginStart(event.getUser(), new WrapperLoginClientLoginStart(event).getUsername());
        }
        capture(event, true);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        capture(event, false);
    }

    @Override
    public void onUserDisconnect(UserDisconnectEvent event) {
        service.onDisconnect(event.getUser());
    }

    private void capture(ProtocolPacketEvent event, boolean inbound) {
        if (!service.hasCapture(event.getUser())) return;
        if (!CaptureFilter.kept(event.getPacketType())) return;
        TGPlayer player = players.getPlayer(event.getUser());
        byte[] payload = payload(event);
        if (!keptVelocity(event, player, payload)) return;
        service.onPacket(event.getUser(), inbound, event.isCancelled(), event.getPacketType(),
                event.getConnectionState(), event.getPacketId(), payload, player);
    }

    private boolean keptVelocity(ProtocolPacketEvent event, @Nullable TGPlayer player, byte[] payload) {
        if (event.getPacketType() != PacketType.Play.Server.ENTITY_VELOCITY || player == null) return true;
        int entityId;
        try {
            entityId = new PayloadReader(payload).readVarInt();
        } catch (RuntimeException malformed) {
            return true;
        }
        return CaptureFilter.keptVelocity(entityId, event.getUser().getEntityId(),
                player.getData().getVehicleId(),
                player.getWorldMirror().entities().announcedType(entityId));
    }

    private byte[] payload(ProtocolPacketEvent event) {
        Object buffer = event.getByteBuf();
        if (!(event instanceof PacketSendEvent send)
                || event.getPacketType() != PacketType.Play.Server.CHUNK_DATA) {
            return ByteBufHelper.copyBytes(buffer);
        }

        int mark = ByteBufHelper.readerIndex(buffer);
        byte[] compacted = ChunkPayloadCompactor.compact(send, event.getServerVersion());
        ByteBufHelper.readerIndex(buffer, mark);
        return compacted == null ? ByteBufHelper.copyBytes(buffer) : compacted;
    }
}
