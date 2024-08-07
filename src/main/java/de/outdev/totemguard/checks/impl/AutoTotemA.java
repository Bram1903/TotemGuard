package de.outdev.totemguard.checks.impl;

import de.outdev.totemguard.TotemGuard;
import de.outdev.totemguard.checks.Check;
import de.outdev.totemguard.config.Settings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.concurrent.ConcurrentHashMap;

public class AutoTotemA extends Check implements Listener {

    private final TotemGuard plugin;
    private final Settings settings;

    private final ConcurrentHashMap<Player, Integer> totemUsage;

    public AutoTotemA(TotemGuard plugin) {
        super(plugin, "AutoTotemA", "Player is too fast to retotem!", plugin.getConfigManager().getSettings().getPunish().getPunishAfter());

        this.plugin = plugin;
        this.settings = plugin.getConfigManager().getSettings();

        this.totemUsage = new ConcurrentHashMap<>();

        Bukkit.getPluginManager().registerEvents(this, plugin);
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
            int timeDifference = currentTime - usageTime;

            totemUsage.remove(player);

            if (timeDifference > settings.getNormalCheckTimeMs()) {
                return;
            }

            int realTotem = timeDifference - player.getPing();

            Component details = Component.text()
                    .append(Component.text("Time Difference: ", NamedTextColor.GOLD))
                    .append(Component.text(timeDifference, NamedTextColor.GREEN))
                    .append(Component.newline())
                    .append(Component.text("Real Totem: ", NamedTextColor.GOLD))
                    .append(Component.text(realTotem, NamedTextColor.GREEN))
                    .build();

            if (settings.isAdvancedSystemCheck()) {
                if (realTotem <= settings.getTriggerAmountMs()) {
                    flag(player, details);
                }
            } else {
                flag(player, details);
            }
        }
    }
}
