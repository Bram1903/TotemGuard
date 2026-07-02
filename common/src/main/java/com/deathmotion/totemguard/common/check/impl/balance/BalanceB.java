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

package com.deathmotion.totemguard.common.check.impl.balance;

import com.deathmotion.totemguard.api.check.CheckType;
import com.deathmotion.totemguard.common.check.annotations.CheckData;
import com.deathmotion.totemguard.common.player.TGPlayer;

@CheckData(description = "Banking game ticks behind artificial latency", type = CheckType.TICK)
public class BalanceB extends BalanceA {

    private static final long MAX_CREDIT_NANOS = 1_000_000_000L;

    public BalanceB(TGPlayer player) {
        super(player);
    }

    @Override
    protected long floorNanos(long now) {
        long limit = now - MAX_CREDIT_NANOS - CLOCK_DRIFT_NANOS;
        return Math.max(super.floorNanos(now), limit);
    }

    @Override
    protected boolean shouldReport(long now) {
        return anchorNanos < now - MAX_CREDIT_NANOS;
    }
}
