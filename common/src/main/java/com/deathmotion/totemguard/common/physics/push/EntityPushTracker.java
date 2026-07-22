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

package com.deathmotion.totemguard.common.physics.push;

import com.deathmotion.totemguard.common.physics.area.AreaBounds;
import com.deathmotion.totemguard.common.util.ClientMath;
import com.deathmotion.totemguard.common.world.entity.EntityTracker;
import com.deathmotion.totemguard.common.world.entity.TrackedEntity;
import com.deathmotion.totemguard.common.world.team.TeamState;

public final class EntityPushTracker {

    private static final double VANILLA_PUSH_SCALAR = 0.05;
    private static final double BOAT_CANDIDATE_INFLATE = 0.2;
    private static final double MAX_FRICTION = 0.99;

    private double carriedLoX, carriedHiX, carriedLoZ, carriedHiZ;
    private double pendingLoX, pendingHiX, pendingLoZ, pendingHiZ;
    private double freshLoX, freshHiX, freshLoZ, freshHiZ;

    private static double intervalGap(double aMin, double aMax, double bMin, double bMax) {
        if (aMax < bMin) return bMin - aMax;
        if (bMax < aMin) return aMin - bMax;
        return 0.0;
    }

    private static double magnitudeMin(double lo, double hi) {
        if (lo <= 0.0 && hi >= 0.0) return 0.0;
        return Math.min(Math.abs(lo), Math.abs(hi));
    }

    private static double magnitudeMax(double lo, double hi) {
        return Math.max(Math.abs(lo), Math.abs(hi));
    }

    private static double axisPush(double ownMagnitude, double chebyshev) {
        if (ownMagnitude <= 0.0 || chebyshev <= 0.0) return 0.0;
        double root = Math.sqrt(chebyshev);
        return VANILLA_PUSH_SCALAR * ownMagnitude / root * Math.min(1.0, 1.0 / root);
    }

    public void advance(EntityTracker entities, TeamState teams, double friction,
                        double centerMinX, double feetMinY, double centerMinZ,
                        double centerMaxX, double feetMaxY, double centerMaxZ,
                        double moveX, double moveY, double moveZ,
                        double playerHalfWidth, double playerHeight) {
        double decay = Math.max(0.0, Math.min(MAX_FRICTION, friction));

        carriedLoX = (carriedLoX + pendingLoX) * decay;
        carriedHiX = (carriedHiX + pendingHiX) * decay;
        carriedLoZ = (carriedLoZ + pendingLoZ) * decay;
        carriedHiZ = (carriedHiZ + pendingHiZ) * decay;
        pendingLoX = freshLoX;
        pendingHiX = freshHiX;
        pendingLoZ = freshLoZ;
        pendingHiZ = freshHiZ;
        freshLoX = freshHiX = freshLoZ = freshHiZ = 0.0;

        if (entities.clientPusherCount() == 0) return;

        int ambiguity = TrackedEntity.LANDING_TICK_AMBIGUITY;
        double playerLoX = centerMinX - ambiguity * Math.abs(moveX);
        double playerHiX = centerMaxX + ambiguity * Math.abs(moveX);
        double playerLoY = feetMinY - ambiguity * Math.abs(moveY);
        double playerHiY = feetMaxY + ambiguity * Math.abs(moveY);
        double playerLoZ = centerMinZ - ambiguity * Math.abs(moveZ);
        double playerHiZ = centerMaxZ + ambiguity * Math.abs(moveZ);

        for (TrackedEntity entity : entities.tracked()) {
            if (!entity.positioned() || !entity.clientSidePusher()) continue;

            boolean boat = entity.boatPusher();
            double reach = playerHalfWidth + entity.halfWidth()
                    + (boat ? BOAT_CANDIDATE_INFLATE : 0.0);
            double entityLoX = entity.reachMinX();
            double entityHiX = entity.reachMaxX();
            double entityLoZ = entity.reachMinZ();
            double entityHiZ = entity.reachMaxZ();
            if (intervalGap(playerLoX, playerHiX, entityLoX, entityHiX) >= reach) continue;
            if (intervalGap(playerLoZ, playerHiZ, entityLoZ, entityHiZ) >= reach) continue;
            if (entity.reachMinY() >= playerHiY + playerHeight) continue;
            if (boat) {
                if (playerLoY > entity.reachMaxY()) continue;
            } else if (playerLoY >= entity.reachMaxY() + entity.height()) {
                continue;
            }
            if (!teams.pushableBy(entity.playerEntity(), entity.uuid(), entity.uuidString())) continue;

            double offsetLoX = playerLoX - entityHiX;
            double offsetHiX = playerHiX - entityLoX;
            double offsetLoZ = playerLoZ - entityHiZ;
            double offsetHiZ = playerHiZ - entityLoZ;

            double maxX = magnitudeMax(offsetLoX, offsetHiX);
            double maxZ = magnitudeMax(offsetLoZ, offsetHiZ);
            double minX = magnitudeMin(offsetLoX, offsetHiX);
            double minZ = magnitudeMin(offsetLoZ, offsetHiZ);

            double ceilingX = axisPush(maxX, Math.max(maxX, minZ));
            double ceilingZ = axisPush(maxZ, Math.max(maxZ, minX));

            if (offsetLoX >= 0.0) {
                freshHiX += ceilingX;
            } else if (offsetHiX <= 0.0) {
                freshLoX -= ceilingX;
            } else {
                freshHiX += ceilingX;
                freshLoX -= ceilingX;
            }

            if (offsetLoZ >= 0.0) {
                freshHiZ += ceilingZ;
            } else if (offsetHiZ <= 0.0) {
                freshLoZ -= ceilingZ;
            } else {
                freshHiZ += ceilingZ;
                freshLoZ -= ceilingZ;
            }
        }
    }

    public double apply(AreaBounds bounds) {
        double lo = grantLoX();
        double hi = grantHiX();
        if (lo < 0.0 || hi > 0.0) bounds.addPushX(lo, hi);
        lo = grantLoZ();
        hi = grantHiZ();
        if (lo < 0.0 || hi > 0.0) bounds.addPushZ(lo, hi);
        return extent();
    }

    public double grantLoX() {
        return carriedLoX + pendingLoX + freshLoX;
    }

    public double grantHiX() {
        return carriedHiX + pendingHiX + freshHiX;
    }

    public double grantLoZ() {
        return carriedLoZ + pendingLoZ + freshLoZ;
    }

    public double grantHiZ() {
        return carriedHiZ + pendingHiZ + freshHiZ;
    }

    public double extent() {
        return ClientMath.horizontalDistance(
                Math.max(-grantLoX(), grantHiX()),
                Math.max(-grantLoZ(), grantHiZ()));
    }

    public boolean active() {
        return grantLoX() < 0.0 || grantHiX() > 0.0 || grantLoZ() < 0.0 || grantHiZ() > 0.0;
    }

    public void reset() {
        carriedLoX = carriedHiX = carriedLoZ = carriedHiZ = 0.0;
        pendingLoX = pendingHiX = pendingLoZ = pendingHiZ = 0.0;
        freshLoX = freshHiX = freshLoZ = freshHiZ = 0.0;
    }
}
