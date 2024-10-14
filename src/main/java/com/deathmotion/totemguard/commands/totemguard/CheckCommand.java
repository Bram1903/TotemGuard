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
import com.deathmotion.totemguard.config.ConfigManager;
import com.deathmotion.totemguard.config.Settings;
import com.deathmotion.totemguard.util.MessageService;
import com.deathmotion.totemguard.util.datastructure.Pair;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import io.github.retrooper.packetevents.util.folia.TaskWrapper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class CheckCommand extends Check implements SubCommand {
    private static CheckCommand instance;

    private final TotemGuard plugin;
    private final ConfigManager configManager;
    private final MessageService messageService;

    private final Material totemMaterial = Material.TOTEM_OF_UNDYING;
    private final ItemStack airItem = new ItemStack(Material.AIR);

    private CheckCommand(TotemGuard plugin) {
        super(plugin, "ManualTotemA", "Manual totem removal");

        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
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
        if (args.length != 2) {
            sender.sendMessage(messageService.getPrefix().append(Component.text("Usage: /totemguard check <player>", NamedTextColor.RED)));
            return false;
        }

        final Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(messageService.getPrefix().append(Component.text("Player not found!", NamedTextColor.RED)));
            return false;
        }

        if (target.getGameMode() != org.bukkit.GameMode.SURVIVAL && target.getGameMode() != org.bukkit.GameMode.ADVENTURE) {
            sender.sendMessage(messageService.getPrefix().append(Component.text("This player is not in survival mode!", NamedTextColor.RED)));
            return false;
        }

        final PlayerInventory inventory = target.getInventory();

        final ItemStack mainHandItem = inventory.getItemInMainHand().clone();
        final ItemStack offHandItem = inventory.getItemInOffHand().clone();
        final boolean totemInMainHand = mainHandItem.getType() == org.bukkit.Material.TOTEM_OF_UNDYING;
        final boolean totemInOffhand = offHandItem.getType() == org.bukkit.Material.TOTEM_OF_UNDYING;

        if (totemInMainHand) {
            if (!totemInOffhand) {
                // Move the totem from the main hand to the offhand
                inventory.setItemInOffHand(mainHandItem);
            }
            // Remove the totem from the main hand in either case
            inventory.setItemInMainHand(airItem);
        } else if (!totemInOffhand) {
            sender.sendMessage(messageService.getPrefix().append(Component.text(target.getName() + " does not have a totem in their main or offhand!", NamedTextColor.RED)));
            return false;
        }

        double health = target.getHealth();
        final Settings.Checks.ManualTotemA settings = configManager.getSettings().getChecks().getManualTotemA();

        // Declare a mutable container to hold the TaskWrapper
        final TaskWrapper[] taskWrapper = new TaskWrapper[1];

        final long startTime = System.currentTimeMillis();
        target.damage(health);

        // Schedule the task and store the reference in the array
        taskWrapper[0] = FoliaScheduler.getAsyncScheduler().runAtFixedRate(plugin, (o) -> {
            long elapsedTime = System.currentTimeMillis() - startTime;

            if (elapsedTime >= settings.getCheckTime()) {
                target.sendMessage(messageService.getPrefix().append(Component.text(target.getName() + " has successfully passed the check!", NamedTextColor.GREEN)));
                resetPlayerState(target, health, mainHandItem, offHandItem);

                // Cancel the task since the check is complete
                taskWrapper[0].cancel();
                return;
            }

            if (inventory.getItemInOffHand().getType() == totemMaterial) {
                resetPlayerState(target, health, mainHandItem, offHandItem);

                // Cancel the task and flag the player since the check has failed
                taskWrapper[0].cancel();
                flag(target, createDetails(sender, elapsedTime), settings);
            }

        }, 0, 50, TimeUnit.MILLISECONDS);

        return true;
    }

    private void resetPlayerState(Player player, double health, ItemStack mainHandItem, ItemStack offHandItem) {
        player.setHealth(health);
        player.getInventory().setItemInMainHand(mainHandItem);
        player.getInventory().setItemInOffHand(offHandItem);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            String argsLowerCase = args[1].toLowerCase();

            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
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

    private Component createDetails(CommandSender sender, long elapsedMs) {
        Pair<TextColor, TextColor> colorScheme = messageService.getColorScheme();

        return Component.text()
                .append(Component.text("Staff: ", colorScheme.getY()))
                .append(Component.text(sender.getName(), colorScheme.getX()))
                .append(Component.newline())
                .append(Component.text("Elapsed Time: ", colorScheme.getY()))
                .append(Component.text(elapsedMs + "ms", colorScheme.getX()))
                .build();
    }
}
