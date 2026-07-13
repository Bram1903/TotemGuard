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
import com.deathmotion.totemguard.common.player.latency.PacketLatencyHandler;
import com.deathmotion.totemguard.common.player.processor.ProcessorOutbound;
import com.deathmotion.totemguard.common.world.WorldMirror;
import com.deathmotion.totemguard.common.world.block.BlockStore;
import com.deathmotion.totemguard.common.world.block.PendingBlocks;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;
import com.github.retrooper.packetevents.protocol.world.chunk.Column;
import com.github.retrooper.packetevents.protocol.world.dimension.DimensionType;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.*;

import java.util.ArrayList;
import java.util.List;

public class OutboundWorldProcessor extends ProcessorOutbound {

    private final WorldMirror mirror;
    private final BlockStore blocks;
    private final PendingBlocks pending;
    private final PacketLatencyHandler latencyHandler;

    public OutboundWorldProcessor(TGPlayer player) {
        super(player);
        this.mirror = player.getWorldMirror();
        this.blocks = mirror.blocks();
        this.pending = mirror.pending();
        this.latencyHandler = player.getLatencyHandler();
    }

    private static int minY(DimensionType dimension) {
        return dimension != null ? dimension.getMinY() : 0;
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
            final int chunkX = packet.getChunkX();
            final int chunkZ = packet.getChunkZ();
            latencyHandler.compensateLazy(event, () -> blocks.unloadChunk(chunkX, chunkZ));
        } else if (type == PacketType.Play.Server.BLOCK_CHANGE) {
            WrapperPlayServerBlockChange packet = new WrapperPlayServerBlockChange(event);
            Vector3i pos = packet.getBlockPosition();
            applyBlock(event, pos.getX(), pos.getY(), pos.getZ(), packet.getBlockId());
        } else if (type == PacketType.Play.Server.MULTI_BLOCK_CHANGE) {
            applyMultiBlock(event, new WrapperPlayServerMultiBlockChange(event));
        } else if (type == PacketType.Play.Server.JOIN_GAME) {
            WrapperPlayServerJoinGame packet = new WrapperPlayServerJoinGame(event);
            mirror.onJoin(packet.getWorldName(), packet.getDimensionType(), minY(packet.getDimensionType()));
        } else if (type == PacketType.Play.Server.RESPAWN) {
            WrapperPlayServerRespawn packet = new WrapperPlayServerRespawn(event);
            final String worldName = packet.getWorldName().orElse(null);
            final DimensionType dimension = packet.getDimensionType();
            final int minY = minY(dimension);
            if (mirror.isWorldChange(packet)) {
                // The client empties its level when the respawn arrives, not when it is sent.
                latencyHandler.compensateLazy(event, () -> mirror.onWorldSwap(worldName, dimension, minY));
            } else {
                mirror.dimension().update(worldName, dimension, minY);
                blocks.minY(minY);
            }
        } else if (type == PacketType.Play.Server.CONFIGURATION_START) {
            mirror.onConfigurationStart();
        }
    }

    private void loadColumn(PacketSendEvent event, int chunkX, int chunkZ, BaseChunk[] sections, boolean fullChunk) {
        Runnable apply = fullChunk
                ? () -> {
            blocks.loadChunk(chunkX, chunkZ, sections);
            mirror.readiness().onChunkApplied();
        }
                : () -> {
            blocks.mergeChunk(chunkX, chunkZ, sections);
            mirror.readiness().onChunkApplied();
        };
        latencyHandler.compensateLazy(event, apply);
    }

    private void applyBlock(PacketSendEvent event, int x, int y, int z, int blockId) {
        if (blockId == effectiveId(x, y, z)) return;

        event.getTasksAfterSend().add(() -> {
            if (event.isCancelled()) return;
            pending.set(x, y, z, blockId);
        });
        latencyHandler.compensateLazy(event, () -> {
            blocks.set(x, y, z, blockId);
            pending.confirm(x, y, z, blockId);
            player.getPhysics().onBlockApplied(x, y, z, blockId);
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

        List<WrapperPlayServerMultiBlockChange.EncodedBlock> blocksChanged = changed;
        event.getTasksAfterSend().add(() -> {
            if (event.isCancelled()) return;
            for (WrapperPlayServerMultiBlockChange.EncodedBlock block : blocksChanged) {
                pending.set(block.getX(), block.getY(), block.getZ(), block.getBlockId());
            }
        });
        latencyHandler.compensateLazy(event, () -> {
            for (WrapperPlayServerMultiBlockChange.EncodedBlock block : blocksChanged) {
                blocks.set(block.getX(), block.getY(), block.getZ(), block.getBlockId());
                pending.confirm(block.getX(), block.getY(), block.getZ(), block.getBlockId());
                player.getPhysics().onBlockApplied(block.getX(), block.getY(), block.getZ(), block.getBlockId());
            }
        });
    }

    private int effectiveId(int x, int y, int z) {
        int pendingId = pending.peek(x, y, z);
        return pendingId != PendingBlocks.NONE ? pendingId : blocks.serverStateId(x, y, z);
    }
}
