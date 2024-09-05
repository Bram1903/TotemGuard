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

package com.deathmotion.totemguard.checks.impl.manual;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.checks.Check;
import com.deathmotion.totemguard.checks.ICheck;
import com.deathmotion.totemguard.config.Settings;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import io.github.retrooper.packetevents.util.folia.TaskWrapper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ManualTotemA extends Check implements ICheck, CommandExecutor, TabExecutor {

    private final TotemGuard plugin;

    public ManualTotemA(TotemGuard plugin) {
        super(plugin, "ManualTotemA", "Attempts to bait the player into replacing their totem.");

        this.plugin = plugin;
        plugin.getCommand("check").setExecutor(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("TotemGuard.Check")) {
            sender.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
            return false;
        }

        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /check <player>", NamedTextColor.RED));
            return false;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found!", NamedTextColor.RED));
            return false;
        }

        if (target.getGameMode() != org.bukkit.GameMode.SURVIVAL && target.getGameMode() != org.bukkit.GameMode.ADVENTURE) {
            sender.sendMessage(Component.text("This player is not in survival mode!", NamedTextColor.RED));
            return false;
        }

        if (!hasTotemInOffhand(target)) {
            sender.sendMessage(Component.text("This player has no totem in their offhand!", NamedTextColor.RED));
            return false;
        }

        final Settings.Checks.ManualTotemA settings = plugin.getConfigManager().getSettings().getChecks().getManualTotemA();
        if (target.isInvulnerable() && settings.isToggleDamageOnCheck()) {
            sender.sendMessage(Component.text("This player is invulnerable and cannot take any damage!", NamedTextColor.RED));
            return false;
        }

        ItemStack originalTotem = removeTotemFromOffhand(target);
        applyDamageIfNeeded(target, settings);

        // This is not the most elegant solution,
        // but it's necessary in this case to allow for cancelling the outer task.
        // If anyone knows a better way to handle this, please let me know.
        final TaskWrapper[] taskWrapper = new TaskWrapper[1];
        final AtomicBoolean checkCompleted = new AtomicBoolean(false);
        final long startTime = System.nanoTime();

        taskWrapper[0] = FoliaScheduler.getAsyncScheduler().runAtFixedRate(plugin, (t) -> {
            FoliaScheduler.getEntityScheduler().run(target, plugin, (o) -> {
                if (checkCompleted.get()) {
                    return;
                }

                long currentElapsedNanos = System.nanoTime() - startTime;
                int currentElapsedMillis = (int) (currentElapsedNanos / 1_000_000);

                if (hasTotemInOffhand(target)) {
                    target.getInventory().setItemInOffHand(originalTotem);
                    flag(target, createDetails(sender, currentElapsedMillis), settings);
                    checkCompleted.set(true);
                    taskWrapper[0].cancel();
                } else if (currentElapsedMillis >= settings.getCheckTime()) {
                    target.getInventory().setItemInOffHand(originalTotem);
                    sender.sendMessage(Component.text(target.getName() + " has passed the check successfully!", NamedTextColor.GREEN));
                    checkCompleted.set(true);
                    taskWrapper[0].cancel();
                }
            }, null);
        }, 0, 50L, TimeUnit.MILLISECONDS);

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("TotemGuard.Check") || args.length != 1) {
            return List.of();
        }

        String argsLowerCase = args[0].toLowerCase();

        if (sender instanceof Player) {
            String senderName = sender.getName().toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> !name.toLowerCase().equals(senderName))
                    .filter(name -> name.toLowerCase().startsWith(argsLowerCase))
                    .toList();
        }

        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(argsLowerCase))
                .toList();
    }

    private boolean hasTotemInOffhand(Player player) {
        return player.getInventory().getItemInOffHand().getType() == Material.TOTEM_OF_UNDYING;
    }

    private ItemStack removeTotemFromOffhand(Player target) {
        ItemStack totem = target.getInventory().getItemInOffHand();
        target.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
        return totem;
    }

    private void applyDamageIfNeeded(Player target, Settings.Checks.ManualTotemA settings) {
        if (!settings.isToggleDamageOnCheck()) {
            return;
        }

        double currentHealth = target.getHealth();
        double damage = Math.min(currentHealth / 1.25, currentHealth - 1);

        if (damage <= 0) {
            return;
        }

        target.damage(damage);
    }


    private Component createDetails(CommandSender sender, int elapsedMs) {
        return Component.text()
                .append(Component.text("Staff: ", NamedTextColor.GRAY))
                .append(Component.text(sender.getName(), NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.text("Elapsed Ms: ", NamedTextColor.GRAY))
                .append(Component.text(elapsedMs, NamedTextColor.GOLD))
                .build();
    }

}
