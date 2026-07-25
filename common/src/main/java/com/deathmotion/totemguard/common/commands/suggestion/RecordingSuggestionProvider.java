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

package com.deathmotion.totemguard.common.commands.suggestion;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.platform.sender.Sender;
import com.deathmotion.totemguard.common.replay.RecordingIndex;
import com.deathmotion.totemguard.common.replay.RecordingLibrary;
import com.deathmotion.totemguard.common.replay.ReplayService;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class RecordingSuggestionProvider {

    private RecordingSuggestionProvider() {
    }

    public static SuggestionProvider<Sender> recordings() {
        return (context, input) -> {
            ReplayService service = service();
            if (service == null) return CompletableFuture.completedFuture(List.<Suggestion>of());
            String needle = input.lastRemainingToken();
            return service.index().entries().thenApply(entries -> suggest(entries, needle));
        };
    }

    public static SuggestionProvider<Sender> scenarios() {
        return SuggestionProvider.blockingStrings((context, input) -> {
            ReplayService service = service();
            if (service == null) return List.of();
            String needle = input.lastRemainingToken().toLowerCase(Locale.ROOT);
            Set<String> names = new LinkedHashSet<>();
            for (RecordingIndex.Entry entry : service.index().current()) {
                if (entry.scenario().startsWith(needle)) names.add(entry.scenario());
            }
            return names;
        });
    }

    public static SuggestionProvider<Sender> tags() {
        return SuggestionProvider.blockingStrings((context, input) -> {
            ReplayService service = service();
            if (service == null) return List.of();
            String needle = input.lastRemainingToken().toLowerCase(Locale.ROOT);
            int comma = needle.lastIndexOf(',');
            String prefix = comma < 0 ? "" : needle.substring(0, comma + 1);
            String partial = comma < 0 ? needle : needle.substring(comma + 1);
            Set<String> names = new LinkedHashSet<>();
            for (RecordingIndex.Entry entry : service.index().current()) {
                for (String tag : entry.tags()) {
                    if (tag.startsWith(partial)) names.add(prefix + tag);
                }
            }
            return names;
        });
    }

    private static List<Suggestion> suggest(List<RecordingIndex.Entry> entries, String needle) {
        List<Suggestion> suggestions = new ArrayList<>();
        for (RecordingIndex.Entry entry : RecordingIndex.matching(entries, needle)) {
            suggestions.add(Suggestion.suggestion(RecordingLibrary.display(entry.path())));
        }
        return suggestions;
    }

    private static @Nullable ReplayService service() {
        return TGPlatform.getInstance().getReplayService();
    }
}
