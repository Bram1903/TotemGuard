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

package com.deathmotion.totemguard.common.features.mods;

import com.deathmotion.totemguard.api.mod.DetectedMod;
import com.deathmotion.totemguard.api.mod.ModAction;
import com.deathmotion.totemguard.api.mod.ModDetectionMethod;
import com.deathmotion.totemguard.common.player.TGPlayer;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public final class ModSession {

    private final TGPlayer player;
    private final ModRegistry.Snapshot snapshot;
    private final boolean checkDebugModifier;

    private final List<AnvilNameEcho.Question> questions;
    private final Map<String, TranslationQuery> translationsById;
    private final Map<String, String> debugModifierTokensById;
    private final Map<String, String> debugModifierAnswers = new ConcurrentHashMap<>();

    private final Object lock = new Object();
    private final Map<String, DetectedMod> detected = new LinkedHashMap<>();
    private final AtomicReference<ModAction> resolvedAction = new AtomicReference<>();
    private State state = State.GATHERING;

    public ModSession(TGPlayer player, ModRegistry.Snapshot snapshot, boolean checkDebugModifier) {
        this.player = player;
        this.snapshot = snapshot;
        this.checkDebugModifier = checkDebugModifier;

        Set<String> usedIds = new HashSet<>();
        List<AnvilNameEcho.Question> built = new ArrayList<>();
        Map<String, TranslationQuery> translations = new HashMap<>();
        Map<String, String> debugTokens = new HashMap<>();

        for (ModDefinition definition : snapshot.definitions().values()) {
            for (String translationKey : definition.translations()) {
                String id = freshId(usedIds);
                translations.put(id, new TranslationQuery(definition, translationKey));
                built.add(new AnvilNameEcho.Question(id, Component.translatable(translationKey)));
            }
        }

        if (checkDebugModifier) {
            for (Map.Entry<String, Component> question : DebugModifierKeybinds.questions().entrySet()) {
                String id = freshId(usedIds);
                debugTokens.put(id, question.getKey());
                built.add(new AnvilNameEcho.Question(id, question.getValue()));
            }
        }

        this.questions = List.copyOf(built);
        this.translationsById = Map.copyOf(translations);
        this.debugModifierTokensById = Map.copyOf(debugTokens);
    }

    private static String freshId(Set<String> usedIds) {
        String id;
        do {
            id = AnvilNameEcho.newId();
        } while (!usedIds.add(id));
        return id;
    }

    public TGPlayer player() {
        return player;
    }

    public ModRegistry.Snapshot snapshot() {
        return snapshot;
    }

    List<AnvilNameEcho.Question> questions() {
        return questions;
    }

    Answer consume(AnvilNameEcho.Reflection reflection) {
        TranslationQuery translation = translationsById.get(reflection.id());
        if (translation != null) {
            String rendered = reflection.rendered();
            boolean localized = !rendered.isBlank() && !translation.key().startsWith(rendered);
            return new Answer(true, localized ? translation.mod() : null);
        }

        String token = debugModifierTokensById.get(reflection.id());
        if (token != null) {
            debugModifierAnswers.put(token, reflection.rendered());
            return Answer.OURS;
        }

        return Answer.NOT_OURS;
    }

    public boolean hasDebugModifierConflict() {
        return checkDebugModifier && DebugModifierKeybinds.conflicts(debugModifierAnswers);
    }

    public boolean isResolved() {
        return state() == State.RESOLVED;
    }

    public State state() {
        synchronized (lock) {
            return state;
        }
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

    public record Answer(boolean ours, @Nullable ModDefinition detectedMod) {
        static final Answer NOT_OURS = new Answer(false, null);
        static final Answer OURS = new Answer(true, null);
    }

    public record RecordResult(DetectedMod mod, boolean late) {
    }

    private record TranslationQuery(ModDefinition mod, String key) {
    }
}
