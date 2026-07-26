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

package com.deathmotion.totemguard.common.replay.viewer;

import com.deathmotion.totemguard.api.config.key.ConfigKey;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.config.key.MessagesKeys;
import com.deathmotion.totemguard.common.platform.player.DetachedPlayer;
import com.deathmotion.totemguard.common.platform.player.PlatformPlayer;
import com.deathmotion.totemguard.common.platform.player.WorldAnchor;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.data.Data;
import com.deathmotion.totemguard.common.player.inventory.enums.Issuer;
import com.deathmotion.totemguard.common.replay.ReplayText;
import com.deathmotion.totemguard.common.replay.format.RecordingFrame;
import com.deathmotion.totemguard.common.replay.format.RecordingHeader;
import com.deathmotion.totemguard.common.replay.viewer.camera.CameraPose;
import com.deathmotion.totemguard.common.replay.viewer.camera.CameraScan;
import com.deathmotion.totemguard.common.replay.viewer.camera.CameraTrack;
import com.deathmotion.totemguard.common.replay.viewer.camera.ViewerCameraRig;
import com.deathmotion.totemguard.common.replay.viewer.hud.ViewerCamera;
import com.deathmotion.totemguard.common.replay.viewer.hud.ViewerControl;
import com.deathmotion.totemguard.common.replay.viewer.hud.ViewerHud;
import com.deathmotion.totemguard.common.replay.viewer.hud.ViewerStatus;
import com.deathmotion.totemguard.common.replay.viewer.net.*;
import com.deathmotion.totemguard.common.replay.viewer.state.ViewerAvatar;
import com.deathmotion.totemguard.common.replay.viewer.state.ViewerWorld;
import com.deathmotion.totemguard.common.util.MetadataIndex;
import com.deathmotion.totemguard.common.util.Palette;
import com.deathmotion.totemguard.common.util.ScheduledTask;
import com.deathmotion.totemguard.common.util.Scheduler;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.protocol.world.Difficulty;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class ReplayViewerSession {

    private static final String REPLAY_LEVEL = "totemguard:replay";
    private static final String FALLBACK_LEVEL = "minecraft:overworld";

    private static final long PUMP_INTERVAL_MILLIS = 50L;
    private static final long CONTROL_COOLDOWN_MILLIS = 250L;
    private static final long WORLD_DEADLINE_MILLIS = 5_000L;
    private static final long HANDOVER_DEADLINE_MILLIS = 10_000L;
    private static final long OPEN_ENDED_NANOS = Long.MAX_VALUE / 4L;
    private static final double[] SPEEDS = {0.25, 0.5, 1.0, 2.0, 4.0};
    private static final int SPARE_ENTITY_ID = ViewerAvatar.ENTITY_ID - 1;
    private static final float FLY_SPEED = 0.06f;
    private static final long NOON = 6000L;
    private static final double FOLLOW_DISTANCE = 4.0;
    private static final double FOLLOW_HEIGHT = 1.8;
    private static final Vector3i REPLAY_COMPASS = new Vector3i(0, 64, 0);

    private final TGPlatform platform;
    private final TGPlayer viewer;
    private final String display;
    private final Runnable onClosed;

    private final ViewerStream stream;
    private final ViewerClock clock;
    private final ViewerSink sink;
    private final ViewerWorld world = new ViewerWorld();
    private final ViewerAvatar avatar;
    private final ViewerEntityIds ids = new ViewerEntityIds();
    private final ViewerRouter router;
    private final ViewerHud hud;
    private final ViewerCameraRig rig;
    private final ServerVersion server;
    private final Path file;
    private final long durationNanos;

    private final Object pumpLock = new Object();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicBoolean released = new AtomicBoolean();
    private final AtomicLong heldAlerts = new AtomicLong();

    private @Nullable DetachedPlayer detached;
    private @Nullable ScheduledTask pump;

    @Getter
    private volatile boolean handingBack;
    private volatile ViewerCamera camera = ViewerCamera.FREE;
    private volatile boolean phased;
    private volatile boolean seeking;
    private volatile boolean placed;
    private volatile int selectedSlot;
    private volatile long lastControlMillis;
    private volatile int speedIndex = 2;
    private volatile @Nullable CameraTrack track;
    private volatile @Nullable RecordedWorld recordedWorld;
    private volatile @Nullable RecordedWorld appliedWorld;
    private volatile long startedMillis;
    private volatile long prologueEnd;
    private volatile long flagsSeen;

    private ReplayViewerSession(TGPlatform platform, TGPlayer viewer, Path file, String display,
                                ViewerStream stream, long durationNanos, Runnable onClosed) {
        this.platform = platform;
        this.viewer = viewer;
        this.file = file;
        this.display = display;
        this.onClosed = onClosed;
        this.stream = stream;

        RecordingHeader header = stream.getHeader();
        this.server = PacketEvents.getAPI().getServerManager().getVersion();
        this.durationNanos = durationNanos;
        this.clock = new ViewerClock(header.startNanos(),
                header.startNanos() + (durationNanos > 0 ? durationNanos : OPEN_ENDED_NANOS));
        this.sink = new ViewerSink(viewer.getUser());
        this.hud = new ViewerHud(sink, viewer.getUser());
        this.rig = new ViewerCameraRig(sink, server);

        MetadataIndex metadata = new MetadataIndex(server.toClientVersion());
        this.avatar = new ViewerAvatar(sink, server, metadata, header.playerUuid(), header.playerName());

        User scratch = new User(null, ConnectionState.PLAY, header.client(),
                new UserProfile(header.playerUuid(), header.playerName()));
        scratch.setClientVersion(header.client());
        this.router = new ViewerRouter(sink, world, avatar, ids, scratch, server,
                header.playerUuid(), viewer.getUuid(), this::onWorld);

        int own = viewer.getUser().getEntityId();
        if (own >= 0) ids.map(own, SPARE_ENTITY_ID);
    }

    static ReplayViewerSession open(TGPlatform platform, TGPlayer viewer, Path file, String display,
                                    long durationNanos, Runnable onClosed) throws IOException {
        return new ReplayViewerSession(platform, viewer, file, display,
                new ViewerStream(file), durationNanos, onClosed);
    }

    boolean start() {
        PlatformPlayer platformPlayer = viewer.getPlatformPlayer();
        DetachedPlayer hold = platformPlayer == null ? null : platformPlayer.detachFromWorld();
        if (hold == null) return false;

        this.detached = hold;
        viewer.setReplayViewing(true);
        viewer.getPingData().forgetPendingTransactions();
        this.startedMillis = System.currentTimeMillis();

        Scheduler scheduler = platform.getScheduler();
        scheduler.runAsyncTask(this::buildCameraTrack);
        this.pump = scheduler.runAsyncTaskAtFixedRate(this::pump, PUMP_INTERVAL_MILLIS,
                PUMP_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
        return true;
    }

    private void buildCameraTrack() {
        if (closed.get()) return;
        try {
            this.track = CameraScan.scan(file, server, stream.getHeader());
        } catch (RuntimeException failure) {
            platform.getLogger().warning("Replay viewer could not build a camera track for "
                    + display + ": " + failure);
        }
    }

    void discard() {
        if (!closed.compareAndSet(false, true)) return;
        try {
            stream.close();
        } catch (IOException ignored) {
        }
    }

    public void hideFrom(UUID joined) {
        DetachedPlayer hold = this.detached;
        if (hold != null) hold.hideFrom(joined);
    }

    private void pump() {
        if (closed.get()) return;
        if (!sink.open()) {
            close("disconnected");
            return;
        }
        synchronized (pumpLock) {
            if (closed.get() || seeking) return;
            try {
                advanceTo(clock.position());
            } catch (IOException | RuntimeException failure) {
                platform.getLogger().warning("Replay viewer for " + viewer.getName()
                        + " stopped: " + failure);
                close("read error");
                return;
            }
            if (stream.exhausted() || clock.finished()) {
                clock.pause(true);
            }
            if (appliedWorld == null
                    && System.currentTimeMillis() - startedMillis > WORLD_DEADLINE_MILLIS) {
                close("the recording never said what world it is");
                return;
            }
            avatar.flush();
            place();
            camera();
            hud.tick(status());
        }
    }

    private void advanceTo(long untilNanos) throws IOException {
        RecordingFrame frame;
        while ((frame = stream.poll(untilNanos)) != null) {
            apply(frame);
        }
    }

    private void apply(RecordingFrame frame) {
        if (frame instanceof RecordingFrame.Packet packet) {
            router.route(packet);
            return;
        }
        if (frame instanceof RecordingFrame.PrologueEnd prologue) {
            this.prologueEnd = prologue.nanos();
            return;
        }
        if (frame instanceof RecordingFrame.Flag flag) {
            onRecordedFlag(flag);
            return;
        }
        if (frame instanceof RecordingFrame.Mark mark) {
            hud.toast(Component.text("✎ " + mark.label(), Palette.BRAND));
            hud.chat(message(MessagesKeys.REPLAY_WATCH_MARK, Map.of(
                    "tg_label", mark.label(),
                    "tg_elapsed", ReplayText.elapsed(clock.elapsed()))));
        }
    }

    private void onRecordedFlag(RecordingFrame.Flag flag) {
        flagsSeen++;
        hud.toast(Component.text("⚑ " + flag.check(), Palette.DANGER, TextDecoration.BOLD)
                .append(Component.text("  ×" + flag.violations(), Palette.VALUE_ON_DANGER)));
        hud.chat(message(MessagesKeys.REPLAY_WATCH_FLAG, Map.of(
                "tg_check", flag.check(),
                "tg_violations", flag.violations(),
                "tg_player", stream.getHeader().playerName(),
                "tg_elapsed", ReplayText.elapsed(clock.elapsed()),
                "tg_debug", flag.debug().isEmpty() ? "no debug" : flag.debug())));
    }

    private Component message(ConfigKey<String> key, Map<String, Object> extras) {
        return platform.getMessageService().getComponent(key, extras);
    }

    public void heldAlert() {
        heldAlerts.incrementAndGet();
    }

    private void onWorld(RecordedWorld recorded) {
        this.recordedWorld = recorded;
        router.light(new ChunkLight(server, recorded.sectionCount()));
        if (router.getRecordedSelfId() >= 0) {
            ids.map(router.getRecordedSelfId(), ViewerAvatar.ENTITY_ID);
        }
        world.clear();
        if (!sink.isMuted()) applyWorld();
    }

    private void applyWorld() {
        RecordedWorld recorded = this.recordedWorld;
        if (recorded == null) return;

        sink.send(new WrapperPlayServerRespawn(recorded.dimensionRef(), REPLAY_LEVEL,
                Difficulty.PEACEFUL, recorded.hashedSeed(), GameMode.ADVENTURE, GameMode.ADVENTURE,
                recorded.debug(), recorded.flat(), WrapperPlayServerRespawn.KEEP_ALL_DATA, null, 0,
                recorded.seaLevel()));

        this.appliedWorld = recorded;
        this.placed = false;
        rig.forget();
        router.dropMirror();
        sink.send(new WrapperPlayServerUpdateViewDistance(recorded.viewDistance()));
        router.recentre();
        selfGameMode(GameMode.ADVENTURE);
        sink.send(new WrapperPlayServerPlayerAbilities(false, true, true, false, FLY_SPEED, 0.1f));
        freezeTime();
        endLoadingScreen(REPLAY_COMPASS);
        hud.hotbar(status());
        hud.selectSlot(selectedSlot);
    }

    private GameMode realGameMode(WorldAnchor anchor) {
        try {
            return GameMode.valueOf(anchor.gameMode());
        } catch (IllegalArgumentException unknown) {
            return GameMode.SURVIVAL;
        }
    }

    private void selfGameMode(GameMode mode) {
        UserProfile self = viewer.getUser().getProfile();
        if (server.isNewerThanOrEquals(ServerVersion.V_1_19_3)) {
            sink.send(new WrapperPlayServerPlayerInfoUpdate(
                    WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_GAME_MODE,
                    new WrapperPlayServerPlayerInfoUpdate.PlayerInfo(self, true, 0, mode, null, null)));
            return;
        }
        sink.send(new WrapperPlayServerPlayerInfo(WrapperPlayServerPlayerInfo.Action.UPDATE_GAME_MODE,
                new WrapperPlayServerPlayerInfo.PlayerData(null, self, mode, 0)));
    }

    private void freezeTime() {
        boolean explicitRate = server.isNewerThanOrEquals(ServerVersion.V_1_21_2);
        sink.send(new WrapperPlayServerTimeUpdate(0L, explicitRate ? NOON : -NOON, false));
    }

    private void endLoadingScreen(@Nullable Vector3i compass) {
        if (compass != null) sink.send(new WrapperPlayServerSpawnPosition(compass, 0f));
        if (server.isNewerThanOrEquals(ServerVersion.V_1_20_3)) {
            sink.send(new WrapperPlayServerChangeGameState(
                    WrapperPlayServerChangeGameState.Reason.START_LOADING_CHUNKS, 0f));
        }
    }

    private void applyWorldIfChanged() {
        RecordedWorld recorded = this.recordedWorld;
        if (recorded == null || recorded.sameWorldAs(this.appliedWorld)) return;
        applyWorld();
    }

    private void place() {
        if (placed || !avatar.isPositioned()) return;
        placed = true;
        teleport(avatar.getX(), avatar.getY() + 1.0, avatar.getZ(), avatar.getYaw(), avatar.getPitch());
    }

    private void camera() {
        if (camera == ViewerCamera.FREE) return;
        CameraPose pose = camera == ViewerCamera.EYES ? eyesPose() : followPose();
        if (pose == null) return;
        boolean fresh = !rig.isSpawned();
        rig.show(pose);
        if (fresh) sink.send(new WrapperPlayServerCamera(ViewerCameraRig.ENTITY_ID));
    }

    private @Nullable CameraPose eyesPose() {
        if (!avatar.isPositioned()) return null;
        return new CameraPose(avatar.getX(), avatar.eyeY(), avatar.getZ(),
                avatar.getYaw(), avatar.getPitch());
    }

    private @Nullable CameraPose followPose() {
        if (!avatar.isPositioned()) return null;
        CameraTrack current = this.track;
        if (current != null) {
            CameraPose smoothed = current.follow(clock.position(), FOLLOW_DISTANCE, FOLLOW_HEIGHT);
            if (smoothed != null) return smoothed;
        }
        return CameraTrack.orbit(avatar.getX(), avatar.getY(), avatar.getZ(),
                avatar.getYaw(), avatar.getPitch(), FOLLOW_DISTANCE, FOLLOW_HEIGHT);
    }

    private void teleport(double x, double y, double z, float yaw, float pitch) {
        sink.send(new WrapperPlayServerPlayerPositionAndLook(
                new Vector3d(x, y, z), yaw, pitch, (byte) 0, 0));
    }

    public void onSelect(int slot) {
        if (closed.get() || slot < 0 || slot > 8) return;
        this.selectedSlot = slot;
    }

    public void onUse() {
        if (rateLimited()) return;
        activate(false);
    }

    public void onSwing() {
        if (rateLimited()) return;
        activate(true);
    }

    private boolean rateLimited() {
        if (closed.get()) return true;
        long now = System.currentTimeMillis();
        if (now - lastControlMillis < CONTROL_COOLDOWN_MILLIS) return true;
        lastControlMillis = now;
        return false;
    }

    private void activate(boolean secondary) {
        if (phased) {
            phase(false);
            return;
        }
        ViewerControl control = ViewerControl.at(selectedSlot);
        if (control == null) return;
        switch (control) {
            case REWIND -> seek(-ViewerStatus.SEEK_SECONDS * 1_000_000_000L);
            case FORWARD -> seek(ViewerStatus.SEEK_SECONDS * 1_000_000_000L);
            case PLAY_PAUSE -> {
                clock.pause(!clock.paused());
                refresh();
            }
            case SPEED -> {
                speedIndex = Math.max(0, Math.min(SPEEDS.length - 1,
                        speedIndex + (secondary ? -1 : 1)));
                clock.speed(SPEEDS[speedIndex]);
                refresh();
            }
            case CAMERA -> cycleCamera();
            case PHASE -> phase(!phased);
            case RESTART -> seekTo(floorNanos(), true);
            case LEAVE -> close("left");
        }
    }

    private void cycleCamera() {
        ViewerCamera next = camera.next();
        this.camera = next;
        avatar.hidden(next == ViewerCamera.EYES);
        if (next != ViewerCamera.FREE) {
            camera();
            refresh();
            return;
        }
        rig.hide();
        sink.send(new WrapperPlayServerCamera(viewer.getUser().getEntityId()));
        refresh();
    }

    private void phase(boolean value) {
        this.phased = value;
        selfGameMode(value ? GameMode.SPECTATOR : GameMode.ADVENTURE);
        sink.send(new WrapperPlayServerPlayerAbilities(false, true, true, false, FLY_SPEED, 0.1f));
        refresh();
    }

    private void refresh() {
        ViewerStatus status = status();
        hud.hotbar(status);
        hud.tick(status, true);
    }

    private long floorNanos() {
        long prologue = this.prologueEnd;
        return prologue == 0L ? stream.getHeader().startNanos() : prologue;
    }

    private void seek(long deltaNanos) {
        seekTo(clock.position() + deltaNanos, false);
    }

    private void seekTo(long destination, boolean resume) {
        platform.getScheduler().runAsyncTask(() -> {
            synchronized (pumpLock) {
                if (closed.get()) return;
                seeking = true;
                boolean wasPaused = !resume && clock.paused();
                try {
                    long target = clock.moveTo(Math.max(floorNanos(), destination));
                    router.wipe();
                    boolean backwards = target < stream.positionNanos();
                    sink.setMuted(true);
                    if (backwards) {
                        world.clear();
                        avatar.forget();
                        stream.rewind();
                        if (!stream.exhausted()) target = Math.max(target, stream.positionNanos());
                    }
                    advanceTo(target);
                    sink.setMuted(false);
                    applyWorldIfChanged();
                    router.resync();
                    this.placed = false;
                    place();
                    clock.moveTo(target);
                    clock.pause(wasPaused);
                    refresh();
                } catch (IOException | RuntimeException failure) {
                    sink.setMuted(false);
                    platform.getLogger().warning("Replay viewer seek for " + viewer.getName()
                            + " failed: " + failure);
                    close("seek error");
                } finally {
                    seeking = false;
                }
            }
        });
    }

    public void close(String reason) {
        if (!closed.compareAndSet(false, true)) return;

        ScheduledTask task = this.pump;
        if (task != null) task.cancel();

        try {
            stream.close();
        } catch (IOException ignored) {
        }

        DetachedPlayer hold = this.detached;
        leaveReplayLevel(hold);
        sink.setMuted(true);
        this.handingBack = true;
        handBackToChecks(hold != null);

        if (hold == null) {
            released(reason);
            return;
        }
        platform.getScheduler().runAsyncTaskDelayed(() -> released(reason),
                HANDOVER_DEADLINE_MILLIS, TimeUnit.MILLISECONDS);
        hold.reattach(() -> released(reason));
    }

    private void handBackToChecks(boolean reattaching) {
        Data data = viewer.getData();
        viewer.getPingData().forgetPendingTransactions();
        viewer.getPhysics().reset();
        data.getMovementData().reset();
        data.getInputData().reset();
        data.forgetSprint();
        data.setSneaking(false);
        data.setSwimming(false);
        data.setSleeping(false);
        data.setVehicleId(-1);
        data.setOpenInventory(false, Issuer.SERVER);
        data.getUseItemData().reset();
        data.getDiggingData().reset();
        data.getFishingData().reset();
        if (reattaching) viewer.getWorldMirror().onLevelRebuilt();
        viewer.setReplayViewing(false);
    }

    private void released(String reason) {
        if (!released.compareAndSet(false, true)) return;

        viewer.setReplayViewing(false);
        report();
        platform.getLogger().info("Replay viewer closed for " + viewer.getName()
                + ": " + display + " (" + reason + ", " + flagsSeen + " recorded flag(s), "
                + heldAlerts.get() + " live alert(s) held)");
        onClosed.run();
    }

    private void report() {
        long held = heldAlerts.get();
        if (held <= 0) return;
        PlatformPlayer target = viewer.getPlatformPlayer();
        if (target == null) return;
        target.sendMessage(message(MessagesKeys.REPLAY_WATCH_ALERTS_HELD, Map.of("tg_alerts", held)));
    }

    private void leaveReplayLevel(@Nullable DetachedPlayer hold) {
        if (appliedWorld == null) return;
        sink.setMuted(false);
        WorldAnchor anchor = hold == null ? null : hold.anchor();
        GameMode restored = anchor == null ? GameMode.SURVIVAL : realGameMode(anchor);
        sink.send(new WrapperPlayServerRespawn(viewer.getUser().getDimensionType(),
                anchor == null ? FALLBACK_LEVEL : anchor.dimensionKey(), Difficulty.NORMAL, 0L,
                restored, null, false, false,
                WrapperPlayServerRespawn.KEEP_ALL_DATA, null, 0));
        if (anchor != null) {
            sink.send(new WrapperPlayServerUpdateViewDistance(anchor.viewDistance()));
            sink.send(new WrapperPlayServerUpdateViewPosition(anchor.chunkX(), anchor.chunkZ()));
            selfGameMode(restored);
        }
        sink.send(new WrapperPlayServerCamera(viewer.getUser().getEntityId()));
        endLoadingScreen(null);
    }

    public ViewerStatus status() {
        boolean finished = stream.exhausted() || (durationNanos > 0 && clock.finished());
        return new ViewerStatus(display, clock.elapsed(), durationNanos, clock.speed(),
                clock.paused(), seeking, finished, phased, camera);
    }
}
