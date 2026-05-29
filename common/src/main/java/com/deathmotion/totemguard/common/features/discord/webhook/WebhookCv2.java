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

package com.deathmotion.totemguard.common.features.discord.webhook;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class WebhookCv2 {

    /**
     * Message flag that tells Discord to render the {@code components} array as V2 layout.
     */
    public static final int IS_COMPONENTS_V2 = 1 << 15;

    private static final int TEXT_LIMIT = 3900;

    private static final int TYPE_SECTION = 9;
    private static final int TYPE_TEXT_DISPLAY = 10;
    private static final int TYPE_THUMBNAIL = 11;
    private static final int TYPE_SEPARATOR = 14;
    private static final int TYPE_CONTAINER = 17;

    private final JsonArray children = new JsonArray();
    private final int accentColor;

    private @Nullable String username;
    private @Nullable String avatarUrl;

    private WebhookCv2(int accentColor) {
        this.accentColor = accentColor;
    }

    public static @NotNull WebhookCv2 container(int accentColor) {
        return new WebhookCv2(accentColor);
    }

    private static JsonObject textDisplay(String content) {
        JsonObject text = typed(TYPE_TEXT_DISPLAY);
        text.addProperty("content", truncate(content));
        return text;
    }

    private static JsonObject typed(int type) {
        JsonObject object = new JsonObject();
        object.addProperty("type", type);
        return object;
    }

    private static String truncate(String value) {
        return value.length() > TEXT_LIMIT ? value.substring(0, TEXT_LIMIT - 3) + "..." : value;
    }

    public @NotNull WebhookCv2 identity(@Nullable String username, @Nullable String avatarUrl) {
        this.username = username;
        this.avatarUrl = avatarUrl;
        return this;
    }

    public @NotNull WebhookCv2 text(@NotNull String markdown) {
        if (!markdown.isBlank()) children.add(textDisplay(markdown));
        return this;
    }

    public @NotNull WebhookCv2 heading(@NotNull String heading) {
        return text("## " + heading);
    }

    public @NotNull WebhookCv2 subtle(@NotNull String text) {
        return text("-# " + text);
    }

    public @NotNull WebhookCv2 field(@NotNull String name, @NotNull String value) {
        return text("**" + name + "**\n" + value);
    }

    public @NotNull WebhookCv2 section(@NotNull String markdown, @NotNull String thumbnailUrl) {
        JsonObject section = typed(TYPE_SECTION);
        JsonArray content = new JsonArray();
        content.add(textDisplay(markdown.isBlank() ? "​" : markdown));
        section.add("components", content);

        JsonObject thumbnail = typed(TYPE_THUMBNAIL);
        JsonObject media = new JsonObject();
        media.addProperty("url", thumbnailUrl);
        thumbnail.add("media", media);
        section.add("accessory", thumbnail);

        children.add(section);
        return this;
    }

    public @NotNull WebhookCv2 divider() {
        children.add(typed(TYPE_SEPARATOR));
        return this;
    }

    public @NotNull JsonObject build() {
        JsonObject message = new JsonObject();
        if (username != null && !username.isBlank()) message.addProperty("username", username);
        if (avatarUrl != null && !avatarUrl.isBlank()) message.addProperty("avatar_url", avatarUrl);
        message.addProperty("flags", IS_COMPONENTS_V2);

        if (children.size() == 0) children.add(textDisplay("​"));

        JsonObject container = typed(TYPE_CONTAINER);
        container.addProperty("accent_color", accentColor & 0xFFFFFF);
        container.add("components", children);

        JsonArray top = new JsonArray();
        top.add(container);
        message.add("components", top);
        return message;
    }
}
