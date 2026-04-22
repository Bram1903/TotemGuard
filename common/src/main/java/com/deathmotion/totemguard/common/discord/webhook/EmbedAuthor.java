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
 *
 * Originally adapted from GrimAC (https://github.com/GrimAnticheat/Grim).
 */

package com.deathmotion.totemguard.common.discord.webhook;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.util.Objects.requireNonNull;

@Getter
@Setter
@Accessors(fluent = true)
public class EmbedAuthor implements JsonSerializable {
    public static final int MAX_NAME_LENGTH = 256;

    private @NotNull String name;
    private @Nullable String url;
    private @Nullable String icon;

    public EmbedAuthor(@NotNull String name) {
        this(name, null, null);
    }

    public EmbedAuthor(@NotNull String name, @Nullable String url, @Nullable String icon) {
        name(name);
        url(url);
        icon(icon);
    }

    public EmbedAuthor(@NotNull JsonElement jsonElement) {
        JsonObject json = jsonElement.getAsJsonObject();
        name(json.get("name").getAsString());

        JsonElement element;
        if ((element = json.get("url")) != null) url(element.getAsString());
        if ((element = json.get("icon_url")) != null) icon(element.getAsString());
    }

    @Contract(value = "_ -> this", mutates = "this")
    public @NotNull EmbedAuthor name(@NotNull String name) {
        requireNonNull(name, "Embed author name cannot be null!");
        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Embed author name too long, " + name.length() + " > " + MAX_NAME_LENGTH);
        }

        this.name = name;
        return this;
    }

    @Override
    public @NotNull JsonElement toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("name", name());
        if (url() != null) json.addProperty("url", url());
        if (icon() != null) json.addProperty("icon_url", icon());
        return json;
    }
}
