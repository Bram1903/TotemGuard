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

package com.deathmotion.totemguard.common.physics.mitigation;

import com.deathmotion.totemguard.common.mitigation.MitigationService;
import com.deathmotion.totemguard.common.physics.prescan.MountFilter;

public final class VehicleSetback {

    public static final long REENTRY_BLOCK_MS = 1000;

    private double safeX, safeY, safeZ;
    private boolean safeKnown;

    public void rememberSafe(double x, double y, double z) {
        safeX = x;
        safeY = y;
        safeZ = z;
        safeKnown = true;
    }

    public boolean requestSetback(MitigationService service, MountFilter mounts, int vehicleId,
                                  long nowMs) {
        if (!mounts.seeded() || !safeKnown) return false;
        boolean issued = service.bootRider(safeX, safeY, safeZ);
        if (issued) {
            mounts.markReentry(vehicleId, safeX, safeY, safeZ, nowMs + REENTRY_BLOCK_MS);
        }
        return issued;
    }

    public void reset() {
        safeKnown = false;
    }
}
