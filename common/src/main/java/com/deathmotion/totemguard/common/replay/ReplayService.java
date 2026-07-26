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

package com.deathmotion.totemguard.common.replay;

import com.deathmotion.totemguard.api.config.key.ConfigKey;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.config.key.MessagesKeys;
import com.deathmotion.totemguard.common.config.view.ConfigView;
import com.deathmotion.totemguard.common.platform.player.PlatformPlayer;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.replay.capture.ArmedRecording;
import com.deathmotion.totemguard.common.replay.capture.RecordingOverlayProvider;
import com.deathmotion.totemguard.common.replay.capture.RecordingSession;
import com.deathmotion.totemguard.common.replay.format.IntegrationInput;
import com.deathmotion.totemguard.common.replay.format.RecordingFrame;
import com.deathmotion.totemguard.common.replay.format.RecordingLabel;
import com.deathmotion.totemguard.common.replay.format.RecordingTrailer;
import com.deathmotion.totemguard.common.replay.playback.ReplayRun;
import com.deathmotion.totemguard.common.replay.retention.PrologueBuilder;
import com.deathmotion.totemguard.common.replay.retention.RetentionBuffer;
import com.deathmotion.totemguard.common.replay.retention.RetentionPolicy;
import com.deathmotion.totemguard.common.replay.retention.RetentionSweep;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.netty.channel.ChannelHelper;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.User;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class ReplayService {

    private static final int LOGIN_BUFFER_LIMIT = 64;
    private static final String DIRECTORY = "replays";
    private static final long ARM_TIMEOUT_MILLIS = 60_000L;
    private static final DateTimeFormatter AUTO_SCENARIO =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final TGPlatform platform;
    private final RecordingLibrary library;
    private final RecordingIndex index;
    private final RecordingSummaryCache summaries;
    private final ConcurrentMap<String, ArmedRecording> arms = new ConcurrentHashMap<>();
    private final ConcurrentMap<User, CaptureState> captures = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, RetentionBuffer> retention = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, AutoSession> autoSessions = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, UUID> hudViewers = new ConcurrentHashMap<>();
    private final AtomicBoolean playback = new AtomicBoolean();

    private volatile boolean retentionEnabled;

    public ReplayService(TGPlatform platform) {
        this.platform = platform;
        this.library = new RecordingLibrary(resolveDirectory(platform));
        this.index = new RecordingIndex(library, platform.getScheduler());
        this.summaries = new RecordingSummaryCache(library, platform.getScheduler());
        this.retentionEnabled = readRetentionEnabled(platform);
    }

    private static boolean readRetentionEnabled(TGPlatform platform) {
        try {
            return platform.getConfigRepository().configView().replayRetentionEnabled();
        } catch (RuntimeException unavailable) {
            return false;
        }
    }

    private static Path resolveDirectory(TGPlatform platform) {
        return Paths.get(platform.getPluginDirectory()).resolve(DIRECTORY);
    }

    private static String tagOf(String clientVersionName) {
        return clientVersionName.startsWith("V_") ? clientVersionName.substring(2) : clientVersionName;
    }

    public RecordingLibrary library() {
        return library;
    }

    public RecordingIndex index() {
        return index;
    }

    public RecordingSummaryCache summaries() {
        return summaries;
    }

    public boolean playing() {
        return playback.get();
    }

    public boolean beginPlayback() {
        return playback.compareAndSet(false, true);
    }

    public void endPlayback() {
        playback.set(false);
    }

    public String relative(Path file) {
        Path root = library.root();
        Path resolved = file.startsWith(root) ? root.relativize(file) : file.getFileName();
        return resolved.toString().replace('\\', '/');
    }

    public String named(Path file) {
        return RecordingLibrary.display(relative(file));
    }

    public void run(String query, Consumer<Component> sink) {
        if (!beginPlayback()) {
            sink.accept(message(MessagesKeys.REPLAY_RUN_BUSY, Map.of()));
            return;
        }

        platform.getScheduler().runAsyncTask(() -> {
            try {
                Path file = library.resolve(query);
                if (file == null) {
                    sink.accept(message(MessagesKeys.REPLAY_FILE_NOT_FOUND, Map.of("tg_file", query)));
                    return;
                }
                String name = named(file);
                sink.accept(message(MessagesKeys.REPLAY_RUN_STARTED, Map.of("tg_file", name)));
                ReplayReport.send(sink, name, ReplayRun.run(file));
            } finally {
                endPlayback();
            }
        });
    }

    private Component message(ConfigKey<String> key, Map<String, Object> extras) {
        return platform.getMessageService().getComponent(key, extras);
    }

    public @NotNull ArmResult arm(TGPlayer player, RecordingLabel label, String scenario, String note,
                                  List<String> tags) {
        return arm(player, label, scenario, note, tags, false);
    }

    public @NotNull ArmResult shadow(TGPlayer player, String scenario, String note, List<String> tags) {
        return arm(player, RecordingLabel.SCRATCH, scenario, note, tags, true);
    }

    private @NotNull ArmResult arm(TGPlayer player, RecordingLabel label, String scenario, String note,
                                   List<String> tags, boolean shadow) {
        RecordingSession running = sessionOf(player);
        if (running != null) stop(player, "re-armed");

        player.clearCachedCheckSnapshot();

        long now = player.getClock().millis();
        ArmedRecording arm = new ArmedRecording(player.getUuid(), player.getName(), label, scenario,
                note, List.copyOf(tags), shadow ? Long.MAX_VALUE : now + ARM_TIMEOUT_MILLIS, shadow, false);
        ArmedRecording replaced = arms.put(key(player.getName()), arm);

        Map<String, Object> extras = Map.of(
                "tg_label", label.id(),
                "tg_scenario", scenario,
                "tg_seconds", shadow ? 0L : arm.secondsLeft(now));
        if (shadow) {
            tell(player, MessagesKeys.REPLAY_SHADOW_ARMED, extras);
            return replaced == null ? ArmResult.ARMED : ArmResult.REPLACED;
        }

        tell(player, MessagesKeys.REPLAY_ARMED, extras);
        Component screen = platform.getMessageService().getComponent(MessagesKeys.REPLAY_ARM_KICK_SCREEN, extras);
        player.disconnect(screen, "replay recording armed");
        return replaced == null ? ArmResult.ARMED : ArmResult.REPLACED;
    }

    public boolean cancel(String playerName) {
        return arms.remove(key(playerName)) != null;
    }

    public @Nullable ArmedRecording armOf(String playerName) {
        return arms.get(key(playerName));
    }

    public Collection<ArmedRecording> arms() {
        return List.copyOf(arms.values());
    }

    private String key(@Nullable String name) {
        return name == null ? "" : name.toLowerCase(Locale.ROOT);
    }

    public void onLoginStart(User user, String username) {
        ArmedRecording arm = arms.get(key(username));
        if (arm == null) return;

        long now = System.currentTimeMillis();
        arms.remove(key(username));
        if (arm.expired(now)) {
            platform.getLogger().info("Replay arm for " + username + " expired before the rejoin.");
            return;
        }
        captures.put(user, new CaptureState(arm));
    }

    public boolean hasCapture(User user) {
        if (retentionEnabled) return true;
        return !captures.isEmpty() && captures.containsKey(user);
    }

    public void onPacket(User user, boolean inbound, boolean cancelled, PacketTypeCommon type,
                         ConnectionState state, int packetId, byte[] payload, @Nullable TGPlayer player) {
        CaptureState capture = captures.get(user);
        if (capture == null) {
            RetentionBuffer buffer = retentionFor(player);
            if (buffer != null) {
                buffer.onPacket(inbound, cancelled, type, state, packetId, payload);
            }
            return;
        }
        if (capture.aborted) return;

        RecordingSession session = capture.session;
        if (session == null) {
            if (player == null) {
                bufferLoginFrame(user, capture, inbound, cancelled, type, state, packetId, payload);
                return;
            }
            session = promote(user, capture, player);
            if (session == null) return;
        }

        session.offer(new RecordingFrame.Packet(inbound, cancelled, state, packetId,
                player == null ? session.getStartNanos() : player.getClock().nanos(), payload));
    }

    private void bufferLoginFrame(User user, CaptureState capture, boolean inbound, boolean cancelled,
                                  PacketTypeCommon type, ConnectionState state, int packetId,
                                  byte[] payload) {
        if (capture.buffered.size() >= LOGIN_BUFFER_LIMIT) {
            capture.aborted = true;
            captures.remove(user);
            platform.getLogger().warning("Replay capture for " + capture.arm.name()
                    + " aborted: the login phase produced more than " + LOGIN_BUFFER_LIMIT + " frames.");
            return;
        }
        capture.buffered.add(new RecordingFrame.Packet(inbound, cancelled, state, packetId,
                System.nanoTime(), payload));
    }

    private @Nullable RecordingSession promote(User user, CaptureState capture, TGPlayer player) {
        ArmedRecording arm = capture.arm;
        ConfigView config = platform.getConfigRepository().configView();
        Path file = library.allocate(arm.label(), arm.scenario(),
                tagOf(player.getClientVersion().name()));

        RecordingSession session;
        try {
            session = new RecordingSession(player, arm.label(), arm.scenario(), arm.note(), arm.tags(),
                    file, !arm.shadow(), config, platform.getLogger());
        } catch (RuntimeException failure) {
            capture.aborted = true;
            captures.remove(user);
            tell(player, MessagesKeys.REPLAY_START_FAILED,
                    Map.of("tg_error", String.valueOf(failure.getMessage())));
            return null;
        }

        capture.session = session;
        capture.player = player;

        long baseNanos = session.getStartNanos();
        for (RecordingFrame buffered : capture.buffered) {
            if (buffered instanceof RecordingFrame.Packet packet) {
                session.offer(new RecordingFrame.Packet(packet.inbound(), packet.cancelled(),
                        packet.state(), packet.packetId(), baseNanos, packet.payload()));
            }
        }
        capture.buffered.clear();
        session.offer(new RecordingFrame.Attach(baseNanos));

        attach(player, session, arm.shadow());
        if (arm.shadow()) {
            platform.getLogger().info("Replay shadow recording started for " + player.getName() + ".");
            return session;
        }
        tell(player, MessagesKeys.REPLAY_STARTED, Map.of(
                "tg_label", arm.label().id(),
                "tg_scenario", arm.scenario()));
        return session;
    }

    private void attach(TGPlayer player, RecordingSession session, boolean shadow) {
        player.setObserveOnly(session.isObserveOnly());
        player.getPhysics().trace().sink(session::onTick);
        player.setFlagSink((check, violations, debug, compiled, extras) -> {
            session.onFlag(check.getName(), violations, debug);
            platform.getAlertRepository().alert(check, violations, debug, compiled, extras);
        });
        player.getLatencyHandler().eventLoopSink(action ->
                player.getLatencyHandler().runInChannelEventLoop(() -> {
                    session.offer(new RecordingFrame.EventLoop(player.getClock().nanos()));
                    action.run();
                }));
        if (shadow) return;
        hudViewers.put(player.getUuid(), player.getUuid());
        player.getDebugOverlayManager().prepend(new RecordingOverlayProvider(session));
    }

    private void detach(TGPlayer player) {
        player.setObserveOnly(false);
        player.getPhysics().trace().sink(null);
        player.setFlagSink(null);
        player.getLatencyHandler().eventLoopSink(null);
        player.getDebugOverlayManager().prepend(null);
        clearHuds(player);
        RetentionBuffer buffer = retention.remove(player.getUuid());
        if (buffer != null) buffer.detach();
    }

    public void onIntegrationInput(TGPlayer player, IntegrationInput input, int id, long timestamp) {
        RecordingSession session = sessionOf(player);
        if (session == null) {
            onIntegrationRetained(player, input, id, timestamp);
            return;
        }
        session.offer(new RecordingFrame.Integration(player.getClock().nanos(), input, id, timestamp));
    }

    public void onServerTick(TGPlayer player, long tick) {
        RecordingSession session = sessionOf(player);
        if (session == null) {
            onRetentionTick(player, tick);
            return;
        }
        long now = player.getClock().nanos();
        session.offer(new RecordingFrame.ServerTick(now, tick));
        if (session.expired(now)) {
            if (session.isObserveOnly()) {
                tell(player, MessagesKeys.REPLAY_MAX_LENGTH, Map.of());
            }
            stop(player, "length cap");
        }
    }

    private @Nullable RetentionBuffer retentionFor(@Nullable TGPlayer player) {
        if (!retentionEnabled || player == null || player.isSynthetic()) return null;
        return retention.computeIfAbsent(player.getUuid(), uuid -> openBuffer(player));
    }

    private RetentionBuffer openBuffer(TGPlayer player) {
        RetentionBuffer buffer = new RetentionBuffer(player,
                PacketEvents.getAPI().getServerManager().getVersion());
        player.getLatencyHandler().eventLoopSink(action ->
                player.getLatencyHandler().runInChannelEventLoop(() -> {
                    buffer.onEventLoop();
                    action.run();
                }));
        return buffer;
    }

    private void onRetentionTick(TGPlayer player, long tick) {
        boolean enabled = platform.getConfigRepository().configView().replayRetentionEnabled();
        if (enabled != retentionEnabled) {
            retentionEnabled = enabled;
            if (!enabled) releaseRetention();
        }
        RetentionBuffer buffer = retentionFor(player);
        if (buffer == null) return;
        buffer.onServerTick(tick);

        AutoSession auto = autoSessions.get(player.getUuid());
        if (auto == null || !buffer.live()) return;
        long now = player.getClock().nanos();
        if (now >= auto.deadlineNanos || now - auto.startedNanos >= RetentionPolicy.MAX_SESSION_NANOS) {
            finishAuto(player, now >= auto.deadlineNanos ? "post-roll elapsed" : "length cap");
        }
    }

    public void onIntegrationRetained(TGPlayer player, IntegrationInput input, int id, long timestamp) {
        RetentionBuffer buffer = retention.get(player.getUuid());
        if (buffer == null) return;
        buffer.onFrame(new RecordingFrame.Integration(player.getClock().nanos(), input, id, timestamp));
    }

    public void onCheckFlag(TGPlayer player, String check, int violations, @Nullable String debug) {
        if (!retentionEnabled || player.isSynthetic()) return;
        if (captureOf(player) != null) return;

        long now = player.getClock().nanos();
        AutoSession auto = autoSessions.get(player.getUuid());
        if (auto != null) {
            auto.deadlineNanos = Math.max(auto.deadlineNanos, now + RetentionPolicy.POST_ROLL_NANOS);
            return;
        }

        RetentionBuffer buffer = retention.get(player.getUuid());
        if (buffer == null) return;
        buffer.onFrame(new RecordingFrame.Flag(now, check, violations, debug == null ? "" : debug));
        beginAuto(player, buffer, check, now);
    }

    public boolean dumpRetained(TGPlayer player, String reason) {
        if (!retentionEnabled || captureOf(player) != null) return false;
        if (autoSessions.containsKey(player.getUuid())) return false;
        RetentionBuffer buffer = retention.get(player.getUuid());
        if (buffer == null) return false;
        long now = player.getClock().nanos();
        buffer.onFrame(new RecordingFrame.Mark(now, reason));
        beginAuto(player, buffer, reason, now);
        return true;
    }

    private void beginAuto(TGPlayer player, RetentionBuffer buffer, String check, long now) {
        RetentionBuffer.Claim claim = buffer.claim();
        if (claim == null) return;

        String folder = player.getUuid().toString();
        long openedNanos = claim.keyframe().openedNanos();
        long openedMillis = player.getClock().millis() - (now - openedNanos) / 1_000_000L;
        String scenario = AUTO_SCENARIO.format(
                Instant.ofEpochMilli(openedMillis).atZone(ZoneId.systemDefault()));

        platform.getScheduler().runAsyncTask(() -> {
            RecordingSession session;
            try {
                Path file = library.allocate(RecordingLabel.AUTO, folder, scenario,
                        tagOf(player.getClientVersion().name()));
                session = new RecordingSession(player, RecordingLabel.AUTO, scenario,
                        "opened by " + check, List.of("auto", RecordingLibrary.sanitize(check)),
                        file, false, platform.getConfigRepository().configView(), platform.getLogger(),
                        openedNanos, openedMillis, claim.keyframe().checks());
            } catch (RuntimeException failure) {
                buffer.abandon();
                platform.getLogger().warning("Retained recording for " + player.getName()
                        + " could not be opened: " + failure.getMessage());
                return;
            }

            if (!ChannelHelper.isOpen(player.getUser().getChannel())) {
                buffer.abandon();
                session.close(player.getClock().nanos());
                discard(session);
                return;
            }

            player.getLatencyHandler().runInChannelEventLoop(
                    () -> materialise(player, buffer, claim, session, now, openedNanos));
        });
    }

    private void materialise(TGPlayer player, RetentionBuffer buffer, RetentionBuffer.Claim claim,
                             RecordingSession session, long now, long openedNanos) {
        try {
            var here = player.getData().getMovementData().getCurrent();
            PrologueBuilder.Result prologue = PrologueBuilder.build(claim.keyframe(),
                    claim.keyframe().journal().rewind(player.getWorldMirror().blocks()),
                    player.getWorldMirror(), claim.visited(),
                    PacketEvents.getAPI().getServerManager().getVersion(),
                    claim.keyframe().openedNanos(),
                    here == null ? 0.0 : here.getX(),
                    here == null ? 0.0 : here.getY(),
                    here == null ? 0.0 : here.getZ());
            buffer.promote(session, prologue, claim.fromCurrentOnly());
            autoSessions.put(player.getUuid(), new AutoSession(session, buffer, player,
                    now + RetentionPolicy.POST_ROLL_NANOS, openedNanos));
            attachAuto(player, session);
            platform.getLogger().info("[Replay] retained recording opened for " + player.getName()
                    + ": " + prologue.columns().size() + " columns, "
                    + claim.keyframe().entities().size() + " entities"
                    + (prologue.chunkFailures() == 0 ? "" : ", " + prologue.chunkFailures() + " chunk(s) unencodable")
                    + (prologue.spawnFailures() == 0 ? "" : ", " + prologue.spawnFailures() + " entity spawn(s) unencodable"));
        } catch (RuntimeException failure) {
            autoSessions.remove(player.getUuid());
            buffer.abandon();
            session.close(player.getClock().nanos());
            discard(session);
            platform.getLogger().warning("Retained recording for " + player.getName()
                    + " could not be built: " + failure);
        }
    }

    private void attachAuto(TGPlayer player, RecordingSession session) {
        player.getPhysics().trace().sink(session::onTick);
        player.setFlagSink((check, violations, debug, compiled, extras) -> {
            session.onFlag(check.getName(), violations, debug);
            platform.getAlertRepository().alert(check, violations, debug, compiled, extras);
        });
    }

    private void finishAuto(TGPlayer player, String reason) {
        AutoSession auto = autoSessions.remove(player.getUuid());
        if (auto == null) return;
        player.getPhysics().trace().sink(null);
        player.setFlagSink(null);
        auto.buffer.demote();

        long closedAt = player.getClock().nanos();
        platform.getScheduler().runAsyncTask(() -> {
            auto.session.close(closedAt);
            platform.getLogger().info("[Replay] retained recording saved: " + auto.session.getFile()
                    + " (" + auto.session.frameCount() + " frames, flags " + auto.session.flagCount()
                    + ", reason " + reason + ")");
            RecordingPostProcessor.run(auto.session, platform.getLogger());
            sweepAuto(auto.player.getUuid());
            index.invalidate();
        });
    }

    private void sweepAuto(UUID uuid) {
        long maxBytes = platform.getConfigRepository().configView().replayRetentionMaxBytesPerPlayer();
        if (maxBytes <= 0L) return;
        RetentionSweep.Result result = RetentionSweep.run(
                library.folder(RecordingLabel.AUTO, uuid.toString()), maxBytes, platform.getLogger());
        if (result.deleted() > 0) index.invalidate();
    }

    private void releaseRetention() {
        for (UUID uuid : List.copyOf(autoSessions.keySet())) {
            TGPlayer player = platform.getPlayerRepository().getPlayer(uuid);
            if (player != null) finishAuto(player, "retention disabled");
        }
        for (RetentionBuffer buffer : List.copyOf(retention.values())) buffer.detach();
        retention.clear();
    }

    private void dropRetention(UUID uuid) {
        AutoSession auto = autoSessions.remove(uuid);
        RetentionBuffer buffer = retention.remove(uuid);
        if (buffer != null) buffer.detach();
        if (auto == null) return;
        auto.player.getPhysics().trace().sink(null);
        auto.player.setFlagSink(null);
        long closedAt = auto.player.getClock().nanos();
        platform.getScheduler().runAsyncTask(() -> {
            auto.session.close(closedAt);
            platform.getLogger().info("[Replay] retained recording saved on disconnect: "
                    + auto.session.getFile() + " (" + auto.session.frameCount() + " frames, flags "
                    + auto.session.flagCount() + ")");
            RecordingPostProcessor.run(auto.session, platform.getLogger());
            sweepAuto(auto.player.getUuid());
            index.invalidate();
        });
    }

    public boolean retaining(TGPlayer player) {
        return retention.containsKey(player.getUuid());
    }

    public @Nullable RetentionBuffer retentionOf(TGPlayer player) {
        return retention.get(player.getUuid());
    }

    public boolean toggleHud(TGPlayer viewer, TGPlayer target) {
        RecordingSession session = sessionOf(target);
        if (session == null) return false;

        if (target.getUuid().equals(hudViewers.get(viewer.getUuid()))) {
            hudViewers.remove(viewer.getUuid());
            viewer.getDebugOverlayManager().prepend(null);
            return false;
        }
        hudViewers.put(viewer.getUuid(), target.getUuid());
        viewer.getDebugOverlayManager().prepend(new RecordingOverlayProvider(session));
        return true;
    }

    public boolean watching(TGPlayer viewer, TGPlayer target) {
        return target.getUuid().equals(hudViewers.get(viewer.getUuid()));
    }

    private void clearHuds(TGPlayer target) {
        UUID uuid = target.getUuid();
        hudViewers.remove(uuid);
        for (Map.Entry<UUID, UUID> entry : hudViewers.entrySet()) {
            if (!uuid.equals(entry.getValue())) continue;
            hudViewers.remove(entry.getKey());
            TGPlayer viewer = platform.getPlayerRepository().getPlayer(entry.getKey());
            if (viewer != null) viewer.getDebugOverlayManager().prepend(null);
        }
    }

    public void mark(TGPlayer player, String label) {
        RecordingSession session = sessionOf(player);
        if (session == null) return;
        session.offer(new RecordingFrame.Mark(player.getClock().nanos(), label));
    }

    public void onDisconnect(User user) {
        UUID uuid = user.getUUID();
        if (uuid != null) {
            hudViewers.remove(uuid);
            dropRetention(uuid);
        }
        CaptureState capture = captures.remove(user);
        if (capture == null) return;
        RecordingSession session = capture.session;
        TGPlayer player = capture.player;
        if (session == null || player == null) return;
        finish(player, session, "disconnect", capture.arm.shadow());
    }

    public boolean stop(TGPlayer player, String reason) {
        CaptureState capture = captureOf(player);
        if (capture == null || capture.session == null) return false;
        captures.values().remove(capture);
        finish(player, capture.session, reason, capture.arm.shadow());
        return true;
    }

    private void finish(TGPlayer player, RecordingSession session, String reason, boolean shadow) {
        detach(player);
        long closedAt = player.getClock().nanos();
        platform.getScheduler().runAsyncTask(() -> {
            session.close(closedAt);
            postProcess(player, session, reason, shadow);
        });
    }

    private void postProcess(TGPlayer player, RecordingSession session, String reason, boolean shadow) {
        if (shadow) {
            if (session.flagCount() == 0) {
                discard(session);
                return;
            }
            platform.getLogger().info("Replay shadow recording kept: " + session.getFile()
                    + " (" + session.flagCount() + " flag(s), reason " + reason + ")");
            RecordingPostProcessor.run(session, platform.getLogger());
            index.invalidate();
            return;
        }
        report(player, session, reason);
        RecordingPostProcessor.run(session, platform.getLogger());
        index.invalidate();
    }

    private void discard(RecordingSession session) {
        try {
            Files.deleteIfExists(session.getFile());
            index.invalidate();
            platform.getLogger().info("Replay shadow recording for " + session.getPlayer().getName()
                    + " discarded: nothing flagged.");
        } catch (IOException failure) {
            platform.getLogger().warning("Could not discard the shadow recording "
                    + session.getFile() + ": " + failure.getMessage());
        }
    }

    private void report(TGPlayer player, RecordingSession session, String reason) {
        RecordingTrailer trailer = session.trailer();
        long frames = trailer == null ? session.frameCount() : trailer.frames();
        long elapsed = trailer == null ? 0L : trailer.endNanos() - session.getStartNanos();
        Map<String, Object> extras = Map.of(
                "tg_file", RecordingLibrary.display(library.root().relativize(session.getFile()).toString()),
                "tg_elapsed", ReplayText.elapsed(elapsed),
                "tg_frames", frames,
                "tg_size", ReplayText.size(fileSize(session)),
                "tg_judged", session.judgedCount(),
                "tg_flags", session.flagCount());
        tell(player, MessagesKeys.REPLAY_STOPPED, extras);
        tell(player, MessagesKeys.REPLAY_STOPPED_REASON, Map.of("tg_reason", reason));
        if (session.degraded()) {
            tell(player, MessagesKeys.REPLAY_DEGRADED, Map.of("tg_dropped", session.droppedCount()));
        }
        platform.getLogger().info("Replay recording saved: " + session.getFile()
                + " (" + frames + " frames, judged " + session.judgedCount()
                + ", flags " + session.flagCount() + ", reason " + reason + ")");
    }

    private long fileSize(RecordingSession session) {
        try {
            return Files.size(session.getFile());
        } catch (IOException failure) {
            return session.byteCount();
        }
    }

    private void tell(TGPlayer player, ConfigKey<String> key, Map<String, Object> extras) {
        PlatformPlayer target = player.getPlatformPlayer();
        if (target == null) return;
        if (!ChannelHelper.isOpen(player.getUser().getChannel())) return;
        target.sendMessage(platform.getMessageService().getComponent(key, player, null, extras));
    }

    public @Nullable RecordingSession sessionOf(TGPlayer player) {
        CaptureState capture = captureOf(player);
        return capture == null ? null : capture.session;
    }

    private @Nullable CaptureState captureOf(TGPlayer player) {
        for (CaptureState capture : captures.values()) {
            if (capture.player == player) return capture;
        }
        return null;
    }

    public Collection<RecordingSession> active() {
        List<RecordingSession> sessions = new ArrayList<>();
        for (CaptureState capture : captures.values()) {
            RecordingSession session = capture.session;
            if (session != null) sessions.add(session);
        }
        return sessions;
    }

    public void shutdown() {
        hudViewers.clear();
        for (Map.Entry<UUID, AutoSession> entry : List.copyOf(autoSessions.entrySet())) {
            autoSessions.remove(entry.getKey());
            AutoSession auto = entry.getValue();
            auto.session.close(auto.player.getClock().nanos());
            platform.getLogger().info("Retained recording saved on shutdown, unverified and unpruned: "
                    + auto.session.getFile());
        }
        for (RetentionBuffer buffer : List.copyOf(retention.values())) buffer.detach();
        retention.clear();
        for (CaptureState capture : List.copyOf(captures.values())) {
            captures.values().remove(capture);
            RecordingSession session = capture.session;
            TGPlayer player = capture.player;
            if (session == null || player == null) continue;
            detach(player);
            session.close(player.getClock().nanos());
            if (capture.arm.shadow() && session.flagCount() == 0) {
                discard(session);
                continue;
            }
            platform.getLogger().info("Replay recording saved on shutdown, unverified and unpruned: "
                    + session.getFile());
        }
        arms.clear();
    }

    public enum ArmResult {

        ARMED,
        REPLACED
    }

    private static final class AutoSession {

        private final RecordingSession session;
        private final RetentionBuffer buffer;
        private final TGPlayer player;
        private final long startedNanos;
        private volatile long deadlineNanos;

        private AutoSession(RecordingSession session, RetentionBuffer buffer, TGPlayer player,
                            long deadlineNanos, long startedNanos) {
            this.session = session;
            this.buffer = buffer;
            this.player = player;
            this.deadlineNanos = deadlineNanos;
            this.startedNanos = startedNanos;
        }
    }

    private static final class CaptureState {

        private final ArmedRecording arm;
        private final List<RecordingFrame> buffered = new ArrayList<>();
        private volatile @Nullable RecordingSession session;
        private volatile @Nullable TGPlayer player;
        private volatile boolean aborted;

        private CaptureState(ArmedRecording arm) {
            this.arm = arm;
        }
    }
}
