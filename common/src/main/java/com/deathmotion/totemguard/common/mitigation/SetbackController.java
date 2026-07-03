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

package com.deathmotion.totemguard.common.mitigation;

import com.deathmotion.totemguard.common.physics.MovementConstants;
import com.deathmotion.totemguard.common.player.data.ExternalVelocityData;
import com.github.retrooper.packetevents.util.Vector3d;

public class SetbackController {

    private static final double MIN_PULL_DOWN = MovementConstants.GRAVITY;
    private static final double TERMINAL_PULL_DOWN = 3.0;
    private static final double GROUND_GAP_MARGIN = 0.001;
    private static final int ANCHOR_FREEZE_TICKS = 3;

    private final MitigationService service;
    private final ExternalVelocityData externalVelocity;

    private boolean hasSafe;
    private double safeX;
    private double safeY;
    private double safeZ;
    private double safeVelY;
    private double safeGroundGap;
    private boolean safeAirborne;

    private int anchorFreeze;

    public SetbackController(MitigationService service, ExternalVelocityData externalVelocity) {
        this.service = service;
        this.externalVelocity = externalVelocity;
    }

    public void markSafe(double x, double y, double z, double velY, double groundGap, boolean airborne) {
        if (anchorFreeze > 0) return;
        hasSafe = true;
        safeX = x;
        safeY = y;
        safeZ = z;
        safeVelY = velY;
        safeGroundGap = groundGap;
        safeAirborne = airborne;
    }

    public void requestAnchorFreeze() {
        anchorFreeze = ANCHOR_FREEZE_TICKS;
    }

    public void tickAnchorFreeze() {
        if (anchorFreeze > 0) anchorFreeze--;
    }

    public boolean hasSafe() {
        return hasSafe;
    }

    public boolean requestSetback() {
        if (!hasSafe) return false;
        if (service.setbackPending()) return false;
        if (!service.setbackIssuable()) return false;

        boolean foldKnockback = externalVelocity.isActive();
        double kbX = foldKnockback ? externalVelocity.x() : 0.0;
        double kbY = foldKnockback ? externalVelocity.y() : 0.0;
        double kbZ = foldKnockback ? externalVelocity.z() : 0.0;

        double drop = safeAirborne ? Math.min(safeVelY, 0.0) - MIN_PULL_DOWN : 0.0;
        drop += kbY;
        double maxDrop = -Math.max(0.0, safeGroundGap - GROUND_GAP_MARGIN);
        if (drop < maxDrop) drop = maxDrop;

        boolean issued = service.setback(new Vector3d(safeX + kbX, safeY + drop, safeZ + kbZ));
        if (!issued) return false;

        if (foldKnockback) externalVelocity.consume();
        if (drop < 0.0) {
            safeY += drop;
            safeGroundGap = Math.max(0.0, safeGroundGap + drop);
            safeVelY = Math.max(drop, -TERMINAL_PULL_DOWN);
        }
        return true;
    }

    public void reset() {
        hasSafe = false;
        anchorFreeze = 0;
    }
}
