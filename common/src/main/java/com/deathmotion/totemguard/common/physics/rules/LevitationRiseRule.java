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

package com.deathmotion.totemguard.common.physics.rules;

import com.deathmotion.totemguard.common.physics.control.ControlEnvelope;

public final class LevitationRiseRule {

    private static final int SETTLE_TICKS = 2;

    private boolean lastLevitation;
    private int lastAmplifier = Integer.MIN_VALUE;
    private int settle;

    public void observe(ControlEnvelope input) {
        boolean levitation = input.levitation();
        int amplifier = input.levitationAmplifier();
        if (levitation != lastLevitation || (levitation && amplifier != lastAmplifier)) {
            settle = SETTLE_TICKS;
        } else if (settle > 0) {
            settle--;
        }
        lastLevitation = levitation;
        lastAmplifier = amplifier;
    }

    public boolean armed(boolean tainted, ControlEnvelope input) {
        return !tainted && settle == 0
                && input.levitation() && input.levitationAmplifier() >= 0;
    }

    public void reset() {
        lastLevitation = false;
        lastAmplifier = Integer.MIN_VALUE;
        settle = 0;
    }
}
