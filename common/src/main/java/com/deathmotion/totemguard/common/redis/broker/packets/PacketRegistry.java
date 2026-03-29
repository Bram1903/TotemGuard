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

import com.deathmotion.totemguard.common.TGPlatform;
import com.google.common.io.ByteArrayDataInput;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class PacketRegistry {

    private final Logger logger = TGPlatform.getInstance().getLogger();
    private final ConcurrentMap<Integer, Packet<?>> packets = new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer, List<PacketProcessor<?>>> processors = new ConcurrentHashMap<>();

    public PacketRegistry() {
        for (Packets packet : Packets.values()) {
            register(packet.packet());
        }
    }

    public <T> void registerProcessor(Packet<T> packet, PacketProcessor<T> processor) {
        if (!packets.containsKey(packet.getId())) {
            throw new IllegalArgumentException("Unknown Redis packet ID: " + packet.getId());
        }

        processors.computeIfAbsent(packet.getId(), ignored -> new CopyOnWriteArrayList<>()).add(processor);
    }

    public void unregister(Packet<?> packet, PacketProcessor<?> processor) {
        List<PacketProcessor<?>> packetProcessors = processors.get(packet.getId());
        if (packetProcessors == null) {
            return;
        }

        packetProcessors.remove(processor);
        if (packetProcessors.isEmpty()) {
            processors.remove(packet.getId(), packetProcessors);
        }
    }

    public void handlePacket(int packetId, ByteArrayDataInput dataInput) {
        Packet<?> packet = packets.get(packetId);
        if (packet == null) {
            logger.warning("Received unknown Redis packet ID " + packetId + ".");
            return;
        }

        List<PacketProcessor<?>> processorsForPacket = processors.get(packetId);
        if (processorsForPacket == null || processorsForPacket.isEmpty()) {
            return;
        }

        Object packetData;
        try {
            packetData = packet.read(dataInput);
        } catch (Exception exception) {
            logger.log(Level.WARNING, "Failed to decode Redis packet " + packetName(packet) + ".", exception);
            return;
        }

        for (PacketProcessor<?> processor : processorsForPacket) {
            try {
                processor.handleAny(packetData);
            } catch (Exception exception) {
                logger.log(Level.WARNING, "Failed to process Redis packet " + packetName(packet) + ".", exception);
            }
        }
    }

    private void register(Packet<?> packet) {
        Packet<?> previous = packets.putIfAbsent(packet.getId(), packet);
        if (previous != null) {
            throw new IllegalStateException("Duplicate Redis packet ID " + packet.getId() + ".");
        }
    }

    private String packetName(Packet<?> packet) {
        String simpleName = packet.getClass().getSimpleName();
        return simpleName.isBlank() ? ("#" + packet.getId()) : simpleName;
    }
}
