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

import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.replay.capture.ChunkPayloadCompactor;
import com.deathmotion.totemguard.common.replay.capture.PayloadReader;
import com.deathmotion.totemguard.common.replay.capture.RecordingSession;
import com.deathmotion.totemguard.common.replay.format.RecordingFrame;
import com.deathmotion.totemguard.common.world.block.BlockStore;
import com.deathmotion.totemguard.common.world.entity.TrackedEntity;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import com.github.retrooper.packetevents.netty.buffer.UnpooledByteBufAllocationHelper;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;
import com.github.retrooper.packetevents.protocol.world.chunk.Column;
import com.github.retrooper.packetevents.protocol.world.chunk.TileEntity;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class RetentionBuffer implements BlockStore.Watcher {

    private static final TileEntity[] NO_TILE_ENTITIES = new TileEntity[0];

    private static final Set<PacketTypeCommon> ENTITY_MOVEMENT = Set.of(
            PacketType.Play.Server.ENTITY_RELATIVE_MOVE,
            PacketType.Play.Server.ENTITY_RELATIVE_MOVE_AND_ROTATION,
            PacketType.Play.Server.ENTITY_TELEPORT,
            PacketType.Play.Server.ENTITY_POSITION_SYNC,
            PacketType.Play.Server.ENTITY_VELOCITY
    );

    private final TGPlayer player;
    private final StickyPackets sticky = new StickyPackets();
    private final ServerVersion server;
    private final Set<Long> visited = ConcurrentHashMap.newKeySet();
    private final Set<Integer> nearby = ConcurrentHashMap.newKeySet();
    private final Set<Long> emitted = ConcurrentHashMap.newKeySet();
    private final Object lock = new Object();
    private long lastColumn = Long.MIN_VALUE;
    private volatile Segment previous;
    private volatile Segment current;
    private volatile State state = State.IDLE;
    private volatile @Nullable RecordingSession session;
    private volatile long dropped;

    public RetentionBuffer(TGPlayer player, ServerVersion server) {
        this.player = player;
        this.server = server;
        this.current = new Segment(RetentionKeyframe.open(player, sticky, player.getClock().nanos()));
        player.getWorldMirror().blocks().watcher(this);
    }

    private static long columnKey(byte[] payload) {
        if (payload.length < 8) return Long.MIN_VALUE;
        int x = ((payload[0] & 0xFF) << 24) | ((payload[1] & 0xFF) << 16)
                | ((payload[2] & 0xFF) << 8) | (payload[3] & 0xFF);
        int z = ((payload[4] & 0xFF) << 24) | ((payload[5] & 0xFF) << 16)
                | ((payload[6] & 0xFF) << 8) | (payload[7] & 0xFF);
        return BlockStore.chunkKey(x, z);
    }

    public StickyPackets sticky() {
        return sticky;
    }

    public boolean live() {
        return state == State.LIVE;
    }

    public long droppedFrames() {
        return dropped;
    }

    public long retainedNanos(long now) {
        Segment oldest = previous != null ? previous : current;
        return now - oldest.keyframe.openedNanos();
    }

    public int retainedFrames() {
        return (previous == null ? 0 : previous.frames.size()) + current.frames.size();
    }

    public void detach() {
        player.getWorldMirror().blocks().watcher(null);
    }

    public void onPacket(boolean inbound, boolean cancelled, PacketTypeCommon type,
                         ConnectionState connectionState, int packetId, byte[] payload) {
        if (inbound) {
            sticky.observeInbound(type, packetId, payload);
        } else {
            sticky.observe(type, packetId, payload);
        }

        long nanos = player.getClock().nanos();
        RecordingSession open = this.session;
        if (open != null) {
            if (inbound) topUp(open, nanos);
            open.offer(new RecordingFrame.Packet(inbound, cancelled, connectionState, packetId,
                    nanos, payload));
            return;
        }

        if (!inbound && type == PacketType.Play.Server.CHUNK_DATA) {
            long key = columnKey(payload);
            if (key != Long.MIN_VALUE) {
                offer(new Retained.Chunk(nanos, packetId, key));
                return;
            }
        }
        if (!inbound && ENTITY_MOVEMENT.contains(type) && !keepEntity(payload, nanos, packetId)) {
            return;
        }
        offer(new Retained.Direct(new RecordingFrame.Packet(inbound, cancelled, connectionState,
                packetId, nanos, payload)));
    }

    public void onServerTick(long tick) {
        long nanos = player.getClock().nanos();
        RecordingSession open = this.session;
        if (open != null) {
            open.offer(new RecordingFrame.ServerTick(nanos, tick));
            return;
        }
        markVisited();
        offer(new Retained.Direct(new RecordingFrame.ServerTick(nanos, tick)));
        rotateIfDue(nanos);
    }

    public void onEventLoop() {
        long nanos = player.getClock().nanos();
        RecordingSession open = this.session;
        if (open != null) {
            open.offer(new RecordingFrame.EventLoop(nanos));
            return;
        }
        offer(new Retained.Direct(new RecordingFrame.EventLoop(nanos)));
    }

    public void onFrame(RecordingFrame frame) {
        RecordingSession open = this.session;
        if (open != null) {
            open.offer(frame);
            return;
        }
        offer(new Retained.Direct(frame));
    }

    private void topUp(RecordingSession target, long nanos) {
        var location = player.getData().getMovementData().getCurrent();
        if (location == null) return;
        int chunkX = (int) Math.floor(location.getX()) >> 4;
        int chunkZ = (int) Math.floor(location.getZ()) >> 4;
        long column = BlockStore.chunkKey(chunkX, chunkZ);
        if (column == lastColumn) return;
        lastColumn = column;

        int chunkId = PacketType.Play.Server.CHUNK_DATA.getId(server.toClientVersion());
        for (int dx = -RetentionPolicy.COLUMN_RADIUS; dx <= RetentionPolicy.COLUMN_RADIUS; dx++) {
            for (int dz = -RetentionPolicy.COLUMN_RADIUS; dz <= RetentionPolicy.COLUMN_RADIUS; dz++) {
                long key = BlockStore.chunkKey(chunkX + dx, chunkZ + dz);
                if (!emitted.add(key)) continue;
                BaseChunk[] sections = player.getWorldMirror().blocks().sections(key);
                if (sections == null) continue;
                byte[] payload = ChunkPayloadCompactor.encode(new Column(chunkX + dx, chunkZ + dz,
                        true, sections, NO_TILE_ENTITIES), server);
                if (payload == null) continue;
                target.offer(new RecordingFrame.Packet(false, false, ConnectionState.PLAY, chunkId,
                        nanos, payload));
            }
        }
    }

    private void markVisited() {
        var location = player.getData().getMovementData().getCurrent();
        if (location == null) return;
        visited.add(BlockStore.chunkKey((int) Math.floor(location.getX()) >> 4,
                (int) Math.floor(location.getZ()) >> 4));
    }

    private boolean keepEntity(byte[] payload, long nanos, int packetId) {
        int entityId;
        try {
            entityId = new PayloadReader(payload).readVarInt();
        } catch (RuntimeException malformed) {
            return true;
        }
        if (entityId == player.getUser().getEntityId() || entityId == player.getData().getVehicleId()) {
            return true;
        }
        TrackedEntity entity = player.getWorldMirror().entities().resolve(entityId);
        if (entity == null || !entity.positioned()) return true;

        var location = player.getData().getMovementData().getCurrent();
        if (location == null) return true;
        double dx = entity.targetX() - location.getX();
        double dy = entity.targetY() - location.getY();
        double dz = entity.targetZ() - location.getZ();
        boolean near = dx * dx + dy * dy + dz * dz
                <= RetentionPolicy.ENTITY_RADIUS * RetentionPolicy.ENTITY_RADIUS;

        if (!near) {
            nearby.remove(entityId);
            return false;
        }
        if (nearby.add(entityId)) {
            resync(entityId, entity, nanos);
        }
        return true;
    }

    private void resync(int entityId, TrackedEntity entity, long nanos) {
        byte[] payload = encodeTeleport(entityId, entity);
        if (payload == null) return;
        offer(new Retained.Direct(new RecordingFrame.Packet(false, false, ConnectionState.PLAY,
                PacketType.Play.Server.ENTITY_TELEPORT.getId(server.toClientVersion()), nanos, payload)));
    }

    private byte @Nullable [] encodeTeleport(int entityId, TrackedEntity entity) {
        Object buffer = UnpooledByteBufAllocationHelper.buffer();
        try {
            WrapperPlayServerEntityTeleport wrapper = new WrapperPlayServerEntityTeleport(entityId,
                    new Vector3d(entity.targetX(), entity.targetY(), entity.targetZ()), 0f, 0f, false);
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

    private void offer(Retained frame) {
        synchronized (lock) {
            if (current.frames.size() >= RetentionPolicy.MAX_SEGMENT_FRAMES) {
                dropped++;
                return;
            }
            current.frames.add(frame);
        }
    }

    private void rotateIfDue(long nanos) {
        synchronized (lock) {
            if (state != State.IDLE) return;
            if (nanos - current.keyframe.openedNanos() < RetentionPolicy.SEGMENT_NANOS) return;
            sticky.prune(id -> player.getWorldMirror().entities().resolve(id) != null);
            nearby.removeIf(id -> player.getWorldMirror().entities().resolve(id) == null);
            previous = current;
            current = new Segment(RetentionKeyframe.open(player, sticky, nanos));
            trimVisited();
        }
    }

    private void trimVisited() {
        if (visited.size() < 4096) return;
        visited.clear();
        markVisited();
    }

    @Override
    public void beforeSet(int x, int y, int z, int previousStateId) {
        if (state == State.LIVE) return;
        Segment older = previous;
        if (older != null) older.keyframe.journal().onBlockSet(x, y, z, previousStateId);
        current.keyframe.journal().onBlockSet(x, y, z, previousStateId);
    }

    @Override
    public void beforeColumn(long key, BaseChunk @Nullable [] value, boolean sharesSections) {
        if (state == State.LIVE) return;
        Segment older = previous;
        if (older != null) older.keyframe.journal().onColumn(key, value, sharesSections);
        current.keyframe.journal().onColumn(key, value, sharesSections);
    }

    @Override
    public void beforeClear() {
        synchronized (lock) {
            if (state != State.IDLE) return;
            previous = null;
            current = new Segment(RetentionKeyframe.open(player, sticky, player.getClock().nanos()));
            visited.clear();
            nearby.clear();
        }
    }

    public @Nullable Claim claim() {
        synchronized (lock) {
            if (state != State.IDLE) return null;
            Segment oldest = previous != null ? previous : current;
            if (!oldest.keyframe.journal().usable()) {
                if (previous == null || !current.keyframe.journal().usable()) return null;
                oldest = current;
                previous = null;
            }
            state = State.DUMPING;
            return new Claim(oldest.keyframe, Set.copyOf(visited), oldest == current);
        }
    }

    public void promote(RecordingSession target, PrologueBuilder.Result prologue, boolean fromCurrentOnly) {
        synchronized (lock) {
            emitted.clear();
            emitted.addAll(prologue.columns());
            lastColumn = Long.MIN_VALUE;
            target.offer(new RecordingFrame.Attach(target.getStartNanos()));
            for (RecordingFrame frame : prologue.frames()) target.offer(frame);
            if (!fromCurrentOnly && previous != null) drain(previous, target);
            drain(current, target);
            previous = null;
            current = new Segment(RetentionKeyframe.open(player, sticky, player.getClock().nanos()));
            this.session = target;
            state = State.LIVE;
        }
    }

    public void abandon() {
        synchronized (lock) {
            state = State.IDLE;
        }
    }

    public void demote() {
        synchronized (lock) {
            this.session = null;
            previous = null;
            current = new Segment(RetentionKeyframe.open(player, sticky, player.getClock().nanos()));
            visited.clear();
            nearby.clear();
            emitted.clear();
            lastColumn = Long.MIN_VALUE;
            state = State.IDLE;
        }
    }

    private void drain(Segment segment, RecordingSession target) {
        for (Retained frame : segment.frames) {
            RecordingFrame resolved = resolve(frame);
            if (resolved != null) target.offer(resolved);
        }
        segment.frames.clear();
    }

    private @Nullable RecordingFrame resolve(Retained frame) {
        if (frame instanceof Retained.Direct direct) return direct.frame();
        Retained.Chunk chunk = (Retained.Chunk) frame;
        BaseChunk[] sections = player.getWorldMirror().blocks().sections(chunk.columnKey());
        if (sections == null) return null;
        emitted.add(chunk.columnKey());
        byte[] payload = ChunkPayloadCompactor.encode(new Column(
                BlockStore.chunkKeyX(chunk.columnKey()), BlockStore.chunkKeyZ(chunk.columnKey()),
                true, sections, NO_TILE_ENTITIES), server);
        if (payload == null) return null;
        return new RecordingFrame.Packet(false, false, ConnectionState.PLAY, chunk.packetId(),
                chunk.nanos(), payload);
    }

    private enum State {

        IDLE,
        DUMPING,
        LIVE
    }

    private sealed interface Retained {

        record Direct(RecordingFrame frame) implements Retained {
        }

        record Chunk(long nanos, int packetId, long columnKey) implements Retained {
        }
    }

    public record Claim(RetentionKeyframe keyframe, Set<Long> visited, boolean fromCurrentOnly) {
    }

    private static final class Segment {

        private final RetentionKeyframe keyframe;
        private final List<Retained> frames = new ArrayList<>();

        private Segment(RetentionKeyframe keyframe) {
            this.keyframe = keyframe;
        }
    }
}
