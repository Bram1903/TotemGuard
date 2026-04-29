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

import com.deathmotion.totemguard.api3.check.CheckType;
import com.deathmotion.totemguard.api3.event.Event;
import com.deathmotion.totemguard.common.check.HeuristicCheck;
import com.deathmotion.totemguard.common.check.annotations.CheckData;
import com.deathmotion.totemguard.common.check.type.EventCheck;
import com.deathmotion.totemguard.common.event.internal.impl.TotemReplenishedEvent;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.util.MathUtil;

import java.util.List;

@CheckData(description = "Suspicious totem delay consistency", type = CheckType.AUTO_TOTEM)
public class AutoTotemB extends HeuristicCheck implements EventCheck {

    private final static int SAMPLE_SIZE = 5;

    public AutoTotemB(TGPlayer player) {
        super(player);
    }

    @Override
    protected double flagThreshold() {
        return 4.0;
    }

    @Override
    protected double decayPerSecond() {
        return 0;
    }

    @Override
    public <T extends Event> void handleEvent(T event) {
        if (!(event instanceof TotemReplenishedEvent)) return;

        List<Long> recent = player.getTotemData().getIntervals().getLast(SAMPLE_SIZE);
        if (recent.size() < SAMPLE_SIZE) return;

        double stdDev = MathUtil.getStandardDeviation(recent);

        if (stdDev < 5.0) {
            punish(3.0);
        } else if (stdDev < 15.0) {
            punish(2.0);
        } else if (stdDev > 60.0) {
            reward(1.0);
        }
    }
}
