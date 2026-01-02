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


package com.deathmotion.totemguard.common.check.impl.autototem;

import com.deathmotion.totemguard.api.check.CheckType;
import com.deathmotion.totemguard.api.event.Event;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.check.CheckData;
import com.deathmotion.totemguard.common.check.CheckImpl;
import com.deathmotion.totemguard.common.check.type.EventCheck;
import com.deathmotion.totemguard.common.event.internal.impl.TotemReplenishedEvent;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.util.MathUtil;

import java.util.List;

@CheckData(description = "Suspicious replenish delay consistency", type = CheckType.AUTO_TOTEM)
public class AutoTotemB extends CheckImpl implements EventCheck {

    private static final int MIN_INTERVALS = 3;

    public AutoTotemB(TGPlayer player) {
        super(player);
    }

    @Override
    public <T extends Event> void handleEvent(T event) {
        if (!(event instanceof TotemReplenishedEvent)) return;

        List<Long> intervals = player.getTotemData().getIntervals().getLast(MIN_INTERVALS);
        int n = intervals.size();

        if (n < MIN_INTERVALS) {
            log("skip: not enough delays (" + n + "/" + MIN_INTERVALS + ")");
            return;
        }

        double standardDeviation = MathUtil.getStandardDeviation(intervals);
        log("stdDev=" + standardDeviation + " samples=" + intervals);
    }

    private void log(String msg) {
        TGPlatform.getInstance().getLogger().info("[AutoTotemB] " + player.getName() + " " + msg);
    }
}
