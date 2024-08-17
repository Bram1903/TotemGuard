package com.strealex.totemguard.manager;

import com.strealex.totemguard.TotemGuard;
import com.strealex.totemguard.config.Settings;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;

public class AlertManager implements Listener {

    @Getter
    private final Set<Player> enabledAlerts = new CopyOnWriteArraySet<>();

    private final TotemGuard plugin;

    public AlertManager(TotemGuard plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void sendAlert(Component alert) {
        enabledAlerts.forEach(player -> player.sendMessage(alert));
    }

    public void toggleAlerts(Player player) {
        if (enabledAlerts.add(player)) {
            sendAlertStatusMessage(player, "Alerts enabled!", NamedTextColor.GREEN);
        } else {
            enabledAlerts.remove(player);
            sendAlertStatusMessage(player, "Alerts disabled!", NamedTextColor.RED);
        }
    }

    public void enableAlerts(Player player) {
        if (enabledAlerts.add(player)) {
            sendAlertStatusMessage(player, "Alerts enabled!", NamedTextColor.GREEN);
        }
    }

    public void removePlayer(UUID player) {
        enabledAlerts.removeIf(p -> p.getUniqueId().equals(player));
    }

    public boolean hasAlertsEnabled(Player player) {
        return enabledAlerts.contains(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        enabledAlerts.remove(event.getPlayer());
    }

    private void sendAlertStatusMessage(Player player, String message, NamedTextColor color) {
        final Settings settings = plugin.getConfigManager().getSettings();

        player.sendMessage(Component.text()
                .append(LegacyComponentSerializer.legacyAmpersand().deserialize(settings.getPrefix()))
                .append(Component.text(message, color))
                .build());
    }
}
