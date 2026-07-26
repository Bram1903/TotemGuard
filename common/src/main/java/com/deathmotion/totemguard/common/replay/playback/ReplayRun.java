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

package com.deathmotion.totemguard.common.replay.playback;

import com.deathmotion.totemguard.common.event.packet.PacketCheckManagerListener;
import com.deathmotion.totemguard.common.features.integration.IntegrationInputs;
import com.deathmotion.totemguard.common.physics.trace.TraceFrame;
import com.deathmotion.totemguard.common.physics.verdict.DeclineReason;
import com.deathmotion.totemguard.common.physics.verdict.TickOutcome;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.replay.format.*;
import com.deathmotion.totemguard.common.util.TGVersions;
import com.deathmotion.totemguard.common.world.block.ClientStateMap;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.*;
import com.github.retrooper.packetevents.manager.InternalPacketListener;
import com.github.retrooper.packetevents.manager.protocol.ProtocolManager;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import com.github.retrooper.packetevents.netty.buffer.UnpooledByteBufAllocationHelper;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.util.EventCreationUtil;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public final class ReplayRun {

    private final Path file;
    private final ReplayObserver observer;

    private final Map<String, Long> declineReasons = new LinkedHashMap<>();
    private final Map<String, Integer> selfEmitted = new LinkedHashMap<>();
    private final Map<String, Integer> dispatchErrors = new LinkedHashMap<>();
    private final List<ReplayFlag> flags = new ArrayList<>();
    private final List<TickDigest> digests = new ArrayList<>();
    private final List<TickDigest> recorded = new ArrayList<>();
    private final List<ReplayFlag> recordedFlags = new ArrayList<>();
    private final ArrayDeque<Runnable> eventLoopQueue = new ArrayDeque<>();

    private @Nullable WorldDigest expectedWorld;
    private ReplayResult.WorldCheck worldCheck = ReplayResult.WorldCheck.ABSENT;

    private boolean attached;
    private boolean prologued;
    private long frames;
    private long judged;
    private long coasted;
    private long declined;

    private ReplayRun(Path file, ReplayObserver observer) {
        this.file = file;
        this.observer = observer;
    }

    public static ReplayResult run(Path file) {
        return run(file, new ReplayObserver() {
        });
    }

    public static ReplayResult run(Path file, ReplayObserver observer) {
        return new ReplayRun(file, observer).execute();
    }

    private ReplayResult execute() {
        long startedAt = System.nanoTime();
        try (RecordingReader reader = new RecordingReader(file)) {
            RecordingHeader header = reader.getHeader();
            String refusal = refuse(header);
            if (refusal != null) {
                return failed(header, refusal, startedAt);
            }
            return drive(reader, header, startedAt);
        } catch (IOException | RuntimeException failure) {
            return failed(null, String.valueOf(failure), startedAt);
        }
    }

    private @Nullable String refuse(RecordingHeader header) {
        ServerVersion current = PacketEvents.getAPI().getServerManager().getVersion();
        if (current.getProtocolVersion() != header.serverProtocol()) {
            return "recorded on server protocol " + header.serverProtocol()
                    + ", this host is " + current.getProtocolVersion();
        }
        ClientVersion client = header.client();
        if (client == null || client == ClientVersion.UNKNOWN) {
            return "unknown client protocol " + header.clientProtocol();
        }
        boolean endTick = client.isNewerThanOrEquals(ClientVersion.V_1_21_2)
                && current.isNewerThanOrEquals(ServerVersion.V_1_21_2);
        if (endTick != header.supportsEndTick()) {
            return "tick model changed: recorded supportsEndTick=" + header.supportsEndTick()
                    + ", recomputed " + endTick;
        }
        return null;
    }

    private ReplayResult drive(RecordingReader reader, RecordingHeader header, long startedAt) throws IOException {
        ClientVersion client = header.client();
        ReplayClock clock = new ReplayClock(header.startNanos(), header.startEpochMillis());
        User user = new User(null, ConnectionState.HANDSHAKING, client,
                new UserProfile(header.playerUuid(), header.playerName()));
        user.setClientVersion(client);
        Object channel = new Object();
        Object liveChannel = PacketEvents.getAPI().getProtocolManager().getChannel(header.playerUuid());

        ClientStateMap states = header.blockStateTable() == null
                ? ClientStateMap.forClient(client, PacketEvents.getAPI().getServerManager().getVersion().toClientVersion())
                : ClientStateMap.fromTable(client,
                PacketEvents.getAPI().getServerManager().getVersion().toClientVersion(),
                header.blockStateTable());

        TGPlayer player = new TGPlayer(user, clock, states);
        attach(player, header);

        EventManager manager = new EventManager();
        manager.registerListener(guarded(new InternalPacketListener(), false));
        manager.registerListener(guarded(new PacketCheckManagerListener(
                candidate -> candidate == user ? player : null), true));

        try {
            RecordingFrame frame;
            while ((frame = reader.next()) != null) {
                frames++;
                clock.advanceTo(frame.nanos());
                if (frame instanceof RecordingFrame.Packet packet) {
                    observer.onPacketFrame(frames - 1, packet, player);
                    dispatch(manager, user, channel, packet);
                } else if (frame instanceof RecordingFrame.ServerTick) {
                    player.onServerTick();
                } else if (frame instanceof RecordingFrame.EventLoop) {
                    Runnable queued = eventLoopQueue.poll();
                    if (queued != null) queued.run();
                } else if (frame instanceof RecordingFrame.Verdict verdict) {
                    recorded.add(verdict.digest());
                } else if (frame instanceof RecordingFrame.Flag flag) {
                    recordedFlags.add(new ReplayFlag(currentTick(), flag.check(), flag.violations(),
                            flag.debug().isEmpty() ? null : flag.debug()));
                } else if (frame instanceof RecordingFrame.Integration integration) {
                    IntegrationInputs.apply(player, integration.input(), integration.id(),
                            integration.timestamp());
                } else if (frame instanceof RecordingFrame.Attach) {
                    attached = true;
                } else if (frame instanceof RecordingFrame.PrologueEnd prologue) {
                    prologued = true;
                    expectedWorld = prologue.digest().present() ? prologue.digest() : null;
                    if (expectedWorld != null) worldCheck = ReplayResult.WorldCheck.unsettled();
                }
                if (expectedWorld != null) compareWorld(player);
            }
        } finally {
            releaseChannelMapping(header, channel, liveChannel);
        }

        return new ReplayResult(header, reader.isTruncated(), frames, judged + coasted + declined,
                judged, coasted, declined, declineReasons, flags, compareFlags(), digests,
                verify(header), worldCheck,
                selfEmitted, dispatchErrors, eventLoopQueue.size(),
                (System.nanoTime() - startedAt) / 1_000_000L, null);
    }

    private void releaseChannelMapping(RecordingHeader header, Object channel, @Nullable Object liveChannel) {
        try {
            ProtocolManager manager = PacketEvents.getAPI().getProtocolManager();
            if (manager.getChannel(header.playerUuid()) != channel) return;
            if (liveChannel == null) {
                manager.removeChannelById(header.playerUuid());
                return;
            }
            manager.setChannel(header.playerUuid(), liveChannel);
        } catch (RuntimeException ignored) {
        }
    }

    private void attach(TGPlayer player, RecordingHeader header) {
        player.setSynthetic(true);
        player.setObserveOnly(header.observeOnly());
        player.setPlatformPlayer(new ReplayPlatformPlayer());
        player.getCheckManager().applySnapshot(header.checkSnapshot());
        player.getPhysics().trace().sink(frame -> onTick(player, frame));
        player.setFlagSink((check, violations, debug, compiled, extras) ->
                flags.add(new ReplayFlag(currentTick(), check.getName(), violations, debug)));
        player.setEnginePacketSink(this::onSelfEmitted);
        player.getLatencyHandler().eventLoopSink(eventLoopQueue::add);
    }

    private PacketListenerAbstract guarded(PacketListenerAbstract delegate, boolean afterAttach) {
        return new PacketListenerAbstract(delegate.getPriority()) {
            @Override
            public void onPacketReceive(PacketReceiveEvent event) {
                if (afterAttach && !attached) return;
                try {
                    delegate.onPacketReceive(event);
                } catch (Exception failure) {
                    record(event, failure);
                }
            }

            @Override
            public void onPacketSend(PacketSendEvent event) {
                if (afterAttach && !attached) return;
                try {
                    delegate.onPacketSend(event);
                } catch (Exception failure) {
                    record(event, failure);
                }
            }

            private void record(ProtocolPacketEvent event, Exception failure) {
                dispatchErrors.merge(event.getPacketType() + " " + delegate.getClass().getSimpleName()
                        + " " + failure, 1, Integer::sum);
            }
        };
    }

    private void onSelfEmitted(PacketWrapper<?> wrapper) {
        String name = wrapper.getClass().getSimpleName();
        selfEmitted.merge(name, 1, Integer::sum);
    }

    private long currentTick() {
        return digests.isEmpty() ? 0L : digests.get(digests.size() - 1).tick();
    }

    private void onTick(TGPlayer player, TraceFrame frame) {
        TickOutcome outcome = TickOutcome.values()[frame.outcome];
        switch (outcome) {
            case JUDGED -> judged++;
            case COASTED -> coasted++;
            case DECLINED -> {
                declined++;
                String reason = frame.reason < 0 ? "NONE" : DeclineReason.values()[frame.reason].name();
                declineReasons.merge(reason, 1L, Long::sum);
            }
            default -> {
            }
        }
        digests.add(TickDigest.of(frame));
        observer.onTick(player, frame);
    }

    private void dispatch(EventManager manager, User user, Object channel, RecordingFrame.Packet packet) {
        if (packet.inbound()) {
            user.setDecoderState(packet.state());
        } else {
            user.setEncoderState(packet.state());
        }

        Object buffer = UnpooledByteBufAllocationHelper.buffer();
        try {
            ByteBufHelper.writeVarInt(buffer, packet.packetId());
            ByteBufHelper.writeBytes(buffer, packet.payload());
            ProtocolPacketEvent event = packet.inbound()
                    ? EventCreationUtil.createReceiveEvent(channel, user, null, buffer, true)
                    : EventCreationUtil.createSendEvent(channel, user, null, buffer, true);
            event.setCancelled(packet.cancelled());
            int processIndex = ByteBufHelper.readerIndex(buffer);
            manager.callEvent(event, () -> ByteBufHelper.readerIndex(buffer, processIndex));
            if (event.hasPostTasks()) {
                for (Runnable task : event.getPostTasks()) {
                    task.run();
                }
            }
            if (event instanceof PacketSendEvent send && send.hasTasksAfterSend()) {
                for (Runnable task : send.getTasksAfterSend()) {
                    task.run();
                }
            }
        } catch (Exception failure) {
            dispatchErrors.merge(packet.state() + (packet.inbound() ? "/in id=" : "/out id=")
                    + packet.packetId() + " " + failure, 1, Integer::sum);
        } finally {
            ByteBufHelper.release(buffer);
        }
    }

    private void compareWorld(TGPlayer player) {
        WorldDigest expected = this.expectedWorld;
        if (expected == null || !player.getWorldMirror().readiness().ready()) return;
        this.expectedWorld = null;
        WorldDigest replayed = expected.resample(player.getWorldMirror());
        String difference = expected.firstDifference(replayed);
        if (difference != null) {
            worldCheck = ReplayResult.WorldCheck.diverged(replayed.columnsLoaded(), difference);
            return;
        }
        worldCheck = replayed.columnsLoaded() == 0
                ? ReplayResult.WorldCheck.vacuous()
                : ReplayResult.WorldCheck.matched(replayed.columnsLoaded());
    }

    private ReplayResult.FlagCheck compareFlags() {
        List<ReplayFlag> flags = settledFlags();
        if (recordedFlags.isEmpty() && flags.isEmpty()) return ReplayResult.FlagCheck.ABSENT;
        int compared = Math.min(recordedFlags.size(), flags.size());
        for (int i = 0; i < compared; i++) {
            ReplayFlag live = recordedFlags.get(i);
            ReplayFlag replayed = flags.get(i);
            if (!live.check().equals(replayed.check())) {
                return ReplayResult.FlagCheck.diverged(recordedFlags.size(), flags.size(),
                        "flag " + i + " was " + live.check() + " live, " + replayed.check() + " on replay");
            }
        }
        if (recordedFlags.size() != flags.size()) {
            List<ReplayFlag> longer = flags.size() > recordedFlags.size() ? flags : recordedFlags;
            ReplayFlag extra = longer.get(compared);
            return ReplayResult.FlagCheck.diverged(recordedFlags.size(), flags.size(),
                    (flags.size() > recordedFlags.size() ? "replay flagged " : "live flagged ")
                            + extra.check() + " that the other did not: " + extra.toLogRow());
        }
        return ReplayResult.FlagCheck.matched(compared);
    }

    private List<ReplayFlag> settledFlags() {
        if (!prologued || digests.isEmpty()) {
            return flags;
        }
        long until = digests.get(0).tick() + ReplayResult.WARM_UP_TICKS;
        return flags.stream().filter(flag -> flag.tick() > until).toList();
    }

    private ReplayResult.Verification verify(RecordingHeader header) {
        if (prologued) {
            return ReplayResult.Verification.skipped(
                    "a prologued recording starts mid-session, so the tick streams do not line up");
        }
        if (recorded.isEmpty()) {
            return ReplayResult.Verification.skipped("the recording carries no verdict frames");
        }
        if (!TGVersions.RAW.equals(header.pluginVersion())) {
            return ReplayResult.Verification.skipped("recorded on " + header.pluginVersion()
                    + ", this build is " + TGVersions.RAW);
        }
        int compared = Math.min(recorded.size(), digests.size());
        for (int i = 0; i < compared; i++) {
            String field = recorded.get(i).firstDifference(digests.get(i));
            if (field != null) {
                return ReplayResult.Verification.diverged(i, recorded.size(), recorded.get(i).tick(), field);
            }
        }
        if (recorded.size() != digests.size()) {
            return ReplayResult.Verification.diverged(compared, recorded.size(), compared,
                    "tick count (" + digests.size() + " replayed vs " + recorded.size() + " recorded)");
        }
        return ReplayResult.Verification.matched(compared);
    }

    private ReplayResult failed(@Nullable RecordingHeader header, String error, long startedAt) {
        return new ReplayResult(header, false, frames, 0, 0, 0, 0, declineReasons, flags,
                ReplayResult.FlagCheck.ABSENT, digests,
                ReplayResult.Verification.skipped("the run did not complete"), worldCheck, selfEmitted,
                dispatchErrors, eventLoopQueue.size(),
                (System.nanoTime() - startedAt) / 1_000_000L, error);
    }
}
