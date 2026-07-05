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

package com.deathmotion.totemguard.common.physics.phase;

import com.deathmotion.totemguard.common.player.data.ClientWorld;
import com.deathmotion.totemguard.common.world.collisions.BlockShapes;
import com.deathmotion.totemguard.common.world.collisions.CollisionBox;
import com.deathmotion.totemguard.common.world.collisions.CollisionContext;
import com.deathmotion.totemguard.common.world.collisions.CollisionShape;
import com.deathmotion.totemguard.common.world.scan.WallGaps;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.HashSet;
import java.util.Set;

@Accessors(fluent = true)
public final class PhaseTracker {

    private static final double HIT_EPSILON = 0.002;
    private static final double ENTRY_TOLERANCE = 0.02;
    private static final double EMBED_TOLERANCE = 0.02;
    private static final double EMBED_GROWTH = 0.02;
    private static final int WINDOW = 6;
    private static final int GRACE_TICKS = 10;
    private static final double MOVE_EPS = 0.015;
    private static final double CONTACT_EPS = 1.0e-7;

    private int window;
    private int grace;
    private double lastEmbedded = -1.0;
    @Getter
    private final Set<Long> exemptCells = new HashSet<>();

    public double excess(WallGaps gaps, double observedSpeed, boolean countGrace, double padScale) {
        double entryTolerance = ENTRY_TOLERANCE * padScale;
        double embedTolerance = EMBED_TOLERANCE * padScale;
        double entry = gaps.crossing() - entryTolerance;
        boolean entering = entry > HIT_EPSILON;
        if (entering) window = WINDOW;

        double embedded = gaps.embedded();
        boolean moving = observedSpeed > MOVE_EPS;
        double growth = lastEmbedded >= 0.0 ? embedded - lastEmbedded : 0.0;
        boolean growing = growth > EMBED_GROWTH && growth <= observedSpeed + EMBED_GROWTH;
        if (grace == 0 && moving && growing && embedded > embedTolerance) {
            window = WINDOW;
        }

        double excess = Math.max(0.0, entry);
        boolean embeddedWhileMoving = window > 0 && moving && embedded > embedTolerance;
        if (embeddedWhileMoving) {
            window = WINDOW;
            excess = Math.max(excess, embedded - embedTolerance);
        } else if (window > 0) {
            window--;
        }

        if (countGrace && grace > 0) grace--;
        lastEmbedded = embedded;
        return excess;
    }

    public void seedGrace() {
        grace = GRACE_TICKS;
    }

    public void invalidateEmbed() {
        lastEmbedded = -1.0;
    }

    public void exemptEmbeddedCells(ClientWorld world, Location current, double half, double poseHeight, boolean sneaking) {
        double minX = current.getX() - half, maxX = current.getX() + half;
        double minY = current.getY(), maxY = current.getY() + poseHeight;
        double minZ = current.getZ() - half, maxZ = current.getZ() + half;
        CollisionContext ctx = new CollisionContext(current.getY(), sneaking);
        for (int x = floor(minX); x <= floor(maxX); x++) {
            for (int y = floor(minY); y <= floor(maxY); y++) {
                for (int z = floor(minZ); z <= floor(maxZ); z++) {
                    WrappedBlockState state = world.getBlockState(x, y, z);
                    CollisionShape shape = BlockShapes.shapeOf(state, x, y, z, ctx);
                    if (shape.isEmpty()) continue;
                    for (CollisionBox box : shape.boxes()) {
                        if (x + box.maxX() <= minX + CONTACT_EPS || x + box.minX() >= maxX - CONTACT_EPS) continue;
                        if (y + box.maxY() <= minY + CONTACT_EPS || y + box.minY() >= maxY - CONTACT_EPS) continue;
                        if (z + box.maxZ() <= minZ + CONTACT_EPS || z + box.minZ() >= maxZ - CONTACT_EPS) continue;
                        exemptCells.add(ClientWorld.blockKey(x, y, z));
                        break;
                    }
                }
            }
        }
    }

    public void onBlockChangeApplied(ClientWorld world, Location current, double half, double poseHeight, boolean sneaking,
                                     int x, int y, int z, int blockId) {
        double minX = current.getX() - half, maxX = current.getX() + half;
        double minY = current.getY(), maxY = current.getY() + poseHeight;
        double minZ = current.getZ() - half, maxZ = current.getZ() + half;
        if (x + 1.0 <= minX || x >= maxX || y + 1.0 <= minY || y >= maxY || z + 1.0 <= minZ || z >= maxZ) return;

        WrappedBlockState state = world.stateForId(blockId);
        CollisionShape shape = BlockShapes.shapeOf(state, x, y, z, new CollisionContext(current.getY(), sneaking));
        for (CollisionBox box : shape.boxes()) {
            if (x + box.maxX() <= minX || x + box.minX() >= maxX) continue;
            if (y + box.maxY() <= minY || y + box.minY() >= maxY) continue;
            if (z + box.maxZ() <= minZ || z + box.minZ() >= maxZ) continue;
            exemptCells.add(ClientWorld.blockKey(x, y, z));
            return;
        }
    }

    public void prune(Location current, double half, double poseHeight) {
        if (exemptCells.isEmpty()) return;
        Set<Long> bodyCells = new HashSet<>();
        for (int x = floor(current.getX() - half); x <= floor(current.getX() + half); x++) {
            for (int y = floor(current.getY()) - 2; y <= floor(current.getY() + poseHeight) + 2; y++) {
                for (int z = floor(current.getZ() - half); z <= floor(current.getZ() + half); z++) {
                    bodyCells.add(ClientWorld.blockKey(x, y, z));
                }
            }
        }
        exemptCells.retainAll(bodyCells);
    }

    public void clear() {
        window = 0;
        grace = 0;
        lastEmbedded = -1.0;
        exemptCells.clear();
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }
}
