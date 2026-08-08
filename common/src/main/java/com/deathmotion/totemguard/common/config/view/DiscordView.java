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

package com.deathmotion.totemguard.common.config.view;

import com.deathmotion.totemguard.api.config.Config;
import com.deathmotion.totemguard.api.config.ConfigSection;
import com.deathmotion.totemguard.common.config.schema.WebhookConfig;
import com.deathmotion.totemguard.common.config.schema.WebhookField;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class DiscordView {

    private final Config config;

    public DiscordView(Config config) {
        this.config = config;
    }

    private static List<WebhookField> parseFields(Object raw) {
        if (!(raw instanceof List<?> list)) return List.of();
        List<WebhookField> out = new ArrayList<>(list.size());
        for (Object entry : list) {
            if (!(entry instanceof Map<?, ?> map)) continue;
            Object name = map.get("name");
            Object value = map.get("value");
            Object inline = map.get("inline");
            if (name == null || value == null) continue;

            boolean inlineFlag = inline instanceof Boolean b ? b
                    : Boolean.parseBoolean(String.valueOf(inline));
            out.add(new WebhookField(String.valueOf(name), String.valueOf(value), inlineFlag));
        }
        return out;
    }

    public int version() {
        return config.version();
    }

    public @NotNull WebhookConfig webhook(@NotNull String prefix) {
        Optional<ConfigSection> section = config.getSection(prefix);
        if (section.isEmpty()) {
            return new WebhookConfig(false, "", "TotemGuard", "", "TotemGuard", "#d9b61a", true, "", "", List.of());
        }
        ConfigSection s = section.get();

        boolean enabled = s.getBoolean("enabled").orElse(false);
        String url = s.getString("url").orElse("");
        String username = s.getString("username").orElse("TotemGuard");
        String avatar = s.getString("avatar").orElse("");
        String title = s.getString("title").orElse("TotemGuard");
        String color = s.getString("color").orElse("#d9b61a");
        boolean timestamp = s.getBoolean("timestamp").orElse(true);
        String thumbnail = s.getString("thumbnail").orElse("");
        String footer = s.getString("footer").orElse("");
        List<WebhookField> fields = parseFields(s.get("fields").orElse(null));

        return new WebhookConfig(enabled, url, username, avatar, title, color, timestamp, thumbnail, footer, fields);
    }
}
