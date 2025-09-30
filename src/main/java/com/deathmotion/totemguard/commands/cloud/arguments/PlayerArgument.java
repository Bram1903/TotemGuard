/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2025 Bram and contributors
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

package com.deathmotion.totemguard.commands.cloud.arguments;

import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.bukkit.parser.PlayerParser;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@UtilityClass
public class PlayerArgument {

    public ParserDescriptor<CommandSender, Player> playerParser() {
        return PlayerParser.playerParser();
    }

    public SuggestionProvider<CommandSender> onlinePlayerSuggestions() {
        return PlayerArgument::suggestOnlinePlayers;
    }

    private CompletableFuture<List<Suggestion>> suggestOnlinePlayers(
            final CommandContext<CommandSender> context,
            final CommandInput input
    ) {
        final CommandSender sender = context.sender();

        final List<Suggestion> suggestions = new ArrayList<>();
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (!(sender instanceof Player) || ((Player) sender).canSee(target)) {
                suggestions.add(Suggestion.suggestion(target.getName()));
            }
        }

        return CompletableFuture.completedFuture(suggestions);
    }
}
