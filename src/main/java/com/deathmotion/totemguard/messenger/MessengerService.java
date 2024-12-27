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
import com.deathmotion.totemguard.messenger.impl.AlertMessageService;
import com.deathmotion.totemguard.messenger.impl.PlaceHolderService;
import com.deathmotion.totemguard.util.datastructure.Pair;
import net.kyori.adventure.text.Component;

public class MessengerService {
    private final ConfigManager configManager;

    private final PlaceHolderService placeHolderService;
    private final AlertMessageService alertMessageService;

    public MessengerService(TotemGuard totemGuard) {
        this.configManager = totemGuard.getConfigManager();

        this.placeHolderService = new PlaceHolderService(this);
        this.alertMessageService = new AlertMessageService(this);
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

    public Pair<Component, Component> createAlert(Check check, Component details) {
        return alertMessageService.createAlert(check, details);
    }
}
