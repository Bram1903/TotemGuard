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

package com.deathmotion.totemguard.common.player.processor.outbound;

import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.data.ClientWorld;
import com.deathmotion.totemguard.common.player.data.Data;
import com.deathmotion.totemguard.common.player.latency.PacketLatencyHandler;
import com.deathmotion.totemguard.common.player.processor.ProcessorOutbound;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;
import com.github.retrooper.packetevents.protocol.world.chunk.Column;
import com.github.retrooper.packetevents.protocol.world.dimension.DimensionType;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.*;

import java.util.*;

public class OutboundChunkProcessor extends ProcessorOutbound {

    private static final int NEAR_BLOCKS = 16;

    private final Data data;
    private final ClientWorld clientWorld;
    private final PacketLatencyHandler latencyHandler;
    private final Map<Long, Integer> pendingBlocks = new HashMap<>();

    private String currentWorld;
    private DimensionType currentDimension;

    public OutboundChunkProcessor(TGPlayer player) {
        super(player);
        this.data = player.getData();
        this.clientWorld = data.getClientWorld();
        this.latencyHandler = player.getLatencyHandler();
    }

    private static long blockKey(int x, int y, int z) {
        return ((x & 0x3FFFFFFL) << 38) | ((z & 0x3FFFFFFL) << 12) | (y & 0xFFFL);
    }

    @Override
    public void handleOutbound(PacketSendEvent event) {
        if (event.isCancelled()) return;
        final PacketTypeCommon type = event.getPacketType();

        if (type == PacketType.Play.Server.CHUNK_DATA) {
            Column column = new WrapperPlayServerChunkData(event).getColumn();
            loadColumn(event, column.getX(), column.getZ(), column.getChunks(), column.isFullChunk());
        } else if (type == PacketType.Play.Server.MAP_CHUNK_BULK) {
            WrapperPlayServerChunkDataBulk packet = new WrapperPlayServerChunkDataBulk(event);
            BaseChunk[][] columns = packet.getChunks();
            int[] xs = packet.getX();
            int[] zs = packet.getZ();
            for (int i = 0; i < columns.length; i++) {
                loadColumn(event, xs[i], zs[i], columns[i], true);
            }
        } else if (type == PacketType.Play.Server.UNLOAD_CHUNK) {
            WrapperPlayServerUnloadChunk packet = new WrapperPlayServerUnloadChunk(event);
            unloadColumn(event, packet.getChunkX(), packet.getChunkZ());
        } else if (type == PacketType.Play.Server.BLOCK_CHANGE) {
            WrapperPlayServerBlockChange packet = new WrapperPlayServerBlockChange(event);
            Vector3i pos = packet.getBlockPosition();
            applyBlock(event, pos.getX(), pos.getY(), pos.getZ(), packet.getBlockId());
        } else if (type == PacketType.Play.Server.MULTI_BLOCK_CHANGE) {
            applyMultiBlock(event, new WrapperPlayServerMultiBlockChange(event));
        } else if (type == PacketType.Play.Server.JOIN_GAME) {
            WrapperPlayServerJoinGame packet = new WrapperPlayServerJoinGame(event);
            currentWorld = packet.getWorldName();
            currentDimension = packet.getDimensionType();
            applyDimension(currentDimension);
            resetWorld();
        } else if (type == PacketType.Play.Server.RESPAWN) {
            WrapperPlayServerRespawn packet = new WrapperPlayServerRespawn(event);
            if (isWorldChange(packet)) resetWorld();
            currentWorld = packet.getWorldName().orElse(null);
            currentDimension = packet.getDimensionType();
            applyDimension(currentDimension);
        } else if (type == PacketType.Play.Server.CONFIGURATION_START) {
            resetWorld();
        }
    }

    private void resetWorld() {
        clientWorld.clear();
        pendingBlocks.clear();
    }

    private void loadColumn(PacketSendEvent event, int chunkX, int chunkZ, BaseChunk[] sections, boolean fullChunk) {
        Runnable apply = fullChunk
                ? () -> clientWorld.loadChunk(chunkX, chunkZ, sections)
                : () -> clientWorld.mergeChunk(chunkX, chunkZ, sections);
        if (isChunkNear(chunkX, chunkZ)) {
            latencyHandler.compensate(event, apply);
        } else {
            apply.run();
        }
    }

    private void unloadColumn(PacketSendEvent event, int chunkX, int chunkZ) {
        if (isChunkNear(chunkX, chunkZ)) {
            latencyHandler.compensate(event, () -> clientWorld.unloadChunk(chunkX, chunkZ));
        } else {
            clientWorld.unloadChunk(chunkX, chunkZ);
        }
    }

    private boolean isWorldChange(WrapperPlayServerRespawn respawn) {
        if (respawn.getWorldName().isPresent()) {
            return !Objects.equals(respawn.getWorldName().orElse(null), currentWorld);
        }
        return !Objects.equals(respawn.getDimensionType(), currentDimension);
    }

    private void applyDimension(DimensionType dimension) {
        if (dimension != null) clientWorld.setDimension(dimension.getMinY());
    }

    private void applyBlock(PacketSendEvent event, int x, int y, int z, int blockId) {
        long key = blockKey(x, y, z);
        if (blockId == effectiveId(x, y, z, key)) return;

        if (isNear(x, y, z)) {
            pendingBlocks.put(key, blockId);
            latencyHandler.compensate(event, () -> {
                clientWorld.updateBlock(x, y, z, blockId);
                clearPending(key, blockId);
            });
        } else {
            clientWorld.updateBlock(x, y, z, blockId);
        }
    }

    private void applyMultiBlock(PacketSendEvent event, WrapperPlayServerMultiBlockChange packet) {
        List<WrapperPlayServerMultiBlockChange.EncodedBlock> changed = null;
        for (WrapperPlayServerMultiBlockChange.EncodedBlock block : packet.getBlocks()) {
            long key = blockKey(block.getX(), block.getY(), block.getZ());
            if (block.getBlockId() == effectiveId(block.getX(), block.getY(), block.getZ(), key)) continue;
            if (changed == null) changed = new ArrayList<>();
            changed.add(block);
        }
        if (changed == null) return;

        List<WrapperPlayServerMultiBlockChange.EncodedBlock> blocks = changed;
        Vector3i section = packet.getChunkPosition();
        if (isSectionNear(section.getX(), section.getY(), section.getZ())) {
            for (WrapperPlayServerMultiBlockChange.EncodedBlock block : blocks) {
                pendingBlocks.put(blockKey(block.getX(), block.getY(), block.getZ()), block.getBlockId());
            }
            latencyHandler.compensate(event, () -> {
                for (WrapperPlayServerMultiBlockChange.EncodedBlock block : blocks) {
                    clientWorld.updateBlock(block.getX(), block.getY(), block.getZ(), block.getBlockId());
                    clearPending(blockKey(block.getX(), block.getY(), block.getZ()), block.getBlockId());
                }
            });
        } else {
            for (WrapperPlayServerMultiBlockChange.EncodedBlock block : blocks) {
                clientWorld.updateBlock(block.getX(), block.getY(), block.getZ(), block.getBlockId());
            }
        }
    }

    private int effectiveId(int x, int y, int z, long key) {
        Integer pending = pendingBlocks.get(key);
        return pending != null ? pending : clientWorld.getBlockId(x, y, z);
    }

    private void clearPending(long key, int blockId) {
        Integer latest = pendingBlocks.get(key);
        if (latest != null && latest == blockId) pendingBlocks.remove(key);
    }

    private boolean isChunkNear(int chunkX, int chunkZ) {
        Location current = data.getMovementData().getCurrent();
        double centerX = (chunkX << 4) + 8;
        double centerZ = (chunkZ << 4) + 8;
        return Math.abs(centerX - current.getX()) < NEAR_BLOCKS
                && Math.abs(centerZ - current.getZ()) < NEAR_BLOCKS;
    }

    private boolean isNear(int x, int y, int z) {
        Location current = data.getMovementData().getCurrent();
        return Math.abs(x - current.getX()) <= NEAR_BLOCKS
                && Math.abs(y - current.getY()) <= NEAR_BLOCKS
                && Math.abs(z - current.getZ()) <= NEAR_BLOCKS;
    }

    private boolean isSectionNear(int sectionX, int sectionY, int sectionZ) {
        Location current = data.getMovementData().getCurrent();
        double cx = (sectionX << 4) + 8;
        double cy = (sectionY << 4) + 8;
        double cz = (sectionZ << 4) + 8;
        return Math.abs(cx - current.getX()) <= NEAR_BLOCKS + 8
                && Math.abs(cy - current.getY()) <= NEAR_BLOCKS + 8
                && Math.abs(cz - current.getZ()) <= NEAR_BLOCKS + 8;
    }
}
