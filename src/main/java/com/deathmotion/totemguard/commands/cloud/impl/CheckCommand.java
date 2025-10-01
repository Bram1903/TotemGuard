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

package com.deathmotion.totemguard.commands.cloud.impl;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.checks.impl.manual.ManualTotemA;
import com.deathmotion.totemguard.commands.cloud.AbstractCommand;
import com.deathmotion.totemguard.commands.cloud.arguments.PlayerSuggestion;
import com.deathmotion.totemguard.config.Checks;
import com.deathmotion.totemguard.messenger.CommandMessengerService;
import com.deathmotion.totemguard.models.TotemPlayer;
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
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.bukkit.parser.PlayerParser;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.paper.LegacyPaperCommandManager;
import org.incendo.cloud.parser.standard.IntegerParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class CheckCommand extends AbstractCommand {

    private final TotemGuard plugin;
    private final CommandMessengerService commandMessengerService;

    /**
     * The material representing the Totem of Undying.
     * Keeping it final for immutability and clarity.
     */
    private final Material totemMaterial = Material.TOTEM_OF_UNDYING;

    /**
     * Map to store cooldowns (in milliseconds) for players who have recently been checked.
     * Uses ExpiringMap to automatically remove entries after 15s, though we manually
     * enforce a total cooldown time that depends on check duration.
     */
    private final ExpiringMap<UUID, Long> cooldownCache = ExpiringMap.builder()
            .expiration(15, TimeUnit.SECONDS)
            .build();

    public CheckCommand(TotemGuard plugin) {
        this.plugin = plugin;
        this.commandMessengerService = plugin.getMessengerService().getCommandMessengerService();
    }

    @Override
    public void register(final LegacyPaperCommandManager<CommandSender> commandManager) {
        commandManager.command(root(commandManager)
                .literal("check", Description.of("Forces a player to re-totem"))
                .required("target", PlayerParser.playerParser(), PlayerSuggestion.onlinePlayerSuggestions())
                .optional("duration", IntegerParser.integerParser(0, 5000))
                .permission(perm("Check"))
                .handler(this::handle)
        );
    }

    private void handle(@NonNull final CommandContext<CommandSender> ctx) {
        final CommandSender sender = ctx.sender();
        final Player target = ctx.getOrDefault("target", null);

        final Checks.ManualTotemA settings = plugin.getConfigManager().getChecks().getManualTotemA();

        // Get the custom duration if supplied, otherwise use the default config value.
        final int checkDuration = ctx.getOrDefault("duration", settings.getCheckTime());

        // Check for cooldown before proceeding.
        if (isTargetOnCooldown(sender, target, checkDuration)) {
            return;
        }

        // Get TotemPlayer data; if unavailable, we cannot proceed.
        final TotemPlayer totemPlayer = plugin.getPlayerDataManager().getPlayer(target);
        if (totemPlayer == null) {
            sender.sendMessage(commandMessengerService.targetCannotBeChecked());
            return;
        }

        // Ensure the target is in a valid state (survival/adventure, not invulnerable, has totem).
        if (!isTargetInValidGameState(sender, target)) {
            return;
        }
        if (!doesTargetHaveTotem(sender, target)) {
            return;
        }

        // Store original player state (health, food, saturation, potion effects, inventory).
        final PlayerInventory playerInventory = target.getInventory();
        final ItemStack[] originalInventory = Arrays.stream(playerInventory.getContents())
                .map(item -> (item == null) ? null : item.clone())
                .toArray(ItemStack[]::new);
        final double originalHealth = target.getHealth();
        final int originalFoodLevel = target.getFoodLevel();
        final float originalSaturation = target.getSaturation();
        final Collection<PotionEffect> originalEffects = new ArrayList<>(target.getActivePotionEffects());

        // Prepare the target for the forced-damage check (adjust inventory and health).
        prepareTargetForCheck(target, playerInventory);

        // We want to verify that damage is actually dealt; we track this in a small array for concurrency.
        final UUID targetUUID = target.getUniqueId();
        final double healthBeforeDamage = target.getHealth();
        final boolean[] damageApplied = {false};

        // Create and register an event listener that observes whether damage actually occurred.
        Listener damageListener = new Listener() {
            @EventHandler(priority = EventPriority.MONITOR)
            public void onDamage(EntityDamageEvent event) {
                // Check if this event is for our targeted player.
                if (!event.getEntity().getUniqueId().equals(targetUUID)) {
                    return;
                }
                // If damage is not canceled and finalDamage > 0, we consider it "actually damaged."
                if (!event.isCancelled() && event.getFinalDamage() > 0) {
                    damageApplied[0] = true;
                }
                // Unregister this listener immediately after checking.
                EntityDamageEvent.getHandlerList().unregister(this);
            }
        };
        Bukkit.getPluginManager().registerEvents(damageListener, plugin);

        // Force damage high enough to trigger the totem if possible.
        target.damage(healthBeforeDamage + 1000);

        // If no damage was dealt, revert the player's state and notify.
        if (!damageApplied[0]) {
            restorePlayerState(target, originalHealth, originalInventory, originalFoodLevel, originalSaturation, originalEffects);
            sender.sendMessage(commandMessengerService.targetNoDamage());
            return;
        }

        // Otherwise, schedule the repeated task to watch for totem usage within the allowed time.
        scheduleCheckTask(
                sender,
                target,
                totemPlayer,
                checkDuration,
                originalHealth,
                originalInventory,
                originalFoodLevel,
                originalSaturation,
                originalEffects
        );
    }

    /**
     * Ensures the target is in SURVIVAL or ADVENTURE mode, and not invulnerable.
     *
     * @param sender the command sender
     * @param target the player being forced to check
     * @return true if the target is in a valid mode and not invulnerable, otherwise false
     */
    private boolean isTargetInValidGameState(CommandSender sender, Player target) {
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
     * Checks if the target has a Totem of Undying in either the main hand or off-hand.
     *
     * @param sender the command sender
     * @param target the player being forced to check
     * @return true if the player holds a totem, otherwise false
     */
    private boolean doesTargetHaveTotem(CommandSender sender, Player target) {
        final PlayerInventory inventory = target.getInventory();
        final boolean hasTotem = (inventory.getItemInMainHand().getType() == totemMaterial) ||
                (inventory.getItemInOffHand().getType() == totemMaterial);

        if (!hasTotem) {
            sender.sendMessage(commandMessengerService.playerNoTotem());
            return false;
        }
        return true;
    }

    /**
     * Checks if the target is currently on cooldown based on previous checks.
     * The total cooldown is (checkTime + 1000ms). If the target is still in cooldown,
     * the sender is notified of the remaining time.
     *
     * @param sender    the command sender
     * @param target    the target player
     * @param checkTime the duration of the check used to extend cooldown
     * @return true if the target is on cooldown, otherwise false
     */
    private boolean isTargetOnCooldown(CommandSender sender, Player target, long checkTime) {
        final UUID targetUUID = target.getUniqueId();
        final long currentTime = System.currentTimeMillis();

        if (cooldownCache.containsKey(targetUUID)) {
            final long lastExecutionTime = cooldownCache.get(targetUUID);
            final long elapsedTime = currentTime - lastExecutionTime;
            // Add an extra 1s (1000ms) to the checkTime for total cooldown.
            final long totalCooldown = checkTime + 1000;

            if (elapsedTime < totalCooldown) {
                long remainingTime = totalCooldown - elapsedTime;
                sender.sendMessage(commandMessengerService.targetOnCooldown(remainingTime));
                return true;
            }
        }

        // If not on cooldown, store the current timestamp.
        cooldownCache.put(targetUUID, currentTime);
        return false;
    }

    /**
     * Prepares the player's inventory and health in such a way that if the totem can be used,
     * it will be forced to trigger. Ensures that the player only retains 1 totem in a single hand.
     *
     * @param target    the player who will be forced to check
     * @param inventory the player's inventory
     */
    private void prepareTargetForCheck(Player target, PlayerInventory inventory) {
        final ItemStack mainHandItem = inventory.getItemInMainHand();
        final ItemStack offHandItem = inventory.getItemInOffHand();
        final boolean hasTotemInMainHand = (mainHandItem.getType() == totemMaterial);
        final boolean hasTotemInOffHand = (offHandItem.getType() == totemMaterial);

        // Ensure only one totem in each hand if they are present.
        if (hasTotemInMainHand) {
            mainHandItem.setAmount(1);
        }
        if (hasTotemInOffHand) {
            offHandItem.setAmount(1);
        }

        // If both hands have totems, clear main hand to force reliance on off-hand.
        if (hasTotemInMainHand && hasTotemInOffHand) {
            inventory.setItemInMainHand(null);
        }

        // Place a single additional totem in one of the hotbar slots (other than the main hand slot).
        final int mainHandSlot = inventory.getHeldItemSlot();
        for (int i = 0; i < 9; i++) {
            if (i != mainHandSlot) {
                inventory.setItem(i, new ItemStack(totemMaterial));
                break;
            }
        }

        // Slightly reduce player's health so that forced damage is guaranteed to be fatal if the totem does not trigger.
        target.setHealth(0.5);
    }

    /**
     * Schedules a periodic task (every 50ms) that checks whether the player still has a totem
     * in their off-hand. If time expires, the player "passes" the check; if the totem remains
     * in off-hand earlier, the player is flagged.
     *
     * @param sender             the command sender
     * @param target             the player under check
     * @param totemPlayer        the TotemPlayer wrapper for check management
     * @param checkTime          total time (ms) the player must keep from using the totem
     * @param originalHealth     the player's health prior to check
     * @param originalInventory  the player's inventory prior to check
     * @param originalFoodLevel  the player's food level prior to check
     * @param originalSaturation the player's saturation prior to check
     * @param originalEffects    the player's potion effects prior to check
     */
    private void scheduleCheckTask(CommandSender sender, Player target, TotemPlayer totemPlayer, long checkTime, double originalHealth, ItemStack[] originalInventory, int originalFoodLevel, float originalSaturation, Collection<PotionEffect> originalEffects) {
        final long startTime = System.currentTimeMillis();
        final PlayerInventory inventory = target.getInventory();

        // TaskWrapper is needed to manage and cancel the repeating task cleanly.
        final TaskWrapper[] taskWrapper = new TaskWrapper[1];
        taskWrapper[0] = FoliaScheduler.getAsyncScheduler().runAtFixedRate(plugin, (o) -> {
            long elapsedTime = System.currentTimeMillis() - startTime;

            // If the check duration has elapsed, the player "passes" the check.
            if (elapsedTime >= checkTime) {
                sender.sendMessage(commandMessengerService.targetPassedCheck(target.getName()));
                restorePlayerState(target, originalHealth, originalInventory, originalFoodLevel, originalSaturation, originalEffects);
                taskWrapper[0].cancel();
                return;
            }

            // If the player suddenly switches a totem to their off-hand, flag them.
            if (inventory.getItemInOffHand().getType() == totemMaterial) {
                restorePlayerState(target, originalHealth, originalInventory, originalFoodLevel, originalSaturation, originalEffects);
                taskWrapper[0].cancel();

                // Trigger the ManualTotemA check handling logic.
                totemPlayer.checkManager.getGenericCheck(ManualTotemA.class).handle(sender, elapsedTime, checkTime);
            }
        }, 0, 50, TimeUnit.MILLISECONDS);
    }

    /**
     * Restores the player's state back to how it was before the check:
     * - Health, food level, saturation, potion effects, and inventory.
     *
     * @param player            the player to restore
     * @param health            the player's original health
     * @param inventoryContents the player's original inventory contents
     * @param foodLevel         the player's original food level
     * @param saturation        the player's original saturation
     * @param effects           the player's original potion effects
     */
    private void restorePlayerState(Player player, double health, ItemStack[] inventoryContents, int foodLevel, float saturation, Collection<PotionEffect> effects) {
        // Schedule restoration on the global region scheduler (main thread) to avoid concurrency issues.
        FoliaScheduler.getEntityScheduler().run(player, plugin, (o) -> {
            player.setHealth(health);
            player.setFoodLevel(foodLevel);
            player.setSaturation(saturation);

            // Remove current potion effects and reapply the originals.
            player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
            player.addPotionEffects(effects);

            // Restore the original inventory contents.
            player.getInventory().setContents(inventoryContents);
        }, null);
    }
}
