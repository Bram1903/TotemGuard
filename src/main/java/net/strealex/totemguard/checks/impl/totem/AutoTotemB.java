package net.strealex.totemguard.checks.impl.totem;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.strealex.totemguard.TotemGuard;
import net.strealex.totemguard.checks.Check;
import net.strealex.totemguard.config.Settings;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * This class implements the AutoTotemB check, which detects if a player is moving
 * (sprinting, sneaking, or blocking) while interacting with their totem slot in the inventory.
 * This is considered suspicious behavior and may trigger a flag.
 * <p>
 * <remarks>
 * This check is really easy to false, so it's mostly recommended to take this flag with a grain of salt.
 * Whenever another check flags, and this check also flags, it's more likely that the player is cheating.
 * </remarks>
 */
public class AutoTotemB extends Check implements Listener {

    private final TotemGuard plugin;

    /**
     * Constructor for AutoTotemB.
     *
     * @param plugin The TotemGuard plugin instance.
     */
    public AutoTotemB(TotemGuard plugin) {
        super(plugin, "AutoTotemB", "Player is moving while clicking his totem slot!", true);

        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Event handler for InventoryClickEvent.
     * This method monitors inventory clicks
     * and checks if the player is interacting with the totem slot (slot 45) while moving.
     *
     * @param event The inventory click event.
     */
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

    /**
     * Creates a detailed message about the player's state (sprinting, sneaking, blocking)
     * when interacting with the totem slot.
     *
     * @param isSpring   Whether the player is sprinting.
     * @param isSneaking Whether the player is sneaking.
     * @param isBlocking Whether the player is blocking.
     * @return A Component containing the formatted details.
     */
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