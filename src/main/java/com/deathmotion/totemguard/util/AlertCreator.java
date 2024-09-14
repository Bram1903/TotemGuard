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

import com.deathmotion.totemguard.data.CheckDetails;
import com.deathmotion.totemguard.data.TotemPlayer;
import com.deathmotion.totemguard.database.entities.impl.Alert;
import com.deathmotion.totemguard.database.entities.impl.Punishment;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.OfflinePlayer;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AlertCreator {
    public static Component createAlertComponent(TotemPlayer player, CheckDetails checkDetails, Component details, String prefix) {
        Component hoverInfo = Component.text()
                .append(Component.text("TPS: ", NamedTextColor.GRAY))
                .append(Component.text(checkDetails.getTps(), NamedTextColor.GOLD))
                .append(Component.text(" |", NamedTextColor.DARK_GRAY))
                .append(Component.text(" Client Version: ", NamedTextColor.GRAY))
                .append(Component.text(player.getClientVersion().getReleaseName(), NamedTextColor.GOLD))
                .append(Component.text(" |", NamedTextColor.DARK_GRAY))
                .append(Component.text(" Client Brand: ", NamedTextColor.GRAY))
                .append(Component.text(player.getClientBrandName(), NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("Player: ", NamedTextColor.GRAY))
                .append(Component.text(player.getUsername(), NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.text("Gamemode: ", NamedTextColor.GRAY))
                .append(Component.text(checkDetails.getGamemode(), NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.text("Ping: ", NamedTextColor.GRAY))
                .append(Component.text(checkDetails.getPing() + "ms", NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.newline())
                .append(details)
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("Click to ", NamedTextColor.GRAY))
                .append(Component.text("teleport ", NamedTextColor.GOLD))
                .append(Component.text("to " + player.getUsername() + ".", NamedTextColor.GRAY))
                .build();

        Component message = Component.text()
                .append(LegacyComponentSerializer.legacyAmpersand().deserialize(prefix))
                .append(Component.text(player.getUsername(), NamedTextColor.YELLOW))
                .append(Component.text(" failed ", NamedTextColor.GRAY))
                .append(Component.text(checkDetails.getCheckName(), NamedTextColor.GOLD)
                        .hoverEvent(HoverEvent.showText(Component.text(checkDetails.getCheckDescription(), NamedTextColor.GRAY))))
                .clickEvent(ClickEvent.runCommand("/tp " + player.getUsername()))
                .build();

        Component totalViolationsComponent;

        if (checkDetails.isPunishable()) {
            totalViolationsComponent = Component.text()
                    .append(Component.text(" VL[", NamedTextColor.GRAY))
                    .append(Component.text(checkDetails.getViolations() + "/" + checkDetails.getMaxViolations(), NamedTextColor.GOLD))
                    .append(Component.text("]", NamedTextColor.GRAY))
                    .build();
        } else {
            totalViolationsComponent = Component.text()
                    .append(Component.text(" VL[", NamedTextColor.GRAY))
                    .append(Component.text(checkDetails.getViolations(), NamedTextColor.GOLD))
                    .append(Component.text("]", NamedTextColor.GRAY))
                    .build();

        }
        message = message.append(totalViolationsComponent);

        message = message
                .append(Component.text(" [Info]", NamedTextColor.DARK_GRAY)
                        .hoverEvent(HoverEvent.showText(hoverInfo)))
                .decoration(TextDecoration.ITALIC, false);

        if (checkDetails.isExperimental()) {
            message = message.append(Component.text(" *", NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD));
        }

        return message;
    }

    public static Component createLogsComponent(OfflinePlayer player, List<Alert> alerts, List<Punishment> punishments, long loadTime) {
        // Group alerts by check name and count them
        Map<String, Long> checkCounts = alerts.stream()
                .collect(Collectors.groupingBy(Alert::getCheckName, Collectors.counting()));

        // Sort the map entries by count in descending order
        List<Map.Entry<String, Long>> sortedCheckCounts = checkCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .toList();

        // Start building the component using a builder
        TextComponent.Builder componentBuilder = Component.text()
                .append(Component.text("TotemGuard Logs ", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.text("Player: ", NamedTextColor.GRAY, TextDecoration.BOLD))
                .append(Component.text(player.getName() != null ? player.getName() : "Unknown", NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.text("Total Logs: ", NamedTextColor.GRAY, TextDecoration.BOLD))
                .append(Component.text(alerts.size(), NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.text("Total Punishments: ", NamedTextColor.GRAY, TextDecoration.BOLD))
                .append(Component.text(punishments.size(), NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.text("Load Time: ", NamedTextColor.GRAY, TextDecoration.BOLD))
                .append(Component.text(loadTime + "ms", NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("> Alert Summary <", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.newline());

        if (sortedCheckCounts.isEmpty()) {
            componentBuilder.append(Component.text(" No logs found.", NamedTextColor.GRAY, TextDecoration.ITALIC));
        } else {
            sortedCheckCounts.forEach(entry -> {
                String checkName = entry.getKey();
                Long count = entry.getValue();
                componentBuilder.append(
                        Component.text("- ", NamedTextColor.DARK_GRAY)
                ).append(
                        Component.text(checkName + ": ", NamedTextColor.GRAY, TextDecoration.BOLD)
                ).append(
                        Component.text(count + "x", NamedTextColor.GOLD)
                ).append(
                        Component.newline()
                );
            });
        }

        componentBuilder.append(Component.newline())
                .append(Component.text("> Punishments <", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.newline());

        if (punishments.isEmpty()) {
            componentBuilder.append(Component.text(" No punishments found.", NamedTextColor.GRAY, TextDecoration.ITALIC));
        } else {
            punishments.forEach(punishment -> {
                componentBuilder.append(
                        Component.text("- ", NamedTextColor.DARK_GRAY)
                ).append(
                        Component.text(punishment.getCheckName() + ": ", NamedTextColor.GRAY, TextDecoration.BOLD)
                ).append(
                        Component.text("Punished", NamedTextColor.RED)
                ).append(
                        Component.newline()
                );
            });
        }

        return componentBuilder.build();
    }
}
