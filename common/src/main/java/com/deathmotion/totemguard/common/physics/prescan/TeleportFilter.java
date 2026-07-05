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

package com.deathmotion.totemguard.common.physics.prescan;

import com.deathmotion.totemguard.common.player.data.MovementData;
import com.deathmotion.totemguard.common.player.data.TeleportData;

public final class TeleportFilter {

    private static final int PRESERVE_GRACE_TICKS = 3;

    public enum Outcome {
        NONE,
        RESYNC_REST_PENDING_SETBACK,
        RESYNC_REST,
        RESYNC_PRESERVED,
        RESYNC_OTHER,
        TELEPORT_PRESERVED,
        TELEPORT
    }

    private int preserveGrace;

    public Outcome classify(MovementData movement, TeleportData teleports, boolean setbackPending) {
        if (movement.isLastFlyingWasResync()) {
            if (movement.isLastFlyingWasTeleportResync()) {
                if (movement.isLastFlyingTeleportVelocityReset()) {
                    return setbackPending ? Outcome.RESYNC_REST_PENDING_SETBACK : Outcome.RESYNC_REST;
                }
                return Outcome.RESYNC_PRESERVED;
            }
            return Outcome.RESYNC_OTHER;
        }
        if (teleports.hasPendingTeleport()) {
            return movement.hasPendingVelocityPreservingTeleport() ? Outcome.TELEPORT_PRESERVED : Outcome.TELEPORT;
        }
        return Outcome.NONE;
    }

    public void startPreserveGrace() {
        preserveGrace = PRESERVE_GRACE_TICKS;
    }

    public boolean inPreserveGrace() {
        return preserveGrace > 0;
    }

    public void tickPreserveGrace() {
        if (preserveGrace > 0) preserveGrace--;
    }

    public void reset() {
        preserveGrace = 0;
    }
}
