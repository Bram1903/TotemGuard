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

package com.deathmotion.totemguard.util.messages;

import com.deathmotion.totemguard.models.TotemPlayer;
import com.deathmotion.totemguard.models.checks.CheckDetails;
import com.deathmotion.totemguard.util.PlaceholderUtil;
import com.deathmotion.totemguard.util.datastructure.Pair;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Map;

public class AlertCreator {
    public Component createAlertComponent(TotemPlayer player, CheckDetails checkDetails, Component details, String prefix, String alertFormat, Pair<TextColor, TextColor> colorScheme) {
        Component hoverInfo = Component.text()
                .append(Component.text("TPS: ", colorScheme.getY()))
                .append(Component.text(checkDetails.getTps(), colorScheme.getX()))
                .append(Component.text(" |", NamedTextColor.DARK_GRAY))
                .append(Component.text(" Client Version: ", colorScheme.getY()))
                .append(Component.text(player.clientVersion().getReleaseName(), colorScheme.getX()))
                .append(Component.text(" |", NamedTextColor.DARK_GRAY))
                .append(Component.text(" Client Brand: ", colorScheme.getY()))
                .append(Component.text(player.clientBrand(), colorScheme.getX()))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("Player: ", colorScheme.getY()))
                .append(Component.text(player.username(), colorScheme.getX()))
                .append(Component.newline())
                .append(Component.text("Ping: ", colorScheme.getY()))
                .append(Component.text(checkDetails.getPing() + "ms", colorScheme.getX()))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("Check: ", colorScheme.getY()))
                .append(Component.text(checkDetails.getCheckName(), colorScheme.getX()))
                .append(Component.newline())
                .append(Component.text("Description: ", colorScheme.getY()))
                .append(Component.text(checkDetails.getCheckDescription(), colorScheme.getX()))
                .append(Component.newline())
                .append(Component.text("Server: ", colorScheme.getY()))
                .append(Component.text(checkDetails.getServerName(), colorScheme.getX()))
                .append(Component.newline())
                .append(Component.newline())
                .append(details)
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("Click to ", colorScheme.getY()))
                .append(Component.text("teleport ", colorScheme.getX()))
                .append(Component.text("to " + player.username() + ".", colorScheme.getY()))
                .build();

        String parsedAlert = PlaceholderUtil.replacePlaceholders(alertFormat, Map.ofEntries(
                Map.entry("%prefix%", prefix),
                Map.entry("%uuid%", player.uuid().toString()),
                Map.entry("%player%", player.username()),
                Map.entry("%check%", checkDetails.getCheckName()),
                Map.entry("%description%", checkDetails.getCheckDescription()),
                Map.entry("%ping%", String.valueOf(checkDetails.getPing())),
                Map.entry("%tps%", String.valueOf(checkDetails.getTps())),
                Map.entry("%server%", checkDetails.getServerName()),
                Map.entry("%punishable%", String.valueOf(checkDetails.isPunishable())),
                Map.entry("%violations%", String.valueOf(checkDetails.getViolations())),
                Map.entry("%max_violations%", checkDetails.isPunishable() ? String.valueOf(checkDetails.getMaxViolations()) : "∞")
        ));

        Component message = Component.text()
                .append(LegacyComponentSerializer.legacyAmpersand().deserialize(parsedAlert))
                .build();

        if (checkDetails.isExperimental()) {
            message = message.append(Component.text(" *", NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD));
        }

        message = message.hoverEvent(HoverEvent.showText(hoverInfo));
        message = message.clickEvent(ClickEvent.runCommand("/tp " + player.username()));

        return message;
    }
}