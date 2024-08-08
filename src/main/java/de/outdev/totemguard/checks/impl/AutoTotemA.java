package de.outdev.totemguard.checks.impl;

import de.outdev.totemguard.TotemGuard;
import de.outdev.totemguard.checks.Check;
import de.outdev.totemguard.config.Settings;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ConcurrentHashMap;

public class AutoTotemA extends Check implements Listener {

    private final TotemGuard plugin;
    private final Settings settings;

    private final ConcurrentHashMap<Player, Integer> totemUsage;
    private final ConcurrentHashMap<Player, Integer> clickTimes;

    public AutoTotemA(TotemGuard plugin) {
        super(plugin, "AutoTotemA", "Player is too fast to retotem!", plugin.getConfigManager().getSettings().getPunish().getPunishAfter());

        this.plugin = plugin;
        this.settings = plugin.getConfigManager().getSettings();

        this.totemUsage = new ConcurrentHashMap<>();
        this.clickTimes = new ConcurrentHashMap<>();

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

        if (event.getEntity() instanceof Player player) {
            if (player.getPing() > settings.getDetermine().getMaxPing()) {
                return;
            }

            int currentTime = (int) System.currentTimeMillis();
            ItemStack mainHandItem = player.getInventory().getItemInMainHand();
            if (mainHandItem.getType() == Material.TOTEM_OF_UNDYING) return;
            totemUsage.put(player, currentTime);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            if (event.getRawSlot() == 45) {
                ItemStack cursorItem = event.getCursor(); // Get the item on the cursor that is being placed into the slot
                if (cursorItem != null && cursorItem.getType() == Material.TOTEM_OF_UNDYING) {
                    Integer clickTime = clickTimes.get(player);
                    if (clickTime != null) {
                        checkSuspiciousActivity(player, clickTime);
                    }
                }
            } else {
                if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.TOTEM_OF_UNDYING) {
                    int clickTime = (int) System.currentTimeMillis();
                    clickTimes.put(player, clickTime);
                }
            }
        }
    }

    private void checkSuspiciousActivity(Player player, int clickTimes) {
        Integer usageTime = totemUsage.get(player);


        if (usageTime != null) {
            int currentTime = (int) System.currentTimeMillis();
            int timeDifference = currentTime - usageTime;
            int clickTimeDifference = currentTime - clickTimes;

            totemUsage.remove(player);

            if (timeDifference > settings.getNormalCheckTimeMs()) {
                return;
            }

            int realTotemTime = timeDifference - player.getPing();

            if (settings.isAdvancedSystemCheck()) {
                if (realTotemTime <= settings.getTriggerAmountMs()) {
                    flag(player, timeDifference, realTotemTime, clickTimeDifference);
                }
            } else {
                if (!(settings.isClickTimeDifference())){
                        flag(player, timeDifference, realTotemTime, clickTimeDifference);
                }else{
                    if (clickTimeDifference <= 25) {
                        flag(player, timeDifference, realTotemTime, clickTimeDifference);
                    }
                }
            }
        }
    }
}
