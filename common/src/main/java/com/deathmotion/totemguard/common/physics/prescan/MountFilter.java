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

import com.deathmotion.totemguard.common.player.data.VehicleData;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
public final class MountFilter {

    @Getter
    private boolean seeded;
    private int reentryVehicleId = -1;
    private long reentryUntilMs;
    @Getter
    private double reentryX;
    @Getter
    private double reentryY;
    @Getter
    private double reentryZ;

    public boolean reentryBlocked(int vehicleId, long nowMs) {
        return vehicleId >= 0 && vehicleId == reentryVehicleId && nowMs < reentryUntilMs;
    }

    public void markReentry(int vehicleId, double x, double y, double z, long untilMs) {
        reentryVehicleId = vehicleId;
        reentryUntilMs = untilMs;
        reentryX = x;
        reentryY = y;
        reentryZ = z;
    }

    public boolean needsSeed(VehicleData vehicle) {
        return !seeded || vehicle.isSeedFromMount();
    }

    public void markSeeded() {
        seeded = true;
    }

    public void reset() {
        seeded = false;
    }
}
