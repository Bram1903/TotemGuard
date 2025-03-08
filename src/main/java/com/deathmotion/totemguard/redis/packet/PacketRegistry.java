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

package com.deathmotion.totemguard.redis.packet;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class PacketRegistry {
    private final ConcurrentMap<Integer, Packet<?>> packets = new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer, List<PacketProcessor<?>>> processors = new ConcurrentHashMap<>();

    public PacketRegistry() {
        for (Packets packet : Packets.values()) {
            packets.put(packet.getId(), packet.getPacket());
        }
    }

    public <T> void registerProcessor(int packetId, PacketProcessor<T> processor) {
        if (packets.containsKey(packetId)) {
            processors.computeIfAbsent(packetId, k -> new ArrayList<>()).add(processor);
        } else {
            throw new IllegalArgumentException("Invalid packet ID: " + packetId);
        }
    }

    public void unregister(int packetId, PacketProcessor<?> processor) {
        processors.get(packetId).remove(processor);
    }

    public void handlePacket(ByteArrayDataInput dataInput) {
        int packetId = dataInput.readInt();
        if (!packets.containsKey(packetId)) throw new IllegalArgumentException("Invalid packet ID: " + packetId);

        if (processors.containsKey(packetId)) {
            // We only read the packet if there are processors registered for it
            Object packet = packets.get(packetId).read(dataInput);

            for (PacketProcessor<?> processor : processors.get(packetId)) {
                processor.handleAny(packet);
            }
        }
    }
}
