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

import com.deathmotion.totemguard.common.world.block.StateFacts;

// Vertical truth comes from the flat clip: step-up only extends the horizontal allowance, its
// synthetic rise must not read as vertical tunneling.
public final class CollisionSweep {

    private static final double FLUSH_TOP_EPS = 1.0e-4;

    private final StepUpResolver stepUp = new StepUpResolver();
    private final double[] stepResult = new double[3];

    private static double embedDepth(ColliderBuffer buffer,
                                     double minX, double minY, double minZ,
                                     double maxX, double maxY, double maxZ) {
        double depth = 0.0;
        int count = buffer.count();
        for (int i = 0; i < count; i++) {
            if (!ColliderBuffer.clipEligible(buffer.tagOf(i))) continue;
            double overlapX = Math.min(maxX, buffer.maxX(i)) - Math.max(minX, buffer.minX(i));
            if (overlapX <= AxisClip.EPS) continue;
            double overlapY = Math.min(maxY, buffer.maxY(i)) - Math.max(minY, buffer.minY(i));
            if (overlapY <= AxisClip.EPS) continue;
            double overlapZ = Math.min(maxZ, buffer.maxZ(i)) - Math.max(minZ, buffer.minZ(i));
            if (overlapZ <= AxisClip.EPS) continue;
            double boxDepth = Math.min(overlapX, Math.min(overlapY, overlapZ));
            if (boxDepth > depth) depth = boxDepth;
        }
        return depth;
    }

    private static void checkStartOverlap(ColliderBuffer buffer, ContactReport report,
                                          double minX, double minY, double minZ,
                                          double maxX, double maxY, double maxZ) {
        int count = buffer.count();
        for (int i = 0; i < count; i++) {
            long tag = buffer.tagOf(i);
            if ((tag & ColliderBuffer.TAG_EXEMPT) != 0) continue;
            if (!AxisClip.overlaps(minX, maxX, buffer.minX(i), buffer.maxX(i))) continue;
            if (!AxisClip.overlaps(minY, maxY, buffer.minY(i), buffer.maxY(i))) continue;
            if (!AxisClip.overlaps(minZ, maxZ, buffer.minZ(i), buffer.maxZ(i))) continue;
            report.startOverlapping(true);
            if (ColliderBuffer.isBlock(tag) && StateFacts.is(tag, StateFacts.SUFFOCATING)) {
                report.startSuffocating(true);
                return;
            }
        }
    }

    private static double stepCandidateMax(ColliderBuffer buffer,
                                           double minX, double feetY, double minZ,
                                           double maxX, double maxZ,
                                           double dx, double dz, double stepHeight) {
        if (stepHeight <= 0.0) return 0.0;
        double x0 = Math.min(minX, minX + dx);
        double x1 = Math.max(maxX, maxX + dx);
        double z0 = Math.min(minZ, minZ + dz);
        double z1 = Math.max(maxZ, maxZ + dz);
        double best = 0.0;
        int count = buffer.count();
        for (int i = 0; i < count; i++) {
            if (!AxisClip.overlaps(x0, x1, buffer.minX(i), buffer.maxX(i))) continue;
            if (!AxisClip.overlaps(z0, z1, buffer.minZ(i), buffer.maxZ(i))) continue;
            long tag = buffer.tagOf(i);
            if (ColliderBuffer.isEntity(tag) || StateFacts.is(tag, StateFacts.SUPPORT_APPROXIMATE)) {
                return stepHeight;
            }
            double top = buffer.maxY(i) - feetY;
            if (top > best && top <= stepHeight) best = top;
            double bottom = buffer.minY(i) - feetY;
            if (bottom > best && bottom <= stepHeight) best = bottom;
        }
        return best;
    }

    public boolean flushTopAt(ColliderBuffer buffer, double feetY,
                              double minX, double minZ, double maxX, double maxZ) {
        int count = buffer.count();
        for (int i = 0; i < count; i++) {
            if (!ColliderBuffer.clipEligible(buffer.tagOf(i))) continue;
            if (Math.abs(buffer.maxY(i) - feetY) > FLUSH_TOP_EPS) continue;
            if (!AxisClip.overlaps(minX, maxX, buffer.minX(i), buffer.maxX(i))) continue;
            if (!AxisClip.overlaps(minZ, maxZ, buffer.minZ(i), buffer.maxZ(i))) continue;
            return true;
        }
        return false;
    }

    public void resolve(ColliderBuffer buffer, ContactReport report,
                        double startX, double startY, double startZ,
                        double halfWidth, double height,
                        double dx, double dy, double dz,
                        double stepHeight, boolean groundedStart) {
        report.reset();

        double minX = startX - halfWidth;
        double minY = startY;
        double minZ = startZ - halfWidth;
        double maxX = startX + halfWidth;
        double maxY = startY + height;
        double maxZ = startZ + halfWidth;

        checkStartOverlap(buffer, report, minX, minY, minZ, maxX, maxY, maxZ);

        double flatY = AxisClip.clip(buffer, AxisClip.AXIS_Y, minX, minY, minZ, maxX, maxY, maxZ, dy, true);
        double y0 = minY + flatY;
        double y1 = maxY + flatY;
        double allowedX;
        double allowedZ;
        if (Math.abs(dx) < Math.abs(dz)) {
            allowedZ = AxisClip.clip(buffer, AxisClip.AXIS_Z, minX, y0, minZ, maxX, y1, maxZ, dz, true);
            allowedX = AxisClip.clip(buffer, AxisClip.AXIS_X, minX, y0, minZ + allowedZ, maxX, y1, maxZ + allowedZ, dx, true);
        } else {
            allowedX = AxisClip.clip(buffer, AxisClip.AXIS_X, minX, y0, minZ, maxX, y1, maxZ, dx, true);
            allowedZ = AxisClip.clip(buffer, AxisClip.AXIS_Z, minX + allowedX, y0, minZ, maxX + allowedX, y1, maxZ, dz, true);
        }

        boolean blockedX = allowedX != dx;
        boolean blockedY = flatY != dy;
        boolean blockedZ = allowedZ != dz;
        boolean downBlocked = blockedY && dy < 0.0;

        if (stepHeight > 0.0 && (downBlocked || groundedStart) && (blockedX || blockedZ)) {
            double baseOffsetY = downBlocked ? flatY : 0.0;
            double flatDistSq = allowedX * allowedX + allowedZ * allowedZ;
            double used = stepUp.tryStep(buffer,
                    minX, minY + baseOffsetY, minZ, maxX, maxY + baseOffsetY, maxZ,
                    dx, dz, stepHeight, flatY,
                    flatDistSq, stepResult);
            if (used > 0.0) {
                allowedX = stepResult[0];
                allowedZ = stepResult[2];
                report.stepUsedHeight(used);
                blockedX = allowedX != dx;
                blockedZ = allowedZ != dz;
            }
        }

        report.allowedX(allowedX);
        report.allowedY(flatY);
        report.allowedZ(allowedZ);
        report.collidedX(blockedX);
        report.collidedY(blockedY);
        report.collidedZ(blockedZ);
        report.groundHit(blockedY && dy < 0.0);
        report.ceilingHit(blockedY && dy > 0.0);
        report.crossX(dx - allowedX);
        report.crossY(dy - flatY);
        report.crossZ(dz - allowedZ);

        report.stepCandidateMax(stepCandidateMax(buffer, minX, minY, minZ, maxX, maxZ, dx, dz, stepHeight));

        report.embedDepth(embedDepth(buffer, minX + dx, minY + dy, minZ + dz,
                maxX + dx, maxY + dy, maxZ + dz));

        SupportScanner.fill(buffer, report,
                minX + dx, minY + dy, minZ + dz,
                maxX + dx, maxY + dy, maxZ + dz,
                dx, dz);
    }
}
