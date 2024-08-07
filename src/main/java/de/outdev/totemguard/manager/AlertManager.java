package de.outdev.totemguard.manager;

import de.outdev.totemguard.TotemGuard;
import de.outdev.totemguard.config.Settings;
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

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class AlertManager implements Listener {

    private final Settings settings;

    @Getter
    private final Set<Player> enabledAlerts = new CopyOnWriteArraySet<>(new HashSet<>());

    private Component alertsEnabled;
    private Component alertsDisabled;

    public AlertManager(TotemGuard plugin) {
        this.settings = plugin.getConfigManager().getSettings();

        initMessages();

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void sentAlert(Component alert) {
        for (Player player : enabledAlerts) {
            player.sendMessage(alert);
        }
    }

    public void toggleAlerts(Player player) {
        if (!enabledAlerts.remove(player)) {
            enabledAlerts.add(player);
            player.sendMessage(alertsEnabled);
        } else {
            enabledAlerts.remove(player);
            player.sendMessage(alertsDisabled);
        }
    }

    public void enableAlerts(Player player) {
        enabledAlerts.add(player);
        player.sendMessage(alertsEnabled);
    }

    public boolean hasAlertsEnabled(Player player) {
        return enabledAlerts.contains(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        enabledAlerts.remove(event.getPlayer());
    }

    private void initMessages() {
        this.alertsEnabled = Component.text()
                .append(LegacyComponentSerializer.legacyAmpersand().deserialize(settings.getPrefix()))
                .append(Component.text("Alerts enabled!", NamedTextColor.GREEN))
                .build();

        this.alertsDisabled = Component.text()
                .append(LegacyComponentSerializer.legacyAmpersand().deserialize(settings.getPrefix()))
                .append(Component.text("Alerts disabled!", NamedTextColor.RED))
                .build();
    }
}
