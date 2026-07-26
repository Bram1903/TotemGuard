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
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.replay.RecordingSummary;
import com.deathmotion.totemguard.common.replay.RecordingSummaryCache;
import com.deathmotion.totemguard.common.replay.ReplayService;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.player.User;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

public final class ReplayViewerService {

    private final TGPlatform platform;
    private final ReplayService replays;
    private final ConcurrentMap<UUID, ReplayViewerSession> sessions = new ConcurrentHashMap<>();

    public ReplayViewerService(TGPlatform platform, ReplayService replays) {
        this.platform = platform;
        this.replays = replays;
    }

    public boolean watching(User user) {
        return session(user) != null;
    }

    public @Nullable ReplayViewerSession session(User user) {
        if (sessions.isEmpty()) return null;
        UUID uuid = user.getUUID();
        if (uuid == null) return null;
        ReplayViewerSession session = sessions.get(uuid);
        return session == null || session.isHandingBack() ? null : session;
    }

    public @Nullable ReplayViewerSession session(UUID viewer) {
        return sessions.isEmpty() ? null : sessions.get(viewer);
    }

    public boolean any() {
        return !sessions.isEmpty();
    }

    public void watch(TGPlayer viewer, String query, Consumer<Component> sink) {
        if (sessions.containsKey(viewer.getUuid())) {
            tell(sink, MessagesKeys.REPLAY_WATCH_ALREADY, Map.of());
            return;
        }

        platform.getScheduler().runAsyncTask(() -> {
            Path file = replays.library().resolve(query);
            if (file == null) {
                tell(sink, MessagesKeys.REPLAY_FILE_NOT_FOUND, Map.of("tg_file", query));
                return;
            }

            RecordingSummaryCache.Entry entry = replays.summaries().require(replays.relative(file));
            RecordingSummary summary = entry.summary();
            if (summary == null) {
                tell(sink, MessagesKeys.REPLAY_INSPECT_UNREADABLE, Map.of(
                        "tg_file", replays.named(file),
                        "tg_error", String.valueOf(entry.error())));
                return;
            }

            String refusal = refuse(summary);
            if (refusal != null) {
                tell(sink, MessagesKeys.REPLAY_WATCH_REFUSED, Map.of(
                        "tg_file", summary.display(), "tg_reason", refusal));
                return;
            }

            begin(viewer, file, summary, sink);
        });
    }

    private void begin(TGPlayer viewer, Path file, RecordingSummary summary, Consumer<Component> sink) {
        String display = summary.display();
        try {
            ReplayViewerSession session = ReplayViewerSession.open(platform, viewer, file, display,
                    durationOf(summary), () -> sessions.remove(viewer.getUuid()));
            if (sessions.putIfAbsent(viewer.getUuid(), session) != null) {
                session.discard();
                tell(sink, MessagesKeys.REPLAY_WATCH_ALREADY, Map.of());
                return;
            }
            if (!session.start()) {
                sessions.remove(viewer.getUuid());
                session.discard();
                tell(sink, MessagesKeys.REPLAY_WATCH_REFUSED, Map.of(
                        "tg_file", display,
                        "tg_reason", "the server would not take you out of the world"));
                return;
            }
            hideFromEveryone(viewer);
            tell(sink, MessagesKeys.REPLAY_WATCH_STARTED, Map.of(
                    "tg_file", display,
                    "tg_player", summary.playerName(),
                    "tg_length", summary.length()));
        } catch (Exception failure) {
            sessions.remove(viewer.getUuid());
            tell(sink, MessagesKeys.REPLAY_WATCH_REFUSED, Map.of(
                    "tg_file", display, "tg_reason", String.valueOf(failure.getMessage())));
            platform.getLogger().warning("Replay viewer for " + viewer.getName()
                    + " could not start: " + failure);
        }
    }

    private long durationOf(RecordingSummary summary) {
        return summary.sealed() ? Math.max(0L, summary.elapsedNanos()) : 0L;
    }

    private @Nullable String refuse(RecordingSummary summary) {
        ServerVersion current = PacketEvents.getAPI().getServerManager().getVersion();
        if (current.getProtocolVersion() != summary.serverProtocol()) {
            return "recorded on server protocol " + summary.serverProtocol()
                    + ", this host is " + current.getProtocolVersion();
        }
        return null;
    }

    public boolean stop(TGPlayer viewer) {
        ReplayViewerSession session = sessions.get(viewer.getUuid());
        if (session == null) return false;
        session.close("stopped");
        return true;
    }

    public void onDisconnect(UUID uuid) {
        ReplayViewerSession session = sessions.get(uuid);
        if (session != null) session.close("disconnected");
    }

    public void onJoin(TGPlayer joined) {
        if (sessions.isEmpty()) return;
        for (ReplayViewerSession session : sessions.values()) {
            session.hideFrom(joined.getUuid());
        }
    }

    public void shutdown() {
        for (ReplayViewerSession session : List.copyOf(sessions.values())) {
            session.close("plugin disabled");
        }
        sessions.clear();
    }

    private void hideFromEveryone(TGPlayer viewer) {
        ReplayViewerSession session = sessions.get(viewer.getUuid());
        if (session == null) return;
        for (TGPlayer other : platform.getPlayerRepository().getPlayers()) {
            if (other != viewer) session.hideFrom(other.getUuid());
        }
    }

    private void tell(Consumer<Component> sink, ConfigKey<String> key, Map<String, Object> extras) {
        sink.accept(platform.getMessageService().getComponent(key, extras));
    }
}
