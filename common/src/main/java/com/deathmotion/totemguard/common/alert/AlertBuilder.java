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

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.check.CheckImpl;
import com.deathmotion.totemguard.common.config.key.MessagesKeys;
import com.deathmotion.totemguard.common.message.MessageService;
import com.deathmotion.totemguard.common.util.MessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class AlertBuilder {

    private static final String UNSPECIFIED_DEBUG = "Not specified";

    private AlertBuilder() {
    }

    public static Component build(CheckImpl check, int violations, @Nullable String debugInfo) {
        return build(check, violations, debugInfo, Map.of());
    }

    public static Component build(CheckImpl check,
                                  int violations,
                                  @Nullable String debugInfo,
                                  @NotNull Map<String, Object> extraPlaceholders) {
        MessageService messageService = TGPlatform.getInstance().getMessageService();
        Map<String, Object> extras = new HashMap<>(extraPlaceholders.size() + 2);
        extras.put("tg_check_violations", violations);
        extras.put("tg_check_debug", debugInfo == null ? UNSPECIFIED_DEBUG : debugInfo);
        extras.putAll(extraPlaceholders);

        Component alert = messageService.getComponent(check.getAlertMessageKey(), check.player, check, extras);

        String hoverText = messageService.getString(MessagesKeys.ALERTS_HOVER, check.player, check, extras).stripTrailing();
        if (!hoverText.isBlank()) {
            alert = alert.hoverEvent(HoverEvent.showText(MessageUtil.formatMessage(hoverText)));
        }

        String command = messageService.getString(MessagesKeys.ALERTS_COMMAND, check.player, check, extras);
        if (!command.isBlank()) {
            alert = alert.clickEvent(ClickEvent.runCommand(command));
        }

        return alert;
    }
}
