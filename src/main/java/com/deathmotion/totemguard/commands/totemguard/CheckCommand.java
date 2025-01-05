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
import com.deathmotion.totemguard.checks.impl.manual.ManualTotemA;
import com.deathmotion.totemguard.config.Checks;
import com.deathmotion.totemguard.messenger.impl.CommandMessengerService;
import com.deathmotion.totemguard.models.TotemPlayer;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import io.github.retrooper.packetevents.util.folia.TaskWrapper;
import net.jodah.expiringmap.ExpiringMap;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class CheckCommand {

    private final TotemGuard plugin;
    private final CommandMessengerService commandMessengerService;
    private final Material totemMaterial = Material.TOTEM_OF_UNDYING;

    private final ExpiringMap<UUID, Long> cooldownCache = ExpiringMap.builder()
            .expiration(15, TimeUnit.SECONDS)
            .build();

    public CheckCommand(TotemGuard plugin) {
        this.plugin = plugin;
        this.commandMessengerService = plugin.getMessengerService().getCommandMessengerService();
    }

    public CommandAPICommand init() {
        return new CommandAPICommand("check")
                .withPermission("TotemGuard.Check")
                .withArguments(new EntitySelectorArgument.OnePlayer("target").replaceSuggestions(ArgumentSuggestions.strings(info -> Bukkit.getOnlinePlayers().stream().map(Player::getName).toArray(String[]::new))))
                .withOptionalArguments(new IntegerArgument("duration", 0, 5000))
                .executes(this::handleCommand);
    }

    private void handleCommand(CommandSender sender, CommandArguments args) {
        Player target = (Player) args.get("target");

        Checks.ManualTotemA settings = plugin.getConfigManager().getChecks().getManualTotemA();
        int checkDuration = (int) args.getOptional("duration").orElse(settings.getCheckTime());
        if (isTargetOnCooldown(sender, target, checkDuration)) return;

        TotemPlayer totemPlayer = plugin.getPlayerDataManager().getPlayer(target);
        if (totemPlayer == null) {
            sender.sendMessage(commandMessengerService.targetCannotBeChecked());
            return;
        }

        if (!isTargetInValidMode(sender, target)) return;
        if (!targetHasTotemInHand(sender, target)) return;

        // Capture original player state for later restoration
        PlayerInventory inventory = target.getInventory();
        ItemStack[] originalInventory = Arrays.stream(inventory.getContents()).map(item -> item == null ? null : item.clone()).toArray(ItemStack[]::new);
        double originalHealth = target.getHealth();
        int originalFoodLevel = target.getFoodLevel();
        float originalSaturation = target.getSaturation();
        Collection<PotionEffect> originalEffects = new ArrayList<>(target.getActivePotionEffects());

        prepareTargetForCheck(target, inventory);

        // Use an event listener to confirm damage application
        final UUID targetUUID = target.getUniqueId();
        final double healthBeforeDamage = target.getHealth();
        final boolean[] damageApplied = {false};

        Listener damageListener = new Listener() {
            @EventHandler(priority = EventPriority.MONITOR)
            public void onDamage(EntityDamageEvent event) {
                if (!event.getEntity().getUniqueId().equals(targetUUID)) return;
                // This is the final state of the event after all plugins
                boolean actuallyDamaged = !event.isCancelled() && event.getFinalDamage() > 0;
                if (actuallyDamaged) {
                    damageApplied[0] = true;
                }
                // Unregister this listener now
                EntityDamageEvent.getHandlerList().unregister(this);
            }
        };

        // Register the listener and then apply damage
        Bukkit.getPluginManager().registerEvents(damageListener, plugin);
        target.damage(healthBeforeDamage + 1000); // Trigger damage to force a check

        // At this point, the event has been fired synchronously.
        if (!damageApplied[0]) {
            // Damage not applied, revert player state
            resetTargetState(target, originalHealth, originalInventory, originalFoodLevel, originalSaturation, originalEffects);
            sender.sendMessage(commandMessengerService.targetNoDamage());
            return;
        }

        // If damage is applied, proceed as normal
        startCheckTimer(sender, target, totemPlayer, checkDuration, originalHealth, originalInventory, originalFoodLevel, originalSaturation, originalEffects, settings);
    }

    /**
     * Check if target is in survival or adventure mode.
     */
    private boolean isTargetInValidMode(CommandSender sender, Player target) {
        if (target.getGameMode() != GameMode.SURVIVAL && target.getGameMode() != GameMode.ADVENTURE) {
            sender.sendMessage(commandMessengerService.playerNotInSurvival());
            return false;
        }

        if (target.isInvulnerable()) {
            sender.sendMessage(commandMessengerService.playerInvulnerable());
            return false;
        }

        return true;
    }

    /**
     * Check if the target currently has a totem in hand.
     */
    private boolean targetHasTotemInHand(CommandSender sender, Player target) {
        PlayerInventory inventory = target.getInventory();
        boolean hasTotem = inventory.getItemInMainHand().getType() == totemMaterial || inventory.getItemInOffHand().getType() == totemMaterial;

        if (!hasTotem) {
            sender.sendMessage(commandMessengerService.playerNoTotem());
            return false;
        }
        return true;
    }

    /**
     * Check if the target is currently on cooldown.
     */
    private boolean isTargetOnCooldown(CommandSender sender, Player target, long checkTime) {
        UUID targetUUID = target.getUniqueId();
        long currentTime = System.currentTimeMillis();

        if (cooldownCache.containsKey(targetUUID)) {
            long lastExecution = cooldownCache.get(targetUUID);
            long elapsedTime = currentTime - lastExecution;
            long totalCooldown = checkTime + 1000; // Additional 1s cooldown

            if (elapsedTime < totalCooldown) {
                long remaining = totalCooldown - elapsedTime;
                sender.sendMessage(commandMessengerService.targetOnCooldown(remaining));
                return true;
            }
        }

        cooldownCache.put(targetUUID, System.currentTimeMillis());
        return false;
    }

    /**
     * Prepare the target for the totem check by modifying their inventory and health.
     */
    private void prepareTargetForCheck(Player target, PlayerInventory inventory) {
        final ItemStack mainHandItem = inventory.getItemInMainHand();
        final ItemStack offHandItem = inventory.getItemInOffHand();
        final boolean hasTotemInMainHand = mainHandItem.getType() == totemMaterial;
        final boolean hasTotemInOffHand = offHandItem.getType() == totemMaterial;

        // Ensure only 1 totem in each hand if they have any
        if (hasTotemInMainHand) mainHandItem.setAmount(1);
        if (hasTotemInOffHand) offHandItem.setAmount(1);

        // If both hands have totems, clear main hand to force them to rely on off-hand
        if (hasTotemInMainHand && hasTotemInOffHand) {
            inventory.setItemInMainHand(new ItemStack(Material.AIR));
        }

        // Place a single additional totem in one of the hotbar slots other than main hand
        int mainHandSlot = inventory.getHeldItemSlot();
        for (int i = 0; i < 9; i++) {
            if (i != mainHandSlot) {
                inventory.setItem(i, new ItemStack(totemMaterial));
                break;
            }
        }

        // Reduce player's health so damage is guaranteed high enough to trigger the totem use if allowed
        target.setHealth(0.5);
    }

    /**
     * Start the periodic check to see if the player uses a totem before the time expires.
     */
    private void startCheckTimer(CommandSender sender, Player target, TotemPlayer totemPlayer, long checkTime, double originalHealth, ItemStack[] originalInventory, int originalFoodLevel, float originalSaturation, Collection<PotionEffect> originalEffects, Checks.ManualTotemA settings) {
        final long startTime = System.currentTimeMillis();
        final PlayerInventory inventory = target.getInventory();
        final TaskWrapper[] taskWrapper = new TaskWrapper[1];

        taskWrapper[0] = FoliaScheduler.getAsyncScheduler().runAtFixedRate(plugin, (o) -> {
            long elapsedTime = System.currentTimeMillis() - startTime;

            // If time expired, player passes the check
            if (elapsedTime >= checkTime) {
                sender.sendMessage(commandMessengerService.targetPassedCheck(target.getName()));
                resetTargetState(target, originalHealth, originalInventory, originalFoodLevel, originalSaturation, originalEffects);
                taskWrapper[0].cancel();
                return;
            }

            // If the player still has a totem in the off-hand, they fail the check
            if (inventory.getItemInOffHand().getType() == totemMaterial) {
                resetTargetState(target, originalHealth, originalInventory, originalFoodLevel, originalSaturation, originalEffects);
                taskWrapper[0].cancel();

                totemPlayer.checkManager.getGenericCheck(ManualTotemA.class).handle(sender, elapsedTime, checkTime);
            }
        }, 0, 50, TimeUnit.MILLISECONDS);
    }

    /**
     * Reset target state to what it was before the check.
     */
    private void resetTargetState(Player player, double health, ItemStack[] inventoryContents, int foodLevel, float saturation, Collection<PotionEffect> effects) {
        FoliaScheduler.getGlobalRegionScheduler().run(plugin, (o) -> {
            player.setHealth(health);
            player.setFoodLevel(foodLevel);
            player.setSaturation(saturation);

            // Clear current effects and re-apply the original ones
            player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
            player.addPotionEffects(effects);

            // Restore inventory
            player.getInventory().setContents(inventoryContents);
        });
    }
}
