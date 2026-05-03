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

package com.deathmotion.totemguard.common.redis.broker.packets;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import com.github.retrooper.packetevents.netty.buffer.UnpooledByteBufAllocationHelper;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class PacketIO {

    private PacketIO() {
    }

    public static void writeUUID(ByteArrayDataOutput out, UUID uuid) {
        out.writeLong(uuid.getMostSignificantBits());
        out.writeLong(uuid.getLeastSignificantBits());
    }

    public static UUID readUUID(ByteArrayDataInput in) {
        long msb = in.readLong();
        long lsb = in.readLong();
        return new UUID(msb, lsb);
    }

    public static void writeOptionalUUID(ByteArrayDataOutput out, @Nullable UUID uuid) {
        if (uuid == null) {
            out.writeBoolean(false);
            return;
        }
        out.writeBoolean(true);
        writeUUID(out, uuid);
    }

    public static @Nullable UUID readOptionalUUID(ByteArrayDataInput in) {
        if (!in.readBoolean()) return null;
        return readUUID(in);
    }

    public static void writeOptionalString(ByteArrayDataOutput out, @Nullable String value) {
        if (value == null) {
            out.writeBoolean(false);
            return;
        }
        out.writeBoolean(true);
        out.writeUTF(value);
    }

    public static @Nullable String readOptionalString(ByteArrayDataInput in) {
        if (!in.readBoolean()) return null;
        return in.readUTF();
    }

    public static void writeItemStack(ByteArrayDataOutput out, @NotNull ItemStack stack, @NotNull ServerVersion version) {
        Object buffer = UnpooledByteBufAllocationHelper.buffer();
        try {
            PacketWrapper<?> wrapper = PacketWrapper.createUniversalPacketWrapper(buffer, version);
            wrapper.writeItemStack(stack);
            int len = ByteBufHelper.readableBytes(buffer);
            byte[] bytes = new byte[len];
            if (len > 0) ByteBufHelper.readBytes(buffer, bytes);
            out.writeInt(len);
            out.write(bytes);
        } finally {
            ByteBufHelper.release(buffer);
        }
    }

    public static @NotNull ItemStack readItemStack(ByteArrayDataInput in, @NotNull ServerVersion version) {
        int len = in.readInt();
        byte[] bytes = new byte[len];
        in.readFully(bytes);
        Object buffer = UnpooledByteBufAllocationHelper.wrappedBuffer(bytes);
        try {
            PacketWrapper<?> wrapper = PacketWrapper.createUniversalPacketWrapper(buffer, version);
            return wrapper.readItemStack();
        } finally {
            ByteBufHelper.release(buffer);
        }
    }

    public static @NotNull ServerVersion resolveServerVersion(int protocolVersion) {
        ServerVersion resolved = ServerVersion.getById(protocolVersion);
        return resolved != null ? resolved : PacketEvents.getAPI().getServerManager().getVersion();
    }
}
