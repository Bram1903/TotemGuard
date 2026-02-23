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

package com.deathmotion.totemguard.common.check;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public record CheckSnapshot(String checkName, double buffer, int violations) {

    public static CheckSnapshot read(DataInput in) throws IOException {
        int len = in.readInt();
        byte[] name = new byte[len];
        in.readFully(name);

        String checkName = new String(name, StandardCharsets.UTF_8);
        double buffer = in.readDouble();
        int violations = in.readInt();

        return new CheckSnapshot(checkName, buffer, violations);
    }

    public static byte[] writeList(List<CheckSnapshot> snapshots) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);

        out.writeInt(snapshots.size());
        for (CheckSnapshot snapshot : snapshots) {
            snapshot.write(out);
        }

        out.flush();
        return baos.toByteArray();
    }

    public static List<CheckSnapshot> readList(byte[] data) throws IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));

        int count = in.readInt();
        List<CheckSnapshot> result = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            result.add(read(in));
        }

        return result;
    }

    public void write(DataOutput out) throws IOException {
        byte[] name = checkName.getBytes(StandardCharsets.UTF_8);
        out.writeInt(name.length);
        out.write(name);
        out.writeDouble(buffer);
        out.writeInt(violations);
    }
}