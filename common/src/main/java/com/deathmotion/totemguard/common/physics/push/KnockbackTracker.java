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
import com.deathmotion.totemguard.common.physics.area.JudgedExcess;
import com.deathmotion.totemguard.common.player.data.ExternalVelocityData;
import com.deathmotion.totemguard.common.util.ClientMath;

public final class KnockbackTracker {

    private final ExternalVelocityData external;

    private double minRequiredMiss = Double.MAX_VALUE;
    private boolean windowTainted;
    private boolean requirementObservedThisTick;
    private int trackedSetSequence = -1;

    public KnockbackTracker(ExternalVelocityData external) {
        this.external = external;
    }

    public void apply(AreaBounds bounds, double knockbackPad, boolean knockbackSetHypothesis) {
        if (!external.isActive()) return;
        double slack = external.slack();
        if (external.hasSet()) {
            if (!knockbackSetHypothesis) return;
            bounds.expandRadius(knockbackPad);
            bounds.ceiling(bounds.ceiling() + knockbackPad);
            bounds.addDescentSlack(knockbackPad);
            return;
        }
        bounds.altCenter(bounds.centerX() + external.x(), bounds.centerZ() + external.z());
        bounds.expandRadius(knockbackPad + slack);
        double up = Math.max(0.0, external.y() + slack);
        if (up > 0.0) bounds.ceiling(bounds.ceiling() + up + knockbackPad);
        double down = Math.max(0.0, -(external.y() - slack));
        if (down > 0.0) bounds.addDescentSlack(down + knockbackPad);
    }

    public void consumeIfExplained(JudgedExcess excess, double horizontalEpsilon, double verticalEpsilon) {
        if (!external.isActive() || external.hasSet()) return;
        if (excess.altCenterUsed() && excess.horizontal() <= horizontalEpsilon
                && excess.ascent() <= verticalEpsilon) {
            external.consume();
            resetRequirement();
        }
    }

    public void consumeExplained() {
        if (!external.isActive()) return;
        external.consume();
        resetRequirement();
    }

    public void observeRequirement(double obsX, double obsZ, double reach,
                                   boolean taintedTick, double consumeEpsilon) {
        requirementObservedThisTick = true;
        if (!external.isActive() || !external.hasSet()) return;
        if (trackedSetSequence != external.setSequence()) {
            trackedSetSequence = external.setSequence();
            minRequiredMiss = Double.MAX_VALUE;
            windowTainted = false;
        }
        if (taintedTick) {
            windowTainted = true;
            return;
        }
        double miss = Math.max(0.0,
                ClientMath.horizontalDistance(obsX - external.x(), obsZ - external.z()) - reach);
        if (miss < minRequiredMiss) minRequiredMiss = miss;
        if (miss <= consumeEpsilon) {
            external.consume();
            resetRequirement();
        }
    }

    public void finishTick() {
        if (external.isActive() && external.hasSet() && !requirementObservedThisTick) {
            windowTainted = true;
        }
        requirementObservedThisTick = false;
    }

    public double pollIgnored() {
        if (!external.pollExpiredSet()) return 0.0;
        double result = windowTainted || minRequiredMiss == Double.MAX_VALUE ? 0.0 : minRequiredMiss;
        resetRequirement();
        return result;
    }

    public boolean active() {
        return external.isActive();
    }

    private void resetRequirement() {
        minRequiredMiss = Double.MAX_VALUE;
        windowTainted = false;
        trackedSetSequence = -1;
    }
}
