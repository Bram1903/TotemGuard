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

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.api.versioning.TGVersion;
import com.deathmotion.totemguard.util.TGVersions;
import com.google.common.io.ByteArrayDataInput;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class PacketRegistry {

    private final ConcurrentMap<Integer, Packet<?>> packets = new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer, List<PacketProcessor<?>>> processors = new ConcurrentHashMap<>();

    public PacketRegistry() {
        for (Packets packet : Packets.values()) {
            packets.put(packet.getId(), packet.getPacket());
        }
    }

    public <T> void registerProcessor(int packetId, PacketProcessor<T> processor) {
        Packet<?> packet = packets.get(packetId);
        if (packet == null) {
            throw new IllegalArgumentException("Invalid packet ID: " + packetId);
        }

        processors.computeIfAbsent(packetId, k -> new CopyOnWriteArrayList<>()).add(processor);
    }

    public void unregister(int packetId, PacketProcessor<?> processor) {
        Optional.ofNullable(processors.get(packetId)).ifPresent(list -> list.remove(processor));
    }

    public void handlePacket(ByteArrayDataInput dataInput) {
        TGVersion version = readVersion(dataInput);
        if (!TGVersions.CURRENT.equalsWithoutCommit(version)) {
            TotemGuard.getInstance().getLogger().warning(
                    "TotemGuard packet version mismatch: received version " + version +
                            " (expected " + TGVersions.CURRENT + "). " +
                            "Ensure all servers are running the latest version of TotemGuard."
            );
            return;
        }

        int packetId = dataInput.readInt();
        Packet<?> packet = packets.get(packetId);
        if (packet == null) throw new IllegalArgumentException("Invalid packet ID: " + packetId);

        List<PacketProcessor<?>> processorsForPacket = processors.get(packetId);
        if (processorsForPacket == null || processorsForPacket.isEmpty()) return;

        Object packetData = packet.read(dataInput);
        processorsForPacket.forEach(processor -> processor.handleAny(packetData));
    }

    /**
     * Reads the version from the input stream.
     *
     * @param input the data input stream
     * @return the version read from the stream
     */
    private TGVersion readVersion(ByteArrayDataInput input) {
        // Read major and minor versions (1 byte each)
        int major = input.readUnsignedByte();
        int minor = input.readUnsignedByte();

        // Read patch and snapshot from a single byte
        int patchAndSnapshot = input.readUnsignedByte();
        boolean snapshot = (patchAndSnapshot & 0x80) != 0; // Extract snapshot flag
        int patch = patchAndSnapshot & 0x7F; // Extract patch (0-127)

        return new TGVersion(major, minor, patch, snapshot);
    }
}

