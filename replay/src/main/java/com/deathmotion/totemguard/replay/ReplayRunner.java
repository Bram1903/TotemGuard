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

import com.deathmotion.totemguard.api.config.ConfigFile;
import com.deathmotion.totemguard.common.HeadlessPlatform;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.config.view.ConfigView;
import com.deathmotion.totemguard.common.physics.preset.PhysicsDebugContext;
import com.deathmotion.totemguard.common.replay.RecordingLibrary;
import com.deathmotion.totemguard.common.replay.format.RecordingHeader;
import com.deathmotion.totemguard.common.replay.format.RecordingLabel;
import com.deathmotion.totemguard.common.replay.format.RecordingReader;
import com.deathmotion.totemguard.common.replay.format.TickDigest;
import com.deathmotion.totemguard.common.replay.playback.ReplayFlag;
import com.deathmotion.totemguard.common.replay.playback.ReplayResult;
import com.deathmotion.totemguard.common.replay.playback.ReplayRun;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public final class ReplayRunner {

    private final ReplayMain.Options options;
    private final List<String> failures = new ArrayList<>();
    private final Map<String, Map<String, String>> matrix = new LinkedHashMap<>();
    private Path configDirectory = Paths.get(".");

    public ReplayRunner(ReplayMain.Options options) {
        this.options = options;
    }

    public int run() {
        Path scratch = Scratch.directory("replay-config");
        try {
            Files.createDirectories(scratch);
        } catch (IOException failure) {
            System.err.println("Could not create the scratch config directory: " + failure);
            return 2;
        }

        HeadlessBootstrap.load();
        this.configDirectory = scratch;
        new HeadlessPlatform(scratch);

        RecordingLibrary library = new RecordingLibrary(options.recordings());
        List<Path> recordings = select(library);
        if (recordings.isEmpty()) {
            System.out.println("No recordings found under " + options.recordings().toAbsolutePath() + ".");
            return 0;
        }

        long startedAt = System.nanoTime();
        for (Path recording : recordings) {
            replay(library, recording);
        }
        printMatrix();

        long elapsed = (System.nanoTime() - startedAt) / 1_000_000L;
        System.out.println();
        if (failures.isEmpty()) {
            System.out.println(recordings.size() + " recording(s) replayed in " + elapsed + "ms, all clean.");
            return 0;
        }
        System.out.println(failures.size() + " of " + recordings.size()
                + " recording(s) failed in " + elapsed + "ms:");
        failures.forEach(failure -> System.out.println("  " + failure));
        return 1;
    }

    private List<Path> select(RecordingLibrary library) {
        List<Path> all = library.all();
        String only = options.only();
        if (only == null || only.isBlank()) return all;

        String needle = only.toLowerCase(Locale.ROOT).replace('\\', '/');
        List<Path> filtered = new ArrayList<>();
        for (Path candidate : all) {
            String relative = library.root().relativize(candidate).toString()
                    .replace('\\', '/').toLowerCase(Locale.ROOT);
            if (relative.contains(needle)) filtered.add(candidate);
        }
        return filtered;
    }

    private void replay(RecordingLibrary library, Path recording) {
        String name = library.root().relativize(recording).toString().replace('\\', '/');

        RecordingHeader header;
        try (RecordingReader reader = new RecordingReader(recording)) {
            header = reader.getHeader();
        } catch (IOException failure) {
            fail(name, "unreadable: " + failure.getMessage());
            return;
        }

        HeadlessBootstrap.serverVersion(serverVersionOf(header));
        applyConfig(header);

        if (options.trace() != null) {
            if (!name.contains(options.trace())) return;
            printTrace(name, ReplayRun.run(recording));
            return;
        }

        ReplayResult result = ReplayRun.run(recording);
        record(header, name, result);

        if (result.error() != null) {
            fail(name, result.error());
            return;
        }

        System.out.printf("%-40s %5d ticks  judged %-5d coasted %-5d declined %-5d flags %-3d  %dms%n",
                name, result.ticks(), result.judged(), result.coasted(), result.declined(),
                result.flags().size(), result.elapsedMillis());

        if (result.truncated()) {
            System.out.println("    truncated: no END frame, replayed as far as it got");
        }
        if (!result.selfEmitted().isEmpty()) {
            System.out.println("    self-emitted: " + result.selfEmitted());
        }
        if (!result.dispatchErrors().isEmpty()) {
            fail(name, "packets failed to dispatch: " + result.dispatchErrors());
        }
        if (result.pendingEventLoopTasks() > 0) {
            System.out.println("    " + result.pendingEventLoopTasks()
                    + " deferred task(s) never reached a LOOP frame");
        }
        reportVerification(name, result);
        reportFlagCheck(name, result);
        reportWorldCheck(name, result);

        if (result.vacuous()) {
            fail(name, "judged no ticks, so it proves nothing. Decline reasons: " + result.declineReasons());
            return;
        }
        checkExpectations(name, header, result);

        if (options.verifyDeterminism()) {
            checkDeterminism(name, recording, result);
        }
        reportGolden(name, recording, result);
    }

    private void applyConfig(RecordingHeader header) {
        RecordingHeader.PhysicsConfig recorded = header.physicsConfig();
        boolean tracing = options.trace() != null && header.playerName() != null;
        Set<PhysicsDebugContext> contexts = tracing
                ? PhysicsDebugContext.parse(header.tags())
                : Set.of();

        if (options.recordedConfig() || tracing) {
            ConfigView current = TGPlatform.getInstance().getConfigRepository().configView();
            boolean useRecorded = options.recordedConfig();
            try {
                Files.writeString(configDirectory.resolve("config.yml"), """
                                config_version: 1
                                physics-engine:
                                  preset: %s
                                  simulate-flying: %s
                                  mitigation:
                                    setback: %s
                                    close-inventory: %s
                                    fall-damage: %s
                                    timer-packet-cancel: %s
                                  debug:
                                    level: %s
                                    context: [ %s ]
                                """.formatted(
                                useRecorded ? recorded.preset() : current.physicsPreset().name(),
                                useRecorded ? recorded.simulateFlying() : current.physicsSimulateFlying(),
                                useRecorded ? recorded.setback() : current.physicsEngineSetback(),
                                useRecorded ? recorded.closeInventory() : current.physicsEngineCloseInventory(),
                                useRecorded ? recorded.fallDamage() : current.physicsEngineFallDamage(),
                                useRecorded ? recorded.timerPacketCancel() : current.physicsEngineTimerPacketCancel(),
                                tracing ? "trace" : current.physicsDebugLevel().name().toLowerCase(Locale.ROOT),
                                contexts.stream().map(c -> c.name().toLowerCase(Locale.ROOT))
                                        .collect(Collectors.joining(", "))),
                        StandardCharsets.UTF_8);
                TGPlatform.getInstance().getConfigRepository().reload(ConfigFile.CONFIG);
                if (tracing && !contexts.isEmpty()) {
                    System.out.println("    trace contexts from tags " + header.tags() + ": " + contexts);
                } else if (tracing) {
                    System.out.println("    no tag mapped to a trace context, plain rows"
                            + (header.tags().isEmpty() ? " (recording has no tags)" : ""));
                }
                if (useRecorded) return;
            } catch (IOException failure) {
                System.out.println("    could not write the replay config: " + failure);
            }
        }

        ConfigView active = TGPlatform.getInstance().getConfigRepository().configView();
        List<String> differences = new ArrayList<>();
        if (!active.physicsPreset().name().equals(recorded.preset())) {
            differences.add("preset " + recorded.preset() + " -> " + active.physicsPreset().name());
        }
        if (active.physicsEngineSetback() != recorded.setback()) differences.add("setback");
        if (active.physicsEngineCloseInventory() != recorded.closeInventory()) differences.add("close-inventory");
        if (active.physicsEngineFallDamage() != recorded.fallDamage()) differences.add("fall-damage");
        if (active.physicsEngineTimerPacketCancel() != recorded.timerPacketCancel())
            differences.add("timer-packet-cancel");
        if (active.physicsSimulateFlying() != recorded.simulateFlying()) differences.add("simulate-flying");
        if (!differences.isEmpty()) {
            System.out.println("    config differs from capture: " + differences);
        }
    }

    private ServerVersion serverVersionOf(RecordingHeader header) {
        for (ServerVersion version : ServerVersion.values()) {
            if (version.getProtocolVersion() == header.serverProtocol()) return version;
        }
        return ServerVersion.getLatest();
    }

    private void record(RecordingHeader header, String name, ReplayResult result) {
        String scenario = header.scenario();
        ClientVersion client = header.client();
        String cell = result.error() != null ? "error"
                : result.vacuous() ? "vacuous"
                : result.flags().isEmpty() ? "clean" : (result.flags().size() + " flags");
        matrix.computeIfAbsent(header.label().id() + "/" + scenario, key -> new LinkedHashMap<>())
                .put(client == null ? "?" : client.getReleaseName(), cell);
    }

    private void printMatrix() {
        if (matrix.size() < 2) return;
        System.out.println();
        System.out.println("Cross-version matrix");
        matrix.forEach((scenario, byClient) -> System.out.println("  " + scenario + "  " + byClient));
    }

    private void reportVerification(String name, ReplayResult result) {
        ReplayResult.Verification verification = result.verification();
        switch (verification.status()) {
            case MATCHED -> System.out.println("    verified " + verification.compared() + "/"
                    + verification.total() + " ticks identical to capture");
            case DIVERGED -> fail(name, "replay diverged from capture at tick "
                    + verification.divergentTick() + " (" + verification.field() + ")");
            case SKIPPED -> System.out.println("    verification skipped: " + verification.skipReason());
        }
    }

    private void reportFlagCheck(String name, ReplayResult result) {
        ReplayResult.FlagCheck check = result.flagCheck();
        switch (check.status()) {
            case ABSENT -> {
            }
            case MATCHED -> System.out.println("    flags identical to capture (" + check.recorded() + ")");
            case DIVERGED -> fail(name, "flags differ from capture: live " + check.recorded()
                    + ", replay " + check.replayed() + ", " + check.difference());
        }
    }

    private void reportWorldCheck(String name, ReplayResult result) {
        ReplayResult.WorldCheck check = result.worldCheck();
        switch (check.status()) {
            case ABSENT -> {
            }
            case MATCHED -> System.out.println("    prologue rebuilt the world exactly ("
                    + check.columnsLoaded() + " columns sampled)");
            case DIVERGED -> fail(name, "the retained prologue rebuilt a different world: " + check.difference());
            case UNSETTLED -> fail(name, "the world never settled, so the prologue could not be checked");
            case VACUOUS -> fail(name, "the prologue proved nothing: " + check.difference());
        }
    }

    private void checkExpectations(String name, RecordingHeader header, ReplayResult result) {
        List<ReplayFlag> flags = result.settledFlags();
        if (result.warmUpFlags() > 0) {
            System.out.println("    " + result.warmUpFlags() + " flag(s) inside the "
                    + ReplayResult.WARM_UP_TICKS + "-tick warm-up ignored: the cut seeds the engine cold");
        }
        if (header.label() == RecordingLabel.LEGIT) {
            if (!flags.isEmpty()) {
                fail(name, "labelled legit but flagged " + flags.size() + " time(s): " + flags.get(0).toLogRow());
            }
            return;
        }
        if (header.label() == RecordingLabel.CHEAT && flags.isEmpty()) {
            fail(name, "labelled cheat but nothing flagged");
        }
    }

    private void checkDeterminism(String name, Path recording, ReplayResult first) {
        ReplayResult second = ReplayRun.run(recording);
        List<TickDigest> a = first.digests();
        List<TickDigest> b = second.digests();
        if (a.size() != b.size()) {
            fail(name, "not deterministic: " + a.size() + " ticks then " + b.size());
            return;
        }
        for (int i = 0; i < a.size(); i++) {
            String field = a.get(i).firstDifference(b.get(i));
            if (field != null) {
                fail(name, "not deterministic: tick " + a.get(i).tick() + " differs on " + field);
                return;
            }
        }
    }

    private void reportGolden(String name, Path recording, ReplayResult result) {
        try {
            if (options.accept()) {
                GoldenStore.write(recording, result);
                System.out.println("    golden accepted (" + result.digests().size() + " rows)");
                return;
            }

            GoldenStore.Diff diff = GoldenStore.diff(recording, result);
            if (diff == null) {
                System.out.println("    golden identical");
            } else if (diff.row() < 0) {
                System.out.println("    " + diff.reason() + ", run with -Paccept to write it");
            } else {
                System.out.println("    golden drifted at row " + diff.row() + " (" + diff.reason() + ")");
                if (diff.golden() != null) System.out.println("      golden: " + diff.golden());
                if (diff.replay() != null) System.out.println("      replay: " + diff.replay());
            }

            GoldenStore.FlagDiff flagDiff = GoldenStore.diffFlags(recording, result);
            if (flagDiff != null) {
                System.out.println("    flag log drifted: " + flagDiff.goldenCount()
                        + " committed, " + flagDiff.replayCount() + " now");
            }
        } catch (IOException failure) {
            fail(name, "golden handling failed: " + failure.getMessage());
        }
    }

    private void printTrace(String name, ReplayResult result) {
        System.out.println(name + " ticks " + options.from() + " to " + options.to());
        for (TickDigest digest : result.digests()) {
            if (digest.tick() < options.from() || digest.tick() > options.to()) continue;
            System.out.println("  " + digest.toGoldenRow());
        }
    }

    private void fail(String name, String reason) {
        failures.add(name + ": " + reason);
        System.out.println("    FAIL " + reason);
    }
}
