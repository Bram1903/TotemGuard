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
import com.deathmotion.totemguard.common.player.data.Data;
import com.deathmotion.totemguard.common.player.data.PistonData;
import com.deathmotion.totemguard.common.player.latency.PacketLatencyHandler;
import com.deathmotion.totemguard.common.player.processor.ProcessorOutbound;
import com.deathmotion.totemguard.common.world.WorldMirror;
import com.deathmotion.totemguard.common.world.block.BlockReader;
import com.deathmotion.totemguard.common.world.block.StateFacts;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockAction;

import java.util.Set;

public class OutboundPistonProcessor extends ProcessorOutbound {

    private static final double ARM_RANGE = 24.0;
    private static final double ARM_RANGE_SQUARED = ARM_RANGE * ARM_RANGE;
    private static final int MAX_PUSH = 12;
    private static final int[] DIR_X = {0, 0, 0, 0, -1, 1};
    private static final int[] DIR_Y = {-1, 1, 0, 0, 0, 0};
    private static final int[] DIR_Z = {0, 0, -1, 1, 0, 0};

    private static final Set<StateType> PUSH_DENIED = Set.of(
            StateTypes.OBSIDIAN, StateTypes.CRYING_OBSIDIAN, StateTypes.RESPAWN_ANCHOR,
            StateTypes.REINFORCED_DEEPSLATE,
            StateTypes.PISTON_HEAD, StateTypes.MOVING_PISTON);

    private static final Set<StateType> BLOCK_ENTITY_NORMAL = Set.of(
            StateTypes.CHEST, StateTypes.TRAPPED_CHEST, StateTypes.ENDER_CHEST,
            StateTypes.FURNACE, StateTypes.BLAST_FURNACE, StateTypes.SMOKER,
            StateTypes.DISPENSER, StateTypes.DROPPER, StateTypes.HOPPER,
            StateTypes.BREWING_STAND, StateTypes.ENCHANTING_TABLE, StateTypes.BEACON,
            StateTypes.SPAWNER, StateTypes.JUKEBOX, StateTypes.LECTERN, StateTypes.BELL,
            StateTypes.CONDUIT, StateTypes.BARREL, StateTypes.BEE_NEST, StateTypes.BEEHIVE,
            StateTypes.SCULK_SENSOR, StateTypes.CALIBRATED_SCULK_SENSOR,
            StateTypes.SCULK_SHRIEKER, StateTypes.SCULK_CATALYST,
            StateTypes.DAYLIGHT_DETECTOR, StateTypes.END_GATEWAY, StateTypes.END_PORTAL,
            StateTypes.CHISELED_BOOKSHELF, StateTypes.DECORATED_POT,
            StateTypes.SUSPICIOUS_SAND, StateTypes.SUSPICIOUS_GRAVEL,
            StateTypes.CRAFTER, StateTypes.TRIAL_SPAWNER, StateTypes.VAULT);

    private final Data data;
    private final WorldMirror world;
    private final PacketLatencyHandler latencyHandler;

    private final long[] toPush = new long[MAX_PUSH];
    private int pushCount;
    private boolean saturated;

    public OutboundPistonProcessor(TGPlayer player) {
        super(player);
        this.data = player.getData();
        this.world = player.getWorldMirror();
        this.latencyHandler = player.getLatencyHandler();
    }

    private static boolean hasNormalReactionBlockEntity(StateType type) {
        if (BLOCK_ENTITY_NORMAL.contains(type)) return true;
        String name = type.getName();
        return name.endsWith("shulker_box") || name.endsWith("_bed") || name.endsWith("_sign");
    }

    private static boolean isPushOnly(StateType type) {
        return type.getName().endsWith("glazed_terracotta");
    }

    private static boolean isSticky(StateType type) {
        return type == StateTypes.SLIME_BLOCK || type == StateTypes.HONEY_BLOCK;
    }

    private static boolean canStickToEachOther(StateType first, StateType second) {
        if (first == StateTypes.HONEY_BLOCK && second == StateTypes.SLIME_BLOCK) return false;
        if (first == StateTypes.SLIME_BLOCK && second == StateTypes.HONEY_BLOCK) return false;
        return isSticky(first) || isSticky(second);
    }

    private static boolean samePos(Vector3i pos, int x, int y, int z) {
        return pos.getX() == x && pos.getY() == y && pos.getZ() == z;
    }

    private static boolean maybePiston(int blockTypeId) {
        StateType.Mapped mapped = StateTypes.getMappedById(
                PacketEvents.getAPI().getServerManager().getVersion().toClientVersion(), blockTypeId);
        if (mapped == null) return true;
        StateType type = mapped.getStateType();
        return type == StateTypes.PISTON || type == StateTypes.STICKY_PISTON;
    }

    @Override
    public void handleOutbound(PacketSendEvent event) {
        if (event.isCancelled()) return;
        if (event.getPacketType() != PacketType.Play.Server.BLOCK_ACTION) return;

        WrapperPlayServerBlockAction packet = new WrapperPlayServerBlockAction(event);
        int actionId = packet.getActionId();
        if (actionId < 0 || actionId > 2) return;
        if (!maybePiston(packet.getBlockTypeId())) return;

        Vector3i pos = packet.getBlockPosition();
        latencyHandler.compensate(event, () -> resolve(pos, actionId));
    }

    private void resolve(Vector3i pos, int actionId) {
        Location current = data.getMovementData().getCurrent();
        double dx = (pos.getX() + 0.5) - current.getX();
        double dy = (pos.getY() + 0.5) - current.getY();
        double dz = (pos.getZ() + 0.5) - current.getZ();
        if (dx * dx + dy * dy + dz * dz > ARM_RANGE_SQUARED) return;

        BlockReader reader = world.reader();
        WrappedBlockState base = reader.state(pos.getX(), pos.getY(), pos.getZ());
        StateType baseType = base.getType();
        if (baseType != StateTypes.PISTON && baseType != StateTypes.STICKY_PISTON) return;
        boolean sticky = baseType == StateTypes.STICKY_PISTON;

        BlockFace facing = base.getFacing();
        int faceX = facing.getModX();
        int faceY = facing.getModY();
        int faceZ = facing.getModZ();
        boolean extend = actionId == 0;
        int pushX = extend ? faceX : -faceX;
        int pushY = extend ? faceY : -faceY;
        int pushZ = extend ? faceZ : -faceZ;

        PistonData.Scene scene = data.getPistonData().armScene();
        scene.push(pushX, pushY, pushZ, !extend);
        scene.addCell(pos.getX(), pos.getY(), pos.getZ());
        scene.addCell(pos.getX() + faceX, pos.getY() + faceY, pos.getZ() + faceZ);

        if (actionId == 2) return;
        if (actionId == 1 && !sticky) return;

        int startX = pos.getX() + (extend ? faceX : 2 * faceX);
        int startY = pos.getY() + (extend ? faceY : 2 * faceY);
        int startZ = pos.getZ() + (extend ? faceZ : 2 * faceZ);

        pushCount = 0;
        saturated = false;
        boolean failed = false;
        if (isPushable(reader, startX, startY, startZ, pushX, pushY, pushZ, false, faceX, faceY, faceZ)) {
            failed = !addBlockLine(reader, pos, startX, startY, startZ, pushX, pushY, pushZ,
                    faceX, faceY, faceZ);
            for (int i = 0; !failed && i < pushCount; i++) {
                long cell = toPush[i];
                int cx = PistonData.Scene.cellX(cell);
                int cy = PistonData.Scene.cellY(cell);
                int cz = PistonData.Scene.cellZ(cell);
                if (isSticky(reader.state(cx, cy, cz).getType())) {
                    failed = !addBranchingBlocks(reader, pos, cx, cy, cz, pushX, pushY, pushZ);
                }
            }
        } else if (extend && !isLikelyDestroyed(reader, startX, startY, startZ)) {
            failed = true;
        }

        for (int i = 0; i < pushCount; i++) {
            long cell = toPush[i];
            int cx = PistonData.Scene.cellX(cell);
            int cy = PistonData.Scene.cellY(cell);
            int cz = PistonData.Scene.cellZ(cell);
            scene.addCell(cx, cy, cz);
            scene.addCell(cx + pushX, cy + pushY, cz + pushZ);
            if (reader.state(cx, cy, cz).getType() == StateTypes.SLIME_BLOCK) {
                scene.launch(pushX, pushY, pushZ);
            }
        }
        if (extend && (failed || saturated)) {
            scene.saturate(pos.getX(), pos.getY(), pos.getZ(), MAX_PUSH + 1);
        }
    }

    private boolean addBlockLine(BlockReader reader, Vector3i pistonPos,
                                 int x, int y, int z,
                                 int pushX, int pushY, int pushZ,
                                 int connX, int connY, int connZ) {
        if (StateFacts.is(reader.facts(x, y, z), StateFacts.AIR)) return true;
        if (!isPushable(reader, x, y, z, pushX, pushY, pushZ, false, connX, connY, connZ)) return true;
        if (samePos(pistonPos, x, y, z)) return true;
        if (contains(x, y, z)) return true;

        int behind = 1;
        if (pushCount + behind > MAX_PUSH) {
            saturated = true;
            return false;
        }
        StateType chain = reader.state(x, y, z).getType();
        while (isSticky(chain)) {
            int bx = x - pushX * behind;
            int by = y - pushY * behind;
            int bz = z - pushZ * behind;
            StateType behindType = reader.state(bx, by, bz).getType();
            if (StateFacts.is(reader.facts(bx, by, bz), StateFacts.AIR)
                    || !canStickToEachOther(chain, behindType)
                    || !isPushable(reader, bx, by, bz, pushX, pushY, pushZ, false, -pushX, -pushY, -pushZ)
                    || samePos(pistonPos, bx, by, bz)) {
                break;
            }
            behind++;
            if (pushCount + behind > MAX_PUSH) {
                saturated = true;
                return false;
            }
            chain = behindType;
        }
        for (int i = behind - 1; i >= 0; i--) {
            add(x - pushX * i, y - pushY * i, z - pushZ * i);
        }

        int forward = 1;
        while (true) {
            int fx = x + pushX * forward;
            int fy = y + pushY * forward;
            int fz = z + pushZ * forward;
            if (contains(fx, fy, fz)) return true;
            if (StateFacts.is(reader.facts(fx, fy, fz), StateFacts.AIR)) return true;
            if (!isPushable(reader, fx, fy, fz, pushX, pushY, pushZ, true, pushX, pushY, pushZ)) {
                return false;
            }
            if (samePos(pistonPos, fx, fy, fz)) return false;
            if (isLikelyDestroyed(reader, fx, fy, fz)) return true;
            if (pushCount >= MAX_PUSH) {
                saturated = true;
                return false;
            }
            add(fx, fy, fz);
            forward++;
        }
    }

    private boolean addBranchingBlocks(BlockReader reader, Vector3i pistonPos,
                                       int x, int y, int z,
                                       int pushX, int pushY, int pushZ) {
        StateType self = reader.state(x, y, z).getType();
        for (int face = 0; face < 6; face++) {
            int dirX = DIR_X[face];
            int dirY = DIR_Y[face];
            int dirZ = DIR_Z[face];
            if ((dirX != 0 && pushX != 0) || (dirY != 0 && pushY != 0) || (dirZ != 0 && pushZ != 0)) {
                continue;
            }
            int nx = x + dirX;
            int ny = y + dirY;
            int nz = z + dirZ;
            if (!canStickToEachOther(reader.state(nx, ny, nz).getType(), self)) continue;
            if (!addBlockLine(reader, pistonPos, nx, ny, nz, pushX, pushY, pushZ, dirX, dirY, dirZ)) {
                return false;
            }
        }
        return true;
    }

    private boolean isPushable(BlockReader reader, int x, int y, int z,
                               int pushX, int pushY, int pushZ, boolean allowDestroy,
                               int connX, int connY, int connZ) {
        if (StateFacts.is(reader.facts(x, y, z), StateFacts.AIR)) return true;
        WrappedBlockState state = reader.state(x, y, z);
        StateType type = state.getType();
        if (PUSH_DENIED.contains(type)) return false;
        if (type == StateTypes.PISTON || type == StateTypes.STICKY_PISTON) {
            return !state.isExtended();
        }
        if (type.getHardness() == -1.0f) return false;
        if (isPushOnly(type)) {
            return pushX == connX && pushY == connY && pushZ == connZ;
        }
        if (isLikelyDestroyed(reader, x, y, z)) return allowDestroy;
        return !hasNormalReactionBlockEntity(type);
    }

    private boolean isLikelyDestroyed(BlockReader reader, int x, int y, int z) {
        return !StateFacts.is(reader.facts(x, y, z), StateFacts.HAS_SHAPE);
    }

    private boolean contains(int x, int y, int z) {
        for (int i = 0; i < pushCount; i++) {
            long cell = toPush[i];
            if (PistonData.Scene.cellX(cell) == x && PistonData.Scene.cellY(cell) == y
                    && PistonData.Scene.cellZ(cell) == z) {
                return true;
            }
        }
        return false;
    }

    private void add(int x, int y, int z) {
        toPush[pushCount++] = PistonData.Scene.packCell(x, y, z);
    }
}
