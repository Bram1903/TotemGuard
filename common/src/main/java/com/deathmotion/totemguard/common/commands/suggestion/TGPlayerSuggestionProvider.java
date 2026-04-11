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
import com.deathmotion.totemguard.common.player.TGPlayer;
import org.incendo.cloud.suggestion.SuggestionProvider;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public final class TGPlayerSuggestionProvider {

    private TGPlayerSuggestionProvider() {
    }

    public static SuggestionProvider<Sender> suggestionProvider() {
        return SuggestionProvider.blockingStrings((ctx, input) -> suggestions(input.lastRemainingToken()));
    }

    public static Iterable<String> suggestions(String currentInput) {
        String normalized = currentInput == null ? "" : currentInput.toLowerCase(Locale.ROOT);

        return TGPlatform.getInstance().getPlayerRepository().getPlayers().stream()
                .map(TGPlayer::getName)
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(normalized))
                .sorted(String::compareToIgnoreCase)
                .toList();
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
}
