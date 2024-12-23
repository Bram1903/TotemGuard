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
import com.deathmotion.totemguard.manager.ConfigManager;
import com.deathmotion.totemguard.models.TotemPlayer;
import io.github.retrooper.packetevents.util.SpigotReflectionUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;

public class AlertMessageService {
    private final ConfigManager configManager;

    public AlertMessageService(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public Component createAlert(Check check, Component details) {
        // Get the alert format as a Component
        Component alertTemplate = configManager.getMessages().getAlertFormat();

        TotemPlayer player = check.getPlayer();

        // Replace placeholders directly in the Component
        return alertTemplate
                .replaceText(createReplacement("%player%", player.getUser().getName()))
                .replaceText(createReplacement("%tps%", String.format("%.2f", SpigotReflectionUtil.getTPS())))
                .replaceText(createReplacement("%client_version%", player.getUser().getClientVersion().getReleaseName()))
                .replaceText(createReplacement("%client_brand%", player.getBrand()))
                .replaceText(createReplacement("%ping%", String.valueOf(player.bukkitPlayer.getPing())))
                .replaceText(createReplacement("%check_name%", check.getCheckName()))
                .replaceText(createReplacement("%check_description%", check.getDescription()))
                .replaceText(createReplacement("%server%", check.getSettings().getServer()))
                .replaceText(createReplacement("%check_details%", details))
                .replaceText(createReplacement("%prefix%", configManager.getMessages().getPrefix()))
                .replaceText(createReplacement("%violations%", String.valueOf(check.getViolations())))
                .replaceText(createReplacement("%max_violations%", String.valueOf(check.getCheckSettings().getMaxViolations())));
    }

    private TextReplacementConfig createReplacement(String placeholder, String replacement) {
        return TextReplacementConfig.builder()
                .matchLiteral(placeholder)
                .replacement(replacement)
                .build();
    }

    private TextReplacementConfig createReplacement(String placeholder, Component replacement) {
        return TextReplacementConfig.builder()
                .matchLiteral(placeholder)
                .replacement(replacement)
                .build();
    }
}
