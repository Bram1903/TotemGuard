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

import com.deathmotion.totemguard.common.physics.preset.PhysicsPreset;
import com.deathmotion.totemguard.common.physics.verdict.MitigationOutcome;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
public final class VehicleMitigationTracker {

    private static final double BASE_GAIN = 1.0;
    private static final double EXCESS_GAIN = 12.0;
    private static final double DECAY_FACTOR = 0.92;
    private static final double DECAY_SUBTRACT = 0.30;

    @Getter
    private double buffer;
    private boolean triggeredThisTick;

    public void observe(PhysicsPreset preset, boolean offense, double excess, boolean setbackPending) {
        triggeredThisTick = false;
        if (!offense) {
            buffer = Math.max(0.0, buffer * DECAY_FACTOR - DECAY_SUBTRACT);
            return;
        }
        double threshold = preset.setbackBufferThreshold();
        buffer += BASE_GAIN + excess * EXCESS_GAIN;
        if (buffer < threshold) return;
        if (setbackPending) {
            buffer = threshold;
            return;
        }
        buffer = 0.0;
        triggeredThisTick = true;
    }

    public MitigationOutcome outcome() {
        return triggeredThisTick
                ? new MitigationOutcome(true, false, false, false)
                : MitigationOutcome.NONE;
    }

    public void reset() {
        buffer = 0.0;
        triggeredThisTick = false;
    }
}
