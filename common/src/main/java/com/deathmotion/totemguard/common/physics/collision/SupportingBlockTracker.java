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

package com.deathmotion.totemguard.common.physics.collision;

import com.deathmotion.totemguard.common.world.block.BlockReader;
import com.deathmotion.totemguard.common.world.block.BlockTraits;
import com.deathmotion.totemguard.common.world.block.StateFacts;
import com.github.retrooper.packetevents.protocol.world.states.defaulttags.BlockTags;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;

public final class SupportingBlockTracker {

    public static final double SLAB_DEPTH = 1.0E-6;

    private static final double LEGACY_COLUMN_DEPTH = 0.5000001;
    private static final double MODERN_COLUMN_DEPTH = 0.500001F;
    private static final long EMPTY = Long.MAX_VALUE;
    private static final long TAINTED = Long.MAX_VALUE - 1;

    private enum Presence {ABSENT, PRESENT, DUAL, UNKNOWN}

    private final boolean supportingBlockClient;

    private Presence support = Presence.ABSENT;
    private long supportCell;
    private long dualCell;
    private Presence noBlocks = Presence.ABSENT;

    private boolean slipCertain;
    private double slip = BlockTraits.DEFAULT_SLIPPERINESS;
    private boolean jumpCertain;
    private double jumpFactor = 1.0;
    private boolean speedCertain;
    private double speedFactor = 1.0;

    public SupportingBlockTracker(boolean supportingBlockClient) {
        this.supportingBlockClient = supportingBlockClient;
    }

    public boolean slipCertain() {
        return slipCertain;
    }

    public double slip() {
        return slip;
    }

    public boolean jumpCertain() {
        return jumpCertain;
    }

    public double jumpFactor() {
        return jumpFactor;
    }

    public boolean speedCertain() {
        return speedCertain;
    }

    public double speedFactor() {
        return speedFactor;
    }

    public void invalidate() {
        support = Presence.UNKNOWN;
        noBlocks = Presence.UNKNOWN;
        slipCertain = false;
        jumpCertain = false;
        speedCertain = false;
    }

    public void reset() {
        support = Presence.ABSENT;
        noBlocks = Presence.ABSENT;
        slipCertain = false;
        jumpCertain = false;
        speedCertain = false;
        slip = BlockTraits.DEFAULT_SLIPPERINESS;
        jumpFactor = 1.0;
        speedFactor = 1.0;
    }

    public void update(ColliderBuffer colliders, BlockReader reader,
                       boolean groundedEnd, double supportGap, boolean supportApproximate,
                       double minX, double minZ, double maxX, double maxZ,
                       double posX, double feetY, double posZ,
                       double dx, double dz) {
        if (!supportingBlockClient) {
            resolveTraits(reader, posX, feetY, posZ, LEGACY_COLUMN_DEPTH, Presence.ABSENT);
            return;
        }

        boolean flush = supportGap <= SLAB_DEPTH;
        boolean groundedCertain = groundedEnd && flush && !supportApproximate;
        boolean airborneCertain = !groundedEnd && !flush;

        if (airborneCertain) {
            support = Presence.ABSENT;
            noBlocks = Presence.ABSENT;
        } else if (groundedCertain) {
            updateGrounded(colliders, minX, minZ, maxX, maxZ, posX, feetY, posZ, dx, dz);
        } else {
            support = Presence.UNKNOWN;
            noBlocks = Presence.UNKNOWN;
        }
        resolveTraits(reader, posX, feetY, posZ, MODERN_COLUMN_DEPTH, support);
    }

    private void updateGrounded(ColliderBuffer colliders,
                                double minX, double minZ, double maxX, double maxZ,
                                double posX, double feetY, double posZ,
                                double dx, double dz) {
        long direct = findSupport(colliders, minX, minZ, maxX, maxZ, feetY, posX, feetY, posZ);
        if (direct == TAINTED) {
            support = Presence.UNKNOWN;
            noBlocks = Presence.UNKNOWN;
            return;
        }
        if (direct != EMPTY) {
            support = Presence.PRESENT;
            supportCell = direct;
            noBlocks = Presence.ABSENT;
            return;
        }
        if (noBlocks == Presence.PRESENT) {
            support = Presence.ABSENT;
            return;
        }

        long retry = findSupport(colliders, minX - dx, minZ - dz, maxX - dx, maxZ - dz,
                feetY, posX, feetY, posZ);
        if (retry == TAINTED) {
            support = Presence.UNKNOWN;
            noBlocks = Presence.UNKNOWN;
            return;
        }
        if (noBlocks == Presence.ABSENT) {
            if (retry != EMPTY) {
                support = Presence.PRESENT;
                supportCell = retry;
                noBlocks = Presence.ABSENT;
            } else {
                support = Presence.ABSENT;
                noBlocks = Presence.PRESENT;
            }
            return;
        }

        if (retry == EMPTY) {
            support = Presence.ABSENT;
            noBlocks = Presence.PRESENT;
        } else {
            support = Presence.DUAL;
            dualCell = retry;
            noBlocks = Presence.UNKNOWN;
        }
    }

    private long findSupport(ColliderBuffer colliders,
                             double minX, double minZ, double maxX, double maxZ, double feetY,
                             double posX, double posY, double posZ) {
        double slabMinY = feetY - SLAB_DEPTH;
        long best = EMPTY;
        double bestDistance = Double.MAX_VALUE;
        int count = colliders.count();
        for (int i = 0; i < count; i++) {
            if (colliders.maxY(i) <= slabMinY || colliders.minY(i) >= feetY) continue;
            if (colliders.maxX(i) <= minX || colliders.minX(i) >= maxX) continue;
            if (colliders.maxZ(i) <= minZ || colliders.minZ(i) >= maxZ) continue;
            long tag = colliders.tagOf(i);
            if (ColliderBuffer.isEntity(tag)) continue;
            long cell = colliders.cellOf(i);
            if (!ColliderBuffer.clipEligible(tag) || cell == ColliderBuffer.NO_CELL
                    || StateFacts.is(tag, StateFacts.SUPPORT_APPROXIMATE)) {
                return TAINTED;
            }
            double distance = distToCenterSqr(cell, posX, posY, posZ);
            if (distance < bestDistance
                    || (distance == bestDistance && best != EMPTY && comparePos(best, cell) < 0)) {
                best = cell;
                bestDistance = distance;
            }
        }
        return best;
    }

    private void resolveTraits(BlockReader reader, double posX, double feetY, double posZ,
                               double columnDepth, Presence state) {
        int feetX = floor(posX);
        int feetCellY = floor(feetY);
        int feetZ = floor(posZ);
        int belowY = floor(feetY - columnDepth);

        long fallbackPos = ColliderBuffer.packCell(feetX, belowY, feetZ);
        long primaryPos;
        long alternatePos;
        switch (state) {
            case PRESENT -> {
                primaryPos = affectsMovementPos(reader, supportCell, belowY);
                alternatePos = primaryPos;
            }
            case ABSENT -> {
                primaryPos = fallbackPos;
                alternatePos = primaryPos;
            }
            case DUAL -> {
                primaryPos = fallbackPos;
                alternatePos = affectsMovementPos(reader, dualCell, belowY);
            }
            default -> {
                resolveUnknown(reader, feetX, feetCellY, feetZ);
                return;
            }
        }

        boolean posCertain = cellKnown(reader, primaryPos) && cellKnown(reader, alternatePos);
        long primaryFacts = facts(reader, primaryPos);
        long alternateFacts = facts(reader, alternatePos);

        double slipPrimary = StateFacts.slipperiness(primaryFacts);
        double slipAlternate = StateFacts.slipperiness(alternateFacts);
        slipCertain = posCertain && slipPrimary == slipAlternate;
        if (slipCertain) slip = slipPrimary;

        boolean feetKnown = !reader.uncertain(feetX, feetCellY, feetZ);
        long feetFacts = reader.facts(feetX, feetCellY, feetZ);

        double jumpFeet = StateFacts.jumpFactor(feetFacts);
        if (feetKnown && jumpFeet != 1.0) {
            jumpCertain = true;
            jumpFactor = jumpFeet;
        } else {
            double jumpPrimary = StateFacts.jumpFactor(primaryFacts);
            double jumpAlternate = StateFacts.jumpFactor(alternateFacts);
            jumpCertain = feetKnown && posCertain && jumpPrimary == jumpAlternate;
            if (jumpCertain) jumpFactor = jumpPrimary;
        }

        boolean feetFluid = (feetFacts & (StateFacts.WATER | StateFacts.BUBBLE_COLUMN)) != 0;
        double speedFeet = StateFacts.speedFactor(feetFacts);
        if (feetKnown && (feetFluid || speedFeet != 1.0)) {
            speedCertain = true;
            speedFactor = feetFluid ? 1.0 : speedFeet;
        } else {
            double speedPrimary = StateFacts.speedFactor(primaryFacts);
            double speedAlternate = StateFacts.speedFactor(alternateFacts);
            speedCertain = feetKnown && posCertain && speedPrimary == speedAlternate;
            if (speedCertain) speedFactor = speedPrimary;
        }
    }

    private void resolveUnknown(BlockReader reader, int feetX, int feetCellY, int feetZ) {
        slipCertain = false;
        boolean feetKnown = !reader.uncertain(feetX, feetCellY, feetZ);
        long feetFacts = reader.facts(feetX, feetCellY, feetZ);

        double jumpFeet = StateFacts.jumpFactor(feetFacts);
        jumpCertain = feetKnown && jumpFeet != 1.0;
        if (jumpCertain) jumpFactor = jumpFeet;

        boolean feetFluid = (feetFacts & (StateFacts.WATER | StateFacts.BUBBLE_COLUMN)) != 0;
        double speedFeet = StateFacts.speedFactor(feetFacts);
        speedCertain = feetKnown && (feetFluid || speedFeet != 1.0);
        if (speedCertain) speedFactor = feetFluid ? 1.0 : speedFeet;
    }

    private long affectsMovementPos(BlockReader reader, long cell, int belowY) {
        StateType type = reader.stateForClientId(
                reader.stateId(ColliderBuffer.cellX(cell), ColliderBuffer.cellY(cell), ColliderBuffer.cellZ(cell)))
                .getType();
        if (BlockTags.WALLS.contains(type) || BlockTags.FENCE_GATES.contains(type)) {
            return cell;
        }
        return ColliderBuffer.packCell(ColliderBuffer.cellX(cell), belowY, ColliderBuffer.cellZ(cell));
    }

    private static boolean cellKnown(BlockReader reader, long pos) {
        return !reader.uncertain(ColliderBuffer.cellX(pos), ColliderBuffer.cellY(pos), ColliderBuffer.cellZ(pos));
    }

    private static long facts(BlockReader reader, long pos) {
        return reader.facts(ColliderBuffer.cellX(pos), ColliderBuffer.cellY(pos), ColliderBuffer.cellZ(pos));
    }

    private static double distToCenterSqr(long cell, double posX, double posY, double posZ) {
        double dx = ColliderBuffer.cellX(cell) + 0.5 - posX;
        double dy = ColliderBuffer.cellY(cell) + 0.5 - posY;
        double dz = ColliderBuffer.cellZ(cell) + 0.5 - posZ;
        return dx * dx + dy * dy + dz * dz;
    }

    private static int comparePos(long left, long right) {
        int leftY = ColliderBuffer.cellY(left), rightY = ColliderBuffer.cellY(right);
        if (leftY != rightY) return leftY - rightY;
        int leftZ = ColliderBuffer.cellZ(left), rightZ = ColliderBuffer.cellZ(right);
        if (leftZ != rightZ) return leftZ - rightZ;
        return ColliderBuffer.cellX(left) - ColliderBuffer.cellX(right);
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }
}
