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

package com.deathmotion.totemguard.discord.alert;

import com.deathmotion.totemguard.api.check.CheckType;
import com.deathmotion.totemguard.api.event.events.TGNetworkAlertEvent;
import com.deathmotion.totemguard.discord.config.BotConfig;
import com.deathmotion.totemguard.discord.ui.Cv2;
import com.deathmotion.totemguard.discord.ui.Format;
import net.dv8tion.jda.api.components.container.Container;
import org.jetbrains.annotations.NotNull;

final class AlertComponentFactory {
    private AlertComponentFactory() {
    }

    static @NotNull Container build(@NotNull TGNetworkAlertEvent event, @NotNull BotConfig.ChannelConfig channel) {
        String body = "## " + applyTokens(channel.title(), event)
                + "\n**Player:** `" + event.getPlayerName() + "`"
                + "\n**Check:** " + event.getCheckName() + "  •  **Violations:** " + event.getViolations()
                + "\n**Type:** " + pretty(event.getCheckType()) + "  •  **Server:** `" + event.getServerName() + "`";

        Cv2 card = Cv2.container(channel.color()).section(body, head(event.getPlayerUuid().toString()));

        if (event.getDebug() != null && !event.getDebug().isBlank()) {
            card.divider().codeBlock(event.getDebug());
        }

        return card.subtle("UUID `" + event.getPlayerUuid() + "`  •  " + Format.relative(event.getTimestamp())).build();
    }

    private static String head(String uuid) {
        return "https://mc-heads.net/avatar/" + uuid + "/100";
    }

    private static String applyTokens(String title, TGNetworkAlertEvent event) {
        return title
                .replace("%player%", event.getPlayerName())
                .replace("%uuid%", event.getPlayerUuid().toString())
                .replace("%check%", event.getCheckName())
                .replace("%violations%", Integer.toString(event.getViolations()))
                .replace("%server%", event.getServerName());
    }

    private static String pretty(CheckType type) {
        String[] words = type.name().toLowerCase().split("_");
        StringBuilder out = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return out.toString();
    }
}
