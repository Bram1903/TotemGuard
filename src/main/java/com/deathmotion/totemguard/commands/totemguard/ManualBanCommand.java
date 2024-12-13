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
import com.deathmotion.totemguard.api.enums.CheckType;
import com.deathmotion.totemguard.checks.Check;
import com.deathmotion.totemguard.commands.SubCommand;
import com.deathmotion.totemguard.models.checks.ICheckSettings;
import com.deathmotion.totemguard.util.MessageService;
import com.deathmotion.totemguard.util.datastructure.Pair;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ManualBanCommand extends Check implements SubCommand {

    private static ManualBanCommand instance;

    private final TotemGuard plugin;
    private final MessageService messageService;

    private ManualBanCommand(TotemGuard plugin) {
        super(plugin, "ManualBan", "Manually ban a player", CheckType.Manual);
        this.plugin = plugin;
        this.messageService = plugin.getMessageService();
    }

    public static ManualBanCommand getInstance(TotemGuard plugin) {
        if (instance == null) {
            instance = new ManualBanCommand(plugin);
        }
        return instance;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!validateArgs(sender, args)) {
            return false;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (!validateTarget(sender, target)) {
            return false;
        }

        flag(target, createDetailsComponent(sender), getCheckSettings());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            String input = args[1].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(input))
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

    private boolean validateArgs(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(getUsageComponent());
            return false;
        }
        return true;
    }

    private boolean validateTarget(CommandSender sender, Player target) {
        if (target == null) {
            sender.sendMessage(messageService.playerNotFound());
            return false;
        }

        if (sender.equals(target)) {
            sender.sendMessage(getCannotBanYourselfComponent());
            return false;
        }

        if (target.hasPermission("TotemGuard.Bypass")) {
            sender.sendMessage(getPlayerCannotBeBannedComponent());
            return false;
        }

        return true;
    }

    private Component getUsageComponent() {
        return messageService.getPrefix()
                .append(Component.text("Usage: /totemguard manualban <player>", NamedTextColor.RED));
    }

    private Component getCannotBanYourselfComponent() {
        return messageService.getPrefix()
                .append(Component.text("You cannot ban yourself!", NamedTextColor.RED));
    }

    private Component getPlayerCannotBeBannedComponent() {
        return messageService.getPrefix()
                .append(Component.text("You cannot ban this player!", NamedTextColor.RED));
    }

    private Component createDetailsComponent(CommandSender sender) {
        Pair<TextColor, TextColor> colors = messageService.getColorScheme();
        return Component.text()
                .append(Component.text("Staff Member: ", colors.getY()))
                .append(Component.text(sender.getName(), colors.getX()))
                .build();
    }

    private ICheckSettings getCheckSettings() {
        var settings = plugin.getConfigManager()
                .getSettings()
                .getChecks()
                .getManualBan();

        return new ICheckSettings() {
            @Override
            public boolean isEnabled() {
                return true;
            }

            @Override
            public boolean isPunishable() {
                return true;
            }

            @Override
            public int getPunishmentDelayInSeconds() {
                return 0;
            }

            @Override
            public int getMaxViolations() {
                return 0;
            }

            @Override
            public List<String> getPunishmentCommands() {
                return settings.getPunishmentCommands();
            }
        };
    }
}
