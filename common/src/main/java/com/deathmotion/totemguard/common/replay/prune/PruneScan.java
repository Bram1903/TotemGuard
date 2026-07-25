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

package com.deathmotion.totemguard.common.replay.prune;

import com.deathmotion.totemguard.common.physics.trace.TraceFrame;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.replay.format.RecordingFrame;
import com.deathmotion.totemguard.common.replay.playback.ReplayObserver;
import com.deathmotion.totemguard.common.world.entity.EntityRoles;
import com.deathmotion.totemguard.common.world.entity.TrackedEntity;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.PacketSide;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.world.Location;

import java.util.*;

public final class PruneScan implements ReplayObserver {

    private static final double ENTITY_RADIUS = 16.0;
    private static final int COLUMN_RADIUS = 2;

    private final Map<Long, Long> candidateColumns = new HashMap<>();
    private final Set<Long> occupied = new HashSet<>();
    private final Map<Integer, EntityWatch> watched = new HashMap<>();

    private static long column(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    private static int readInt(byte[] payload, int offset) {
        return ((payload[offset] & 0xFF) << 24)
                | ((payload[offset + 1] & 0xFF) << 16)
                | ((payload[offset + 2] & 0xFF) << 8)
                | (payload[offset + 3] & 0xFF);
    }

    private static int entityId(byte[] payload) {
        int value = 0;
        int shift = 0;
        for (int index = 0; index < payload.length && shift <= 28; index++) {
            int b = payload[index] & 0xFF;
            value |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) return value;
            shift += 7;
        }
        return -1;
    }

    @Override
    public void onPacketFrame(long frameIndex, RecordingFrame.Packet frame, TGPlayer player) {
        if (frame.inbound() || frame.payload().length < 1) return;
        PacketTypeCommon type = PacketType.getById(PacketSide.SERVER, frame.state(),
                PacketEvents.getAPI().getServerManager().getVersion().toClientVersion(), frame.packetId());
        if (type == PacketType.Play.Server.CHUNK_DATA) {
            if (frame.payload().length < 8 || !player.getWorldMirror().readiness().ready()) return;
            candidateColumns.put(frameIndex, column(readInt(frame.payload(), 0), readInt(frame.payload(), 4)));
            return;
        }
        boolean movement = type == PacketType.Play.Server.ENTITY_RELATIVE_MOVE
                || type == PacketType.Play.Server.ENTITY_RELATIVE_MOVE_AND_ROTATION
                || type == PacketType.Play.Server.ENTITY_POSITION_SYNC
                || type == PacketType.Play.Server.ENTITY_TELEPORT;
        boolean spawn = type == PacketType.Play.Server.SPAWN_ENTITY
                || type == PacketType.Play.Server.SPAWN_LIVING_ENTITY;
        if (!movement && !spawn) return;

        int entityId = entityId(frame.payload());
        if (entityId < 0) return;
        EntityWatch watch = watched.computeIfAbsent(entityId, id -> new EntityWatch());
        if (movement) watch.frames.add(frameIndex);
    }

    @Override
    public void onTick(TGPlayer player, TraceFrame frame) {
        Location current = player.getData().getMovementData().getCurrent();
        occupied.add(column((int) Math.floor(current.getX()) >> 4, (int) Math.floor(current.getZ()) >> 4));

        for (Map.Entry<Integer, EntityWatch> entry : watched.entrySet()) {
            EntityWatch watch = entry.getValue();
            EntityType type = player.getWorldMirror().entities().announcedType(entry.getKey());
            if (type != null) watch.pushable = EntityRoles.pushable(type);
            TrackedEntity entity = player.getWorldMirror().entities().resolve(entry.getKey());
            if (entity == null || !entity.positioned()) continue;
            double dx = entity.renderX() - current.getX();
            double dy = entity.renderY() - current.getY();
            double dz = entity.renderZ() - current.getZ();
            watch.sampled = true;
            watch.nearestSquared = Math.min(watch.nearestSquared, dx * dx + dy * dy + dz * dz);
        }
    }

    public Set<Long> droppableFrames() {
        Set<Long> kept = new HashSet<>();
        for (long center : occupied) {
            int x = (int) (center >> 32);
            int z = (int) center;
            for (int dx = -COLUMN_RADIUS; dx <= COLUMN_RADIUS; dx++) {
                for (int dz = -COLUMN_RADIUS; dz <= COLUMN_RADIUS; dz++) {
                    kept.add(column(x + dx, z + dz));
                }
            }
        }

        Set<Long> droppable = new HashSet<>();
        for (Map.Entry<Long, Long> candidate : candidateColumns.entrySet()) {
            if (!kept.contains(candidate.getValue())) droppable.add(candidate.getKey());
        }
        for (EntityWatch watch : watched.values()) {
            if (watch.prunable()) droppable.addAll(watch.frames);
        }
        return droppable;
    }

    private static final class EntityWatch {

        private final List<Long> frames = new ArrayList<>();
        private double nearestSquared = Double.MAX_VALUE;
        private boolean pushable;
        private boolean sampled;

        private boolean prunable() {
            return pushable && sampled && nearestSquared > ENTITY_RADIUS * ENTITY_RADIUS;
        }
    }
}
