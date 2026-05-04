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
import com.deathmotion.totemguard.common.player.TGPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public final class ModSession {

    private final TGPlayer player;
    private final ModRegistry.Snapshot snapshot;
    private final ModTranslationDetector translationDetector;
    private final Object lock = new Object();
    private final Map<String, DetectedMod> detected = new LinkedHashMap<>();
    private final AtomicReference<ModAction> resolvedAction = new AtomicReference<>();
    private State state = State.GATHERING;

    public ModSession(TGPlayer player, ModRegistry.Snapshot snapshot, ModTranslationDetector translationDetector) {
        this.player = player;
        this.snapshot = snapshot;
        this.translationDetector = translationDetector;
    }

    public TGPlayer player() {
        return player;
    }

    public ModRegistry.Snapshot snapshot() {
        return snapshot;
    }

    public ModTranslationDetector translationDetector() {
        return translationDetector;
    }

    public State state() {
        synchronized (lock) {
            return state;
        }
    }

    public boolean isResolved() {
        return state() == State.RESOLVED;
    }

    public Set<DetectedMod> snapshotDetected() {
        synchronized (lock) {
            return Set.copyOf(detected.values());
        }
    }

    public boolean hasDetected(String modId) {
        synchronized (lock) {
            return detected.containsKey(modId);
        }
    }

    public @Nullable ModAction resolvedAction() {
        return resolvedAction.get();
    }

    public @Nullable RecordResult record(ModDefinition definition, ModDetectionMethod method) {
        synchronized (lock) {
            if (state == State.CANCELLED) return null;
            if (detected.containsKey(definition.id())) return null;

            DetectedMod entry = new DetectedMod(definition.id(), definition.severity(), method);
            detected.put(definition.id(), entry);
            return new RecordResult(entry, state == State.RESOLVED);
        }
    }

    public boolean tryEnterAwaitingBoundary() {
        synchronized (lock) {
            if (state != State.GATHERING) return false;
            state = State.AWAITING_BOUNDARY;
            return true;
        }
    }

    public boolean tryResolve() {
        synchronized (lock) {
            if (state != State.AWAITING_BOUNDARY) return false;
            state = State.RESOLVED;
            return true;
        }
    }

    public void markResolvedAction(ModAction action) {
        resolvedAction.set(action);
    }

    public void cancel() {
        synchronized (lock) {
            state = State.CANCELLED;
        }
    }

    public enum State {
        GATHERING,
        AWAITING_BOUNDARY,
        RESOLVED,
        CANCELLED
    }

    public record RecordResult(DetectedMod mod, boolean late) {
    }
}
