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
import com.deathmotion.totemguard.checks.Check;
import com.deathmotion.totemguard.commands.SubCommand;
import com.deathmotion.totemguard.util.MessageService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class CheckCommand extends Check implements SubCommand {
    private static CheckCommand instance;

    private final TotemGuard plugin;
    private final MessageService messageService;

    private CheckCommand(TotemGuard plugin) {
        super(plugin, "ManualTotemA", "Manual totem removal");

        this.plugin = plugin;
        this.messageService = plugin.getMessageService();
    }

    public static CheckCommand getInstance(TotemGuard plugin) {
        if (instance == null) {
            instance = new CheckCommand(plugin);
        }
        return instance;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(messageService.getPrefix().append(Component.text("Usage: /totemguard check <player>", NamedTextColor.RED)));
            return false;
        }

        final Player player = Bukkit.getPlayer(args[1]);
        if (player == null) {
            sender.sendMessage(messageService.getPrefix().append(Component.text("Player not found!", NamedTextColor.RED)));
            return false;
        }

        sender.sendMessage(messageService.getPrefix().append(Component.text("Checking player " + player.getName() + "...", NamedTextColor.GRAY)));

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            String argsLowerCase = args[1].toLowerCase();

            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> !name.equalsIgnoreCase(sender.getName()))
                    .filter(name -> name.toLowerCase().startsWith(argsLowerCase))
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    @Override
    public void resetData() {
        super.resetData();
    }

    @Override
    public void resetData(UUID uuid) {
        super.resetData(uuid);
    }
}
