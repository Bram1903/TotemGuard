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

package com.deathmotion.totemguard.common.check.impl.physics.report;

import com.deathmotion.totemguard.common.physics.verdict.FallFinding;
import com.deathmotion.totemguard.common.physics.verdict.PhysicsVerdict;

import java.util.Map;

public final class FallReporter {

    private final Flagger flagger;

    public FallReporter(Flagger flagger) {
        this.flagger = flagger;
    }

    public void report(PhysicsVerdict verdict) {
        FallFinding fall = verdict.fall();
        if (!fall.violation()) return;

        String shownType = fall.damageApplied() ? "nofall (damaged)" : "nofall";
        flagger.flag(Map.of("tg_physics_type", "nofall"),
                "{0} | fell {1} blocks, dodged {2} damage", shownType,
                String.format("%.1f", fall.fallDistance()),
                String.format("%.1f", fall.avoidedDamage()));
    }
}
