/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2026 Bram and contributors
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

package com.deathmotion.totemguard.common.alert;

import com.deathmotion.totemguard.api3.config.key.impl.MessagesKeys;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.check.CheckImpl;
import com.deathmotion.totemguard.common.message.MessageService;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class AlertBuilder {

    private AlertBuilder() {
    }

    public static String build(CheckImpl check, int violations, @Nullable String debugInfo) {
        MessageService messageService = TGPlatform.getInstance().getMessageService();

        Map<String, Object> extras = Map.of(
                "tg_check_violations", violations,
                "tg_check_debug", debugInfo == null ? "" : debugInfo
        );

        if (debugInfo != null) {
            return messageService.getJoined(
                    check.player,
                    check,
                    extras,
                    MessagesKeys.ALERTS_MESSAGE,
                    MessagesKeys.ALERTS_DEBUG
            );
        }

        return messageService.getString(
                MessagesKeys.ALERTS_MESSAGE,
                check.player,
                check,
                extras
        );
    }
}
