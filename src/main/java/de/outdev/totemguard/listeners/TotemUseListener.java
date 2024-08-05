package de.outdev.totemguard.listeners;

import de.outdev.totemguard.TotemGuard;
import de.outdev.totemguard.commands.TotemGuardCommand;
import de.outdev.totemguard.discord.DiscordWebhook;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TotemUseListener implements Listener {

    private final TotemGuard plugin;
    private final HashMap<Player, Integer> totemUsage;
    private final HashMap<UUID, Integer> flagCounts;

    public TotemUseListener(TotemGuard plugin) {
        this.plugin = plugin;
        this.totemUsage = new HashMap<>();
        this.flagCounts = new HashMap<>();


        Bukkit.getPluginManager().registerEvents(this, plugin); // registering the events

        // Schedule the reset task
        long resetInterval = plugin.getConfig().getInt("remove_flags_min") * 60L * 20L; // Convert minutes to ticks (20 ticks = 1 second)
        Bukkit.getScheduler().runTaskTimer(plugin, this::resetAllFlagCounts, resetInterval, resetInterval);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTotemUse(EntityResurrectEvent event) {
        boolean toggleCheck = plugin.getConfig().getBoolean("toggle_automatic_normal_checks");

        if (!toggleCheck) {
            return;
        }

        double minTps = plugin.getConfig().getDouble("min_tps");
        int maxPing = plugin.getConfig().getInt("max_ping");

        if (plugin.getTPS() < minTps) {
            return;
        }

        if (event.getEntity() instanceof Player) { // Checks if the entity is a player
            Player player = (Player) event.getEntity();
            if (player.getPing() > maxPing) {
                return;
            }

            int currentTime = (int) System.currentTimeMillis(); // Saves the current time to calculate the time it took to retotem

            totemUsage.put(player, currentTime);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClick(InventoryClickEvent event) { // Inventory click event to check for the totem
        if (event.getWhoClicked() instanceof Player) { // Checks if the cause is a player
            Player player = (Player) event.getWhoClicked();
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

            int normalTime = plugin.getConfig().getInt("normal_check_time_ms");
            boolean advancedCheck = plugin.getConfig().getBoolean("advanced_system_check");

            if (timeDifference > normalTime) {
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

            if (advancedCheck) {
                int triggerAmount = plugin.getConfig().getInt("trigger_amount_ms");
                if (realTotem <= triggerAmount) {
                    flag(player, flag_01, flag_02, flag_03, timeDifference, realTotem);
                } else {
                    return;
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
        boolean advancedCheck = plugin.getConfig().getBoolean("advanced_system_check");
        boolean checkToggle = plugin.getConfig().getBoolean("toggle_extra_flags");
        String prefix = plugin.getConfig().getString("prefix");
        int punishAfter = plugin.getConfig().getInt("punish_after");
        boolean punish = plugin.getConfig().getBoolean("punish");

        String alertMessage;
        String extraFlags = ", &8[&7" + flag_01 + "&7, " + flag_02 + "&7, " + flag_03 + "&8]";
        int flagCount = flagCounts.getOrDefault(player.getUniqueId(), 0) + 1;
        flagCounts.put(player.getUniqueId(), flagCount);
        String flags = "&e[" + flagCount + "/" + punishAfter + "] ";


        if (!punish) {
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
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {

            String alertPerm = plugin.getConfig().getString("alert_permissions");

            if (onlinePlayer.hasPermission(alertPerm) && TotemGuardCommand.getToggle(onlinePlayer)) {
                sendMiniMessage(onlinePlayer, alertMessage);
            }
        }

        String finalExtraFlags = extraFlags;
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", player.getName());
            placeholders.put("ping", String.valueOf(player.getPing()));
            placeholders.put("retotem_time", String.valueOf(timeDifference));
            placeholders.put("moving_status", finalExtraFlags);
            placeholders.put("tps", String.valueOf(Math.round(plugin.getTPS())));
            placeholders.put("flag_count", String.valueOf(flagCounts.get(player.getUniqueId())));
            placeholders.put("punish_after", String.valueOf(punishAfter));
            placeholders.put("brand", player.getClientBrandName());


            DiscordWebhook.sendWebhook(placeholders);
        });

        if (!punish) {
            return;
        }
        String punishCommand = plugin.getConfig().getString("punish_command").replace("%player%", player.getName());

        if (flagCounts.get(player.getUniqueId()) >= punishAfter) {

            Bukkit.getScheduler().runTask(plugin, () -> {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), punishCommand);
            });
            flagCounts.remove(player.getUniqueId());
        }

    }

    public void resetAllFlagCounts() {
        flagCounts.clear();
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {

            String alertPerm = plugin.getConfig().getString("alert_permissions");
            String prefix = plugin.getConfig().getString("prefix");

            if (onlinePlayer.hasPermission(alertPerm)) {
                sendMiniMessage(onlinePlayer, prefix + "&fAll flag counts have been reset.");
            }
        }
    }
}
