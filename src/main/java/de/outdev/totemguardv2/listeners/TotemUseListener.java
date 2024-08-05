package de.outdev.totemguardv2.listeners;

import de.outdev.totemguardv2.TotemGuardV2;
import de.outdev.totemguardv2.commands.TotemGuardCommand;
import de.outdev.totemguardv2.data.PermissionConstants;
import de.outdev.totemguardv2.data.Settings;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.HashMap;

public class TotemUseListener implements Listener {

    private final TotemGuardV2 plugin;
    private final Settings settings;

    private final HashMap<Player, Integer> totemUsage;
    private final HashMap<Player, Integer> flagCounts;

    public TotemUseListener(TotemGuardV2 plugin) {
        this.plugin = plugin;
        this.settings = plugin.configManager.getSettings();

        this.totemUsage = new HashMap<>();
        this.flagCounts = new HashMap<>();

        Bukkit.getPluginManager().registerEvents(this, plugin); // registering the events

        // Schedule the reset task
        long resetInterval = settings.getPunish().getRemoveFlagsMin() * 60L * 20L; // Convert minutes to ticks (20 ticks = 1 second)
        Bukkit.getScheduler().runTaskTimer(plugin, this::resetAllFlagCounts, resetInterval, resetInterval);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTotemUse(EntityResurrectEvent event) {
        if (!settings.isToggleAutomaticNormalChecks()) {
            return;
        }

        if (plugin.getTPS() < settings.getDetements().getMinTps()) {
            return;
        }

        if (event.getEntity() instanceof Player player) { // Checks if the entity is a player
            if (player.getPing() > settings.getDetements().getMaxPing()) {
                return;
            }

            int currentTime = (int) System.currentTimeMillis(); // Saves the current time to calculate the time it took to retotem

            totemUsage.put(player, currentTime);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClick(InventoryClickEvent event) { // Inventory click event to check for the totem
        if (event.getWhoClicked() instanceof Player player) { // Checks if the cause is a player
            if (event.getRawSlot() == 45) { // Checks slot 45 which is usually the offhand slot
                checkSuspiciousActivity(player);
            }
        }
    }

    private void checkSuspiciousActivity(Player player) {
        Integer usageTime = totemUsage.get(player);
        if (usageTime != null) {
            int currentTime = (int) System.currentTimeMillis();
            int timeDifference = currentTime - usageTime; // Calculates the time difference

            totemUsage.remove(player);

            if (timeDifference > settings.getNormalCheckTimeMs()) {
                return;
            }

            String flag_01 = "&cS";
            String flag_02 = "&cB";
            String flag_03 = "&cM";
            if (player.isSneaking()) {
                flag_01 = "&aS";
            }
            if (player.isBlocking()) {
                flag_02 = "&aB";
            }
            if (player.isSprinting() || player.isClimbing() || player.isJumping() || player.isSwimming()) { // Set in the config later
                flag_03 = "&aM";
            }

            int realTotem = timeDifference - player.getPing();

            if (settings.isAdvancedSystemCheck()) {
                if (realTotem <= settings.getTriggerAmountMs()) {
                    flag(player, flag_01, flag_02, flag_03, timeDifference, realTotem);
                }
            } else {
                flag(player, flag_01, flag_02, flag_03, timeDifference, realTotem);
            }
        }
    }

    private void sendMiniMessage(Player player, String message) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    public void flag(Player player, String flag_01, String flag_02, String flag_03, int timeDifference, int realTotem) {

        boolean advancedCheck = settings.isAdvancedSystemCheck();
        boolean checkToggle = settings.isToggleExtraFlags();
        String prefix = settings.getPrefix();
        int punishAfter = settings.getPunish().getPunishAfter();
        boolean punish = settings.getPunish().isEnabled();

        String alertMessage;
        String extraFlags = ", &8[&7" + flag_01 + "&7, " + flag_02 + "&7, " + flag_03 + "&8]";
        int flagCount = flagCounts.getOrDefault(player, 0) + 1;
        String flags = "&e[" + flagCount + "/" + punishAfter + "] ";

        if (punish) {
            flagCounts.put(player, flagCount);
        } else {
            flags = "";
        }
        if (!checkToggle) {
            extraFlags = "";
        }
        if (advancedCheck) {
            alertMessage = prefix + "&e" + player.getName() + " Flagged for AutoTotem " + flags + "&7(Ping: " + player.getPing() + ", In: " + timeDifference + "&8[" + realTotem + "]&7ms, " + player.getClientBrandName() + extraFlags + "&7)";
        } else {
            alertMessage = prefix + "&e" + player.getName() + " Flagged for AutoTotem " + flags + "&7(Ping: " + player.getPing() + ", In: " + timeDifference + "ms, " + player.getClientBrandName() + extraFlags + "&7)";
        }

        Bukkit.getOnlinePlayers().forEach(onlinePlayer -> {
            if (onlinePlayer.hasPermission(PermissionConstants.AlertPermission) && TotemGuardCommand.getToggle(onlinePlayer)) {
                sendMiniMessage(onlinePlayer, alertMessage);
            }
        });

        if (!punish) {
            return;
        }

        String punishCommand = settings.getPunish().getPunishCommand().replace("%player%", player.getName());

        if (flagCounts.get(player) >= punishAfter) {

            Bukkit.getScheduler().runTask(plugin, () -> {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), punishCommand);
            });
            flagCounts.remove(player);
        }
    }



    public void resetAllFlagCounts() {
        flagCounts.clear();
        String prefix = settings.getPrefix();

        Bukkit.getOnlinePlayers().forEach(onlinePlayer -> {
            if (onlinePlayer.hasPermission(PermissionConstants.AlertPermission)) {
                sendMiniMessage(onlinePlayer, prefix + "&fAll flag counts have been reset.");
            }
        });
    }
}
