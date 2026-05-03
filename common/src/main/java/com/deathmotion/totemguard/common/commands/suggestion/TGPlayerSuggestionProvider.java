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
import com.deathmotion.totemguard.common.network.NetworkPresenceRepository;
import com.deathmotion.totemguard.common.network.RemotePlayerEntry;
import com.deathmotion.totemguard.common.platform.sender.Sender;
import com.deathmotion.totemguard.common.player.TGPlayer;
import org.incendo.cloud.suggestion.SuggestionProvider;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;

public final class TGPlayerSuggestionProvider {

    private TGPlayerSuggestionProvider() {
    }

    public static SuggestionProvider<Sender> suggestionProvider() {
        return SuggestionProvider.blockingStrings((ctx, input) -> suggestions(input.lastRemainingToken(), null));
    }

    public static SuggestionProvider<Sender> suggestionProviderExcludingSelf() {
        return SuggestionProvider.blockingStrings((ctx, input) ->
                suggestions(input.lastRemainingToken(), ctx.sender().getName()));
    }

    public static Iterable<String> suggestions(String currentInput) {
        return suggestions(currentInput, null);
    }

    public static Iterable<String> suggestions(String currentInput, @Nullable String excludeName) {
        String prefix = currentInput == null ? "" : currentInput;
        Set<String> names = new LinkedHashSet<>();

        NetworkPresenceRepository presence = TGPlatform.getInstance().getNetworkPresenceRepository();
        if (presence != null) {
            for (String name : presence.suggestNames(prefix)) {
                if (excludeName != null && name.equalsIgnoreCase(excludeName)) continue;
                names.add(name);
            }
        }

        String lower = prefix.toLowerCase(java.util.Locale.ROOT);
        for (TGPlayer player : TGPlatform.getInstance().getPlayerRepository().getPlayers()) {
            String name = player.getName();
            if (name == null) continue;
            if (excludeName != null && name.equalsIgnoreCase(excludeName)) continue;
            if (lower.isEmpty() || name.toLowerCase(java.util.Locale.ROOT).startsWith(lower)) {
                names.add(name);
            }
        }
        return names;
    }

    public static @Nullable TGPlayer findPlayer(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }

        for (TGPlayer player : TGPlatform.getInstance().getPlayerRepository().getPlayers()) {
            if (player.getName().equalsIgnoreCase(name)) {
                return player;
            }
        }

        return null;
    }

    public static @Nullable RemotePlayerEntry findNetworkPlayer(String name) {
        if (name == null || name.isBlank()) return null;
        NetworkPresenceRepository presence = TGPlatform.getInstance().getNetworkPresenceRepository();
        if (presence == null) return null;
        return presence.findByName(name);
    }
}
