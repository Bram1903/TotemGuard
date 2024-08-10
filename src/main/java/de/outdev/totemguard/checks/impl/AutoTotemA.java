package de.outdev.totemguard.checks.impl;

import de.outdev.totemguard.TotemGuard;
import de.outdev.totemguard.checks.Check;
import de.outdev.totemguard.config.Settings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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

    private void checkSuspiciousActivity(Player player, int clickTime) {
        Integer usageTime = totemUsage.get(player);

        if (usageTime != null) {
            int currentTime = (int) System.currentTimeMillis();
            int timeDifference = currentTime - usageTime;
            int clickTimeDifference = currentTime - clickTime;

            totemUsage.remove(player);
            clickTimes.remove(player);

            if (timeDifference > settings.getNormalCheckTimeMs()) {
                return;
            }

            int realTotemTime = timeDifference - player.getPing();

            Component checkDetails = Component.text()
                    .append(Component.text("TotemTime: ", NamedTextColor.GRAY))
                    .append(Component.text(timeDifference + "ms", NamedTextColor.GOLD))
                    .append(Component.newline())
                    .append(Component.text("RealTotemTime: ", NamedTextColor.GRAY))
                    .append(Component.text(realTotemTime + "ms", NamedTextColor.GOLD))
                    .append(Component.newline())
                    .append(Component.text("ClickTimeDifference: ", NamedTextColor.GRAY))
                    .append(Component.text(clickTimeDifference + "ms", NamedTextColor.GOLD))
                    .append(Component.newline())
                    .append(Component.newline())
                    .append(Component.text("Sneaking: ", NamedTextColor.GRAY))
                    .append(Component.text(player.isSneaking(), NamedTextColor.GOLD))
                    .append(Component.newline())
                    .append(Component.text("Blocking: ", NamedTextColor.GRAY))
                    .append(Component.text(player.isBlocking(), NamedTextColor.GOLD))
                    .append(Component.newline())
                    .append(Component.text("Sprinting: ", NamedTextColor.GRAY))
                    .append(Component.text(player.isSprinting(), NamedTextColor.GOLD))
                    .build();

            if (settings.isAdvancedSystemCheck()) {
                if (realTotemTime <= settings.getTriggerAmountMs()) {
                    flag(player, checkDetails);
                }
            } else {
                if (!(settings.isClickTimeDifference())) {
                    flag(player, checkDetails);
                } else {
                    if (clickTimeDifference <= 25) {
                        flag(player, checkDetails);
                    }
                }
            }
        }
    }
}
