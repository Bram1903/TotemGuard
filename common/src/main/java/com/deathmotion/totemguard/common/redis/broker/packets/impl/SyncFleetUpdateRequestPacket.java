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

package com.deathmotion.totemguard.common.redis.broker.packets.impl;

import com.deathmotion.totemguard.common.redis.broker.MessagingTopic;
import com.deathmotion.totemguard.common.redis.broker.packets.Packet;
import com.deathmotion.totemguard.common.redis.broker.packets.PacketIO;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;

import java.util.UUID;

/**
 * Fleet-wide trigger to run an update. Each receiving instance re-resolves its own
 * loader config locally and downloads / stages accordingly. Dedup of downloads is
 * coordinated separately via {@link SyncFleetJarReadyPacket}.
 */
public final class SyncFleetUpdateRequestPacket extends Packet<SyncFleetUpdateRequestPacket.Payload> {

    public SyncFleetUpdateRequestPacket(int id) {
        super(id, MessagingTopic.UPDATES);
    }

    @Override
    public Payload read(ByteArrayDataInput input) {
        UUID requestId = PacketIO.readUUID(input);
        UUID originator = PacketIO.readUUID(input);
        String originatorName = input.readUTF();
        boolean force = input.readBoolean();
        boolean dryRun = input.readBoolean();
        boolean restartFleet = input.readBoolean();
        return new Payload(requestId, originator, originatorName, force, dryRun, restartFleet);
    }

    @Override
    public void writeData(ByteArrayDataOutput output, Payload payload) {
        PacketIO.writeUUID(output, payload.requestId);
        PacketIO.writeUUID(output, payload.originatorInstanceId);
        output.writeUTF(payload.originatorName);
        output.writeBoolean(payload.force);
        output.writeBoolean(payload.dryRun);
        output.writeBoolean(payload.restartFleet);
    }

    public record Payload(
            UUID requestId,
            UUID originatorInstanceId,
            String originatorName,
            boolean force,
            boolean dryRun,
            boolean restartFleet
    ) {
    }
}
