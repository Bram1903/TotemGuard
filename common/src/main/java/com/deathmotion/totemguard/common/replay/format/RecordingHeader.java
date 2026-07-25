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

import com.deathmotion.totemguard.common.cache.data.CheckSnapshot;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import org.jetbrains.annotations.Nullable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

public record RecordingHeader(
        int formatVersion,
        String pluginVersion,
        String gitHash,
        int serverProtocol,
        int clientProtocol,
        boolean supportsEndTick,
        String playerName,
        UUID playerUuid,
        long startEpochMillis,
        long startNanos,
        RecordingLabel label,
        String scenario,
        String note,
        List<String> tags,
        boolean observeOnly,
        List<CheckSnapshot> checkSnapshot,
        PhysicsConfig physicsConfig,
        Map<String, Boolean> versionGates,
        int @Nullable [] blockStateTable,
        int filterVersion
) {
    public static RecordingHeader read(DataInputStream in) throws IOException {
        int formatVersion = in.readInt();
        if (formatVersion != ReplayFormat.VERSION) {
            throw new IOException("Recording format version " + formatVersion
                    + " is not readable by this build (expected " + ReplayFormat.VERSION + ")");
        }
        String pluginVersion = in.readUTF();
        String gitHash = in.readUTF();
        int serverProtocol = in.readInt();
        int clientProtocol = in.readInt();
        boolean supportsEndTick = in.readBoolean();
        String playerName = in.readUTF();
        UUID playerUuid = new UUID(in.readLong(), in.readLong());
        long startEpochMillis = in.readLong();
        long startNanos = in.readLong();
        RecordingLabel label = RecordingLabel.valueOf(in.readUTF());
        String scenario = in.readUTF();
        String note = in.readUTF();
        int tagCount = in.readInt();
        List<String> tags = new ArrayList<>(tagCount);
        for (int i = 0; i < tagCount; i++) tags.add(in.readUTF());
        boolean observeOnly = in.readBoolean();

        int snapshotCount = in.readInt();
        List<CheckSnapshot> checkSnapshot = new ArrayList<>(snapshotCount);
        for (int i = 0; i < snapshotCount; i++) {
            checkSnapshot.add(new CheckSnapshot(in.readUTF(), in.readDouble(), in.readInt()));
        }

        PhysicsConfig physicsConfig = PhysicsConfig.read(in);

        int gateCount = in.readInt();
        Map<String, Boolean> versionGates = new LinkedHashMap<>();
        for (int i = 0; i < gateCount; i++) {
            versionGates.put(in.readUTF(), in.readBoolean());
        }

        int[] blockStateTable = in.readBoolean() ? readStateTable(in) : null;
        int filterVersion = in.readInt();

        return new RecordingHeader(formatVersion, pluginVersion, gitHash, serverProtocol, clientProtocol,
                supportsEndTick, playerName, playerUuid, startEpochMillis, startNanos, label, scenario,
                note, tags, observeOnly, checkSnapshot, physicsConfig, versionGates, blockStateTable,
                filterVersion);
    }

    private static int[] readStateTable(DataInputStream in) throws IOException {
        int[] table = new int[ReplayFormat.STATE_TABLE_SIZE];
        for (int id = 0; id < table.length; id++) {
            table[id] = id + readZigZag(in);
        }
        return table;
    }

    private static void writeStateTable(DataOutputStream out, int[] table) throws IOException {
        for (int id = 0; id < ReplayFormat.STATE_TABLE_SIZE; id++) {
            writeZigZag(out, table[id] - id);
        }
    }

    private static int readZigZag(DataInputStream in) throws IOException {
        int raw = readVarInt(in);
        return (raw >>> 1) ^ -(raw & 1);
    }

    private static void writeZigZag(DataOutputStream out, int value) throws IOException {
        writeVarInt(out, (value << 1) ^ (value >> 31));
    }

    private static int readVarInt(DataInputStream in) throws IOException {
        int value = 0;
        int shift = 0;
        while (true) {
            int b = in.readUnsignedByte();
            value |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) return value;
            shift += 7;
            if (shift > 35) throw new IOException("VarInt too long");
        }
    }

    private static void writeVarInt(DataOutputStream out, int value) throws IOException {
        int remaining = value;
        while ((remaining & ~0x7F) != 0) {
            out.writeByte((remaining & 0x7F) | 0x80);
            remaining >>>= 7;
        }
        out.writeByte(remaining);
    }

    public void write(DataOutputStream out) throws IOException {
        out.writeInt(formatVersion);
        out.writeUTF(pluginVersion);
        out.writeUTF(gitHash);
        out.writeInt(serverProtocol);
        out.writeInt(clientProtocol);
        out.writeBoolean(supportsEndTick);
        out.writeUTF(playerName);
        out.writeLong(playerUuid.getMostSignificantBits());
        out.writeLong(playerUuid.getLeastSignificantBits());
        out.writeLong(startEpochMillis);
        out.writeLong(startNanos);
        out.writeUTF(label.name());
        out.writeUTF(scenario);
        out.writeUTF(note);
        out.writeInt(tags.size());
        for (String tag : tags) out.writeUTF(tag);
        out.writeBoolean(observeOnly);

        out.writeInt(checkSnapshot.size());
        for (CheckSnapshot snapshot : checkSnapshot) {
            out.writeUTF(snapshot.checkName());
            out.writeDouble(snapshot.buffer());
            out.writeInt(snapshot.violations());
        }

        physicsConfig.write(out);

        out.writeInt(versionGates.size());
        for (Map.Entry<String, Boolean> gate : versionGates.entrySet()) {
            out.writeUTF(gate.getKey());
            out.writeBoolean(gate.getValue());
        }

        out.writeBoolean(blockStateTable != null);
        if (blockStateTable != null) writeStateTable(out, blockStateTable);
        out.writeInt(filterVersion);
    }

    public ClientVersion client() {
        return ClientVersion.getById(clientProtocol);
    }

    public String clientTag() {
        String name = client().name();
        return name.startsWith("V_") ? name.substring(2) : name;
    }

    public record PhysicsConfig(
            String preset,
            boolean setback,
            boolean closeInventory,
            boolean fallDamage,
            boolean timerPacketCancel,
            boolean simulateFlying
    ) {
        static PhysicsConfig read(DataInputStream in) throws IOException {
            return new PhysicsConfig(in.readUTF(), in.readBoolean(), in.readBoolean(),
                    in.readBoolean(), in.readBoolean(), in.readBoolean());
        }

        void write(DataOutputStream out) throws IOException {
            out.writeUTF(preset);
            out.writeBoolean(setback);
            out.writeBoolean(closeInventory);
            out.writeBoolean(fallDamage);
            out.writeBoolean(timerPacketCancel);
            out.writeBoolean(simulateFlying);
        }
    }
}
