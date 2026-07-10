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

package com.deathmotion.totemguard.common.physics.area;

import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
public final class HypothesisSet {

    public enum Slot {
        MAIN,
        IMPULSE,
        SNAP
    }

    private final AreaBounds main = new AreaBounds();
    private final AreaBounds impulse = new AreaBounds();
    private final AreaBounds snap = new AreaBounds();
    private boolean mainEnabled;
    private boolean impulseEnabled;
    private boolean snapEnabled;

    @Getter
    private Slot chosenSlot = Slot.MAIN;
    @Getter
    private JudgedExcess chosenExcess = JudgedExcess.NONE;

    public void reset(MotionArea carried) {
        main.reset(carried);
        mainEnabled = true;
        impulseEnabled = false;
        snapEnabled = false;
        chosenSlot = Slot.MAIN;
        chosenExcess = JudgedExcess.NONE;
    }

    public AreaBounds bounds(Slot slot) {
        return switch (slot) {
            case MAIN -> main;
            case IMPULSE -> impulse;
            case SNAP -> snap;
        };
    }

    public void enable(Slot slot) {
        switch (slot) {
            case MAIN -> mainEnabled = true;
            case IMPULSE -> impulseEnabled = true;
            case SNAP -> snapEnabled = true;
        }
    }

    public Slot judge(double dx, double dy, double dz, double arrestCap) {
        Slot winner = Slot.MAIN;
        JudgedExcess winning = JudgedExcess.NONE;
        double best = Double.MAX_VALUE;
        if (mainEnabled) {
            winning = AreaJudge.judge(main, dx, dy, dz, arrestCap);
            best = maxExcess(winning);
        }
        if (impulseEnabled) {
            JudgedExcess judged = AreaJudge.judge(impulse, dx, dy, dz, arrestCap);
            double excess = maxExcess(judged);
            if (excess < best) {
                winner = Slot.IMPULSE;
                winning = judged;
                best = excess;
            }
        }
        if (snapEnabled) {
            JudgedExcess judged = AreaJudge.judge(snap, dx, dy, dz, arrestCap);
            double excess = maxExcess(judged);
            if (excess < best) {
                winner = Slot.SNAP;
                winning = judged;
            }
        }
        chosenSlot = winner;
        chosenExcess = winning;
        return winner;
    }

    public AreaBounds chosenBounds() {
        return bounds(chosenSlot);
    }

    private static double maxExcess(JudgedExcess judged) {
        return Math.max(judged.horizontal(), Math.max(judged.ascent(), judged.descent()));
    }
}
