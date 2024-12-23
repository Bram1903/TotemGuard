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
import net.kyori.adventure.text.Component;

public class MessengerService {
    private final ConfigManager configManager;
    private final AlertMessageService alertMessageService;

    public MessengerService(TotemGuard totemGuard) {
        this.configManager = totemGuard.getConfigManager();
        this.alertMessageService = new AlertMessageService(configManager);
    }

    public Component getPrefix() {
        return configManager.getMessages().getPrefix();
    }

    public Component toggleAlerts(boolean enabled) {
        Component message = enabled
                ? configManager.getMessages().getAlertsEnabled()
                : configManager.getMessages().getAlertsDisabled();

        return message.replaceText(builder ->
                builder.matchLiteral("%prefix%").replacement(getPrefix())
        );
    }

    public Component createAlert(Check check, Component details) {
        return alertMessageService.createAlert(check, details);
    }
}
