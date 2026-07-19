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

import com.deathmotion.totemguard.common.physics.medium.MediumKind;
import com.deathmotion.totemguard.common.physics.verdict.PhysicsVerdict;
import com.deathmotion.totemguard.common.player.data.Data;

import java.util.Map;

public final class MovementReporter {

    private final Flagger flagger;
    private final Data data;

    public MovementReporter(Flagger flagger, Data data) {
        this.flagger = flagger;
        this.data = data;
    }

    public void report(PhysicsVerdict verdict) {
        if (!verdict.mitigation().triggered() || verdict.breach() == null) return;

        double excess = Math.max(Math.max(verdict.horizontalExcess(), verdict.ascentExcess()),
                Math.max(verdict.descentExcess(), verdict.phaseExcess()));
        String type = classify(verdict);
        Map<String, Object> extras = Map.of("tg_physics_type", type);
        String shownType = type;
        if (verdict.mitigation().setbackIssued()) {
            shownType = type + " (setback)";
        } else if (verdict.mitigation().setbackSkipped()) {
            shownType = type + " (setback off)";
        }

        switch (verdict.breach()) {
            case GROUNDSPOOF -> flagger.flag(extras, "{0} | claims onGround, vy={1} | over={2}",
                    shownType, PhysicsFormat.d(verdict.observedY()), PhysicsFormat.d(excess));
            case HOVER ->
                    flagger.flag(extras, "{0} | airborne, not falling | over={1}", shownType, PhysicsFormat.d(excess));
            case DESCENT_FLOOR -> flagger.flag(extras, "{0} | vy={1} fell faster than gravity | over={2}",
                    shownType, PhysicsFormat.d(verdict.observedY()), PhysicsFormat.d(excess));
            case BOUNCE_RISE, FORCED_RISE -> flagger.flag(extras, "{0} | vy={1} required>={2} | over={3}",
                    shownType, PhysicsFormat.d(verdict.observedY()), PhysicsFormat.d(verdict.boundFloor()),
                    PhysicsFormat.d(excess));
            case PHASE_CROSS, PHASE_EMBED -> flagger.flag(extras, "{0} | moved {1} through a wall | over={2}",
                    shownType, PhysicsFormat.d(verdict.observedSpeed()), PhysicsFormat.d(excess));
            case FAST -> flagger.flag(extras, "{0} | speed={1} | over={2}",
                    shownType, PhysicsFormat.d(verdict.observedSpeed()), PhysicsFormat.d(excess));
            case ASCENT -> flagger.flag(extras, "{0} | vy={1} allowed<={2} | over={3}",
                    shownType, PhysicsFormat.d(verdict.observedY()), PhysicsFormat.d(verdict.boundCeiling()), PhysicsFormat.d(excess));
            case HORIZONTAL_DISK -> flagger.flag(extras, "{0} | speed={1} allowed<={2} | over={3}",
                    shownType, PhysicsFormat.d(verdict.observedSpeed()),
                    PhysicsFormat.d(Math.hypot(verdict.boundCenterX(), verdict.boundCenterZ()) + verdict.boundRadius()),
                    PhysicsFormat.d(excess));
            case KNOCKBACK -> flagger.flag(extras, "{0} | ignored knockback | off={1}",
                    shownType, PhysicsFormat.d(excess));
            case MOTION_SILENCE -> flagger.flag(extras, "{0} | client keeps ticking, movement packets withheld",
                    shownType);
        }
    }

    private String classify(PhysicsVerdict verdict) {
        return switch (verdict.breach()) {
            case GROUNDSPOOF -> "groundspoof";
            case HOVER -> "hover";
            case DESCENT_FLOOR -> "fastfall";
            case BOUNCE_RISE -> "nobounce";
            case FORCED_RISE -> "nolevitation";
            case PHASE_CROSS, PHASE_EMBED -> "phase";
            case FAST -> "speed";
            case ASCENT -> switch (verdict.medium()) {
                case WATER, LAVA -> "water-fly";
                case CLIMB -> "climb-fly";
                case GLIDE -> "glide-fly";
                default -> "fly";
            };
            case HORIZONTAL_DISK -> {
                if (verdict.inventoryOpen()) yield "inventory-move";
                if (verdict.improperSprint()) yield "sprint";
                if (verdict.medium() == MediumKind.WATER || verdict.medium() == MediumKind.LAVA) yield "water-speed";
                if (verdict.medium() == MediumKind.GLIDE) yield "glide-speed";
                yield data.isSneaking() ? "nosneak" : "speed";
            }
            case KNOCKBACK -> "velocity";
            case MOTION_SILENCE -> "silence";
        };
    }
}
