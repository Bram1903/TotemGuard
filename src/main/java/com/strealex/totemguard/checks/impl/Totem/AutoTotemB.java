package com.strealex.totemguard.checks.impl.Totem;

import com.strealex.totemguard.TotemGuard;
import com.strealex.totemguard.checks.Check;
import com.strealex.totemguard.config.Settings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class AutoTotemB extends Check implements Listener {

    private final TotemGuard plugin;

    public AutoTotemB(TotemGuard plugin) {
        super(plugin, "AutoTotemB", "Player is moving while clicking his totem slot!", true);

        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClick(InventoryClickEvent event) {
        final Settings.Checks.AutoTotemB settings = plugin.getConfigManager().getSettings().getChecks().getAutoTotemB();
        if (!settings.isEnabled()) {
            return;
        }

        if (event.getWhoClicked() instanceof Player player) {
            boolean isSpring = player.isSprinting();
            boolean isSneaking = player.isSneaking();
            boolean isBlocking = player.isBlocking();

            if (!isSpring && !isSneaking && !isBlocking) {
                return;
            }

            if (event.getRawSlot() != 45) {
                return;
            }

            Component checkDetails = createDetails(isSpring, isSneaking, isBlocking);
            flag(player, checkDetails, settings);
        }
    }

    private Component createDetails(boolean isSpring, boolean isSneaking, boolean isBlocking) {
        return Component.text()
                .append(Component.text("Sprinting: ", NamedTextColor.GRAY))
                .append(Component.text(isSpring, isSpring ? NamedTextColor.GREEN : NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.text("Sneaking: ", NamedTextColor.GRAY))
                .append(Component.text(isSneaking, isSneaking ? NamedTextColor.GREEN : NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.text("Blocking: ", NamedTextColor.GRAY))
                .append(Component.text(isBlocking, isBlocking ? NamedTextColor.GREEN : NamedTextColor.GOLD))
                .build();
    }
}
