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

package com.deathmotion.totemguard.common.check;

import com.deathmotion.totemguard.common.player.TGPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class HeuristicCheck extends CheckImpl {

    public static final String OVERRIDE_BYPASS_HEURISTIC_GUARD = "BYPASS_HEURISTIC_GUARD";

    private long lastDecayAt;

    protected HeuristicCheck(TGPlayer player) {
        super(player);
        this.lastDecayAt = System.currentTimeMillis();
    }

    @Override
    public boolean isHeuristic() {
        return true;
    }

    protected abstract double flagThreshold();

    protected double decayPerSecond() {
        return 0.5;
    }

    protected long maxInventoryCampMs() {
        return 10_000L;
    }

    protected boolean requiresCombat() {
        return false;
    }

    protected boolean inCombat() {
        return true;
    }

    protected final boolean punish(double weight) {
        if (!buildPressure(weight)) return false;
        return fail();
    }

    protected final boolean punish(double weight, @NotNull String template, @Nullable Object @NotNull ... args) {
        if (!buildPressure(weight)) return false;
        return fail(template, args);
    }

    private boolean buildPressure(double weight) {
        if (guarded()) return false;

        applyDecay();
        double after = buffer.increase(weight);

        if (after >= flagThreshold()) {
            buffer.reset();
            return true;
        }
        return false;
    }

    protected final void reward(double weight) {
        applyDecay();
        buffer.decrease(weight);
    }

    protected final double touchBuffer() {
        return applyDecay();
    }

    private boolean guarded() {
        if (guardsBypassed()) return false;

        if (data.getInventoryOpenDurationMs() > maxInventoryCampMs()) return true;
        if (requiresCombat() && !inCombat()) return true;
        return false;
    }

    private boolean guardsBypassed() {
        return platform.getConfigRepository().configView().hasDeveloperOverride(OVERRIDE_BYPASS_HEURISTIC_GUARD);
    }

    private double applyDecay() {
        long now = System.currentTimeMillis();
        double elapsedSeconds = Math.max(0L, now - lastDecayAt) / 1000.0;
        lastDecayAt = now;
        if (elapsedSeconds <= 0.0) return buffer.get();
        double decay = elapsedSeconds * decayPerSecond();
        return decay <= 0.0 ? buffer.get() : buffer.decrease(decay);
    }
}
