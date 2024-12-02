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
import org.bukkit.potion.PotionEffect;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class CheckCommand extends Check implements SubCommand {
    private static CheckCommand instance;
    private final ConcurrentHashMap<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    private final TotemGuard plugin;
    private final ConfigManager configManager;
    private final MessageService messageService;

    private final Material totemMaterial = Material.TOTEM_OF_UNDYING;

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

        final Settings.Checks.ManualTotemA settings = configManager.getSettings().getChecks().getManualTotemA();
        UUID targetUUID = target.getUniqueId();
        long currentTime = System.currentTimeMillis();

        if (target.getGameMode() != org.bukkit.GameMode.SURVIVAL && target.getGameMode() != org.bukkit.GameMode.ADVENTURE) {
            sender.sendMessage(messageService.getPrefix().append(Component.text("This player is not in survival mode!", NamedTextColor.RED)));
            return false;
        }

        final PlayerInventory inventory = target.getInventory();
        final boolean hasTotemInMainHand = inventory.getItemInMainHand().getType() == totemMaterial;
        final boolean hasTotemInOffHand = inventory.getItemInOffHand().getType() == totemMaterial;

        if (!hasTotemInMainHand && !hasTotemInOffHand) {
            sender.sendMessage(messageService.getPrefix().append(Component.text("This player does not have a totem in their hands!", NamedTextColor.RED)));
            return false;
        }

        if (cooldowns.containsKey(targetUUID)) {
            long lastExecutionTime = cooldowns.get(targetUUID);
            long elapsedTime = currentTime - lastExecutionTime;
            long totalCooldown = settings.getCheckTime() + 1000;

            if (elapsedTime < totalCooldown) {
                long remainingTime = totalCooldown - elapsedTime;
                sender.sendMessage(messageService.getPrefix().append(Component.text("This player is on cooldown for " + remainingTime + "ms!", NamedTextColor.RED)));
                return false;
            }
        }

        cooldowns.put(targetUUID, currentTime);

        // Deep clone the player's inventory
        final ItemStack[] originalInventoryContents = Arrays.stream(inventory.getContents())
                .map(item -> item == null ? null : item.clone())
                .toArray(ItemStack[]::new);

        final double originalHealth = target.getHealth();
        final int originalFoodLevel = target.getFoodLevel();
        final float originalSaturation = target.getSaturation();

        final Collection<PotionEffect> originalEffects = new ArrayList<>(target.getActivePotionEffects());

        final int mainHandSlot = inventory.getHeldItemSlot();
        if (hasTotemInMainHand && hasTotemInOffHand) {
            inventory.setItemInMainHand(new ItemStack(Material.AIR));
        }

        // Iterate only over hotbar slots (0 to 8)
        for (int i = 0; i < 9; i++) {
            if (i == mainHandSlot) {
                continue; // Skip main hand slot
            }

            inventory.setItem(i, new ItemStack(Material.TOTEM_OF_UNDYING));
            break;
        }

        // Damage the player to ensure the totem is used
        target.damage(Double.MAX_VALUE);

        // Start monitoring for totem replacement
        final long startTime = System.currentTimeMillis();
        final TaskWrapper[] taskWrapper = new TaskWrapper[1];

        taskWrapper[0] = FoliaScheduler.getAsyncScheduler().runAtFixedRate(plugin, (o) -> {
            long elapsedTime = System.currentTimeMillis() - startTime;

            if (elapsedTime >= settings.getCheckTime()) {
                sender.sendMessage(messageService.getPrefix().append(Component.text(target.getName() + " has successfully passed the check!", NamedTextColor.GREEN)));
                resetPlayerState(target, originalHealth, originalInventoryContents, originalFoodLevel, originalSaturation, originalEffects);
                taskWrapper[0].cancel();
                return;
            }

            ItemStack currentOffHandItem = inventory.getItemInOffHand();
            if (currentOffHandItem.getType() == totemMaterial) {
                resetPlayerState(target, originalHealth, originalInventoryContents, originalFoodLevel, originalSaturation, originalEffects);
                taskWrapper[0].cancel();
                flag(target, createDetails(sender, elapsedTime), settings);
            }
        }, 0, 50, TimeUnit.MILLISECONDS);

        return true;
    }

    private void resetPlayerState(Player player, double health, ItemStack[] inventoryContents, int foodLevel, float saturation, Collection<PotionEffect> effects) {
        FoliaScheduler.getGlobalRegionScheduler().run(plugin, (o) -> {
            player.setHealth(health);
            player.setFoodLevel(foodLevel);
            player.setSaturation(saturation);

            player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
            player.addPotionEffects(effects);

            final PlayerInventory inventory = player.getInventory();

            // Restore inventory
            inventory.setContents(inventoryContents);
        });
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
        cooldowns.clear();
        super.resetData();
    }

    @Override
    public void resetData(UUID uuid) {
        cooldowns.remove(uuid);
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
