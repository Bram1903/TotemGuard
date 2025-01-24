/*
 *  This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 *  Copyright (C) 2024 Bram and contributors
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.deathmotion.totemguard.messenger;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.checks.Check;
import com.deathmotion.totemguard.manager.ConfigManager;
import com.deathmotion.totemguard.messenger.impl.*;
import com.deathmotion.totemguard.util.TGVersions;
import com.deathmotion.totemguard.util.datastructure.Pair;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

@Getter
public class MessengerService {
    private final ConfigManager configManager;

    private final PlaceHolderService placeHolderService;
    private final AlertMessageService alertMessageService;
    private final CommandMessengerService commandMessengerService;
    private final ProfileMessageService profileMessageService;
    private final DatabaseMessageService databaseMessageService;
    private final ClearLogsMessageService clearLogsMessageService;
    private final StatsMessageService statsMessageService;

    public MessengerService(TotemGuard plugin) {
        this.configManager = plugin.getConfigManager();

        this.placeHolderService = new PlaceHolderService(this);
        this.alertMessageService = new AlertMessageService(this);
        this.commandMessengerService = new CommandMessengerService(plugin, this);
        this.profileMessageService = new ProfileMessageService(plugin, this);
        this.databaseMessageService = new DatabaseMessageService(plugin, this);
        this.clearLogsMessageService = new ClearLogsMessageService(plugin, this);
        this.statsMessageService = new StatsMessageService(plugin, this);
    }

    public Component format(String text) {
        return configManager.getMessages().getFormat().format(text);
    }

    public String unformat(Component component) {
        return configManager.getMessages().getFormat().unformat(component);
    }

    public String getPrefix() {
        return configManager.getMessages().getPrefix();
    }

    public String replacePlaceholders(String text, Check check) {
        return placeHolderService.replacePlaceHolders(text, check);
    }

    public Component toggleAlerts(boolean enabled) {
        String message = enabled
                ? configManager.getMessages().getAlertsEnabled()
                : configManager.getMessages().getAlertsDisabled();

        message = message.replace("%prefix%", getPrefix());

        return format(message);
    }

    public Component totemGuardInfo() {
        Component versionComponent = Component.text()
                .append(Component.text(" Running ", NamedTextColor.WHITE))
                .append(Component.text("TotemGuard", NamedTextColor.GREEN))
                .append(Component.text(" v" + TGVersions.CURRENT, NamedTextColor.GREEN))
                .build();

        if (TGVersions.CURRENT.snapshotCommit() != null) {
            versionComponent = versionComponent.append(Component.text()
                    .append(Component.text(" (Git: ", NamedTextColor.WHITE))
                    .append(Component.text(TGVersions.CURRENT.snapshotCommit(), NamedTextColor.WHITE))
                    .append(Component.text(")", NamedTextColor.WHITE))
                    .build());
        }

        return format(getPrefix())
                .append(versionComponent)
                .append(Component.text(" by ", NamedTextColor.WHITE))
                .append(Component.text("Bram", NamedTextColor.GREEN))
                .append(Component.text(" and ", NamedTextColor.WHITE))
                .append(Component.text("OutDev", NamedTextColor.GREEN))
                .hoverEvent(HoverEvent.showText(Component.text()
                        .append(Component.text("Open Github Page!", NamedTextColor.GREEN, TextDecoration.BOLD, TextDecoration.UNDERLINED))
                        .build()))
                .decorate(TextDecoration.BOLD)
                .clickEvent(ClickEvent.openUrl("https://github.com/Bram1903/TotemGuard"));
    }

    public Pair<Component, Component> createAlert(Check check, Component details) {
        return alertMessageService.createAlert(check, details);
    }
}
