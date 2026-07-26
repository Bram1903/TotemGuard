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

import com.deathmotion.totemguard.common.HeadlessPlatform;
import com.deathmotion.totemguard.common.cache.data.CheckSnapshot;
import com.deathmotion.totemguard.common.physics.trace.TraceFrame;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.replay.format.*;
import com.deathmotion.totemguard.common.replay.playback.ReplayObserver;
import com.deathmotion.totemguard.common.replay.playback.ReplayResult;
import com.deathmotion.totemguard.common.replay.playback.ReplayRun;
import com.deathmotion.totemguard.common.replay.retention.RetentionSweep;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.logging.Logger;

public final class SelfTest {

    private SelfTest() {
    }

    public static int run() {
        List<String> failures = new ArrayList<>();
        try {
            HeadlessBootstrap.load();
            ServerVersion server = ServerVersion.getLatest();
            HeadlessBootstrap.serverVersion(server);

            Path scratch = Files.createTempDirectory("totemguard-replay-selftest");
            new HeadlessPlatform(scratch);

            Path file = scratch.resolve("selftest" + ReplayFormat.EXTENSION);
            RecordingHeader written = synthesize(server);
            write(file, written);
            check(failures, "the file exists", Files.isRegularFile(file));

            RecordingHeader read;
            List<RecordingFrame> frames = new ArrayList<>();
            try (RecordingReader reader = new RecordingReader(file)) {
                read = reader.getHeader();
                RecordingFrame frame;
                while ((frame = reader.next()) != null) frames.add(frame);
                check(failures, "the reader saw a clean close", !reader.isTruncated());
            }

            check(failures, "the header round-trips", sameExceptTable(written, read));
            check(failures, "tags round-trip", read.tags().equals(List.of("elytra", "piston")));
            check(failures, "the state table round-trips",
                    Arrays.equals(written.blockStateTable(), read.blockStateTable()));
            check(failures, "every frame kind round-trips", frames.size() == 12);
            check(failures, "an integration input round-trips",
                    frames.stream().anyMatch(f -> f instanceof RecordingFrame.Integration i
                            && i.input() == IntegrationInput.TRANSACTION && i.id() == 7));
            check(failures, "the attach boundary survives",
                    frames.stream().anyMatch(f -> f instanceof RecordingFrame.Attach));
            check(failures, "a recorded flag survives",
                    frames.stream().anyMatch(f -> f instanceof RecordingFrame.Flag flag
                            && flag.check().equals("TickB") && flag.violations() == 3));
            check(failures, "the mark survives",
                    frames.stream().anyMatch(f -> f instanceof RecordingFrame.Mark m
                            && m.label().equals("selftest")));
            check(failures, "the trailer survives",
                    frames.stream().anyMatch(f -> f instanceof RecordingFrame.End e
                            && e.trailer().judgedTicks() == 1));

            checkPayloadDedup(failures, scratch, written);
            checkRetentionSweep(failures, scratch);

            TGPlayer[] replayed = new TGPlayer[1];
            ReplayResult result = ReplayRun.run(file, new ReplayObserver() {
                @Override
                public void onPacketFrame(long frameIndex, RecordingFrame.Packet frame, TGPlayer player) {
                    replayed[0] = player;
                }

                @Override
                public void onTick(TGPlayer player, TraceFrame frame) {
                    replayed[0] = player;
                }
            });
            if (result.error() != null) System.out.println("       error: " + result.error());
            check(failures, "the replay completed", result.error() == null);
            check(failures, "the replay saw the frames", result.frames() == 12);
            check(failures, "a flag the replay never reproduced is reported",
                    result.flagCheck().status() == ReplayResult.FlagCheck.Status.DIVERGED
                            && result.flagCheck().recorded() == 1 && result.flagCheck().replayed() == 0);
            check(failures, "a recording with no movement is reported as vacuous", result.vacuous());
            check(failures, "the engine built a player headless", replayed[0] != null);
            check(failures, "the transaction reached the ledger",
                    replayed[0] != null && replayed[0].getPingData().getAcceptedTransactionCount() >= 1);
            check(failures, "an integration ack drains a transaction no packet ever answered",
                    replayed[0] != null && replayed[0].getPingData().getAcceptedTransactionCount() == 2
                            && replayed[0].getPingData().getPendingTransactionCount() == 0);
            check(failures, "26 checks were constructed",
                    replayed[0] != null && replayed[0].getCheckManager().allChecks.size() > 20);


            Path golden = GoldenStore.goldenPath(file);
            GoldenStore.write(file, result);
            check(failures, "a golden trace is written", Files.isRegularFile(golden));
            check(failures, "an identical run diffs clean", GoldenStore.diff(file, result) == null);
        } catch (Exception failure) {
            failures.add("threw " + failure);
            failure.printStackTrace();
        }

        if (failures.isEmpty()) {
            System.out.println("Replay self-test passed.");
            return 0;
        }
        System.out.println("Replay self-test failed:");
        failures.forEach(failure -> System.out.println("  " + failure));
        return 1;
    }

    private static boolean sameExceptTable(RecordingHeader written, RecordingHeader read) {
        return withoutTable(written).equals(withoutTable(read));
    }

    private static RecordingHeader withoutTable(RecordingHeader header) {
        return new RecordingHeader(header.formatVersion(), header.pluginVersion(), header.gitHash(),
                header.serverProtocol(), header.clientProtocol(), header.supportsEndTick(),
                header.playerName(), header.playerUuid(), header.startEpochMillis(), header.startNanos(),
                header.label(), header.scenario(), header.note(), header.tags(), header.observeOnly(),
                header.checkSnapshot(), header.physicsConfig(), header.versionGates(), null,
                header.filterVersion());
    }

    private static void check(List<String> failures, String what, boolean holds) {
        System.out.println((holds ? "  ok   " : "  FAIL ") + what);
        if (!holds) failures.add(what);
    }

    private static RecordingHeader synthesize(ServerVersion server) {
        ClientVersion client = server.toClientVersion();
        int[] table = new int[ReplayFormat.STATE_TABLE_SIZE];
        for (int id = 0; id < table.length; id++) {
            table[id] = id % 4096 == 0 ? Math.max(0, id - 1) : id;
        }
        Map<String, Boolean> gates = new LinkedHashMap<>();
        gates.put("selftest", true);
        boolean endTick = client.isNewerThanOrEquals(ClientVersion.V_1_21_2)
                && server.isNewerThanOrEquals(ServerVersion.V_1_21_2);
        return new RecordingHeader(ReplayFormat.VERSION, "selftest", "0000000",
                server.getProtocolVersion(), client.getProtocolVersion(), endTick,
                "SelfTest", new UUID(1L, 2L), 1_700_000_000_000L, 42_000_000_000L,
                RecordingLabel.SCRATCH, "selftest", "hand built", List.of("elytra", "piston"),
                true, List.of(new CheckSnapshot("SelfTest", 0.5, 3)),
                new RecordingHeader.PhysicsConfig("strict", true, true, true, true, true),
                gates, table, ReplayFormat.FILTER_VERSION);
    }

    private static void checkRetentionSweep(List<String> failures, Path scratch) throws Exception {
        Path folder = scratch.resolve("sweep");
        Files.createDirectories(folder);
        byte[] block = new byte[400 * 1024];
        List<Path> written = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            Path file = folder.resolve("tape" + i + ReplayFormat.EXTENSION);
            Files.write(file, block);
            Files.setLastModifiedTime(file, FileTime.fromMillis(1_700_000_000_000L + i * 1_000L));
            written.add(file);
        }
        Files.write(folder.resolve("notes.txt"), block);

        RetentionSweep.Result result = RetentionSweep.run(folder, 1024L * 1024L,
                Logger.getLogger("SelfTest"));

        check(failures, "the sweep keeps only what fits in the budget", result.keptBytes() <= 1024L * 1024L);
        check(failures, "the sweep removes the oldest first",
                !Files.exists(written.get(0)) && Files.exists(written.get(5)));
        check(failures, "the sweep deleted the ones over budget", result.deleted() == 4);
        check(failures, "the sweep leaves files it does not own alone",
                Files.exists(folder.resolve("notes.txt")));
        check(failures, "a budget of zero sweeps nothing",
                RetentionSweep.run(folder, 0L, Logger.getLogger("SelfTest")).deleted() == 0);
    }

    private static void checkPayloadDedup(List<String> failures, Path scratch,
                                          RecordingHeader header) throws Exception {
        Path file = scratch.resolve("dedup" + ReplayFormat.EXTENSION);
        ClientVersion client = ClientVersion.getById(header.clientProtocol());
        int chunkId = PacketType.Play.Server.CHUNK_DATA.getId(client);

        int repeats = 64;
        byte[] wide = new byte[ReplayFormat.REPEAT_MIN_PAYLOAD * 16];
        long seed = 0x5DEECE66DL;
        for (int i = 0; i < wide.length; i++) {
            seed = seed * 6364136223846793005L + 1442695040888963407L;
            wide[i] = (byte) (seed >>> 40);
        }
        byte[] narrow = intPayload(7);

        try (RecordingWriter writer = new RecordingWriter(file, header)) {
            for (int i = 0; i < repeats; i++) {
                writer.write(new RecordingFrame.Packet(false, false, ConnectionState.PLAY, chunkId,
                        header.startNanos() + i, wide.clone()));
                writer.write(new RecordingFrame.Packet(true, false, ConnectionState.PLAY, chunkId,
                        header.startNanos() + i, narrow.clone()));
            }
        }

        List<RecordingFrame> frames = new ArrayList<>();
        try (RecordingReader reader = new RecordingReader(file)) {
            RecordingFrame frame;
            while ((frame = reader.next()) != null) frames.add(frame);
        }

        boolean restored = frames.size() == repeats * 2 && frames.stream()
                .allMatch(f -> f instanceof RecordingFrame.Packet packet
                        && Arrays.equals(packet.payload(), packet.inbound() ? narrow : wide));
        check(failures, "a repeated payload round-trips to the bytes it stood for", restored);
        check(failures, "a repeat costs a reference rather than the payload",
                Files.size(file) < (long) wide.length * 2);
    }

    private static byte[] intPayload(int value) {
        return new byte[]{(byte) (value >>> 24), (byte) (value >>> 16), (byte) (value >>> 8), (byte) value};
    }

    private static void write(Path file, RecordingHeader header) throws Exception {
        ClientVersion client = ClientVersion.getById(header.clientProtocol());
        int pingId = PacketType.Play.Server.PING.getId(client);
        int pongId = PacketType.Play.Client.PONG.getId(client);
        try (RecordingWriter writer = new RecordingWriter(file, header)) {
            writer.write(new RecordingFrame.Mark(header.startNanos(), "selftest"));
            writer.write(new RecordingFrame.Attach(header.startNanos()));
            writer.write(new RecordingFrame.Packet(false, false, ConnectionState.PLAY, pingId,
                    header.startNanos(), intPayload(4242)));
            writer.write(new RecordingFrame.Packet(true, false, ConnectionState.PLAY, pongId,
                    header.startNanos(), intPayload(4242)));
            writer.write(new RecordingFrame.ServerTick(header.startNanos() + 1, 1L));
            writer.write(new RecordingFrame.EventLoop(header.startNanos() + 2));
            writer.write(new RecordingFrame.Verdict(header.startNanos() + 3,
                    new TickDigest(1L, (byte) 0, (byte) 0, (byte) -1, (byte) -1, (byte) 0, (byte) 0,
                            0.1, -0.2, 0.3, 0.4, 0.5, 0.6, -0.7, 0.8, 0.0, 0.0, 0.0, 0.0,
                            7, 11L, 13L, (byte) 1, (byte) 2, (byte) 4, 1.25)));
            writer.write(new RecordingFrame.Flag(header.startNanos() + 4, "TickB", 3, "selftest"));
            writer.write(new RecordingFrame.Packet(false, false, ConnectionState.PLAY, pingId,
                    header.startNanos() + 5, intPayload(7)));
            writer.write(new RecordingFrame.Integration(header.startNanos() + 6,
                    IntegrationInput.TRANSACTION, 7, 0L));
            writer.write(new RecordingFrame.PrologueEnd(header.startNanos() + 7, WorldDigest.ABSENT));
            writer.write(new RecordingFrame.End(header.startNanos() + 8,
                    new RecordingTrailer(header.startNanos() + 8, 12, 0, 1, 0, 0, 1, false, 0)));
        }
    }
}
