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

package com.deathmotion.totemguard.common.cache;

import com.deathmotion.totemguard.api.punishment.PunishmentType;
import com.deathmotion.totemguard.api.stats.StatsSnapshot;
import com.deathmotion.totemguard.common.cache.data.CheckSnapshot;
import com.deathmotion.totemguard.common.cache.data.FocusTarget;
import com.deathmotion.totemguard.common.database.model.AlertCheckSummary;
import com.deathmotion.totemguard.common.database.model.AlertRecord;
import com.deathmotion.totemguard.common.database.model.PunishmentRecord;
import com.deathmotion.totemguard.common.database.model.StaffAlertPref;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class CacheCodecs {

    public static final Codec<Boolean> BOOLEAN = new Codec<>() {
        @Override
        public byte[] encode(Boolean value) {
            return new byte[]{(byte) (value ? 1 : 0)};
        }

        @Override
        public Boolean decode(byte[] bytes) {
            return bytes.length > 0 && bytes[0] != 0;
        }
    };

    public static final Codec<StaffAlertPref> STAFF_ALERT_PREF = new Codec<>() {
        @Override
        public byte[] encode(StaffAlertPref value) {
            int bits = (value.enabled() ? 0x1 : 0) | (value.localOnly() ? 0x2 : 0);
            return new byte[]{(byte) bits};
        }

        @Override
        public StaffAlertPref decode(byte[] bytes) {
            int bits = bytes.length > 0 ? bytes[0] : 0;
            return new StaffAlertPref((bits & 0x1) != 0, (bits & 0x2) != 0);
        }
    };

    public static final Codec<Long> LONG = new Codec<>() {
        @Override
        public byte[] encode(Long value) throws Exception {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(8);
            new DataOutputStream(baos).writeLong(value);
            return baos.toByteArray();
        }

        @Override
        public Long decode(byte[] bytes) throws Exception {
            return new DataInputStream(new ByteArrayInputStream(bytes)).readLong();
        }
    };
    public static final Codec<Integer> INT = new Codec<>() {
        @Override
        public byte[] encode(Integer value) throws Exception {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(4);
            new DataOutputStream(baos).writeInt(value);
            return baos.toByteArray();
        }

        @Override
        public Integer decode(byte[] bytes) throws Exception {
            return new DataInputStream(new ByteArrayInputStream(bytes)).readInt();
        }
    };
    public static final Codec<List<CheckSnapshot>> CHECK_SNAPSHOTS = new Codec<>() {
        @Override
        public byte[] encode(List<CheckSnapshot> list) throws Exception {
            return encodeList(list, (out, s) -> {
                writeUtf8(out, s.checkName());
                out.writeDouble(s.buffer());
                out.writeInt(s.violations());
            });
        }

        @Override
        public List<CheckSnapshot> decode(byte[] bytes) throws Exception {
            return decodeList(bytes, in -> new CheckSnapshot(readUtf8(in), in.readDouble(), in.readInt()));
        }
    };

    public static final Codec<List<AlertRecord>> ALERT_RECORDS = new Codec<>() {
        @Override
        public byte[] encode(List<AlertRecord> list) throws Exception {
            return encodeList(list, (out, r) -> {
                out.writeLong(r.id());
                writeUtf8(out, r.checkName());
                writeUtf8(out, r.serverName());
                out.writeInt(r.violations());
                writeNullableUtf8(out, r.debug());
                writeNullableInt(out, r.keepalivePing());
                writeNullableInt(out, r.transactionPing());
                writeNullableUtf8(out, r.clientBrand());
                writeNullableInt(out, r.clientVersion());
                out.writeLong(r.createdAt());
            });
        }

        @Override
        public List<AlertRecord> decode(byte[] bytes) throws Exception {
            return decodeList(bytes, in -> new AlertRecord(
                    in.readLong(),
                    readUtf8(in),
                    readUtf8(in),
                    in.readInt(),
                    readNullableUtf8(in),
                    readNullableInt(in),
                    readNullableInt(in),
                    readNullableUtf8(in),
                    readNullableInt(in),
                    in.readLong()
            ));
        }
    };
    public static final Codec<List<PunishmentRecord>> PUNISHMENT_RECORDS = new Codec<>() {
        @Override
        public byte[] encode(List<PunishmentRecord> list) throws Exception {
            return encodeList(list, (out, r) -> {
                out.writeLong(r.id());
                writeUtf8(out, r.checkName());
                writeUtf8(out, r.serverName());
                out.writeInt(r.type().ordinal());
                writeUtf8(out, r.command());
                writeNullableUtf8(out, r.debug());
                out.writeLong(r.createdAt());
            });
        }

        @Override
        public List<PunishmentRecord> decode(byte[] bytes) throws Exception {
            PunishmentType[] types = PunishmentType.values();
            return decodeList(bytes, in -> {
                long id = in.readLong();
                String checkName = readUtf8(in);
                String serverName = readUtf8(in);
                int typeOrdinal = in.readInt();
                PunishmentType type = (typeOrdinal >= 0 && typeOrdinal < types.length)
                        ? types[typeOrdinal] : PunishmentType.GENERIC;
                String command = readUtf8(in);
                String debug = readNullableUtf8(in);
                long createdAt = in.readLong();
                return new PunishmentRecord(id, checkName, serverName, type, command, debug, createdAt);
            });
        }
    };
    public static final Codec<StatsSnapshot> STATS_SNAPSHOT = new Codec<>() {
        @Override
        public byte[] encode(StatsSnapshot value) throws Exception {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(24);
            try (DataOutputStream out = new DataOutputStream(baos)) {
                out.writeInt(value.alertCount());
                out.writeInt(value.punishmentCount());
                out.writeInt(value.uniquePlayers());
                out.writeLong(value.databaseBytes());
            }
            return baos.toByteArray();
        }

        @Override
        public StatsSnapshot decode(byte[] bytes) throws Exception {
            try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
                int alerts = in.readInt();
                int punishments = in.readInt();
                int uniquePlayers = in.readInt();
                long databaseBytes = in.readLong();
                return new StatsSnapshot(alerts, punishments, uniquePlayers, databaseBytes);
            }
        }
    };

    public static final Codec<FocusTarget> FOCUS_TARGET = new Codec<>() {
        @Override
        public byte[] encode(FocusTarget value) throws Exception {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (DataOutputStream out = new DataOutputStream(baos)) {
                out.writeLong(value.targetUuid().getMostSignificantBits());
                out.writeLong(value.targetUuid().getLeastSignificantBits());
                writeUtf8(out, value.displayLabel());
            }
            return baos.toByteArray();
        }

        @Override
        public FocusTarget decode(byte[] bytes) throws Exception {
            try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
                long msb = in.readLong();
                long lsb = in.readLong();
                String label = readUtf8(in);
                return new FocusTarget(new UUID(msb, lsb), label);
            }
        }
    };

    public static final Codec<Set<String>> STRING_SET = new Codec<>() {
        @Override
        public byte[] encode(Set<String> value) throws Exception {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (DataOutputStream out = new DataOutputStream(baos)) {
                out.writeInt(value.size());
                for (String item : value) writeUtf8(out, item);
            }
            return baos.toByteArray();
        }

        @Override
        public Set<String> decode(byte[] bytes) throws Exception {
            try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
                int n = in.readInt();
                LinkedHashSet<String> out = new LinkedHashSet<>(n);
                for (int i = 0; i < n; i++) out.add(readUtf8(in));
                return out;
            }
        }
    };

    public static final Codec<List<AlertCheckSummary>> ALERT_CHECK_SUMMARIES = new Codec<>() {
        @Override
        public byte[] encode(List<AlertCheckSummary> list) throws Exception {
            return encodeList(list, (out, s) -> {
                writeUtf8(out, s.checkName());
                out.writeInt(s.alertCount());
            });
        }

        @Override
        public List<AlertCheckSummary> decode(byte[] bytes) throws Exception {
            return decodeList(bytes, in -> new AlertCheckSummary(readUtf8(in), in.readInt()));
        }
    };

    private CacheCodecs() {
    }

    private static <T> byte[] encodeList(List<T> list, ElementWriter<T> writer) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(baos)) {
            out.writeInt(list.size());
            for (T item : list) writer.write(out, item);
        }
        return baos.toByteArray();
    }

    private static <T> List<T> decodeList(byte[] bytes, ElementReader<T> reader) throws Exception {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            int n = in.readInt();
            List<T> out = new ArrayList<>(n);
            for (int i = 0; i < n; i++) out.add(reader.read(in));
            return out;
        }
    }

    private static void writeUtf8(DataOutputStream out, String value) throws Exception {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    private static String readUtf8(DataInputStream in) throws Exception {
        int len = in.readInt();
        byte[] bytes = new byte[len];
        in.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static void writeNullableUtf8(DataOutputStream out, String value) throws Exception {
        if (value == null) {
            out.writeBoolean(false);
        } else {
            out.writeBoolean(true);
            writeUtf8(out, value);
        }
    }

    private static String readNullableUtf8(DataInputStream in) throws Exception {
        return in.readBoolean() ? readUtf8(in) : null;
    }


    private static void writeNullableInt(DataOutputStream out, Integer value) throws Exception {
        if (value == null) {
            out.writeBoolean(false);
        } else {
            out.writeBoolean(true);
            out.writeInt(value);
        }
    }

    private static Integer readNullableInt(DataInputStream in) throws Exception {
        return in.readBoolean() ? in.readInt() : null;
    }

    private interface ElementWriter<T> {
        void write(DataOutputStream out, T value) throws Exception;
    }

    private interface ElementReader<T> {
        T read(DataInputStream in) throws Exception;
    }
}
