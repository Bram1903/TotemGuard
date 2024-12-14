/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2024 Bram
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
        super(plugin, "ManualTotemA", "Manual totem removal", CheckType.Manual);
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
        if (!isArgumentLengthValid(sender, args)) return false;

        Player target = getTargetPlayer(sender, args[1]);
        if (target == null) return false;

        Settings.Checks.ManualTotemA settings = configManager.getSettings().getChecks().getManualTotemA();
        long checkTime = getCheckTime(sender, args, settings);
        if (checkTime <= 0) return false; // Error message already sent in getCheckTime

        if (!isPlayerInValidMode(sender, target)) return false;
        if (!playerHasTotemInHand(sender, target)) return false;
        if (isPlayerOnCooldown(sender, target, checkTime)) return false;

        // Record the start of the cooldown
        cooldowns.put(target.getUniqueId(), System.currentTimeMillis());

        // Capture original player state for later restoration
        PlayerInventory inventory = target.getInventory();
        ItemStack[] originalInventory = Arrays.stream(inventory.getContents()).map(item -> item == null ? null : item.clone()).toArray(ItemStack[]::new);
        double originalHealth = target.getHealth();
        int originalFoodLevel = target.getFoodLevel();
        float originalSaturation = target.getSaturation();
        Collection<PotionEffect> originalEffects = new ArrayList<>(target.getActivePotionEffects());

        preparePlayerForCheck(target, inventory);

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
            resetPlayerState(target, originalHealth, originalInventory, originalFoodLevel, originalSaturation, originalEffects);
            sendErrorMessage(sender, "The player did not receive any damage! Are they protected by a plugin or in a safe zone?");
            return false;
        }

        // If damage is applied, proceed as normal
        startCheckTimer(sender, target, checkTime, originalHealth, originalInventory, originalFoodLevel, originalSaturation, originalEffects, settings);

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            String prefix = args[1].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(prefix))
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

    /**
     * Validate command arguments length.
     */
    private boolean isArgumentLengthValid(CommandSender sender, String[] args) {
        if (args.length < 2 || args.length > 3) {
            sendErrorMessage(sender, "Usage: /totemguard check <player> [ms]");
            return false;
        }
        return true;
    }

    /**
     * Retrieve the target player from args.
     */
    private Player getTargetPlayer(CommandSender sender, String playerName) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sendErrorMessage(sender, "Player not found!");
        }
        return target;
    }

    /**
     * Determine the check time either from args or from default settings.
     */
    private long getCheckTime(CommandSender sender, String[] args, Settings.Checks.ManualTotemA settings) {
        if (args.length == 3) {
            try {
                long time = Long.parseLong(args[2]);

                if (time <= 0) {
                    sendErrorMessage(sender, "The check time must be a positive number!");
                    return -1;
                }

                if (time > 5000) {
                    sendErrorMessage(sender, "The check time must be less than 5000ms!");
                    return -1;
                }

                return time;
            } catch (NumberFormatException e) {
                sendErrorMessage(sender, "Invalid time format! Please enter a valid number.");
                return -1;
            }
        } else {
            return settings.getCheckTime();
        }
    }

    /**
     * Check if player is in survival or adventure mode.
     */
    private boolean isPlayerInValidMode(CommandSender sender, Player target) {
        if (target.getGameMode() != GameMode.SURVIVAL && target.getGameMode() != GameMode.ADVENTURE) {
            sendErrorMessage(sender, "This player is not in survival mode!");
            return false;
        }

        if (target.isInvulnerable()) {
            sendErrorMessage(sender, "This player is invulnerable!");
            return false;
        }

        return true;
    }

    /**
     * Check if the player currently has a totem in hand.
     */
    private boolean playerHasTotemInHand(CommandSender sender, Player target) {
        PlayerInventory inventory = target.getInventory();
        boolean hasTotem = inventory.getItemInMainHand().getType() == totemMaterial || inventory.getItemInOffHand().getType() == totemMaterial;

        if (!hasTotem) {
            sendErrorMessage(sender, "This player does not have a totem in their hands!");
            return false;
        }
        return true;
    }

    /**
     * Check if the player is currently on cooldown.
     */
    private boolean isPlayerOnCooldown(CommandSender sender, Player target, long checkTime) {
        UUID targetUUID = target.getUniqueId();
        long currentTime = System.currentTimeMillis();

        if (cooldowns.containsKey(targetUUID)) {
            long lastExecution = cooldowns.get(targetUUID);
            long elapsedTime = currentTime - lastExecution;
            long totalCooldown = checkTime + 1000; // Additional 1s cooldown

            if (elapsedTime < totalCooldown) {
                long remaining = totalCooldown - elapsedTime;
                sendErrorMessage(sender, "This player is on cooldown for " + remaining + "ms!");
                return true;
            }
        }
        return false;
    }

    /**
     * Prepare the player for the totem check by modifying their inventory and health.
     */
    private void preparePlayerForCheck(Player target, PlayerInventory inventory) {
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
    private void startCheckTimer(CommandSender sender, Player target, long checkTime, double originalHealth, ItemStack[] originalInventory, int originalFoodLevel, float originalSaturation, Collection<PotionEffect> originalEffects, Settings.Checks.ManualTotemA settings) {
        final long startTime = System.currentTimeMillis();
        final PlayerInventory inventory = target.getInventory();
        final TaskWrapper[] taskWrapper = new TaskWrapper[1];

        taskWrapper[0] = FoliaScheduler.getAsyncScheduler().runAtFixedRate(plugin, (o) -> {
            long elapsedTime = System.currentTimeMillis() - startTime;

            // If time expired, player passes the check
            if (elapsedTime >= checkTime) {
                sender.sendMessage(messageService.getPrefix().append(Component.text(target.getName() + " has successfully passed the check!", NamedTextColor.GREEN)));
                resetPlayerState(target, originalHealth, originalInventory, originalFoodLevel, originalSaturation, originalEffects);
                taskWrapper[0].cancel();
                return;
            }

            // If the player still has a totem in the off-hand, they fail the check
            if (inventory.getItemInOffHand().getType() == totemMaterial) {
                resetPlayerState(target, originalHealth, originalInventory, originalFoodLevel, originalSaturation, originalEffects);
                taskWrapper[0].cancel();
                flag(target, createDetails(sender, elapsedTime, checkTime), settings);
            }
        }, 0, 50, TimeUnit.MILLISECONDS);
    }

    /**
     * Reset player's state to what it was before the check.
     */
    private void resetPlayerState(Player player, double health, ItemStack[] inventoryContents, int foodLevel, float saturation, Collection<PotionEffect> effects) {
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

    /**
     * Create the component containing details about the failed check.
     */
    private Component createDetails(CommandSender sender, long elapsedMs, long checkTime) {
        Pair<TextColor, TextColor> colorScheme = messageService.getColorScheme();

        return Component.text()
                .append(Component.text("Staff: ", colorScheme.getY()))
                .append(Component.text(sender.getName(), colorScheme.getX()))
                .append(Component.newline())
                .append(Component.text("Elapsed Time: ", colorScheme.getY()))
                .append(Component.text(elapsedMs + "ms", colorScheme.getX()))
                .append(Component.newline())
                .append(Component.text("Max Check Duration: ", colorScheme.getY()))
                .append(Component.text(checkTime + "ms", colorScheme.getX()))
                .build();
    }

    /**
     * Send a formatted error message to the sender.
     */
    private void sendErrorMessage(CommandSender sender, String message) {
        sender.sendMessage(messageService.getPrefix().append(Component.text(message, NamedTextColor.RED)));
    }
}
