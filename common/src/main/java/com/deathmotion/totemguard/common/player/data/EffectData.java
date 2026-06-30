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

public class EffectData {

    private static final int UNSET = -1;

    private int levitationAmplifier = UNSET;
    private int levitationTicks;

    private int slowFallingTicks;

    public void setLevitation(int amplifier, int durationTicks) {
        this.levitationAmplifier = amplifier;
        this.levitationTicks = Math.max(0, durationTicks);
    }

    public void clearLevitation() {
        this.levitationAmplifier = UNSET;
        this.levitationTicks = 0;
    }

    public void setSlowFalling(int durationTicks) {
        this.slowFallingTicks = Math.max(0, durationTicks);
    }

    public void clearSlowFalling() {
        this.slowFallingTicks = 0;
    }

    public void tick() {
        if (levitationTicks > 0 && --levitationTicks <= 0) {
            levitationAmplifier = UNSET;
        }
        if (slowFallingTicks > 0) {
            slowFallingTicks--;
        }
    }

    public boolean hasLevitation() {
        return levitationAmplifier != UNSET && levitationTicks > 0;
    }

    public int levitationAmplifier() {
        return levitationAmplifier;
    }

    public boolean hasSlowFalling() {
        return slowFallingTicks > 0;
    }

    public void reset() {
        clearLevitation();
        clearSlowFalling();
    }
}
