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
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class SyncFocusAlertPacket extends Packet<SyncFocusAlertPacket.Payload> {

    public SyncFocusAlertPacket(int id) {
        super(id, MessagingTopic.FOCUS);
    }

    @Override
    public Payload read(ByteArrayDataInput input) {
        UUID violatorUuid = new UUID(input.readLong(), input.readLong());
        String violatorName = input.readUTF();
        Component component = GsonComponentSerializer.gson().deserialize(input.readUTF());
        return new Payload(violatorUuid, violatorName, component);
    }

    @Override
    public void writeData(ByteArrayDataOutput output, Payload payload) {
        output.writeLong(payload.violatorUuid.getMostSignificantBits());
        output.writeLong(payload.violatorUuid.getLeastSignificantBits());
        output.writeUTF(payload.violatorName);
        output.writeUTF(GsonComponentSerializer.gson().serialize(payload.component));
    }

    public record Payload(@NotNull UUID violatorUuid, @NotNull String violatorName, @NotNull Component component) {
    }
}
