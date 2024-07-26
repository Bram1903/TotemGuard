package de.outdev.totemguardv2.listeners;

import de.outdev.totemguardv2.TotemGuardV2;
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
    private final HashMap<Player, Integer> totemUsage;

    public TotemUseListener(TotemGuardV2 plugin) {
        this.plugin = plugin;
        this.totemUsage = new HashMap<>();
        Bukkit.getPluginManager().registerEvents(this, plugin); // registering the events
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTotemUse(EntityResurrectEvent event) {
        boolean toggleCheck = plugin.getConfig().getBoolean("toggle_automatic_normal_checks");

        if (toggleCheck == false){
            return;
        }

        double minTps = plugin.getConfig().getDouble("min_tps");
        int maxPing = plugin.getConfig().getInt("max_ping");

        if (plugin.getTPS() < minTps){
            return;
        }

        if (event.getEntity() instanceof Player) { // Checks if the entity is a player
            Player player = (Player) event.getEntity();
            if (player.getPing() > maxPing){
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

            if (timeDifference < normalTime) { // Time set in the config later for what to check etc
                String flag_01 = "&cS";
                String flag_02 = "&cB";
                String flag_03 = "&cM";
                if (player.isSneaking()) {
                    flag_01 = "&aS";
                }
                if (player.isBlocking()) {
                    flag_02 = "&aB";
                }
                if (player.isSprinting() || player.isClimbing() || player.isJumping()) { // Set in the config later
                    flag_03 = "&aM";
                }
                int realTotem = timeDifference - player.getPing();
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

        String alertMessage;

        String extraFlags = " &8[&7" + flag_01 + "&7, " + flag_02 +"&7, " + flag_03 +"&8]";

        if (checkToggle == false) {
            extraFlags = "";
        }
        if (advancedCheck == true){
            alertMessage = prefix + "&e" + player.getName() + " Flagged for AutoTotem &7(Ping: " + player.getPing() + ", In: " + timeDifference + "&8[" + realTotem + "]&7ms, "+ player.getClientBrandName()+ ", " +extraFlags +"&7)";
        }else{
            alertMessage = prefix + "&e" + player.getName() + " Flagged for AutoTotem &7(Ping: " + player.getPing() + ", In: " + timeDifference + "ms, "+ player.getClientBrandName()+ ", " +extraFlags +"&7)";
        }
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {

            String alertPerm = plugin.getConfig().getString("alert_permissions");

            if (onlinePlayer.hasPermission(alertPerm)) {
                sendMiniMessage(onlinePlayer, alertMessage);
            }
        }

    }
}
