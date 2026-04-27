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

import com.deathmotion.totemguard.common.redis.broker.packets.Packet;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;

/**
 * Broadcast that the publishing server has discovered a new TotemGuard release.
 * Payload is the raw GitHub tag (e.g. {@code v3.1.0}).
 */
public class SyncUpdateAvailablePacket extends Packet<String> {

    public SyncUpdateAvailablePacket(int id) {
        super(id);
    }

    @Override
    public String read(ByteArrayDataInput input) {
        return input.readUTF();
    }

    @Override
    public void writeData(ByteArrayDataOutput output, String tag) {
        output.writeUTF(tag);
    }
}
