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

package com.deathmotion.totemguard.common.world.scan;

import com.deathmotion.totemguard.common.player.data.ClientWorld;
import com.deathmotion.totemguard.common.player.data.WorldEntityData;
import com.deathmotion.totemguard.common.util.BoundingBox;
import com.deathmotion.totemguard.common.world.collisions.CollisionContext;
import com.github.retrooper.packetevents.protocol.world.Location;

import java.util.Set;

import static com.deathmotion.totemguard.common.world.scan.Scans.floor;

public final class BlockEnvironmentScanner {

    private static final double WALL_PROBE_MARGIN = 0.15;

    private BlockEnvironmentScanner() {
    }

    public static BlockEnvironment scan(ClientWorld world, WorldEntityData entities, Location current, Location previous,
                                        double width, double poseHeight, double stepHeight, boolean sneaking,
                                        Set<Long> wallExemptCells) {
        if (!world.isLoaded(floor(current.getX()) >> 4, floor(current.getZ()) >> 4)) {
            return BlockEnvironment.UNLOADED;
        }

        BoundingBox startBody = BoundingBox.player(previous, width, poseHeight);
        BoundingBox sweptBody = BoundingBox.sweptPlayer(current, previous, width, poseHeight);
        boolean fluid = MediumScanner.fluidReachesFeet(world, startBody, previous.getY());
        double bubbleAscent = MediumScanner.bubbleColumnAscent(world, sweptBody);
        MediumScanner.Stuck stuck = MediumScanner.stuck(world, previous, width, poseHeight);
        boolean stuckSwept = MediumScanner.stuckAlongPath(world, sweptBody);
        boolean climbable = MediumScanner.climbableAt(world, previous);

        CollisionContext ctx = new CollisionContext(current.getY(), sneaking);
        SupportScanner.Support below = SupportScanner.scan(world, entities, current, previous, width, ctx);
        WallGaps wallGaps = WallScanner.walls(world, current, previous, width / 2.0, stepHeight, ctx, wallExemptCells);
        double ceilingGap = WallScanner.ceilingGap(world, current, previous, width / 2.0, poseHeight, ctx, wallExemptCells);
        int overlapState = WallScanner.overlapState(world, startBody, ctx);
        boolean horizontalObstacle = WallScanner.horizontalObstacle(world, current, width / 2.0, poseHeight, ctx, WALL_PROBE_MARGIN);

        return new BlockEnvironment(true, fluid, climbable, stuck.active(), stuck.horizontal(), stuck.vertical(),
                stuckSwept, below.bounceFactor(), below.slipperinessMin(), below.slipperinessMax(), below.blockSpeedFactor(),
                below.groundGap(),
                bubbleAscent, wallGaps, ceilingGap,
                (overlapState & WallScanner.OVERLAP_SUFFOCATING) != 0, overlapState != 0, horizontalObstacle);
    }
}
