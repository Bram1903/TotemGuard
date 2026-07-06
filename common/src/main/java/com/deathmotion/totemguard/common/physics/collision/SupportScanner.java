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

final class SupportScanner {

    static final double PROBE_DEPTH = 2.5;
    static final double CEILING_PROBE = 2.0;
    static final double WALL_PROBE = 0.1;

    private static final double TOP_BAND = 1.0e-6;

    private SupportScanner() {
    }

    static void fill(ColliderBuffer buffer, ContactReport report,
                     double minX, double minY, double minZ,
                     double maxX, double maxY, double maxZ,
                     double dx, double dz) {
        probeSupport(buffer, report, minX, minY, minZ, maxX, maxZ, true);
        report.trailingSupportGap(trailingGap(buffer, minX - dx, minY, minZ - dz, maxX - dx, maxZ - dz));
        probeCeiling(buffer, report, minX, minZ, maxX, maxY, maxZ);
        report.wallNear(wallNear(buffer, minX, minY, minZ, maxX, maxY, maxZ));
    }

    private static void probeSupport(ColliderBuffer buffer, ContactReport report,
                                     double minX, double feetY, double minZ,
                                     double maxX, double maxZ, boolean aggregate) {
        double bestTop = Double.NEGATIVE_INFINITY;
        int count = buffer.count();
        for (int i = 0; i < count; i++) {
            double top = supportTopOf(buffer, i, minX, feetY, minZ, maxX, maxZ);
            if (top > bestTop) bestTop = top;
        }
        if (bestTop == Double.NEGATIVE_INFINITY) {
            report.supportGap(ContactReport.NO_SUPPORT);
            return;
        }
        report.supportGap(Math.max(0.0, feetY - bestTop));
        report.supportTop(bestTop);
        if (!aggregate) return;

        boolean first = true;
        for (int i = 0; i < count; i++) {
            double top = supportTopOf(buffer, i, minX, feetY, minZ, maxX, maxZ);
            if (top < bestTop - TOP_BAND) continue;
            long tag = buffer.tagOf(i);
            if (ColliderBuffer.isEntity(tag)) {
                report.supportIsEntity(true);
                report.supportApproximate(true);
                continue;
            }
            double slip = StateFacts.slipperiness(tag);
            double bounce = StateFacts.bounceFactor(tag);
            double speed = StateFacts.speedFactor(tag);
            double jump = StateFacts.jumpFactor(tag);
            if (first) {
                report.supportSlipMin(slip);
                report.supportSlipMax(slip);
                report.supportJumpMin(jump);
                report.supportJumpMax(jump);
                first = false;
            } else {
                report.supportSlipMin(Math.min(report.supportSlipMin(), slip));
                report.supportSlipMax(Math.max(report.supportSlipMax(), slip));
                report.supportJumpMin(Math.min(report.supportJumpMin(), jump));
                report.supportJumpMax(Math.max(report.supportJumpMax(), jump));
            }
            report.supportBounce(Math.max(report.supportBounce(), bounce));
            report.supportSpeedFactor(Math.min(report.supportSpeedFactor(), speed));
            if (StateFacts.is(tag, StateFacts.SUPPORT_APPROXIMATE)) report.supportApproximate(true);
        }
    }

    private static double trailingGap(ColliderBuffer buffer,
                                      double minX, double feetY, double minZ,
                                      double maxX, double maxZ) {
        double bestTop = Double.NEGATIVE_INFINITY;
        int count = buffer.count();
        for (int i = 0; i < count; i++) {
            double top = supportTopOf(buffer, i, minX, feetY, minZ, maxX, maxZ);
            if (top > bestTop) bestTop = top;
        }
        return bestTop == Double.NEGATIVE_INFINITY ? ContactReport.NO_SUPPORT : Math.max(0.0, feetY - bestTop);
    }

    // Boxes that never clip (uncertain cells, entities) can legally contain the feet, so their
    // support top clamps to feet level.
    private static double supportTopOf(ColliderBuffer buffer, int i,
                                       double minX, double feetY, double minZ,
                                       double maxX, double maxZ) {
        if (!AxisClip.overlaps(minX, maxX, buffer.minX(i), buffer.maxX(i))) return Double.NEGATIVE_INFINITY;
        if (!AxisClip.overlaps(minZ, maxZ, buffer.minZ(i), buffer.maxZ(i))) return Double.NEGATIVE_INFINITY;
        double top = buffer.maxY(i);
        if (top > feetY + AxisClip.EPS) {
            long tag = buffer.tagOf(i);
            boolean softBox = (tag & ColliderBuffer.TAG_UNCERTAIN) != 0 || ColliderBuffer.isEntity(tag);
            if (!softBox || buffer.minY(i) > feetY + AxisClip.EPS) return Double.NEGATIVE_INFINITY;
            top = feetY;
        }
        if (top < feetY - PROBE_DEPTH) return Double.NEGATIVE_INFINITY;
        return top;
    }

    private static void probeCeiling(ColliderBuffer buffer, ContactReport report,
                                     double minX, double minZ, double maxX, double headY, double maxZ) {
        double clearance = CEILING_PROBE;
        int count = buffer.count();
        for (int i = 0; i < count; i++) {
            if (!ColliderBuffer.clipEligible(buffer.tagOf(i))) continue;
            if (!AxisClip.overlaps(minX, maxX, buffer.minX(i), buffer.maxX(i))) continue;
            if (!AxisClip.overlaps(minZ, maxZ, buffer.minZ(i), buffer.maxZ(i))) continue;
            double gap = buffer.minY(i) - headY;
            if (gap >= -AxisClip.EPS && gap < clearance) clearance = Math.max(0.0, gap);
        }
        report.ceilingClearance(clearance);
    }

    private static boolean wallNear(ColliderBuffer buffer,
                                    double minX, double minY, double minZ,
                                    double maxX, double maxY, double maxZ) {
        int count = buffer.count();
        for (int i = 0; i < count; i++) {
            if (!ColliderBuffer.clipEligible(buffer.tagOf(i))) continue;
            boolean yOverlap = AxisClip.overlaps(minY, maxY, buffer.minY(i), buffer.maxY(i));
            if (!yOverlap) continue;
            boolean xOverlap = AxisClip.overlaps(minX, maxX, buffer.minX(i), buffer.maxX(i));
            boolean zOverlap = AxisClip.overlaps(minZ, maxZ, buffer.minZ(i), buffer.maxZ(i));
            if (zOverlap && !xOverlap) {
                double gapEast = buffer.minX(i) - maxX;
                double gapWest = minX - buffer.maxX(i);
                if ((gapEast >= -AxisClip.EPS && gapEast < WALL_PROBE)
                        || (gapWest >= -AxisClip.EPS && gapWest < WALL_PROBE)) {
                    return true;
                }
            } else if (xOverlap && !zOverlap) {
                double gapSouth = buffer.minZ(i) - maxZ;
                double gapNorth = minZ - buffer.maxZ(i);
                if ((gapSouth >= -AxisClip.EPS && gapSouth < WALL_PROBE)
                        || (gapNorth >= -AxisClip.EPS && gapNorth < WALL_PROBE)) {
                    return true;
                }
            }
        }
        return false;
    }
}
