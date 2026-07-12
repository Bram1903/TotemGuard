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
    private static final int INFINITE = -1;

    private int levitationAmplifier = UNSET;
    private int levitationTicks;

    private int jumpBoostAmplifier = UNSET;
    private int jumpBoostTicks;

    private int slowFallingTicks;
    private int dolphinsGraceTicks;
    private int weavingTicks;

    private static int normalize(int durationTicks) {
        return durationTicks < 0 ? INFINITE : durationTicks;
    }

    public void setLevitation(int amplifier, int durationTicks) {
        this.levitationAmplifier = amplifier;
        this.levitationTicks = normalize(durationTicks);
    }

    public void clearLevitation() {
        this.levitationAmplifier = UNSET;
        this.levitationTicks = 0;
    }

    public void setJumpBoost(int amplifier, int durationTicks) {
        this.jumpBoostAmplifier = amplifier;
        this.jumpBoostTicks = normalize(durationTicks);
    }

    public void clearJumpBoost() {
        this.jumpBoostAmplifier = UNSET;
        this.jumpBoostTicks = 0;
    }

    public void setSlowFalling(int durationTicks) {
        this.slowFallingTicks = normalize(durationTicks);
    }

    public void clearSlowFalling() {
        this.slowFallingTicks = 0;
    }

    public void setDolphinsGrace(int durationTicks) {
        this.dolphinsGraceTicks = normalize(durationTicks);
    }

    public void clearDolphinsGrace() {
        this.dolphinsGraceTicks = 0;
    }

    public void setWeaving(int durationTicks) {
        this.weavingTicks = normalize(durationTicks);
    }

    public void clearWeaving() {
        this.weavingTicks = 0;
    }

    public void tick() {
        if (levitationTicks > 0 && --levitationTicks <= 0) {
            levitationAmplifier = UNSET;
        }
        if (jumpBoostTicks > 0 && --jumpBoostTicks <= 0) {
            jumpBoostAmplifier = UNSET;
        }
        if (slowFallingTicks > 0) {
            slowFallingTicks--;
        }
        if (dolphinsGraceTicks > 0) {
            dolphinsGraceTicks--;
        }
        if (weavingTicks > 0) {
            weavingTicks--;
        }
    }

    public boolean hasLevitation() {
        return levitationAmplifier != UNSET && levitationTicks != 0;
    }

    public int levitationAmplifier() {
        return levitationAmplifier;
    }

    public boolean hasJumpBoost() {
        return jumpBoostAmplifier != UNSET && jumpBoostTicks != 0;
    }

    public int jumpBoostAmplifier() {
        return jumpBoostAmplifier;
    }

    public boolean hasSlowFalling() {
        return slowFallingTicks != 0;
    }

    public boolean hasDolphinsGrace() {
        return dolphinsGraceTicks != 0;
    }

    public boolean hasWeaving() {
        return weavingTicks != 0;
    }

    public void reset() {
        clearLevitation();
        clearJumpBoost();
        clearSlowFalling();
        clearDolphinsGrace();
        clearWeaving();
    }
}
