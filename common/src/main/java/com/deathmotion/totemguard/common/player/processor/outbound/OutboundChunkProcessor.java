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
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;
import com.github.retrooper.packetevents.protocol.world.chunk.Column;
import com.github.retrooper.packetevents.protocol.world.dimension.DimensionType;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.*;

import java.util.*;

public class OutboundChunkProcessor extends ProcessorOutbound {

    private final Data data;
    private final ClientWorld clientWorld;
    private final PacketLatencyHandler latencyHandler;

    private String currentWorld;
    private DimensionType currentDimension;

    public OutboundChunkProcessor(TGPlayer player) {
        super(player);
        this.data = player.getData();
        this.clientWorld = data.getClientWorld();
        this.latencyHandler = player.getLatencyHandler();
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
        data.getMovementEstimator().onWorldReset();
    }

    private void loadColumn(PacketSendEvent event, int chunkX, int chunkZ, BaseChunk[] sections, boolean fullChunk) {
        Runnable apply = fullChunk
                ? () -> {
                    clientWorld.loadChunk(chunkX, chunkZ, sections);
                    data.getMovementEstimator().onWorldChunkLoaded();
                }
                : () -> {
                    clientWorld.mergeChunk(chunkX, chunkZ, sections);
                    data.getMovementEstimator().onWorldChunkLoaded();
                };
        latencyHandler.compensateLazy(event, apply);
    }

    private void unloadColumn(PacketSendEvent event, int chunkX, int chunkZ) {
        latencyHandler.compensateLazy(event, () -> clientWorld.unloadChunk(chunkX, chunkZ));
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
        if (blockId == effectiveId(x, y, z)) return;

        event.getTasksAfterSend().add(() -> {
            if (event.isCancelled()) return;
            clientWorld.setPendingBlock(x, y, z, blockId);
        });
        latencyHandler.compensateLazy(event, () -> {
            clientWorld.updateBlock(x, y, z, blockId);
            clientWorld.confirmPendingBlock(x, y, z, blockId);
            data.getMovementEstimator().onBlockChangeApplied(x, y, z, blockId);
        });
    }

    private void applyMultiBlock(PacketSendEvent event, WrapperPlayServerMultiBlockChange packet) {
        List<WrapperPlayServerMultiBlockChange.EncodedBlock> changed = null;
        for (WrapperPlayServerMultiBlockChange.EncodedBlock block : packet.getBlocks()) {
            if (block.getBlockId() == effectiveId(block.getX(), block.getY(), block.getZ())) continue;
            if (changed == null) changed = new ArrayList<>();
            changed.add(block);
        }
        if (changed == null) return;

        List<WrapperPlayServerMultiBlockChange.EncodedBlock> blocks = changed;
        event.getTasksAfterSend().add(() -> {
            if (event.isCancelled()) return;
            for (WrapperPlayServerMultiBlockChange.EncodedBlock block : blocks) {
                clientWorld.setPendingBlock(block.getX(), block.getY(), block.getZ(), block.getBlockId());
            }
        });
        latencyHandler.compensateLazy(event, () -> {
            for (WrapperPlayServerMultiBlockChange.EncodedBlock block : blocks) {
                clientWorld.updateBlock(block.getX(), block.getY(), block.getZ(), block.getBlockId());
                clientWorld.confirmPendingBlock(block.getX(), block.getY(), block.getZ(), block.getBlockId());
                data.getMovementEstimator().onBlockChangeApplied(block.getX(), block.getY(), block.getZ(), block.getBlockId());
            }
        });
    }

    private int effectiveId(int x, int y, int z) {
        Integer pending = clientWorld.pendingBlockId(x, y, z);
        return pending != null ? pending : clientWorld.getBlockId(x, y, z);
    }
}
