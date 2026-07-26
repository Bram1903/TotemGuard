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
import com.deathmotion.totemguard.common.physics.trace.TraceFrame;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.replay.RecordingLibrary;
import com.deathmotion.totemguard.common.replay.format.*;
import com.deathmotion.totemguard.common.replay.playback.ReplayObserver;
import com.deathmotion.totemguard.common.replay.playback.ReplayResult;
import com.deathmotion.totemguard.common.replay.playback.ReplayRun;
import com.deathmotion.totemguard.common.replay.retention.PrologueBuilder;
import com.deathmotion.totemguard.common.replay.retention.RetentionKeyframe;
import com.deathmotion.totemguard.common.replay.retention.StickyPackets;
import com.deathmotion.totemguard.common.world.block.BlockStore;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class PrologueRehearsal {

    private final Path recordings;
    private final @Nullable String only;
    private final double cutSeconds;

    public PrologueRehearsal(Path recordings, @Nullable String only, double cutSeconds) {
        this.recordings = recordings;
        this.only = only;
        this.cutSeconds = cutSeconds;
    }

    private static boolean verdictAgrees(TickDigest reference, TickDigest replayed) {
        Object[] mine = reference.values();
        Object[] theirs = replayed.values();
        for (int i = 1; i <= 6; i++) {
            if (!mine[i].equals(theirs[i])) return false;
        }
        return true;
    }

    private static int align(List<TickDigest> reference, List<TickDigest> replayed, int nominal) {
        if (nominal < 0 || replayed.isEmpty()) return nominal;
        int best = nominal;
        int bestScore = -1;
        for (int candidate = Math.max(0, nominal - 8); candidate <= nominal + 8; candidate++) {
            if (candidate >= reference.size()) break;
            int compared = Math.min(reference.size() - candidate, replayed.size());
            int score = 0;
            for (int i = 0; i < compared; i++) {
                if (difference(reference.get(candidate + i), replayed.get(i)) == null) score++;
            }
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    private static @Nullable String difference(TickDigest reference, TickDigest replayed) {
        Object[] mine = reference.values();
        Object[] theirs = replayed.values();
        for (int i = 1; i < mine.length; i++) {
            if (!mine[i].equals(theirs[i])) return TickDigest.FIELDS[i];
        }
        return null;
    }

    private static int indexOfTick(List<TickDigest> digests, long tick) {
        for (int i = 0; i < digests.size(); i++) {
            if (digests.get(i).tick() == tick) return i;
        }
        return -1;
    }

    public int run() {
        HeadlessBootstrap.load();
        Path scratch = Scratch.directory("replay-config");
        try {
            Files.createDirectories(scratch);
        } catch (IOException failure) {
            System.err.println("Could not create the scratch directory: " + failure);
            return 2;
        }
        new HeadlessPlatform(scratch);

        RecordingLibrary library = new RecordingLibrary(recordings);
        List<Path> selected = new ArrayList<>();
        for (Path candidate : library.all()) {
            String relative = library.root().relativize(candidate).toString().replace('\\', '/');
            if (only == null || relative.toLowerCase().contains(only.toLowerCase())) selected.add(candidate);
        }
        if (selected.isEmpty()) {
            System.out.println("No recordings found under " + recordings.toAbsolutePath() + ".");
            return 0;
        }

        int failures = 0;
        for (Path recording : selected) {
            if (!rehearse(library, recording)) failures++;
        }
        System.out.println();
        if (failures == 0) {
            System.out.println(selected.size() + " recording(s) rehearsed, every prologue converged.");
            return 0;
        }
        System.out.println(failures + " of " + selected.size() + " prologue(s) did not converge.");
        return 1;
    }

    private boolean rehearse(RecordingLibrary library, Path recording) {
        String name = library.root().relativize(recording).toString().replace('\\', '/');
        RecordingHeader header;
        List<RecordingFrame> frames = new ArrayList<>();
        try (RecordingReader reader = new RecordingReader(recording)) {
            header = reader.getHeader();
            RecordingFrame frame;
            while ((frame = reader.next()) != null) frames.add(frame);
        } catch (IOException failure) {
            System.out.println(name + ": unreadable, " + failure.getMessage());
            return false;
        }

        ServerVersion server = ServerVersion.getById(header.serverProtocol());
        if (server == null) {
            System.out.println(name + ": unknown server protocol " + header.serverProtocol());
            return false;
        }
        HeadlessBootstrap.serverVersion(server);

        Traversal traversal = new Traversal();
        ReplayRun.run(recording, traversal);

        Cut cut = new Cut(header, server, header.startNanos() + (long) (cutSeconds * 1_000_000_000.0),
                traversal.visited);
        ReplayResult whole = ReplayRun.run(recording, cut);
        if (whole.error() != null) {
            System.out.println(name + ": the whole recording did not replay, " + whole.error());
            return false;
        }
        if (cut.prologue == null) {
            System.out.println(name + ": never reached " + cutSeconds + "s of judged ticks, nothing to cut");
            return false;
        }

        Path cutFile;
        try {
            cutFile = writeCut(name, header, cut, frames);
        } catch (IOException failure) {
            System.out.println(name + ": could not write the cut recording, " + failure);
            return false;
        }

        ReplayResult tail = ReplayRun.run(cutFile);
        if (tail.error() != null) {
            System.out.println(name + ": the cut recording did not replay, " + tail.error());
            return false;
        }

        return report(name, cut, whole, tail);
    }

    private boolean report(String name, Cut cut, ReplayResult whole, ReplayResult tail) {
        System.out.printf("%-40s cut at %.1fs  prologue %d columns, %d entities, %d frames%n",
                name, cutSeconds, cut.prologue.columns().size(), cut.keyframe.entities().size(),
                cut.prologue.frames().size());

        switch (tail.worldCheck().status()) {
            case MATCHED -> System.out.println("    world rebuilt exactly ("
                    + tail.worldCheck().columnsLoaded() + " columns sampled)");
            case DIVERGED -> System.out.println("    WORLD DIVERGED: " + tail.worldCheck().difference());
            case UNSETTLED -> System.out.println("    the world never settled");
            case VACUOUS -> System.out.println("    WORLD NOT CHECKED: " + tail.worldCheck().difference());
            case ABSENT -> System.out.println("    no prologue frame reached the replay");
        }
        System.out.println("    tail replayed " + tail.ticks() + " ticks"
                + " judged " + tail.judged() + " coasted " + tail.coasted()
                + " declined " + tail.declined()
                + (tail.declineReasons().isEmpty() ? "" : " " + tail.declineReasons()));

        List<TickDigest> reference = whole.digests();
        List<TickDigest> replayed = tail.digests();
        int offset = align(reference, replayed, indexOfTick(reference, cut.firstTailTick));
        if (offset < 0 || replayed.isEmpty()) {
            System.out.println("    could not align the two tick streams");
            return false;
        }

        int compared = Math.min(reference.size() - offset, replayed.size());
        Map<String, Integer> fields = new LinkedHashMap<>();
        int matched = 0;
        int agreed = 0;
        int lastDifference = -1;
        int lastDisagreement = -1;
        for (int i = 0; i < compared; i++) {
            if (verdictAgrees(reference.get(offset + i), replayed.get(i))) {
                agreed++;
            } else {
                lastDisagreement = i;
            }
            String field = difference(reference.get(offset + i), replayed.get(i));
            if (field == null) {
                matched++;
                continue;
            }
            lastDifference = i;
            fields.merge(field, 1, Integer::sum);
        }

        System.out.println("    verdicts agree on " + agreed + "/" + compared + " ticks"
                + (lastDisagreement < 0 ? " (all of them)"
                : ", last disagreement at tail tick " + lastDisagreement
                + " (" + String.format("%.2f", (lastDisagreement + 1) / 20.0) + "s in)"));
        System.out.println("    " + matched + "/" + compared + " bit-identical to the whole run");
        if (lastDifference < 0) {
            System.out.println("    identical from the first tick");
            return true;
        }
        System.out.println("    last bound difference at tail tick " + lastDifference + " ("
                + String.format("%.2f", (lastDifference + 1) / 20.0) + "s in)");
        System.out.println("    differing fields: " + fields);
        List<Integer> differing = new ArrayList<>();
        for (int i = 0; i < compared; i++) {
            if (difference(reference.get(offset + i), replayed.get(i)) != null) differing.add(i);
        }
        for (int sample = 0; sample < 4 && sample < differing.size(); sample++) {
            int i = differing.get(sample * (differing.size() - 1) / 3);
            System.out.println("      whole  " + reference.get(offset + i).toGoldenRow());
            System.out.println("      tail   " + replayed.get(i).toGoldenRow());
        }
        return lastDisagreement < ReplayResult.WARM_UP_TICKS;
    }

    private Path writeCut(String name, RecordingHeader header, Cut cut,
                          List<RecordingFrame> frames) throws IOException {
        Path file = Scratch.directory("replay-prologue")
                .resolve(name.replace('/', '.'));
        Files.createDirectories(file.getParent());

        RecordingHeader cutHeader = new RecordingHeader(header.formatVersion(), header.pluginVersion(),
                header.gitHash(), header.serverProtocol(), header.clientProtocol(),
                header.supportsEndTick(), header.playerName(), header.playerUuid(),
                header.startEpochMillis() + (cut.cutNanos - header.startNanos()) / 1_000_000L,
                cut.cutNanos, RecordingLabel.SCRATCH, "prologue-rehearsal", header.note(), header.tags(),
                header.observeOnly(), cut.keyframe.checks(), header.physicsConfig(),
                header.versionGates(), header.blockStateTable(), header.filterVersion());

        try (RecordingWriter writer = new RecordingWriter(file, cutHeader)) {
            writer.write(new RecordingFrame.Attach(cut.cutNanos));
            for (RecordingFrame frame : cut.prologue.frames()) writer.write(frame);
            for (int i = (int) cut.cutFrameIndex; i < frames.size(); i++) {
                RecordingFrame frame = frames.get(i);
                if (frame instanceof RecordingFrame.Attach) continue;
                if (frame instanceof RecordingFrame.PrologueEnd) continue;
                if (frame instanceof RecordingFrame.End) continue;
                if (frame.nanos() < cut.cutNanos) continue;
                writer.write(frame);
            }
        }
        return file;
    }

    private static final class Traversal implements ReplayObserver {

        private final Set<Long> visited = new HashSet<>();

        @Override
        public void onTick(TGPlayer player, TraceFrame frame) {
            var location = player.getData().getMovementData().getCurrent();
            if (location == null) return;
            visited.add(BlockStore.chunkKey((int) Math.floor(location.getX()) >> 4,
                    (int) Math.floor(location.getZ()) >> 4));
        }
    }

    private static final class Cut implements ReplayObserver {

        private final ServerVersion server;
        private final ClientVersion wire;
        private final StickyPackets sticky = new StickyPackets();
        private final Set<Long> visited;
        private final long cutNanos;

        private long frameIndex;
        private long cutFrameIndex = -1;
        private long firstTailTick = -1;
        private @Nullable RetentionKeyframe keyframe;
        private PrologueBuilder.@Nullable Result prologue;

        private Cut(RecordingHeader header, ServerVersion server, long cutNanos, Set<Long> visited) {
            this.server = server;
            this.wire = server.toClientVersion();
            this.cutNanos = cutNanos;
            this.visited = visited;
        }

        @Override
        public void onPacketFrame(long index, RecordingFrame.Packet frame, TGPlayer player) {
            this.frameIndex = index;
            if (frame.state() != ConnectionState.PLAY) return;
            if (frame.inbound()) {
                PacketTypeCommon type = PacketType.Play.Client.getById(wire, frame.packetId());
                if (type != null) sticky.observeInbound(type, frame.packetId(), frame.payload());
                return;
            }
            PacketTypeCommon type = PacketType.Play.Server.getById(wire, frame.packetId());
            if (type != null) sticky.observe(type, frame.packetId(), frame.payload());
        }

        @Override
        public void onTick(TGPlayer player, TraceFrame frame) {
            if (prologue != null || player.getClock().nanos() < cutNanos) return;

            this.cutFrameIndex = frameIndex + 1;
            this.firstTailTick = frame.tick + 1;
            this.keyframe = RetentionKeyframe.open(player, sticky, cutNanos);
            var here = player.getData().getMovementData().getCurrent();
            this.prologue = PrologueBuilder.build(keyframe,
                    keyframe.journal().rewind(player.getWorldMirror().blocks()),
                    player.getWorldMirror(), visited, server, cutNanos,
                    here == null ? 0.0 : here.getX(),
                    here == null ? 0.0 : here.getY(),
                    here == null ? 0.0 : here.getZ());
        }
    }
}
