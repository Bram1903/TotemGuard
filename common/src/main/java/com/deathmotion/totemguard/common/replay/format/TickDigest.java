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

package com.deathmotion.totemguard.common.replay.format;

import com.deathmotion.totemguard.common.physics.trace.TraceFrame;
import org.jetbrains.annotations.Nullable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public record TickDigest(
        long tick,
        byte stream,
        byte outcome,
        byte reason,
        byte breach,
        byte medium,
        byte ground,
        double obsX,
        double obsY,
        double obsZ,
        double centerX,
        double centerZ,
        double radius,
        double floor,
        double ceiling,
        double horizontalExcess,
        double ascentExcess,
        double descentExcess,
        double phaseExcess,
        int flags,
        long contributors,
        long taints,
        byte chosenSlot,
        byte liveCount,
        byte mitigation,
        double buffer
) {
    public static final String[] FIELDS = {
            "tick", "stream", "outcome", "reason", "breach", "medium", "ground",
            "obsX", "obsY", "obsZ", "centerX", "centerZ", "radius", "floor", "ceiling",
            "horizontalExcess", "ascentExcess", "descentExcess", "phaseExcess",
            "flags", "contributors", "taints", "chosenSlot", "liveCount", "mitigation", "buffer"
    };

    public static TickDigest of(TraceFrame frame) {
        return new TickDigest(frame.tick, frame.stream, frame.outcome, frame.reason, frame.breach,
                frame.medium, frame.ground, frame.obsX, frame.obsY, frame.obsZ,
                frame.centerX, frame.centerZ, frame.radius, frame.floor, frame.ceiling,
                frame.horizontalExcess, frame.ascentExcess, frame.descentExcess, frame.phaseExcess,
                frame.flags, frame.contributors, frame.taints, frame.chosenSlot, frame.liveCount,
                frame.mitigation, frame.buffer);
    }

    public static TickDigest read(DataInputStream in) throws IOException {
        long tick = VarCodec.readUnsigned(in);
        byte stream = in.readByte();
        byte outcome = in.readByte();
        byte reason = in.readByte();
        byte breach = in.readByte();
        byte medium = in.readByte();
        byte ground = in.readByte();
        int present = in.readUnsignedShort();
        double[] doubles = new double[13];
        for (int i = 0; i < doubles.length; i++) {
            doubles[i] = (present & (1 << i)) == 0 ? 0.0 : Double.longBitsToDouble(in.readLong());
        }
        int flags = VarCodec.readInt(in);
        long contributors = VarCodec.readUnsigned(in);
        long taints = VarCodec.readUnsigned(in);
        return new TickDigest(tick, stream, outcome, reason, breach, medium, ground,
                doubles[0], doubles[1], doubles[2], doubles[3], doubles[4], doubles[5], doubles[6],
                doubles[7], doubles[8], doubles[9], doubles[10], doubles[11],
                flags, contributors, taints, in.readByte(), in.readByte(), in.readByte(), doubles[12]);
    }

    public void write(DataOutputStream out) throws IOException {
        VarCodec.writeUnsigned(out, tick);
        out.writeByte(stream);
        out.writeByte(outcome);
        out.writeByte(reason);
        out.writeByte(breach);
        out.writeByte(medium);
        out.writeByte(ground);

        double[] doubles = {obsX, obsY, obsZ, centerX, centerZ, radius, floor, ceiling,
                horizontalExcess, ascentExcess, descentExcess, phaseExcess, buffer};
        int present = 0;
        for (int i = 0; i < doubles.length; i++) {
            if (Double.doubleToRawLongBits(doubles[i]) != 0L) present |= 1 << i;
        }
        out.writeShort(present);
        for (int i = 0; i < doubles.length; i++) {
            if ((present & (1 << i)) != 0) out.writeLong(Double.doubleToRawLongBits(doubles[i]));
        }

        VarCodec.writeInt(out, flags);
        VarCodec.writeUnsigned(out, contributors);
        VarCodec.writeUnsigned(out, taints);
        out.writeByte(chosenSlot);
        out.writeByte(liveCount);
        out.writeByte(mitigation);
    }

    public @Nullable String firstDifference(TickDigest other) {
        Object[] mine = values();
        Object[] theirs = other.values();
        for (int i = 0; i < mine.length; i++) {
            if (!mine[i].equals(theirs[i])) return FIELDS[i];
        }
        return null;
    }

    public Object[] values() {
        return new Object[]{
                tick, stream, outcome, reason, breach, medium, ground,
                Double.doubleToRawLongBits(obsX), Double.doubleToRawLongBits(obsY), Double.doubleToRawLongBits(obsZ),
                Double.doubleToRawLongBits(centerX), Double.doubleToRawLongBits(centerZ),
                Double.doubleToRawLongBits(radius), Double.doubleToRawLongBits(floor),
                Double.doubleToRawLongBits(ceiling),
                Double.doubleToRawLongBits(horizontalExcess), Double.doubleToRawLongBits(ascentExcess),
                Double.doubleToRawLongBits(descentExcess), Double.doubleToRawLongBits(phaseExcess),
                flags, contributors, taints, chosenSlot, liveCount, mitigation,
                Double.doubleToRawLongBits(buffer)
        };
    }

    public String toGoldenRow() {
        StringBuilder row = new StringBuilder(256);
        Object[] values = values();
        row.append(tick);
        for (int i = 1; i < FIELDS.length; i++) {
            row.append(' ').append(FIELDS[i]).append('=');
            appendValue(row, i, values[i]);
        }
        return row.toString();
    }

    private void appendValue(StringBuilder row, int index, Object raw) {
        String field = FIELDS[index];
        switch (field) {
            case "obsX" -> row.append(obsX);
            case "obsY" -> row.append(obsY);
            case "obsZ" -> row.append(obsZ);
            case "centerX" -> row.append(centerX);
            case "centerZ" -> row.append(centerZ);
            case "radius" -> row.append(radius);
            case "floor" -> row.append(floor);
            case "ceiling" -> row.append(ceiling);
            case "horizontalExcess" -> row.append(horizontalExcess);
            case "ascentExcess" -> row.append(ascentExcess);
            case "descentExcess" -> row.append(descentExcess);
            case "phaseExcess" -> row.append(phaseExcess);
            default -> row.append(raw);
        }
    }
}
