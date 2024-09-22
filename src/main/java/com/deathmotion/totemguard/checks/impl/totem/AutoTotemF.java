package com.deathmotion.totemguard.checks.impl.totem;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.checks.Check;
import com.deathmotion.totemguard.config.Settings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AutoTotemF extends Check implements Listener {

    private final TotemGuard plugin;
    private final ConcurrentHashMap<UUID, Long> invClick;

    public AutoTotemF(TotemGuard plugin) {
        super(plugin, "AutoTotemF", "Invalid actions with inventory open.");
        this.plugin = plugin;

        this.invClick = new ConcurrentHashMap<>();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.TOTEM_OF_UNDYING) {
            invClick.put(player.getUniqueId(), System.currentTimeMillis());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        plugin.debug("Inventory close event");
        invClick.remove(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (!invClick.containsKey(playerId)) return;

        long storedTime = invClick.get(playerId);
        long timeDifference = Math.abs(System.currentTimeMillis() - storedTime);
        invClick.remove(playerId);

        plugin.debug("Time difference: " + timeDifference + "ms");

        if (timeDifference <= 1000) {
            Action interaction = event.getAction();
            final Settings.Checks.AutoTotemF settings = plugin.getConfigManager().getSettings().getChecks().getAutoTotemF();
            flag(player, createDetails(interaction, timeDifference, player), settings);
        }
    }

    @Override
    public void resetData() {
        super.resetData();
        invClick.clear();
    }

    @Override
    public void resetData(UUID uuid) {
        super.resetData(uuid);
        invClick.clear();
    }

    private String getMainHandItemString(Player player) {
        return player.getInventory().getItemInMainHand().getType() == Material.AIR
                ? "Empty Hand"
                : player.getInventory().getItemInMainHand().getType().toString();
    }

    private Component createDetails(Action interaction, Long timeDifference, Player player) {
        Component component = Component.text()
                .append(Component.text("Type: ", NamedTextColor.GRAY))
                .append(Component.text(interaction.toString(), NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.text("Time Difference: ", NamedTextColor.GRAY))
                .append(Component.text(timeDifference, NamedTextColor.GOLD))
                .append(Component.text("ms", NamedTextColor.GRAY))
                .append(Component.newline())
                .append(Component.text("Main Hand: ", NamedTextColor.GRAY))
                .append(Component.text(getMainHandItemString(player), NamedTextColor.GOLD))
                .append(Component.newline())
                .build();

        StringBuilder states = new StringBuilder();
        if (player.isSprinting()) {
            states.append("Sprinting, ");
        }
        if (player.isSneaking()) {
            states.append("Sneaking, ");
        }
        if (player.isBlocking()) {
            states.append("Blocking, ");
        }

        // If any states are active, add them to the component
        if (!states.isEmpty()) {
            states.setLength(states.length() - 2);
            component = component.append(Component.text("States: ", NamedTextColor.GRAY))
                    .append(Component.text(states.toString(), NamedTextColor.GOLD));
        }

        return component;
    }
}
