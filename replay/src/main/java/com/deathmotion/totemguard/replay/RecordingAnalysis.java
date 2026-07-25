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

package com.deathmotion.totemguard.replay;

import com.deathmotion.totemguard.common.replay.RecordingLibrary;
import com.deathmotion.totemguard.common.replay.capture.CaptureFilter;
import com.deathmotion.totemguard.common.replay.capture.ChunkPayloadCompactor;
import com.deathmotion.totemguard.common.replay.format.*;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import com.github.retrooper.packetevents.netty.buffer.UnpooledByteBufAllocationHelper;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.PacketSide;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.util.EventCreationUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class RecordingAnalysis {

    private final Path recordings;

    public RecordingAnalysis(Path recordings) {
        this.recordings = recordings;
    }

    private static String bytes(long value) {
        if (value < 1024) return value + " B";
        if (value < 1024 * 1024) return String.format("%.1f KiB", value / 1024.0);
        return String.format("%.2f MiB", value / (1024.0 * 1024.0));
    }

    public int run() {
        HeadlessBootstrap.load();
        RecordingLibrary library = new RecordingLibrary(recordings);
        List<Path> all = library.all();
        if (all.isEmpty()) {
            System.out.println("No recordings found under " + recordings.toAbsolutePath() + ".");
            return 0;
        }
        for (Path recording : all) {
            try {
                analyse(library.root().relativize(recording).toString().replace('\\', '/'), recording);
            } catch (IOException failure) {
                System.out.println(recording + ": unreadable, " + failure.getMessage());
                return 1;
            }
        }
        return 0;
    }

    private void analyse(String name, Path recording) throws IOException {
        List<RecordingFrame> frames = new ArrayList<>();
        RecordingHeader header;
        try (RecordingReader reader = new RecordingReader(recording)) {
            header = reader.getHeader();
            RecordingFrame frame;
            while ((frame = reader.next()) != null) frames.add(frame);
        }
        HeadlessBootstrap.serverVersion(ServerVersion.getById(header.serverProtocol()));

        long onDisk = Files.size(recording);
        long baseline = encoded(header, frames, null);

        Map<String, Group> groups = new HashMap<>();
        long structural = 0;
        for (RecordingFrame frame : frames) {
            if (frame instanceof RecordingFrame.Packet packet) {
                groups.computeIfAbsent(label(header, packet), key -> new Group()).add(packet.payload().length);
            } else {
                structural++;
            }
        }

        List<Map.Entry<String, Group>> ranked = new ArrayList<>(groups.entrySet());
        for (Map.Entry<String, Group> entry : ranked) {
            entry.getValue().compressed = baseline - encoded(header, frames, entry.getKey());
        }
        ranked.sort(Comparator.comparingLong((Map.Entry<String, Group> entry) -> entry.getValue().compressed).reversed());

        System.out.println();
        System.out.println(name + "  " + bytes(onDisk) + " on disk, " + frames.size()
                + " frames (" + structural + " structural)");
        System.out.printf("  %-46s %7s %11s %11s %6s%n", "packet", "count", "raw", "compressed", "share");
        long shown = 0;
        for (Map.Entry<String, Group> entry : ranked) {
            Group group = entry.getValue();
            shown += group.compressed;
            System.out.printf("  %-46s %7d %11s %11s %5.1f%%%n", entry.getKey(), group.count,
                    bytes(group.raw), bytes(group.compressed), 100.0 * group.compressed / baseline);
        }
        System.out.printf("  %-46s %7s %11s %11s %5.1f%%%n", "(framing, digests, header)", "",
                "", bytes(baseline - shown), 100.0 * (baseline - shown) / baseline);

        project(header, frames, baseline);
    }

    private void project(RecordingHeader header, List<RecordingFrame> frames, long baseline) throws IOException {
        ServerVersion server = ServerVersion.getById(header.serverProtocol());
        User user = new User(null, ConnectionState.PLAY, header.client(),
                new UserProfile(header.playerUuid(), header.playerName()));
        List<RecordingFrame> rewritten = new ArrayList<>(frames.size());
        int dropped = 0;
        int compacted = 0;
        long before = 0;
        long after = 0;

        for (RecordingFrame frame : frames) {
            if (!(frame instanceof RecordingFrame.Packet packet)) {
                rewritten.add(frame);
                continue;
            }
            PacketTypeCommon type = PacketType.getById(packet.inbound() ? PacketSide.CLIENT : PacketSide.SERVER,
                    packet.state(), server.toClientVersion(), packet.packetId());
            if (type == null || !CaptureFilter.kept(type)) {
                dropped++;
                continue;
            }
            if (type == PacketType.Play.Server.CHUNK_DATA) {
                byte[] payload = compactChunk(packet, server, user);
                if (payload != null) {
                    compacted++;
                    before += packet.payload().length;
                    after += payload.length;
                    rewritten.add(new RecordingFrame.Packet(packet.inbound(), packet.cancelled(),
                            packet.state(), packet.packetId(), packet.nanos(), payload));
                    continue;
                }
            }
            rewritten.add(frame);
        }

        long projected = encoded(header, rewritten, null);
        System.out.println("  the current filter would drop " + dropped + " frame(s); "
                + compacted + " chunk(s) compacted " + bytes(before) + " -> " + bytes(after) + " raw");
        System.out.println("  projected " + bytes(projected) + ", down "
                + String.format("%.1f%%", 100.0 * (baseline - projected) / baseline));
        writeProjection(header, rewritten);
    }

    private byte[] compactChunk(RecordingFrame.Packet packet, ServerVersion server, User user) {
        Object buffer = UnpooledByteBufAllocationHelper.buffer();
        try {
            ByteBufHelper.writeVarInt(buffer, packet.packetId());
            ByteBufHelper.writeBytes(buffer, packet.payload());
            return ChunkPayloadCompactor.compact(
                    EventCreationUtil.createSendEvent(null, user, null, buffer, true), server);
        } finally {
            ByteBufHelper.release(buffer);
        }
    }

    private void writeProjection(RecordingHeader header, List<RecordingFrame> frames) throws IOException {
        Path target = recordings.resolveSibling("build").resolve("replay-projected")
                .resolve(header.playerName() + ReplayFormat.EXTENSION);
        Files.createDirectories(target.getParent());
        try (RecordingWriter writer = new RecordingWriter(target, header)) {
            for (RecordingFrame frame : frames) writer.write(frame);
        }
        System.out.println("  rewritten to " + target);
    }

    private long encoded(RecordingHeader header, List<RecordingFrame> frames, String without) throws IOException {
        ByteArrayOutputStream sink = new ByteArrayOutputStream(1 << 20);
        try (RecordingWriter writer = new RecordingWriter(sink, header)) {
            for (RecordingFrame frame : frames) {
                if (without != null && frame instanceof RecordingFrame.Packet packet
                        && without.equals(label(header, packet))) {
                    continue;
                }
                writer.write(frame);
            }
        }
        return sink.size();
    }

    private String label(RecordingHeader header, RecordingFrame.Packet packet) {
        PacketSide side = packet.inbound() ? PacketSide.CLIENT : PacketSide.SERVER;
        ClientVersion version = ServerVersion.getById(header.serverProtocol()).toClientVersion();
        PacketTypeCommon type = PacketType.getById(side, packet.state(), version, packet.packetId());
        String arrow = packet.inbound() ? "in  " : "out ";
        if (type == null) return arrow + packet.state() + "/unknown-" + packet.packetId();
        return arrow + type;
    }

    private static final class Group {

        private int count;
        private long raw;
        private long compressed;

        private void add(int payload) {
            count++;
            raw += payload;
        }
    }
}
