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

package com.deathmotion.totemguard.common.player.data;

import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
public final class GlideData {

    private static final int CLAIM_WINDOW_TICKS = 20;
    private static final int RIPTIDE_WINDOW_TICKS = 3;
    private static final int EXIT_WINDOW_TICKS = 10;

    private int claimTicks;
    private int riptideTicks;
    private int exitTicks;
    @Getter
    private double riptideStrength;
    @Getter
    private boolean riptideGrounded;

    public void armClaim() {
        claimTicks = CLAIM_WINDOW_TICKS;
    }

    public void answerClaim() {
        claimTicks = 0;
    }

    public boolean claimActive() {
        return claimTicks > 0;
    }

    public void armRiptide(double strength, boolean grounded) {
        riptideTicks = RIPTIDE_WINDOW_TICKS;
        riptideStrength = strength;
        riptideGrounded = grounded;
    }

    public boolean riptideActive() {
        return riptideTicks > 0;
    }

    public void armExit() {
        exitTicks = EXIT_WINDOW_TICKS;
    }

    public boolean exitActive() {
        return exitTicks > 0;
    }

    public void tick() {
        if (claimTicks > 0) claimTicks--;
        if (riptideTicks > 0) riptideTicks--;
        if (exitTicks > 0) exitTicks--;
    }

    public void reset() {
        claimTicks = 0;
        riptideTicks = 0;
        exitTicks = 0;
        riptideStrength = 0.0;
        riptideGrounded = false;
    }
}
