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

package com.deathmotion.totemguard.util;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.config.ConfigManager;
import com.deathmotion.totemguard.config.Settings;
import com.deathmotion.totemguard.data.Constants;
import com.deathmotion.totemguard.database.entities.impl.Alert;
import com.deathmotion.totemguard.database.entities.impl.Punishment;
import com.deathmotion.totemguard.models.CheckDetails;
import com.deathmotion.totemguard.models.SafetyStatus;
import com.deathmotion.totemguard.models.TotemPlayer;
import com.deathmotion.totemguard.util.datastructure.Pair;
import com.deathmotion.totemguard.util.messages.AlertCreator;
import com.deathmotion.totemguard.util.messages.ProfileCreator;
import com.deathmotion.totemguard.util.messages.StatsCreator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public class MessageService {
    private final TotemGuard plugin;
    private final ConfigManager configManager;
    private final ProfileCreator profileCreator;
    private final AlertCreator alertCreator;
    private final StatsCreator statsCreator;

    public MessageService(TotemGuard plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.profileCreator = new ProfileCreator();
        this.alertCreator = new AlertCreator();
        this.statsCreator = new StatsCreator();
    }

    public Component getPrefix() {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(configManager.getSettings().getPrefix());
    }

    public Pair<TextColor, TextColor> getColorScheme() {
        final Settings.ColorScheme colorScheme = configManager.getSettings().getColorScheme();

        // Replace '&' with '§' to convert Minecraft color codes
        String primaryColorCode = colorScheme.getPrimaryColor().replace('&', '§');
        String secondaryColorCode = colorScheme.getSecondaryColor().replace('&', '§');

        // Deserialize the color codes and extract the TextColor
        TextColor primaryColor = LegacyComponentSerializer.legacySection().deserialize(primaryColorCode).color();
        TextColor secondaryColor = LegacyComponentSerializer.legacySection().deserialize(secondaryColorCode).color();

        return new Pair<>(primaryColor, secondaryColor);
    }

    public Component version() {
        return getPrefix()
                .append(Component.text("Running ", NamedTextColor.GRAY).decorate(TextDecoration.BOLD))
                .append(Component.text("TotemGuard", NamedTextColor.GREEN).decorate(TextDecoration.BOLD))
                .append(Component.text(" v" + plugin.getVersion().toString(), NamedTextColor.GREEN).decorate(TextDecoration.BOLD))
                .append(Component.text(" by ", NamedTextColor.GRAY).decorate(TextDecoration.BOLD))
                .append(Component.text("Bram", NamedTextColor.GREEN).decorate(TextDecoration.BOLD))
                .append(Component.text(" and ", NamedTextColor.GRAY).decorate(TextDecoration.BOLD))
                .append(Component.text("OutDev", NamedTextColor.GREEN).decorate(TextDecoration.BOLD))
                .hoverEvent(HoverEvent.showText(Component.text("Open Github Page!", NamedTextColor.GREEN)
                        .decorate(TextDecoration.BOLD)
                        .decorate(TextDecoration.UNDERLINED)))
                .clickEvent(ClickEvent.openUrl(Constants.GITHUB_URL));
    }

    public Component toggleAlerts(boolean enabled, Player player) {
        String alertToggleFormat;

        if (enabled) {
            alertToggleFormat = configManager.getSettings().getAlertsEnabled();
        } else {
            alertToggleFormat = configManager.getSettings().getAlertsDisabled();
        }

        String parsedMessage = PlaceholderUtil.replacePlaceholders(alertToggleFormat, Map.of(
                "%prefix%", configManager.getSettings().getPrefix(),
                "%uuid%", player.getUniqueId().toString(),
                "%player%", player.getName()
        ));

        return Component.text()
                .append(LegacyComponentSerializer.legacyAmpersand().deserialize(parsedMessage))
                .build();
    }

    public Component playerNotFound() {
        return getPrefix().append(Component.text("Player not found!", NamedTextColor.RED));
    }

    public Component getPluginReloaded() {
        return getPrefix().append(Component.text("The plugin has been reloaded!", NamedTextColor.GREEN));
    }

    public Component getProfileComponent(String username, List<Alert> alerts, List<Punishment> punishments, long loadTime, SafetyStatus safetyStatus) {
        return profileCreator.createProfileComponent(username, alerts, punishments, loadTime, safetyStatus, getColorScheme());
    }

    public Component getAlertComponent(TotemPlayer player, CheckDetails checkDetails, Component details, String prefix, String alertFormat) {
        return alertCreator.createAlertComponent(player, checkDetails, details, prefix, alertFormat, getColorScheme());
    }

    public Component getStatsComponent(int punishmentCount, int alertCount, long punishmentsLast30Days, long punishmentsLast7Days, long punishmentsLastDay, long alertsLast30Days, long alertsLast7Days, long alertsLastDay) {
        return statsCreator.createStatsComponent(punishmentCount, alertCount, punishmentsLast30Days, punishmentsLast7Days, punishmentsLastDay, alertsLast30Days, alertsLast7Days, alertsLastDay, getColorScheme());
    }

    public Component getJoinMessage(String username, String brand) {
        Pair<TextColor, TextColor> colorScheme = getColorScheme();

        return getPrefix()
                .append(Component.text(username, colorScheme.getX()))
                .append(Component.text(" joined using: ", colorScheme.getY()))
                .append(Component.text(brand, colorScheme.getX()));
    }
}
