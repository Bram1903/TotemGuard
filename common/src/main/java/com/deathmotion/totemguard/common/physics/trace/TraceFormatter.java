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

package com.deathmotion.totemguard.common.physics.trace;

import com.deathmotion.totemguard.common.physics.body.BodyKind;
import com.deathmotion.totemguard.common.physics.ground.GroundState;
import com.deathmotion.totemguard.common.physics.medium.MediumKind;
import com.deathmotion.totemguard.common.physics.preset.PhysicsDebugContext;
import com.deathmotion.totemguard.common.physics.verdict.BoundBreach;
import com.deathmotion.totemguard.common.physics.verdict.DeclineReason;
import com.deathmotion.totemguard.common.physics.verdict.TickOutcome;

import java.util.Locale;
import java.util.Set;

public final class TraceFormatter {

    private static final double FLUID_DEFLATE = 0.001;

    private static final TickOutcome[] OUTCOMES = TickOutcome.values();
    private static final BodyKind[] BODIES = BodyKind.values();
    private static final DeclineReason[] REASONS = DeclineReason.values();
    private static final BoundBreach[] BREACHES = BoundBreach.values();
    private static final MediumKind[] MEDIUMS = MediumKind.values();
    private static final GroundState[] GROUND_STATES = GroundState.values();

    private TraceFormatter() {
    }

    public static String format(TraceFrame f, Set<PhysicsDebugContext> contexts) {
        StringBuilder sb = new StringBuilder(220);
        sb.append("t").append(f.tick);
        if (f.stream == 1) sb.append(" VEH:").append(name(BODIES, f.body));
        sb.append(String.format(Locale.ROOT, " | obs %+.4f %+.4f %+.4f", f.obsX, f.obsY, f.obsZ));
        sb.append(String.format(Locale.ROOT, " | disk c(%+.3f,%+.3f) r%.3f v[%.3f,%.3f]",
                f.centerX, f.centerZ, f.radius, f.floor, f.ceiling));
        if (f.altPresent) {
            sb.append(String.format(Locale.ROOT, " alt(%+.3f,%+.3f)", f.altCenterX, f.altCenterZ));
        }
        sb.append(String.format(Locale.ROOT, " | rot y%+.2f p%+.2f (prev y%+.2f p%+.2f)",
                f.yaw, f.pitch, f.prevYaw, f.prevPitch));
        sb.append(String.format(Locale.ROOT, " | pre c(%+.3f,%+.3f) v[%.3f,%.3f]",
                f.preCarriedX, f.preCarriedZ, f.preCarriedFloor, f.preCarriedCeil));
        sb.append(String.format(Locale.ROOT, " | ex h%.3f a%.3f d%.3f p%.3f",
                f.horizontalExcess, f.ascentExcess, f.descentExcess, f.phaseExcess));
        sb.append(" | ").append(name(OUTCOMES, f.outcome));
        if (f.reason >= 0) sb.append(':').append(name(REASONS, f.reason));
        if (f.breach >= 0) sb.append(" BREACH:").append(name(BREACHES, f.breach));
        sb.append(' ').append(name(MEDIUMS, f.medium)).append('/').append(name(GROUND_STATES, f.ground));
        if (f.liveCount > 1) sb.append(" hyp=").append(f.chosenSlot).append('/').append(f.liveCount);
        sb.append(f.has(TraceFrame.FLAG_GROUNDED_END) ? " gnd" : " air");
        sb.append(String.format(Locale.ROOT, " gap%.3f ceil%.2f", cap(f.supportGap), cap(f.ceilingClearance)));
        if (f.stuckHorizontal != 1.0 || f.stuckVertical != 1.0) {
            sb.append(String.format(Locale.ROOT, " stk(%.2f,%.2f)", f.stuckHorizontal, f.stuckVertical));
        }
        sb.append(" | in ");
        appendFlags(sb, f);
        sb.append(String.format(Locale.ROOT, " | rd %d/%d/%d | buf %.1f", f.reads, f.misses, f.uncertainHits, f.buffer));
        if (f.contributors != 0L) sb.append(" | wid ").append(TraceFrame.describeWidenings(f.contributors));
        if (f.engineFall > 0.0) sb.append(String.format(Locale.ROOT, " fall %.1f", f.engineFall));
        if (f.mitigation != 0) sb.append(" | mit ").append(Integer.toBinaryString(f.mitigation & 0xF));
        if (!contexts.isEmpty()) appendContexts(sb, f, contexts);
        return sb.toString();
    }

    private static void appendContexts(StringBuilder sb, TraceFrame f, Set<PhysicsDebugContext> contexts) {
        if (contexts.contains(PhysicsDebugContext.LAND)) {
            sb.append(String.format(Locale.ROOT, " | land move%.3f jmp%.3f step%.2f stk(%.2f,%.2f)",
                    f.moveSpeed, f.jumpStrength, f.stepHeight,
                    f.stuckHorizontal, f.stuckVertical));
        }
        if (contexts.contains(PhysicsDebugContext.WATER)) {
            sb.append(String.format(Locale.ROOT, " | water fric%.3f acc%.3f push(%+.3f,%+.3f,%+.3f) bub%.3f",
                    f.fluidFriction, f.fluidAccel, f.pushX, f.pushY, f.pushZ, f.bubbleAscent));
            if (f.has(TraceFrame.FLAG_SWIMMING)) sb.append(" swim");
            if (f.has(TraceFrame.FLAG_SWIM_STEER)) sb.append(" steer");
            if (f.has(TraceFrame.FLAG_FLUID_HOP)) sb.append(" hop");
        }
        if (contexts.contains(PhysicsDebugContext.FLUIDBOX)) {
            sb.append(String.format(Locale.ROOT,
                    " | fbox feet(%.5f,%.5f,%.5f) head%.5f h%.4f eye%.5f",
                    f.boxMinX + (f.boxMaxX - f.boxMinX) / 2.0, f.boxFeetY,
                    f.boxMinZ + (f.boxMaxZ - f.boxMinZ) / 2.0,
                    f.boxHeadY, f.boxHeadY - f.boxFeetY, f.eyeSampleY));
            sb.append(String.format(Locale.ROOT, " cells y[%d..%d] x[%d..%d] z[%d..%d]",
                    f.fluidCellY0, f.fluidCellY1, f.fluidCellX0, f.fluidCellX1, f.fluidCellZ0, f.fluidCellZ1));
            int my0 = floor(f.boxFeetY + FLUID_DEFLATE), my1 = ceil(f.boxHeadY - FLUID_DEFLATE) - 1;
            int mx0 = floor(f.boxMinX + FLUID_DEFLATE), mx1 = ceil(f.boxMaxX - FLUID_DEFLATE) - 1;
            int mz0 = floor(f.boxMinZ + FLUID_DEFLATE), mz1 = ceil(f.boxMaxZ - FLUID_DEFLATE) - 1;
            if (my0 != f.fluidCellY0 || my1 != f.fluidCellY1 || mx0 != f.fluidCellX0
                    || mx1 != f.fluidCellX1 || mz0 != f.fluidCellZ0 || mz1 != f.fluidCellZ1) {
                sb.append(String.format(Locale.ROOT, " MISMATCH-mc y[%d..%d] x[%d..%d] z[%d..%d]",
                        my0, my1, mx0, mx1, mz0, mz1));
            }
            if (f.wetCellFound) {
                sb.append(String.format(Locale.ROOT, " wet(%d,%d,%d) surf%.5f need%.5f",
                        f.wetCellX, f.wetCellY, f.wetCellZ, f.wetCellSurface, f.boxFeetY + FLUID_DEFLATE));
            } else {
                sb.append(" dry");
            }
            if (f.has(TraceFrame.FLAG_RAW_SPRINT)) sb.append(" rspr");
            if (f.has(TraceFrame.FLAG_EYE_IN_WATER)) sb.append(" eyewet");
            if (f.has(TraceFrame.FLAG_WATER_AT_FEET)) sb.append(" feetwet");
            if (f.has(TraceFrame.FLAG_SWIMMING)) sb.append(" swim");
            sb.append(String.format(Locale.ROOT, " echo%s(spr%d swim%d)",
                    f.has(TraceFrame.FLAG_ECHO_LANDED) ? "!" : "",
                    f.has(TraceFrame.FLAG_ECHO_SPRINT) ? 1 : 0,
                    f.has(TraceFrame.FLAG_ECHO_SWIM) ? 1 : 0));
        }
        if (contexts.contains(PhysicsDebugContext.LAVA)) {
            sb.append(String.format(Locale.ROOT, " | lava fric%.3f acc%.3f push(%+.3f,%+.3f,%+.3f)",
                    f.fluidFriction, f.fluidAccel, f.pushX, f.pushY, f.pushZ));
            if (f.has(TraceFrame.FLAG_FLUID_HOP)) sb.append(" hop");
        }
        if (contexts.contains(PhysicsDebugContext.CLIMB)) {
            sb.append(" | climb");
            if (f.has(TraceFrame.FLAG_CLIMBABLE)) sb.append(" able");
            if (f.has(TraceFrame.FLAG_CLIMB_UNCERTAIN)) sb.append(" unc");
            if (f.has(TraceFrame.FLAG_WALL_NEAR)) sb.append(" wall");
            sb.append(String.format(Locale.ROOT, " gap%.3f", cap(f.supportGap)));
            if (f.has(TraceFrame.FLAG_FLUID_HOP)) sb.append(" hop");
        }
        if (contexts.contains(PhysicsDebugContext.GLIDE)) {
            sb.append(String.format(Locale.ROOT, " | glide pitch%+.2f fw%d/%d", f.pitch, f.fireworkMin, f.fireworkMax));
            if (f.has(TraceFrame.FLAG_GLIDE_CLAIM)) sb.append(" claim");
            if (f.has(TraceFrame.FLAG_GLIDE_RIPTIDE))
                sb.append(String.format(Locale.ROOT, " rip%.2f", f.riptideStrength));
            if (f.has(TraceFrame.FLAG_GLIDE_EXIT)) sb.append(" exit");
        }
    }

    private static void appendFlags(StringBuilder sb, TraceFrame f) {
        int before = sb.length();
        if (f.has(TraceFrame.FLAG_SPRINT)) sb.append("SPR,");
        if (f.has(TraceFrame.FLAG_SNEAK)) sb.append("SNK,");
        if (f.has(TraceFrame.FLAG_JUMP_POSSIBLE)) sb.append("JMP,");
        if (f.has(TraceFrame.FLAG_INVENTORY_OPEN)) sb.append("INV,");
        if (f.has(TraceFrame.FLAG_CLAIMED_GROUND)) sb.append("CLM,");
        if (f.has(TraceFrame.FLAG_WALL_NEAR)) sb.append("WAL,");
        if (f.has(TraceFrame.FLAG_START_OVERLAP)) sb.append("OVL,");
        if (f.has(TraceFrame.FLAG_BUBBLE)) sb.append("BUB,");
        if (f.has(TraceFrame.FLAG_STUCK)) sb.append("STK,");
        if (f.has(TraceFrame.FLAG_ARRESTED)) sb.append("ARR,");
        if (f.has(TraceFrame.FLAG_STEP_USED)) sb.append("STP,");
        if (f.has(TraceFrame.FLAG_ALT_CENTER)) sb.append("ALT,");
        if (sb.length() == before) {
            sb.append('-');
        } else {
            sb.setLength(sb.length() - 1);
        }
    }

    private static double cap(double value) {
        return Math.min(value, 9.999);
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }

    private static int ceil(double value) {
        return (int) Math.ceil(value);
    }

    private static String name(Enum<?>[] values, byte ordinal) {
        return ordinal >= 0 && ordinal < values.length ? values[ordinal].name() : "?";
    }
}
