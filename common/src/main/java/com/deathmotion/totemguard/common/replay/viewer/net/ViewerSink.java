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

package com.deathmotion.totemguard.common.replay.viewer.net;

import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import com.github.retrooper.packetevents.netty.buffer.UnpooledByteBufAllocationHelper;
import com.github.retrooper.packetevents.netty.channel.ChannelHelper;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import lombok.Getter;
import lombok.Setter;

public final class ViewerSink {

    private final User user;

    @Getter
    @Setter
    private volatile boolean muted;

    public ViewerSink(User user) {
        this.user = user;
    }

    public boolean open() {
        return ChannelHelper.isOpen(user.getChannel());
    }

    public void send(PacketWrapper<?> wrapper) {
        if (muted || !open()) return;
        user.sendPacketSilently(wrapper);
    }

    public void send(int packetId, byte[] payload) {
        send(packetId, payload, payload.length, null);
    }

    public void send(int packetId, byte[] payload, int length, byte[] suffix) {
        if (muted || !open()) return;
        Object buffer = UnpooledByteBufAllocationHelper.buffer();
        boolean handed = false;
        try {
            ByteBufHelper.writeVarInt(buffer, packetId);
            ByteBufHelper.writeBytes(buffer, payload, 0, length);
            if (suffix != null) ByteBufHelper.writeBytes(buffer, suffix);
            handed = true;
            user.sendPacketSilently(buffer);
        } finally {
            if (!handed) ByteBufHelper.release(buffer);
        }
    }
}
