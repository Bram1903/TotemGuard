package de.outdev.totemguard.checks.impl;

import de.outdev.totemguard.TotemGuard;
import de.outdev.totemguard.checks.Check;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class AutoTotemB extends Check implements Listener {

    public AutoTotemB(TotemGuard plugin) {
        super(plugin, "AutoTotemB", "Player is moving while clicking his totem slot!", plugin.getConfigManager().getSettings().getPunish().getPunishAfter(), true);

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            boolean isSpring = player.isSprinting();
            boolean isSneaking = player.isSneaking();
            boolean isBlocking = player.isBlocking();

            if (!isSpring && !isSneaking && !isBlocking) {
                return;
            }

            Component details = Component.text()
                    .append(Component.text("Sprinting: ", NamedTextColor.GRAY))
                    .append(Component.text(isSpring, isSpring ? NamedTextColor.GREEN : NamedTextColor.RED))
                    .append(Component.newline())
                    .append(Component.text("Sneaking: ", NamedTextColor.GRAY))
                    .append(Component.text(isSneaking, isSneaking ? NamedTextColor.GREEN : NamedTextColor.RED))
                    .append(Component.newline())
                    .append(Component.text("Blocking: ", NamedTextColor.GRAY))
                    .append(Component.text(isBlocking, isBlocking ? NamedTextColor.GREEN : NamedTextColor.RED))
                    .build();

            if (event.getRawSlot() == 45) {
                flag(player, details);
            }
        }
    }
}
