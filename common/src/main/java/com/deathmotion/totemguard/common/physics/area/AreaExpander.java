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

import com.deathmotion.totemguard.common.physics.ground.GroundFacts;
import com.deathmotion.totemguard.common.physics.push.KnockbackTracker;
import com.deathmotion.totemguard.common.physics.push.PistonWindow;
import com.deathmotion.totemguard.common.physics.push.RiptideWindow;
import com.deathmotion.totemguard.common.physics.input.PlayerInput;
import com.deathmotion.totemguard.common.physics.medium.BubbleLift;
import com.deathmotion.totemguard.common.physics.medium.FlowSolver;
import com.deathmotion.totemguard.common.physics.medium.MediumKind;
import com.deathmotion.totemguard.common.physics.medium.MediumModel;
import com.deathmotion.totemguard.common.physics.medium.MediumSample;
import com.deathmotion.totemguard.common.physics.medium.StuckFactor;
import com.deathmotion.totemguard.common.physics.preset.PhysicsPreset;
import com.deathmotion.totemguard.common.physics.collision.ContactReport;

public final class AreaExpander {

    private static final double OVERLAP_SHOVE = 0.25;

    private AreaExpander() {
    }

    public static void grow(MotionArea area, MediumModel medium, MediumSample sample,
                            PlayerInput input, GroundFacts ground, ContactReport contact,
                            StuckFactor stuckFactor, BubbleLift bubble,
                            KnockbackTracker knockback, PistonWindow pistons, RiptideWindow riptide,
                            ResidualCarry carry, PhysicsPreset preset, AreaBounds bounds) {
        bounds.reset(area);
        medium.horizontalOptions(input, ground, bounds);
        medium.verticalOptions(input, ground, contact, bounds);
        applyFluidPush(area, sample, bounds);

        if (sample.stuck() && !sample.fluid()) stuckFactor.apply(bounds, sample);
        if (medium.kind() == MediumKind.LAND) stuckFactor.applyArrestWindow(bounds);
        bubble.apply(bounds);

        bounds.expandRadius(preset.horizontalNoisePad() + carry.horizontal());
        bounds.ceiling(bounds.ceiling() + preset.verticalNoisePad() + carry.vertical());
        bounds.addDescentSlack(preset.verticalNoisePad());
        if (medium.kind() == MediumKind.WATER || medium.kind() == MediumKind.LAVA) {
            bounds.addDescentSlack(preset.fluidDescentSlack());
        }
        if (ground.groundedEnd()) bounds.addDescentSlack(input.stepHeight());
        if (contact.startOverlapping()) bounds.expandRadius(OVERLAP_SHOVE);

        knockback.apply(bounds, preset.knockbackPad());
        riptide.apply(bounds, input);
        pistons.apply(bounds);
    }

    private static void applyFluidPush(MotionArea area, MediumSample sample, AreaBounds bounds) {
        if (!sample.pushed()) return;
        double px = sample.pushX();
        double py = sample.pushY();
        double pz = sample.pushZ();
        double length = Math.sqrt(px * px + py * py + pz * pz);
        double kick = FlowSolver.minKickScale(length, area.centerX(), area.centerZ());
        if (kick != 1.0) {
            px *= kick;
            py *= kick;
            pz *= kick;
        }
        if (length < FlowSolver.MIN_PUSH) {
            bounds.expandRadius(FlowSolver.MIN_PUSH - length);
        }
        bounds.centerX(bounds.centerX() + px);
        bounds.centerZ(bounds.centerZ() + pz);
        if (py < 0.0) bounds.floor(bounds.floor() + py);
        else if (py > 0.0) bounds.ceiling(bounds.ceiling() + py);
    }
}
