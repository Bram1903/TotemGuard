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

package com.deathmotion.totemguard.common.mod;

import com.deathmotion.totemguard.api.mod.DetectedMod;
import com.deathmotion.totemguard.api.mod.ModAction;
import com.deathmotion.totemguard.api.mod.ModDetectionMethod;
import com.deathmotion.totemguard.api.mod.ModDetectionRepository;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.player.TGPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ModDetectionService implements ModDetectionRepository {

    private final TGPlatform platform;
    private final ModResolver resolver;
    private final ModKickThenBanTracker kickThenBanTracker;

    private final ConcurrentHashMap<UUID, ModSession> sessions = new ConcurrentHashMap<>();

    public ModDetectionService(TGPlatform platform,
                               ModResolver resolver,
                               ModKickThenBanTracker kickThenBanTracker) {
        this.platform = platform;
        this.resolver = resolver;
        this.kickThenBanTracker = kickThenBanTracker;
    }

    public @Nullable ModSession sessionFor(@NotNull UUID uuid) {
        return sessions.get(uuid);
    }

    public void onPlayerLogin(@NotNull TGPlayer player) {
        ModRegistry.Snapshot snapshot = ModRegistry.snapshot();
        ModTranslationDetector detector = new ModTranslationDetector(player, snapshot);

        ModSession session = new ModSession(player, snapshot, detector);
        sessions.put(player.getUuid(), session);

        detector.start(() -> session.tryEnterAwaitingBoundary());
    }

    public void onPlayerLogout(@NotNull UUID uuid) {
        ModSession removed = sessions.remove(uuid);
        if (removed != null) removed.cancel();
    }

    public void recordDetection(@NotNull ModSession session,
                                @NotNull ModDefinition definition,
                                @NotNull ModDetectionMethod method) {
        ModSession.RecordResult result = session.record(definition, method);
        if (result == null) return;
        if (result.late()) {
            resolver.resolveLate(session, result.mod());
        }
    }

    public void onTickBoundary(@NotNull ModSession session) {
        if (session.state() != ModSession.State.AWAITING_BOUNDARY) return;
        if (!session.tryResolve()) return;
        resolver.resolve(session);
    }

    @Override
    public @NotNull Set<DetectedMod> getDetectedMods(@NotNull UUID uuid) {
        ModSession session = sessions.get(uuid);
        return session == null ? Set.of() : session.snapshotDetected();
    }

    @Override
    public boolean hasDetectedMod(@NotNull UUID uuid, @NotNull String modId) {
        ModSession session = sessions.get(uuid);
        return session != null && session.hasDetected(modId);
    }

    @Override
    public boolean isSessionResolved(@NotNull UUID uuid) {
        ModSession session = sessions.get(uuid);
        return session != null && session.isResolved();
    }

    @Override
    public @Nullable ModAction getResolvedAction(@NotNull UUID uuid) {
        ModSession session = sessions.get(uuid);
        return session == null ? null : session.resolvedAction();
    }

    public boolean isDetectionActiveFor(@NotNull UUID uuid) {
        ModSession session = sessions.get(uuid);
        return session != null && session.translationDetector().isActive();
    }
}
