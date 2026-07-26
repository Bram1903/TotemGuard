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

package com.deathmotion.totemguard.common.replay.retention;

import com.deathmotion.totemguard.common.replay.capture.ChunkPayloadCompactor;
import com.deathmotion.totemguard.common.replay.format.RecordingFrame;
import com.deathmotion.totemguard.common.replay.format.WorldDigest;
import com.deathmotion.totemguard.common.world.WorldMirror;
import com.deathmotion.totemguard.common.world.block.BlockStore;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import com.github.retrooper.packetevents.netty.buffer.UnpooledByteBufAllocationHelper;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;
import com.github.retrooper.packetevents.protocol.world.chunk.Column;
import com.github.retrooper.packetevents.protocol.world.chunk.TileEntity;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPositionAndRotation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class PrologueBuilder {

    private static final TileEntity[] NO_TILE_ENTITIES = new TileEntity[0];

    private PrologueBuilder() {
    }

    public static Result build(RetentionKeyframe keyframe, BlockJournal.Rewind rewind,
                               WorldMirror mirror, Set<Long> visited, ServerVersion server, long nanos,
                               double fallbackX, double fallbackY, double fallbackZ) {
        RetentionKeyframe anchored = keyframe.positioned() ? keyframe : new RetentionKeyframe(
                keyframe.openedNanos(), keyframe.sticky(), keyframe.entities(), keyframe.checks(),
                keyframe.journal(), fallbackX, fallbackY, fallbackZ, true,
                keyframe.previousLocation(), keyframe.onGround());
        return build(anchored, rewind, mirror, visited, server, nanos);
    }

    private static Result build(RetentionKeyframe keyframe, BlockJournal.Rewind rewind,
                                WorldMirror mirror, Set<Long> visited, ServerVersion server, long nanos) {
        ClientVersion wire = server.toClientVersion();
        List<RecordingFrame> frames = new ArrayList<>();
        int spawnFailures = 0;
        int chunkFailures = 0;

        for (StickyPackets.Entry entry : keyframe.sticky()) {
            if (entry.order() >= StickyPackets.ORDER_ENTITY_ATTRIBUTES) continue;
            frames.add(sticky(entry, nanos));
        }

        int spawnId = PacketType.Play.Server.SPAWN_ENTITY.getId(wire);
        for (RetentionKeyframe.Entity entity : keyframe.entities()) {
            byte[] payload = encode(new WrapperPlayServerSpawnEntity(entity.entityId(),
                    Optional.ofNullable(entity.uuid()), entity.type(),
                    new Vector3d(entity.x(), entity.y(), entity.z()),
                    0f, 0f, 0f, 0, Optional.empty()), server);
            if (payload == null) {
                spawnFailures++;
                continue;
            }
            frames.add(outbound(spawnId, payload, nanos));
        }

        for (StickyPackets.Entry entry : keyframe.sticky()) {
            if (entry.order() < StickyPackets.ORDER_ENTITY_ATTRIBUTES) continue;
            frames.add(sticky(entry, nanos));
        }

        Set<Long> columns = selectColumns(rewind, visited, keyframe);
        int chunkId = PacketType.Play.Server.CHUNK_DATA.getId(wire);
        for (long key : columns) {
            BaseChunk[] sections = rewind.columns().get(key);
            if (sections == null) continue;
            byte[] payload = ChunkPayloadCompactor.encode(
                    new Column(BlockStore.chunkKeyX(key), BlockStore.chunkKeyZ(key), true, sections,
                            NO_TILE_ENTITIES),
                    server);
            if (payload == null) {
                chunkFailures++;
                continue;
            }
            frames.add(outbound(chunkId, payload, nanos));
        }

        int blockChangeId = PacketType.Play.Server.BLOCK_CHANGE.getId(wire);
        for (Map.Entry<Long, Integer> undo : rewind.blockUndo().entrySet()) {
            int x = BlockJournal.unpackX(undo.getKey());
            int y = BlockJournal.unpackY(undo.getKey());
            int z = BlockJournal.unpackZ(undo.getKey());
            if (!columns.contains(BlockStore.chunkKey(x >> 4, z >> 4))) continue;
            byte[] payload = encode(
                    new WrapperPlayServerBlockChange(new Vector3i(x, y, z), undo.getValue()), server);
            if (payload == null) continue;
            frames.add(outbound(blockChangeId, payload, nanos));
        }

        seedMovement(keyframe, wire, server, frames, nanos);

        frames.add(new RecordingFrame.PrologueEnd(nanos, digest(keyframe, rewind, mirror, columns)));
        return new Result(frames, columns, spawnFailures, chunkFailures);
    }

    private static void seedMovement(RetentionKeyframe keyframe, ClientVersion wire,
                                     ServerVersion server, List<RecordingFrame> frames, long nanos) {
        Location previous = keyframe.previousLocation();
        if (previous == null) return;
        byte[] payload = encode(new WrapperPlayClientPlayerPositionAndRotation(
                new Vector3d(previous.getX(), previous.getY(), previous.getZ()),
                previous.getYaw(), previous.getPitch(), keyframe.onGround()), server);
        if (payload == null) return;
        frames.add(new RecordingFrame.Packet(true, false, ConnectionState.PLAY,
                PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION.getId(wire), nanos, payload));
    }

    private static WorldDigest digest(RetentionKeyframe keyframe, BlockJournal.Rewind rewind,
                                      WorldMirror mirror, Set<Long> columns) {
        int centerChunkX = (int) Math.floor(keyframe.playerX()) >> 4;
        int centerChunkZ = (int) Math.floor(keyframe.playerZ()) >> 4;
        int radius = sampleRadius(columns, centerChunkX, centerChunkZ);
        if (radius < 0) return WorldDigest.ABSENT;

        int baseY = (int) Math.floor(keyframe.playerY()) - RetentionPolicy.DIGEST_HEIGHT / 2;
        WorldDigest sampled = WorldDigest.sample(mirror, centerChunkX, centerChunkZ, radius, baseY,
                RetentionPolicy.DIGEST_HEIGHT, rewind::stateId, rewind::isLoaded);
        return new WorldDigest(sampled.centerChunkX(), sampled.centerChunkZ(), sampled.radius(),
                sampled.baseY(), sampled.height(), sampled.columnsLoaded(), sampled.blockHash(),
                -1, 0L, sampled.borderHash());
    }

    private static int sampleRadius(Set<Long> columns, int centerChunkX, int centerChunkZ) {
        for (int radius = RetentionPolicy.DIGEST_RADIUS; radius >= 0; radius--) {
            if (covers(columns, centerChunkX, centerChunkZ, radius)) return radius;
        }
        return -1;
    }

    private static boolean covers(Set<Long> columns, int centerChunkX, int centerChunkZ, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (!columns.contains(BlockStore.chunkKey(centerChunkX + dx, centerChunkZ + dz))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static Set<Long> selectColumns(BlockJournal.Rewind rewind, Set<Long> visited,
                                           RetentionKeyframe keyframe) {
        int homeX = (int) Math.floor(keyframe.playerX()) >> 4;
        int homeZ = (int) Math.floor(keyframe.playerZ()) >> 4;

        Set<Long> wanted = new HashSet<>();
        for (int dx = -RetentionPolicy.COLUMN_RADIUS; dx <= RetentionPolicy.COLUMN_RADIUS; dx++) {
            for (int dz = -RetentionPolicy.COLUMN_RADIUS; dz <= RetentionPolicy.COLUMN_RADIUS; dz++) {
                wanted.add(BlockStore.chunkKey(homeX + dx, homeZ + dz));
            }
        }
        for (long key : visited) {
            int x = BlockStore.chunkKeyX(key);
            int z = BlockStore.chunkKeyZ(key);
            for (int dx = -RetentionPolicy.VISITED_DILATION; dx <= RetentionPolicy.VISITED_DILATION; dx++) {
                for (int dz = -RetentionPolicy.VISITED_DILATION; dz <= RetentionPolicy.VISITED_DILATION; dz++) {
                    wanted.add(BlockStore.chunkKey(x + dx, z + dz));
                }
            }
        }
        wanted.retainAll(rewind.columns().keySet());

        if (wanted.size() <= RetentionPolicy.MAX_PROLOGUE_COLUMNS) return wanted;
        List<Long> ordered = new ArrayList<>(wanted);
        ordered.sort(Comparator.comparingLong(key -> distanceSquared(key, homeX, homeZ)));
        return new HashSet<>(ordered.subList(0, RetentionPolicy.MAX_PROLOGUE_COLUMNS));
    }

    private static long distanceSquared(long key, int homeX, int homeZ) {
        long dx = BlockStore.chunkKeyX(key) - (long) homeX;
        long dz = BlockStore.chunkKeyZ(key) - (long) homeZ;
        return dx * dx + dz * dz;
    }

    private static RecordingFrame.Packet outbound(int packetId, byte[] payload, long nanos) {
        return new RecordingFrame.Packet(false, false, ConnectionState.PLAY, packetId, nanos, payload);
    }

    private static RecordingFrame.Packet sticky(StickyPackets.Entry entry, long nanos) {
        return new RecordingFrame.Packet(entry.inbound(), false, ConnectionState.PLAY,
                entry.packetId(), nanos, entry.payload());
    }

    private static byte @Nullable [] encode(PacketWrapper<?> wrapper, ServerVersion server) {
        Object buffer = UnpooledByteBufAllocationHelper.buffer();
        try {
            wrapper.setServerVersion(server);
            wrapper.setClientVersion(server.toClientVersion());
            wrapper.setBuffer(buffer);
            wrapper.write();
            return ByteBufHelper.copyBytes(buffer);
        } catch (RuntimeException unsupported) {
            return null;
        } finally {
            ByteBufHelper.release(buffer);
        }
    }

    public record Result(List<RecordingFrame> frames, Set<Long> columns, int spawnFailures,
                         int chunkFailures) {
    }
}
