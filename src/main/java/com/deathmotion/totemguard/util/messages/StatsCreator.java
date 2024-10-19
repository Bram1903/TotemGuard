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

import com.deathmotion.totemguard.util.datastructure.Pair;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class StatsCreator {
    public Component createStatsComponent(int punishmentCount, int alertCount, long punishmentsLast30Days, long punishmentsLast7Days, long punishmentsLastDay, long alertsLast30Days, long alertsLast7Days, long alertsLastDay, Pair<TextColor, TextColor> colorScheme) {
        TextComponent.Builder componentBuilder = Component.text()
                .append(Component.text("TotemGuard Stats", colorScheme.getX(), TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.text("Total Punishments: ", colorScheme.getY(), TextDecoration.BOLD))
                .append(Component.text(punishmentCount, colorScheme.getX()))
                .append(Component.newline())
                .append(Component.text("Total Alerts: ", colorScheme.getY(), TextDecoration.BOLD))
                .append(Component.text(alertCount, colorScheme.getX()))
                .append(Component.newline())
                .append(Component.newline());

        // Section for Punishments
        componentBuilder.append(Component.text("> Punishments <", colorScheme.getX(), TextDecoration.BOLD))
                .append(Component.newline());

        if (punishmentsLast30Days == 0 && punishmentsLast7Days == 0 && punishmentsLastDay == 0) {
            componentBuilder.append(Component.text(" No punishments found.", colorScheme.getY(), TextDecoration.ITALIC))
                    .append(Component.newline());
        } else {
            componentBuilder.append(Component.text("Last 30 days: ", colorScheme.getY(), TextDecoration.BOLD))
                    .append(Component.text(punishmentsLast30Days, colorScheme.getX()))
                    .append(Component.newline())
                    .append(Component.text("Last 7 days: ", colorScheme.getY(), TextDecoration.BOLD))
                    .append(Component.text(punishmentsLast7Days, colorScheme.getX()))
                    .append(Component.newline())
                    .append(Component.text("Last 24 hours: ", colorScheme.getY(), TextDecoration.BOLD))
                    .append(Component.text(punishmentsLastDay, colorScheme.getX()))
                    .append(Component.newline());
        }

        componentBuilder.append(Component.newline());

        // Section for Alerts
        componentBuilder.append(Component.text("> Alerts <", colorScheme.getX(), TextDecoration.BOLD))
                .append(Component.newline());

        if (alertsLast30Days == 0 && alertsLast7Days == 0 && alertsLastDay == 0) {
            componentBuilder.append(Component.text(" No alerts found.", colorScheme.getY(), TextDecoration.ITALIC))
                    .append(Component.newline());
        } else {
            componentBuilder.append(Component.text("Last 30 days: ", colorScheme.getY(), TextDecoration.BOLD))
                    .append(Component.text(alertsLast30Days, colorScheme.getX()))
                    .append(Component.newline())
                    .append(Component.text("Last 7 days: ", colorScheme.getY(), TextDecoration.BOLD))
                    .append(Component.text(alertsLast7Days, colorScheme.getX()))
                    .append(Component.newline())
                    .append(Component.text("Last 24 hours: ", colorScheme.getY(), TextDecoration.BOLD))
                    .append(Component.text(alertsLastDay, colorScheme.getX()))
                    .append(Component.newline());
        }

        return componentBuilder.build();
    }

}
