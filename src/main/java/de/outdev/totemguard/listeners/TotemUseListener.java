package de.outdev.totemguard.listeners;

import de.outdev.totemguard.data.PermissionConstants;
import de.outdev.totemguard.config.Settings;
import de.outdev.totemguard.TotemGuard;
import de.outdev.totemguard.commands.TotemGuardCommand;
import de.outdev.totemguard.discord.DiscordWebhook;
import de.outdev.totemguard.manager.AlertManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TotemUseListener implements Listener {

    private final TotemGuard plugin;
    private final Settings settings;
    private final AlertManager alertManager;

    private final HashMap<Player, Integer> totemUsage;
    private final HashMap<UUID, Integer> flagCounts;

    public TotemUseListener(TotemGuard plugin) {
        this.plugin = plugin;
        this.settings = plugin.getConfigManager().getSettings();
        this.alertManager = plugin.getAlertManager();

        this.totemUsage = new HashMap<>();
        this.flagCounts = new HashMap<>();

        Bukkit.getPluginManager().registerEvents(this, plugin); // registering the events

        // Schedule the reset task
        long resetInterval = settings.getPunish().getRemoveFlagsMin() * 60L * 20L; // Convert minutes to ticks (20 ticks = 1 second)
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::resetAllFlagCounts, resetInterval, resetInterval);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTotemUse(EntityResurrectEvent event) {
        if (!settings.isToggleAutomaticNormalChecks()) {
            return;
        }

        if (plugin.getTPS() < settings.getDetermine().getMinTps()) {
            return;
        }

        if (event.getEntity() instanceof Player player) { // Checks if the entity is a player
            if (player.getPing() > settings.getDetermine().getMaxPing()) {
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
        int punishAfter = settings.getPunish().getPunishAfter();
        boolean punish = settings.getPunish().isEnabled();

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

        Component alert;

        if (advancedCheck) {
            alert = Component.text()
                    .append(LegacyComponentSerializer.legacyAmpersand().deserialize(settings.getPrefix()))
                    .append(Component.text(player.getName(), NamedTextColor.GOLD))
                    .append(Component.text(" Flagged for AutoTotem ", NamedTextColor.YELLOW))
                    .append(Component.text(flags, NamedTextColor.YELLOW))
                    .append(Component.text("(Ping: ", NamedTextColor.GRAY))
                    .append(Component.text(player.getPing(), NamedTextColor.GRAY))
                    .append(Component.text(", In: ", NamedTextColor.GRAY))
                    .append(Component.text(timeDifference, NamedTextColor.GRAY))
                    .append(Component.text(" [", NamedTextColor.GRAY))
                    .append(Component.text(realTotem, NamedTextColor.GRAY))
                    .append(Component.text("]", NamedTextColor.GRAY))
                    .append(Component.text("ms, ", NamedTextColor.GRAY))
                    .append(Component.text(player.getClientBrandName(), NamedTextColor.GRAY))
                    .append(Component.text(extraFlags, NamedTextColor.GRAY))
                    .build();
        } else {
            alert = Component.text()
                    .append(LegacyComponentSerializer.legacyAmpersand().deserialize(settings.getPrefix()))
                    .append(Component.text(player.getName(), NamedTextColor.GOLD))
                    .append(Component.text(" Flagged for AutoTotem ", NamedTextColor.YELLOW))
                    .append(Component.text(flags, NamedTextColor.YELLOW))
                    .append(Component.text("(Ping: ", NamedTextColor.GRAY))
                    .append(Component.text(player.getPing(), NamedTextColor.GRAY))
                    .append(Component.text(", In: ", NamedTextColor.GRAY))
                    .append(Component.text(timeDifference, NamedTextColor.GRAY))
                    .append(Component.text("ms, ", NamedTextColor.GRAY))
                    .append(Component.text(player.getClientBrandName(), NamedTextColor.GRAY))
                    .append(Component.text(extraFlags, NamedTextColor.GRAY))
                    .build();
        }

        this.alertManager.sentAlert(alert);

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

        String punishCommand = settings.getPunish().getPunishCommand().replace("%player%", player.getName());

        if (flagCounts.get(player.getUniqueId()) >= punishAfter) {

            Bukkit.getScheduler().runTask(plugin, () -> {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), punishCommand);
            });
            flagCounts.remove(player.getUniqueId());
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
