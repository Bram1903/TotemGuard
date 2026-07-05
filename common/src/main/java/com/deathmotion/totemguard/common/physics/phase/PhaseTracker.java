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

package com.deathmotion.totemguard.common.physics.phase;

import com.deathmotion.totemguard.common.physics.preset.PhysicsPreset;

public final class PhaseTracker {

    private static final double HIT_EPSILON = 0.002;
    private static final double EMBED_GROWTH = 0.02;
    private static final int WINDOW = 6;
    private static final int GRACE_TICKS = 10;
    private static final double MOVE_EPS = 0.015;

    private int window;
    private int grace;
    private double lastEmbedded = -1.0;

    public double excess(double crossing, double embedded, double observedSpeed,
                         boolean countGrace, PhysicsPreset preset) {
        double entry = crossing - preset.phaseCrossTolerance();
        boolean entering = entry > HIT_EPSILON;
        if (entering) window = WINDOW;

        double embedTolerance = preset.phaseEmbedTolerance();
        boolean moving = observedSpeed > MOVE_EPS;
        double growth = lastEmbedded >= 0.0 ? embedded - lastEmbedded : 0.0;
        boolean growing = growth > EMBED_GROWTH && growth <= observedSpeed + EMBED_GROWTH;
        if (grace == 0 && moving && growing && embedded > embedTolerance) {
            window = WINDOW;
        }

        double excess = Math.max(0.0, entry);
        boolean embeddedWhileMoving = window > 0 && moving && embedded > embedTolerance;
        if (embeddedWhileMoving) {
            window = WINDOW;
            excess = Math.max(excess, embedded - embedTolerance);
        } else if (window > 0) {
            window--;
        }

        if (countGrace && grace > 0) grace--;
        lastEmbedded = embedded;
        return excess;
    }

    public void seedGrace() {
        grace = GRACE_TICKS;
    }

    public void invalidateEmbed() {
        lastEmbedded = -1.0;
    }

    public void clear() {
        window = 0;
        grace = 0;
        lastEmbedded = -1.0;
    }
}
