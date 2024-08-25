package net.strealex.totemguard.checks.impl.totem;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.strealex.totemguard.TotemGuard;
import net.strealex.totemguard.checks.Check;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AutoTotemA extends Check implements Listener {

    private final TotemGuard plugin;
    private final ConcurrentHashMap<UUID, Long> totemUsage;
    private final ConcurrentHashMap<UUID, Long> clickTimes;

    public AutoTotemA(TotemGuard plugin) {
        super(plugin, "AutoTotemA", "Player is replacing their totem too fast!");

        this.plugin = plugin;
        this.totemUsage = new ConcurrentHashMap<>();
        this.clickTimes = new ConcurrentHashMap<>();

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTotemUse(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        ItemStack mainHandItem = player.getInventory().getItemInMainHand();
        if (mainHandItem.getType() != Material.TOTEM_OF_UNDYING) {
            totemUsage.put(player.getUniqueId(), System.nanoTime());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (event.getRawSlot() == 45 && event.getCursor().getType() == Material.TOTEM_OF_UNDYING) {
            clickTimes.computeIfPresent(player.getUniqueId(), (uuid, clickTime) -> {
                checkSuspiciousActivity(player, clickTime);
                return clickTime;
            });

            return;
        }

        if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.TOTEM_OF_UNDYING) {
            clickTimes.put(player.getUniqueId(), System.nanoTime());
        }
    }

    @Override
    public void resetData() {
        totemUsage.clear();
        clickTimes.clear();
    }

    private void checkSuspiciousActivity(Player player, long clickTime) {
        Long usageTime = totemUsage.remove(player.getUniqueId());
        if (usageTime == null) {
            return;
        }

        long currentTime = System.nanoTime();
        long timeDifference = (currentTime - usageTime) / 1_000_000; // Convert to milliseconds
        long clickTimeDifference = (currentTime - clickTime) / 1_000_000; // Convert to milliseconds
        long realTotemTime = timeDifference - player.getPing();

        var checkSettings = plugin.getConfigManager().getSettings().getChecks().getAutoTotemA();

        if (clickTimeDifference <= checkSettings.getClickTimeDifference() && timeDifference <= checkSettings.getNormalCheckTimeMs()) {
            flag(player, createDetails(timeDifference, realTotemTime, clickTimeDifference, player), checkSettings);
        }
    }

    private String getMainHandItemString(Player player) {
        return player.getInventory().getItemInMainHand().getType() == Material.AIR
                ? "Empty Hand"
                : player.getInventory().getItemInMainHand().getType().toString();
    }

    private Component createDetails(long timeDifference, long realTotemTime, long clickTimeDifference, Player player) {
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
                .append(Component.text(player.isSprinting(), player.isSprinting() ? NamedTextColor.GREEN : NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.text("Sneaking: ", NamedTextColor.GRAY))
                .append(Component.text(player.isSneaking(), player.isSneaking() ? NamedTextColor.GREEN : NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.text("Blocking: ", NamedTextColor.GRAY))
                .append(Component.text(player.isBlocking(), player.isBlocking() ? NamedTextColor.GREEN : NamedTextColor.GOLD))
                .build();
    }
}