package net.strealex.totemguard.checks.impl.totem;

import net.strealex.totemguard.TotemGuard;
import net.strealex.totemguard.checks.Check;
import net.strealex.totemguard.config.Settings;
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

    private final ConcurrentHashMap<Player, Integer> totemUsage;
    private final ConcurrentHashMap<Player, Integer> clickTimes;

    public AutoTotemA(TotemGuard plugin) {
        super(plugin, "AutoTotemA", "Player is replacing their totem too fast!");

        this.plugin = plugin;

        this.totemUsage = new ConcurrentHashMap<>();
        this.clickTimes = new ConcurrentHashMap<>();

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTotemUse(EntityResurrectEvent event) {
        final Settings settings = plugin.getConfigManager().getSettings();

        if (!settings.getChecks().getAutoTotemA().isEnabled()) {
            return;
        }

        if (plugin.getTps() < settings.getDetermine().getMinTps()) {
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
        final Settings settings = plugin.getConfigManager().getSettings();
        if (!settings.getChecks().getAutoTotemA().isEnabled()) {
            return;
        }

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

    @Override
    public void resetData() {
        totemUsage.clear();
        clickTimes.clear();
    }

    private void checkSuspiciousActivity(Player player, int clickTime) {
        Integer usageTime = totemUsage.get(player);

        if (usageTime != null) {
            int currentTime = (int) System.currentTimeMillis();
            int timeDifference = currentTime - usageTime;
            int clickTimeDifference = currentTime - clickTime;

            totemUsage.remove(player);
            clickTimes.remove(player);

            final Settings.Checks.AutoTotemA settings = plugin.getConfigManager().getSettings().getChecks().getAutoTotemA();

            if (timeDifference > settings.getNormalCheckTimeMs()) {
                return;
            }

            int realTotemTime = timeDifference - player.getPing();

            boolean isSprinting = player.isSprinting();
            boolean isSneaking = player.isSneaking();
            boolean isBlocking = player.isBlocking();

            Component checkDetails = createDetails(timeDifference, realTotemTime, clickTimeDifference, player, isSprinting, isSneaking, isBlocking);

            if (settings.isAdvancedSystemCheck()) {
                if (realTotemTime <= settings.getTriggerAmountMs()) {
                    flag(player, checkDetails, settings);
                }
            } else {
                if (!(settings.isClickTimeDifference())) {
                    flag(player, checkDetails, settings);
                } else {
                    if (clickTimeDifference <= settings.getClickTimeDifferenceValue()) {
                        flag(player, checkDetails, settings);
                    }
                }
            }
        }
    }

    private String getMainHandItemString(Player player) {
        ItemStack itemInMainHand = player.getInventory().getItemInMainHand();
        if (itemInMainHand.getType() != Material.AIR) {
            return itemInMainHand.toString();
        } else {
            return "Empty Hand";
        }
    }

    private Component createDetails(int timeDifference, int realTotemTime, int clickTimeDifference, Player player, boolean isSprinting, boolean isSneaking, boolean isBlocking) {
        return Component.text()
                .append(Component.text("Totem Time: ", NamedTextColor.GRAY))
                .append(Component.text(timeDifference + "ms", NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.text("Real Totem Time: ", NamedTextColor.GRAY))
                .append(Component.text(realTotemTime + "ms", NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.text("Click Difference: ", NamedTextColor.GRAY))
                .append(Component.text(clickTimeDifference + "ms", NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.text("Main Hand: ", NamedTextColor.GRAY))
                .append(Component.text(getMainHandItemString(player), NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("Sprinting: ", NamedTextColor.GRAY))
                .append(Component.text(isSprinting, isSprinting ? NamedTextColor.GREEN : NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.text("Sneaking: ", NamedTextColor.GRAY))
                .append(Component.text(isSneaking, isSneaking ? NamedTextColor.GREEN : NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.text("Blocking: ", NamedTextColor.GRAY))
                .append(Component.text(isBlocking, isBlocking ? NamedTextColor.GREEN : NamedTextColor.GOLD))
                .build();
    }
}
