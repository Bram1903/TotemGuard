/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2024 Bram and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.deathmotion.totemguard.manager;

import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.config.Settings;
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

        long resetInterval = plugin.getConfigManager().getSettings().getResetViolationsInterval() * 60L * 20L;
        FoliaScheduler.getAsyncScheduler().runAtFixedRate(plugin, (o) -> violationsCleared(), resetInterval, resetInterval);
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

    private void violationsCleared() {
        final Settings settings = plugin.getConfigManager().getSettings();
        Component resetComponent = Component.text()
                .append(LegacyComponentSerializer.legacyAmpersand().deserialize(settings.getPrefix()))
                .append(Component.text("All flag counts have been reset.", NamedTextColor.GREEN))
                .build();

        sendAlert(resetComponent);
    }
}
