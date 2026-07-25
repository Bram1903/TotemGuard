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

package com.deathmotion.totemguard.common.replay.capture;

import com.deathmotion.totemguard.common.config.view.ConfigView;
import com.deathmotion.totemguard.common.physics.trace.TraceFrame;
import com.deathmotion.totemguard.common.physics.verdict.TickOutcome;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.replay.format.*;
import com.deathmotion.totemguard.common.util.TGVersions;
import com.github.retrooper.packetevents.PacketEvents;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

public final class RecordingSession {

    private static final RecordingFrame POISON = new RecordingFrame.EventLoop(Long.MIN_VALUE);
    private static final int QUEUE_CAPACITY = 16384;
    private static final long MAX_NANOS = 60L * 60L * 1_000_000_000L;

    private final Logger logger;
    private final ArrayBlockingQueue<RecordingFrame> queue;
    private final Thread writerThread;
    private final AtomicBoolean closing = new AtomicBoolean();

    private final AtomicLong frames = new AtomicLong();
    private final AtomicLong dropped = new AtomicLong();
    private final AtomicLong judged = new AtomicLong();
    private final AtomicLong coasted = new AtomicLong();
    private final AtomicLong declined = new AtomicLong();
    private final AtomicLong flags = new AtomicLong();

    @Getter
    private final TGPlayer player;
    @Getter
    private final RecordingLabel label;
    @Getter
    private final String scenario;
    @Getter
    private final List<String> tags;
    @Getter
    private final Path file;
    @Getter
    private final long startNanos;
    @Getter
    private final long startEpochMillis;
    @Getter
    private final boolean observeOnly;

    private final long openedNanos;

    private volatile @Nullable RecordingWriter writer;
    private volatile @Nullable IOException failure;
    private volatile @Nullable RecordingTrailer trailer;

    public RecordingSession(TGPlayer player, RecordingLabel label, String scenario, String note,
                            List<String> tags, Path file, boolean observeOnly, ConfigView config,
                            Logger logger) {
        this(player, label, scenario, note, tags, file, observeOnly, config, logger,
                player.getClock().nanos(), player.getClock().millis());
    }

    public RecordingSession(TGPlayer player, RecordingLabel label, String scenario, String note,
                            List<String> tags, Path file, boolean observeOnly, ConfigView config,
                            Logger logger, long startNanos, long startEpochMillis) {
        this.player = player;
        this.observeOnly = observeOnly;
        this.label = label;
        this.scenario = scenario;
        this.tags = List.copyOf(tags);
        this.file = file;
        this.logger = logger;
        this.startNanos = startNanos;
        this.startEpochMillis = startEpochMillis;
        this.openedNanos = player.getClock().nanos();

        this.queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);

        RecordingHeader header = buildHeader(player, label, scenario, note, tags, config);
        this.writerThread = new Thread(() -> run(header), "TotemGuard-Replay-" + player.getName());
        this.writerThread.setDaemon(true);
        this.writerThread.start();
    }

    private RecordingHeader buildHeader(TGPlayer player, RecordingLabel label, String scenario,
                                        String note, List<String> tags, ConfigView config) {
        return new RecordingHeader(
                ReplayFormat.VERSION,
                TGVersions.RAW,
                TGVersions.CURRENT.snapshotCommit() == null ? "" : TGVersions.CURRENT.snapshotCommit(),
                PacketEvents.getAPI().getServerManager().getVersion().getProtocolVersion(),
                player.getClientVersion().getProtocolVersion(),
                player.supportsEndTick(),
                player.getName(),
                player.getUuid(),
                startEpochMillis,
                startNanos,
                label,
                scenario,
                note,
                List.copyOf(tags),
                observeOnly,
                player.getCheckManager().getSnapshot(),
                new RecordingHeader.PhysicsConfig(
                        config.physicsPreset().name(),
                        config.physicsEngineSetback(),
                        config.physicsEngineCloseInventory(),
                        config.physicsEngineFallDamage(),
                        config.physicsEngineTimerPacketCancel(),
                        config.physicsSimulateFlying()),
                player.getVersionGates().snapshot(),
                null,
                ReplayFormat.FILTER_VERSION);
    }

    private void run(RecordingHeader header) {
        try {
            int[] table = player.getWorldMirror().reader().stateMap().snapshotTable();
            RecordingHeader resolved = new RecordingHeader(header.formatVersion(), header.pluginVersion(),
                    header.gitHash(), header.serverProtocol(), header.clientProtocol(),
                    header.supportsEndTick(), header.playerName(), header.playerUuid(),
                    header.startEpochMillis(), header.startNanos(), header.label(), header.scenario(),
                    header.note(), header.tags(), header.observeOnly(), header.checkSnapshot(), header.physicsConfig(),
                    header.versionGates(), table, header.filterVersion());

            RecordingWriter open = new RecordingWriter(file, resolved);
            this.writer = open;

            while (true) {
                RecordingFrame frame = queue.take();
                if (frame == POISON) break;
                open.write(frame);
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } catch (IOException io) {
            this.failure = io;
            logger.warning("Replay recording " + file.getFileName() + " failed: " + io.getMessage());
        }
    }

    public void offer(RecordingFrame frame) {
        if (closing.get()) return;
        if (!queue.offer(frame)) {
            dropped.incrementAndGet();
            return;
        }
        frames.incrementAndGet();
    }

    public void onTick(TraceFrame frame) {
        TickOutcome outcome = TickOutcome.values()[frame.outcome];
        switch (outcome) {
            case JUDGED -> judged.incrementAndGet();
            case COASTED -> coasted.incrementAndGet();
            case DECLINED -> declined.incrementAndGet();
            default -> {
            }
        }
        offer(new RecordingFrame.Verdict(player.getClock().nanos(), TickDigest.of(frame)));
    }

    public void onFlag() {
        flags.incrementAndGet();
    }

    public void onFlag(String check, int violations, @Nullable String debug) {
        flags.incrementAndGet();
        offer(new RecordingFrame.Flag(player.getClock().nanos(), check, violations,
                debug == null ? "" : debug));
    }

    public boolean expired(long nowNanos) {
        return nowNanos - openedNanos >= MAX_NANOS;
    }

    public long elapsedNanos(long nowNanos) {
        return nowNanos - startNanos;
    }

    public long frameCount() {
        return frames.get();
    }

    public long droppedCount() {
        return dropped.get();
    }

    public long byteCount() {
        RecordingWriter open = this.writer;
        return open == null ? 0L : open.compressedBytes();
    }

    public long judgedCount() {
        return judged.get();
    }

    public long flagCount() {
        return flags.get();
    }

    public boolean degraded() {
        return dropped.get() > 0;
    }

    public @Nullable IOException failure() {
        return failure;
    }

    public @Nullable RecordingTrailer trailer() {
        return trailer;
    }

    public void close(long nowNanos) {
        if (!closing.compareAndSet(false, true)) return;
        RecordingTrailer stamped = new RecordingTrailer(nowNanos, frames.get(), dropped.get(),
                judged.get(), coasted.get(), declined.get(), flags.get(), false, 0L);
        this.trailer = stamped;
        try {
            queue.offer(new RecordingFrame.End(nowNanos, stamped), 2, TimeUnit.SECONDS);
            if (!queue.offer(POISON, 2, TimeUnit.SECONDS)) {
                writerThread.interrupt();
            }
            writerThread.join(10_000L);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
        RecordingWriter open = this.writer;
        if (open != null) {
            try {
                open.close();
            } catch (IOException io) {
                this.failure = io;
            }
        }
    }

    public void applyPruneResult(long prunedFrames) {
        RecordingTrailer current = this.trailer;
        if (current == null) return;
        this.trailer = new RecordingTrailer(current.endNanos(), current.frames(), current.droppedFrames(),
                current.judgedTicks(), current.coastedTicks(), current.declinedTicks(), current.flags(),
                true, prunedFrames);
    }
}
