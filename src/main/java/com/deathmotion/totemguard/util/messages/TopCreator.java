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

import com.deathmotion.totemguard.models.TopViolation;
import com.deathmotion.totemguard.util.datastructure.Pair;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class TopCreator {

    public @NotNull Component createTopComponent(List<TopViolation> topViolations, Pair<TextColor, TextColor> colorScheme) {
        TextComponent.Builder componentBuilder = Component.text()
                .append(Component.text("Top Violators", colorScheme.getX(), TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.text("Online Players:", colorScheme.getY(), TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.newline());

        topViolations.forEach(topViolation -> {
            TextComponent.Builder hoverTextBuilder = Component.text().append(Component.text("Alerts per check:\n", colorScheme.getY(), TextDecoration.BOLD));

            topViolation.checkViolations().forEach((checkName, violationCount) ->
                    hoverTextBuilder.append(Component.text("- ", colorScheme.getX()))
                            .append(Component.text(checkName + ": ", colorScheme.getY()))
                            .append(Component.text(violationCount + "\n", colorScheme.getX()))
            );

            // Build the hover event component
            HoverEvent<Component> hoverEvent = HoverEvent.showText(hoverTextBuilder.build());

            componentBuilder.append(Component.text("- ", colorScheme.getX()))
                    .append(Component.text(topViolation.username() + ": ", colorScheme.getX(), TextDecoration.BOLD)
                            .hoverEvent(hoverEvent))
                    .append(Component.text(topViolation.violations() + " violations", colorScheme.getY()))
                    .append(Component.newline());
        });

        return componentBuilder.build();
    }

}
