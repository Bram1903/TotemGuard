package net.strealex.totemguard.manager;

import net.strealex.totemguard.TotemGuard;
import net.strealex.totemguard.config.Settings;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;

public class AlertManager {

    @Getter
    private final Set<Player> enabledAlerts = new CopyOnWriteArraySet<>();

    private final TotemGuard plugin;

    public AlertManager(TotemGuard plugin) {
        this.plugin = plugin;
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

    private void sendAlertStatusMessage(Player player, String message, NamedTextColor color) {
        final Settings settings = plugin.getConfigManager().getSettings();

        player.sendMessage(Component.text()
                .append(LegacyComponentSerializer.legacyAmpersand().deserialize(settings.getPrefix()))
                .append(Component.text(message, color))
                .build());
    }
}
