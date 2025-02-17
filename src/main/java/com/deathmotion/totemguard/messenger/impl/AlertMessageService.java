/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2025 Bram and contributors
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

package com.deathmotion.totemguard.messenger.impl;

import com.deathmotion.totemguard.checks.Check;
import com.deathmotion.totemguard.config.Messages;
import com.deathmotion.totemguard.messenger.MessengerService;
import com.deathmotion.totemguard.util.datastructure.Pair;
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

        alertHoverTemplate = messengerService.replacePlaceholders(alertHoverTemplate, check);

        Component hoverMessage = messengerService.format(alertHoverTemplate.replace("%check_details%", messengerService.unformat(details)));
        Component alertMessage = messengerService.format(messengerService.replacePlaceholders(alertTemplate, check));
        Component consoleMessage = messengerService.format(messengerService.replacePlaceholders(alertConsoleTemplate, check));

        alertMessage = alertMessage.hoverEvent(hoverMessage);
        alertMessage = alertMessage.clickEvent(ClickEvent.runCommand(messengerService.replacePlaceholders(alertClickTemplate, check)));

        return new Pair<>(alertMessage, consoleMessage);
    }
}
