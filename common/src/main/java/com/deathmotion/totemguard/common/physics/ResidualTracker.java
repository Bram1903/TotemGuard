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

package com.deathmotion.totemguard.common.physics;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.physics.sim.MovementInput;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.world.scan.BlockEnvironment;
import com.github.retrooper.packetevents.util.Vector3d;

import java.util.Locale;

public final class ResidualTracker {

    private static final double NOISE_FLOOR = 1.0e-6;

    private double landAscent = Double.NEGATIVE_INFINITY;
    private double landDescent = Double.NEGATIVE_INFINITY;
    private double landHorizontal = Double.NEGATIVE_INFINITY;
    private double fluidAscent = Double.NEGATIVE_INFINITY;
    private double fluidDescent = Double.NEGATIVE_INFINITY;

    public void observe(TGPlayer player, MovementCause cause, BlockEnvironment env, MovementInput input,
                        Vector3d observed, double ascent, double descent, double horizontal) {
        if (env.stuck() || env.climbable()) return;
        boolean fluid = env.fluid();

        boolean recAscent = ascent > NOISE_FLOOR && ascent > (fluid ? fluidAscent : landAscent);
        boolean recDescent = descent > NOISE_FLOOR && descent > (fluid ? fluidDescent : landDescent);
        boolean recHorizontal = !fluid && horizontal > NOISE_FLOOR && horizontal > landHorizontal;

        if (fluid) {
            fluidAscent = Math.max(fluidAscent, ascent);
            fluidDescent = Math.max(fluidDescent, descent);
        } else {
            landAscent = Math.max(landAscent, ascent);
            landDescent = Math.max(landDescent, descent);
            landHorizontal = Math.max(landHorizontal, horizontal);
        }

        if (!MovementDebug.enabled()) return;
        if (ascent <= NOISE_FLOOR && descent <= NOISE_FLOOR && horizontal <= NOISE_FLOOR) return;

        boolean newPeak = recAscent || recDescent || recHorizontal;
        StringBuilder sb = new StringBuilder(192);
        sb.append("[PhysRes] ").append(player.getUser().getName());
        sb.append(newPeak ? " PEAK " : " over ").append(cause).append(fluid ? " fluid" : " land");
        sb.append(" g=").append(input.groundedStart() ? 1 : 0).append('>').append(input.groundedEnd() ? 1 : 0);
        if (input.jumpPossible()) sb.append(" jmp=1");
        sb.append(String.format(Locale.ROOT, " gap=%.3f Vobs=%+.4f Hobs=%.4f",
                env.groundGap(), observed.getY(), Math.hypot(observed.getX(), observed.getZ())));
        sb.append(String.format(Locale.ROOT, " | resid asc=%+.5f desc=%+.5f hor=%+.5f", ascent, descent, horizontal));
        sb.append(" | peak asc=").append(fmt(fluid ? fluidAscent : landAscent))
                .append(" desc=").append(fmt(fluid ? fluidDescent : landDescent))
                .append(" hor=").append(fmt(landHorizontal));
        TGPlatform.getInstance().getLogger().info(sb.toString());
    }

    public void reset() {
        landAscent = Double.NEGATIVE_INFINITY;
        landDescent = Double.NEGATIVE_INFINITY;
        landHorizontal = Double.NEGATIVE_INFINITY;
        fluidAscent = Double.NEGATIVE_INFINITY;
        fluidDescent = Double.NEGATIVE_INFINITY;
    }

    private static String fmt(double v) {
        return Double.isFinite(v) ? String.format(Locale.ROOT, "%+.5f", v) : "n/a";
    }
}
