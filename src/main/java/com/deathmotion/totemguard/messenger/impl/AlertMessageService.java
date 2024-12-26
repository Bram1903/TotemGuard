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

package com.deathmotion.totemguard.messenger.impl;

import com.deathmotion.totemguard.checks.Check;
import com.deathmotion.totemguard.config.Messages;
import com.deathmotion.totemguard.messenger.MessengerService;
import com.deathmotion.totemguard.models.TotemPlayer;
import com.deathmotion.totemguard.util.datastructure.Pair;
import io.github.retrooper.packetevents.util.SpigotReflectionUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;

public class AlertMessageService {
    private final MessengerService messengerService;

    public AlertMessageService(MessengerService messengerService) {
        this.messengerService = messengerService;
    }

    public Pair<Component, Component> createAlert(Check check, Component details) {
        Messages messages = check.getMessages();

        String alertTemplate = messages.getAlertFormat().getAlertFormat();
        String alertConsoleTemplate = messages.getAlertFormat().getAlertFormatConsole();
        String alertHoverTemplate = messages.getAlertFormat().getAlertHoverMessage();
        String alertClickTemplate = messages.getAlertFormat().getAlertClickCommand();

        Component hoverMessage = createHoverMessage(alertHoverTemplate, check, details);
        Component alertMessage = createAlertMessage(alertTemplate, check);
        Component consoleMessage = createAlertMessage(alertConsoleTemplate, check);

        alertMessage = alertMessage.hoverEvent(hoverMessage);
        alertMessage = alertMessage.clickEvent(ClickEvent.runCommand(alertClickTemplate.replace("%player%", check.getPlayer().getName())));

        return new Pair<>(alertMessage, consoleMessage);
    }

    private Component createHoverMessage(String alertHoverTemplate, Check check, Component details) {
        TotemPlayer player = check.getPlayer();

        return messengerService.format(alertHoverTemplate
                .replace("%tps%", String.format("%.2f", SpigotReflectionUtil.getTPS()))
                .replace("%client_version%", player.getVersionName())
                .replace("%client_brand%", player.getBrand())
                .replace("%player%", player.getName())
                .replace("%ping%", String.valueOf(player.getKeepAlivePing()))
                .replace("%check_name%", check.getCheckName())
                .replace("%check_description%", check.getDescription())
                .replace("%server%", check.getSettings().getServer())
                .replace("%check_details%", messengerService.unformat(details)));
    }

    private Component createAlertMessage(String alertTemplate, Check check) {
        TotemPlayer player = check.getPlayer();

        return messengerService.format(alertTemplate
                .replace("%prefix%", messengerService.getPrefix())
                .replace("%player%", player.getName())
                .replace("%check_name%", check.getCheckName())
                .replace("%violations%", String.valueOf(check.getViolations()))
                .replace("%max_violations%", check.getCheckSettings().isPunishable() ? String.valueOf(check.getMaxViolations()) : "∞"));
    }
}
