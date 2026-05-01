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

import com.deathmotion.totemguard.common.redis.broker.MessagingTopic;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;

/**
 * Abstract representation of a Packet that can be serialized and deserialized.
 *
 * @param <T> the type of data carried by the packet
 */
public abstract class Packet<T> {

    private final int id;
    private final MessagingTopic topic;

    protected Packet(int id, MessagingTopic topic) {
        this.id = id;
        this.topic = topic;
    }

    public final MessagingTopic topic() {
        return topic;
    }

    /**
     * Reads the packet payload from a ByteArrayDataInput stream.
     *
     * @param input the data input containing the serialized packet
     * @return the deserialized packet payload
     */
    public abstract T read(ByteArrayDataInput input);

    public final int getId() {
        return id;
    }

    /**
     * Writes the packet payload to the output stream.
     *
     * @param output the data output stream
     * @param obj    the packet payload
     */
    public abstract void writeData(ByteArrayDataOutput output, T obj);
}
