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

package com.deathmotion.totemguard.messenger.impl;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.commands.totemguard.database.util.ValidationType;
import com.deathmotion.totemguard.config.Messages;
import com.deathmotion.totemguard.manager.ConfigManager;
import com.deathmotion.totemguard.messenger.MessengerService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;

public class DatabaseMessageService {

    private final MessengerService messengerService;
    private final ConfigManager configManager;

    public DatabaseMessageService(TotemGuard plugin, MessengerService messengerService) {
        this.messengerService = messengerService;
        this.configManager = plugin.getConfigManager();
    }

    private Messages.CommandMessages.DatabaseCommand getDatabaseCommandMessages() {
        return configManager.getMessages().getCommandMessages().getDatabaseCommand();
    }

    public Component clearingStartedComponent() {
        return messengerService.format(getDatabaseCommandMessages().getClearingStarted().replace("%prefix%", messengerService.getPrefix()));
    }

    public Component trimmingStartedComponent() {
        return messengerService.format(getDatabaseCommandMessages().getTrimmingStarted().replace("%prefix%", messengerService.getPrefix()));
    }

    public Component invalidConfirmationCode() {
        return messengerService.format(getDatabaseCommandMessages().getInvalidConfirmationCode().replace("%prefix%", messengerService.getPrefix()));
    }

    public Component confirmationMessage(ValidationType type, int code) {
        Messages.CommandMessages.DatabaseCommand.ActionConfirmationFormat format = getDatabaseCommandMessages().getActionConfirmationFormat();
        String messageFormat = format.getActionConfirmationFormat();
        String command = format.getConfirmCommand().replace("%action%", type.toString().toLowerCase()).replace("%code%", String.valueOf(code));

        messageFormat = messageFormat.replace("%action%", type.toString().toLowerCase());
        messageFormat = messageFormat.replace("%command%", command);

        Component message = messengerService.format(messageFormat);
        Component confirm = messengerService.format(format.getConfirmButton())
                .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .hoverEvent(messengerService.format(format.getConfirmHover().replace("%command%", command)));

        message = message.replaceText(builder -> builder.matchLiteral("%confirm_button%").replacement(confirm));
        return message;
    }
}
