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

package com.deathmotion.totemguard.common.player.movement;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.movement.area.MotionArea;
import com.deathmotion.totemguard.common.player.movement.sim.MovementInput;
import com.deathmotion.totemguard.common.player.movement.world.BlockEnvironment;
import com.github.retrooper.packetevents.util.Vector3d;

import java.util.Locale;

public final class MovementDebug {

    private MovementDebug() {
    }

    public static boolean enabled() {
        return TGPlatform.getInstance().getConfigRepository().configView().physicsEngineDebug();
    }

    public static void log(TGPlayer player, String phase, Vector3d observed, MovementInput in,
                           BlockEnvironment env, MotionArea predicted, double horizontalExcess, double verticalExcess) {
        if (!enabled()) return;

        StringBuilder sb = new StringBuilder(160);
        sb.append("[PhysDbg] ").append(player.getUser().getName()).append(' ').append(phase);
        if (in != null) {
            sb.append(" g=").append(in.groundedStart() ? 1 : 0).append('>').append(in.groundedEnd() ? 1 : 0);
            sb.append(" jmp=").append(in.jumpPossible() ? 1 : 0);
            sb.append(" spr=").append(in.sprinting() ? 1 : 0);
        }
        if (env != null) {
            sb.append(" med=").append(medium(env));
            sb.append(String.format(Locale.ROOT, " gap=%.3f", env.groundGap()));
        }
        sb.append(String.format(Locale.ROOT, " | H obs=%.3f", Math.hypot(observed.getX(), observed.getZ())));
        if (predicted != null) {
            sb.append(String.format(Locale.ROOT, " cap<=%.3f ex=%.3f", predicted.horizontalSpeed().max(), horizontalExcess));
        }
        sb.append(String.format(Locale.ROOT, " | V obs=%+.3f", observed.getY()));
        if (predicted != null) {
            sb.append(String.format(Locale.ROOT, " in[%+.3f,%+.3f] ex=%.3f",
                    predicted.vertical().min(), predicted.vertical().max(), verticalExcess));
        }
        TGPlatform.getInstance().getLogger().info(sb.toString());
    }

    private static String medium(BlockEnvironment env) {
        if (env.fluid()) return "fluid";
        if (env.stuck()) return "stuck";
        if (env.climbable()) return "climb";
        if (env.bounceFactor() > 0.0) return "bounce";
        return "land";
    }
}
