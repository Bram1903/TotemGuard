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
import com.deathmotion.totemguard.messenger.MessengerService;
import com.deathmotion.totemguard.models.TotemPlayer;
import io.github.retrooper.packetevents.util.SpigotReflectionUtil;

public class PlaceHolderService {
    private final MessengerService messengerService;

    public PlaceHolderService(MessengerService messengerService) {
        this.messengerService = messengerService;
    }

    public String replacePlaceHolders(String text, Check check) {
        TotemPlayer player = check.getPlayer();

        return text
                .replace("%tps%", String.format("%.2f", SpigotReflectionUtil.getTPS()))
                .replace("%client_version%", player.getVersionName())
                .replace("%client_brand%", player.getBrand())
                .replace("%player%", player.getName())
                .replace("%ping%", String.valueOf(player.getKeepAlivePing()))
                .replace("%check_name%", check.getCheckName())
                .replace("%check_description%", check.getDescription())
                .replace("%server%", check.getSettings().getServer())
                .replace("%prefix%", messengerService.getPrefix())
                .replace("%violations%", String.valueOf(check.getViolations()))
                .replace("%max_violations%", check.getCheckSettings().isPunishable() ? String.valueOf(check.getMaxViolations()) : "∞")
                .replace("%dev%", check.isExperimental() ? check.getMessages().getAlertFormat().getDevPrefix() : "");
    }
}