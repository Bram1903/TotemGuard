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

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class StatsCreator {
    public static Component createStatsComponent(int punishmentCount, int alertCount, long punishmentsLast30Days, long punishmentsLast7Days, long punishmentsLastDay, long alertsLast30Days, long alertsLast7Days, long alertsLastDay) {
        TextComponent.Builder componentBuilder = Component.text()
                .append(Component.text("TotemGuard Stats", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.text("Overall Statistics", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.text("Total Punishments: ", NamedTextColor.GRAY, TextDecoration.BOLD))
                .append(Component.text(punishmentCount, NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.text("Total Alerts: ", NamedTextColor.GRAY, TextDecoration.BOLD))
                .append(Component.text(alertCount, NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.newline());

        // Section for Punishments
        componentBuilder.append(Component.text("> Punishments <", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.newline());

        if (punishmentsLast30Days == 0 && punishmentsLast7Days == 0 && punishmentsLastDay == 0) {
            componentBuilder.append(Component.text(" No punishments found.", NamedTextColor.GRAY, TextDecoration.ITALIC))
                    .append(Component.newline());
        } else {
            componentBuilder.append(Component.text("Last 30 days: ", NamedTextColor.GRAY, TextDecoration.BOLD))
                    .append(Component.text(punishmentsLast30Days, NamedTextColor.GOLD))
                    .append(Component.newline())
                    .append(Component.text("Last 7 days: ", NamedTextColor.GRAY, TextDecoration.BOLD))
                    .append(Component.text(punishmentsLast7Days, NamedTextColor.GOLD))
                    .append(Component.newline())
                    .append(Component.text("Last day: ", NamedTextColor.GRAY, TextDecoration.BOLD))
                    .append(Component.text(punishmentsLastDay, NamedTextColor.GOLD))
                    .append(Component.newline());
        }

        componentBuilder.append(Component.newline());

        // Section for Alerts
        componentBuilder.append(Component.text("> Alerts <", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.newline());

        if (alertsLast30Days == 0 && alertsLast7Days == 0 && alertsLastDay == 0) {
            componentBuilder.append(Component.text(" No alerts found.", NamedTextColor.GRAY, TextDecoration.ITALIC))
                    .append(Component.newline());
        } else {
            componentBuilder.append(Component.text("Last 30 days: ", NamedTextColor.GRAY, TextDecoration.BOLD))
                    .append(Component.text(alertsLast30Days, NamedTextColor.GOLD))
                    .append(Component.newline())
                    .append(Component.text("Last 7 days: ", NamedTextColor.GRAY, TextDecoration.BOLD))
                    .append(Component.text(alertsLast7Days, NamedTextColor.GOLD))
                    .append(Component.newline())
                    .append(Component.text("Last day: ", NamedTextColor.GRAY, TextDecoration.BOLD))
                    .append(Component.text(alertsLastDay, NamedTextColor.GOLD))
                    .append(Component.newline());
        }

        return componentBuilder.build();
    }

}
