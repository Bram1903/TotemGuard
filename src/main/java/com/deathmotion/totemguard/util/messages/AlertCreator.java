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

import com.deathmotion.totemguard.models.CheckDetails;
import com.deathmotion.totemguard.models.TotemPlayer;
import com.deathmotion.totemguard.util.PlaceholderUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Map;

public class AlertCreator {
    public static Component createAlertComponent(TotemPlayer player, CheckDetails checkDetails, Component details, String prefix, String alertFormat) {
        Component hoverInfo = Component.text()
                .append(Component.text("TPS: ", NamedTextColor.GRAY))
                .append(Component.text(checkDetails.getTps(), NamedTextColor.GOLD))
                .append(Component.text(" |", NamedTextColor.DARK_GRAY))
                .append(Component.text(" Client Version: ", NamedTextColor.GRAY))
                .append(Component.text(player.clientVersion().getReleaseName(), NamedTextColor.GOLD))
                .append(Component.text(" |", NamedTextColor.DARK_GRAY))
                .append(Component.text(" Client Brand: ", NamedTextColor.GRAY))
                .append(Component.text(player.clientBrand(), NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("Player: ", NamedTextColor.GRAY))
                .append(Component.text(player.username(), NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.text("Ping: ", NamedTextColor.GRAY))
                .append(Component.text(checkDetails.getPing() + "ms", NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("Check: ", NamedTextColor.GRAY))
                .append(Component.text(checkDetails.getCheckName(), NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.text("Description: ", NamedTextColor.GRAY))
                .append(Component.text(checkDetails.getCheckDescription(), NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.newline())
                .append(details)
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("Click to ", NamedTextColor.GRAY))
                .append(Component.text("teleport ", NamedTextColor.GOLD))
                .append(Component.text("to " + player.username() + ".", NamedTextColor.GRAY))
                .build();

        String parsedAlert = PlaceholderUtil.replacePlaceholders(alertFormat, Map.of(
                "%prefix%", prefix,
                "%uuid%", player.uuid().toString(),
                "%player%", player.username(),
                "%check%", checkDetails.getCheckName(),
                "%description%", checkDetails.getCheckDescription(),
                "%ping%", String.valueOf(checkDetails.getPing()),
                "%tps%", String.valueOf(checkDetails.getTps()),
                "%punishable%", String.valueOf(checkDetails.isPunishable()),
                "%violations%", String.valueOf(checkDetails.getViolations()),
                "%max_violations%", checkDetails.isPunishable() ? String.valueOf(checkDetails.getMaxViolations()) : "∞"
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