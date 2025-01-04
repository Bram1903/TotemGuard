/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2024 Bram and contributors
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

package com.deathmotion.totemguard.commands.totemguard;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.config.Checks;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import net.jodah.expiringmap.ExpiringMap;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class CheckCommand {

    private final TotemGuard plugin;

    private final ExpiringMap<UUID, Long> cooldownCache = ExpiringMap.builder()
            .expiration(15, TimeUnit.SECONDS)
            .build();

    public CheckCommand(TotemGuard plugin) {
        this.plugin = plugin;
    }

    public CommandAPICommand init() {
        return new CommandAPICommand("check")
                .withPermission("TotemGuard.Check")
                .withArguments(new EntitySelectorArgument.OnePlayer("target").replaceSuggestions(ArgumentSuggestions.strings(info -> Bukkit.getOnlinePlayers().stream().map(Player::getName).toArray(String[]::new))))
                .withOptionalArguments(new IntegerArgument("duration", 0, 5000))
                .executes(this::handleCommand);
    }

    private void handleCommand(CommandSender sender, CommandArguments args) {
        Checks.ManualTotemA settings = plugin.getConfigManager().getChecks().getManualTotemA();
        int checkDuration = (int) args.getOptional("duration").orElse(settings.getCheckTime());

        Player target = (Player) args.get("target");
    }
}
