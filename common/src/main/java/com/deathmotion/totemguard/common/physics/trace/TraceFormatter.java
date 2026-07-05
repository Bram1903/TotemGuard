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

import com.deathmotion.totemguard.common.physics.ground.GroundState;
import com.deathmotion.totemguard.common.physics.medium.MediumKind;
import com.deathmotion.totemguard.common.physics.verdict.BoundBreach;
import com.deathmotion.totemguard.common.physics.verdict.DeclineReason;
import com.deathmotion.totemguard.common.physics.verdict.TickOutcome;

import java.util.Locale;

public final class TraceFormatter {

    private static final TickOutcome[] OUTCOMES = TickOutcome.values();
    private static final DeclineReason[] REASONS = DeclineReason.values();
    private static final BoundBreach[] BREACHES = BoundBreach.values();
    private static final MediumKind[] MEDIUMS = MediumKind.values();
    private static final GroundState[] GROUND_STATES = GroundState.values();

    private TraceFormatter() {
    }

    public static String format(TraceFrame f) {
        StringBuilder sb = new StringBuilder(220);
        sb.append("t").append(f.tick);
        sb.append(String.format(Locale.ROOT, " | obs %+.4f %+.4f %+.4f", f.obsX, f.obsY, f.obsZ));
        sb.append(String.format(Locale.ROOT, " | disk c(%+.3f,%+.3f) r%.3f v[%.3f,%.3f]",
                f.centerX, f.centerZ, f.radius, f.floor, f.ceiling));
        sb.append(String.format(Locale.ROOT, " | ex h%.3f a%.3f d%.3f p%.3f",
                f.horizontalExcess, f.ascentExcess, f.descentExcess, f.phaseExcess));
        sb.append(" | ").append(name(OUTCOMES, f.outcome));
        if (f.reason >= 0) sb.append(':').append(name(REASONS, f.reason));
        if (f.breach >= 0) sb.append(" BREACH:").append(name(BREACHES, f.breach));
        sb.append(' ').append(name(MEDIUMS, f.medium)).append('/').append(name(GROUND_STATES, f.ground));
        sb.append(f.has(TraceFrame.FLAG_GROUNDED_END) ? " gnd" : " air");
        sb.append(String.format(Locale.ROOT, " gap%.3f ceil%.2f", cap(f.supportGap), cap(f.ceilingClearance)));
        sb.append(" | in ");
        appendFlags(sb, f);
        sb.append(String.format(Locale.ROOT, " | rd %d/%d/%d | buf %.1f", f.reads, f.misses, f.uncertainHits, f.buffer));
        if (f.engineFall > 0.0) sb.append(String.format(Locale.ROOT, " fall %.1f", f.engineFall));
        if (f.mitigation != 0) sb.append(" | mit ").append(Integer.toBinaryString(f.mitigation & 0xF));
        return sb.toString();
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

    private static String name(Enum<?>[] values, byte ordinal) {
        return ordinal >= 0 && ordinal < values.length ? values[ordinal].name() : "?";
    }
}
