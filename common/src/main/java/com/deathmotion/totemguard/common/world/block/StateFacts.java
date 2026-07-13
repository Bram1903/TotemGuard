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

package com.deathmotion.totemguard.common.world.block;

import com.deathmotion.totemguard.common.world.shape.ShapeRegistry;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// The unsynchronized table writes are a benign race: classification is deterministic and
// idempotent, and aligned long stores do not tear on the 64-bit JVMs servers run on.
public final class StateFacts {

    public static final long CLASSIFIED = 1L;
    public static final long AIR = 1L << 1;
    public static final long WATER = 1L << 2;
    public static final long LAVA = 1L << 3;
    public static final long BUBBLE_COLUMN = 1L << 4;
    public static final long CLIMBABLE = 1L << 5;
    public static final long STUCK = 1L << 6;
    public static final long BOUNCY = 1L << 7;
    public static final long HAS_SHAPE = 1L << 8;
    public static final long FULL_CUBE = 1L << 9;
    public static final long SUPPORT_APPROXIMATE = 1L << 10;
    public static final long SUFFOCATING = 1L << 11;
    public static final long NETHER_PORTAL = 1L << 12;
    public static final long WALL_TRUSTED = 1L << 13;
    public static final long BUBBLE_DRAG = 1L << 14;
    public static final long HONEY = 1L << 15;
    public static final long ANY_FLUID = WATER | LAVA;
    public static final long FLUID_FALLING = 1L << 27;
    public static final long FLUID_SOURCE = 1L << 28;
    public static final long FLOW_STURDY = 1L << 29;
    public static final long BLOCKS_MOTION = 1L << 30;
    private static final int SLIP_SHIFT = 16;
    private static final long SLIP_MASK = 3L << SLIP_SHIFT;
    private static final long SLOW_SPEED_FACTOR = 1L << 18;
    private static final long HALF_JUMP_FACTOR = 1L << 19;
    private static final int STUCK_SHIFT = 20;
    private static final long STUCK_MASK = 3L << STUCK_SHIFT;
    private static final long BED_BOUNCE = 1L << 22;
    private static final int FLUID_AMOUNT_SHIFT = 23;
    private static final long FLUID_AMOUNT_MASK = 15L << FLUID_AMOUNT_SHIFT;
    private static final int TABLE_SIZE = 1 << 16;

    private static final Map<ClientVersion, StateFacts> TABLES = new ConcurrentHashMap<>();

    private final ClientVersion stateVersion;
    private final long[] table = new long[TABLE_SIZE];

    private StateFacts(ClientVersion stateVersion) {
        this.stateVersion = stateVersion;
    }

    public static StateFacts tableFor(ClientVersion stateVersion) {
        return TABLES.computeIfAbsent(stateVersion, StateFacts::new);
    }

    public static boolean is(long facts, long flag) {
        return (facts & flag) != 0;
    }

    public static double slipperiness(long facts) {
        return switch ((int) ((facts & SLIP_MASK) >> SLIP_SHIFT)) {
            case 1 -> BlockTraits.SLIME_SLIPPERINESS;
            case 2 -> BlockTraits.ICE_SLIPPERINESS;
            case 3 -> BlockTraits.BLUE_ICE_SLIPPERINESS;
            default -> BlockTraits.DEFAULT_SLIPPERINESS;
        };
    }

    public static double speedFactor(long facts) {
        return (facts & SLOW_SPEED_FACTOR) != 0 ? BlockTraits.SLOWING_SPEED_FACTOR : 1.0;
    }

    public static double jumpFactor(long facts) {
        return (facts & HALF_JUMP_FACTOR) != 0 ? BlockTraits.HALF_JUMP_FACTOR : 1.0;
    }

    public static double bounceFactor(long facts) {
        if ((facts & BOUNCY) == 0) return 0.0;
        return (facts & BED_BOUNCE) != 0 ? BlockTraits.BED_BOUNCE : BlockTraits.SLIME_BOUNCE;
    }

    public static boolean bedBounce(long facts) {
        return (facts & BED_BOUNCE) != 0;
    }

    public static double stuckHorizontal(long facts, boolean weavingCobweb) {
        return switch ((int) ((facts & STUCK_MASK) >> STUCK_SHIFT)) {
            case 1 -> weavingCobweb ? 0.5 : 0.25;
            case 2 -> 0.8;
            case 3 -> 0.9;
            default -> 1.0;
        };
    }

    public static double stuckVertical(long facts, boolean weavingCobweb) {
        return switch ((int) ((facts & STUCK_MASK) >> STUCK_SHIFT)) {
            case 1 -> weavingCobweb ? 0.25 : 0.05;
            case 2 -> 0.75;
            case 3 -> 1.5;
            default -> 1.0;
        };
    }

    public static int fluidAmount(long facts) {
        return (int) ((facts & FLUID_AMOUNT_MASK) >> FLUID_AMOUNT_SHIFT);
    }

    public static double fluidHeight(long facts) {
        return fluidAmount(facts) / 9.0;
    }

    private static long fluidLevelFacts(WrappedBlockState state) {
        int level = state.getLevel();
        if (level == 0) return (8L << FLUID_AMOUNT_SHIFT) | FLUID_SOURCE;
        if (level >= 8) return (8L << FLUID_AMOUNT_SHIFT) | FLUID_FALLING;
        return (long) (8 - level) << FLUID_AMOUNT_SHIFT;
    }

    private static boolean suffocating(WrappedBlockState state, StateType type, long facts) {
        if (ShapeRegistry.suffocatingOverride(type)) return true;
        if (ShapeRegistry.suffocatingNever(type)) return false;
        return type.isBlocking() && (facts & FULL_CUBE) != 0;
    }

    public long of(int stateId) {
        if (stateId < 0 || stateId >= TABLE_SIZE) return classify(stateId);
        long facts = table[stateId];
        if ((facts & CLASSIFIED) == 0) {
            facts = classify(stateId);
            table[stateId] = facts;
        }
        return facts;
    }

    private long classify(int stateId) {
        WrappedBlockState state = WrappedBlockState.getByGlobalId(stateVersion, stateId, false);
        StateType type = state.getType();
        long facts = CLASSIFIED;

        if (type == StateTypes.AIR || type == StateTypes.CAVE_AIR || type == StateTypes.VOID_AIR) {
            facts |= AIR;
            return facts;
        }
        if (type == StateTypes.LAVA) {
            facts |= LAVA | fluidLevelFacts(state);
        } else if (BlockTraits.isFluid(state)) {
            facts |= WATER;
            if (type == StateTypes.WATER) {
                facts |= fluidLevelFacts(state);
            } else {
                facts |= (8L << FLUID_AMOUNT_SHIFT) | FLUID_SOURCE;
            }
        }
        if (BlockTraits.isBubbleColumn(type)) {
            facts |= BUBBLE_COLUMN;
            if (state.isDrag()) facts |= BUBBLE_DRAG;
        }
        if (BlockTraits.isClimbable(type)) facts |= CLIMBABLE;
        if (BlockTraits.isStuck(type)) {
            facts |= STUCK;
            if (type == StateTypes.COBWEB) facts |= 1L << STUCK_SHIFT;
            else if (type == StateTypes.SWEET_BERRY_BUSH) facts |= 2L << STUCK_SHIFT;
            else facts |= 3L << STUCK_SHIFT;
        }
        double bounce = BlockTraits.bounceFactor(type);
        if (bounce > 0.0) {
            facts |= BOUNCY;
            if (bounce == BlockTraits.BED_BOUNCE) facts |= BED_BOUNCE;
        }
        double slip = BlockTraits.slipperiness(type);
        if (slip == BlockTraits.SLIME_SLIPPERINESS) facts |= 1L << SLIP_SHIFT;
        else if (slip == BlockTraits.ICE_SLIPPERINESS) facts |= 2L << SLIP_SHIFT;
        else if (slip == BlockTraits.BLUE_ICE_SLIPPERINESS) facts |= 3L << SLIP_SHIFT;
        if (BlockTraits.speedFactor(type) != 1.0) facts |= SLOW_SPEED_FACTOR;
        if (BlockTraits.jumpFactor(type) != 1.0) facts |= HALF_JUMP_FACTOR;
        if (type == StateTypes.HONEY_BLOCK) facts |= HONEY;

        if (ShapeRegistry.hasShape(state)) facts |= HAS_SHAPE;
        if (ShapeRegistry.fullCubeDefault(state)) facts |= FULL_CUBE;
        if (ShapeRegistry.supportApproximate(type)) facts |= SUPPORT_APPROXIMATE;
        if (ShapeRegistry.wallTrusted(type)) facts |= WALL_TRUSTED;
        if (suffocating(state, type, facts)) facts |= SUFFOCATING;
        if (type == StateTypes.NETHER_PORTAL) facts |= NETHER_PORTAL;
        if ((facts & FULL_CUBE) != 0
                && type != StateTypes.ICE && type != StateTypes.FROSTED_ICE) {
            facts |= FLOW_STURDY;
        }
        if ((facts & HAS_SHAPE) != 0
                && type != StateTypes.SNOW && type != StateTypes.LILY_PAD) {
            facts |= BLOCKS_MOTION;
        }
        return facts;
    }
}
