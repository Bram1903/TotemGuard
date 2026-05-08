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

package com.deathmotion.totemguard.common.features.discord.webhook;

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
public class EmbedFooter implements JsonSerializable {
    public static final int MAX_TEXT_LENGTH = 2048;

    private @NotNull String text;
    private @Nullable String icon;

    public EmbedFooter(@NotNull String text) {
        this(text, null);
    }

    public EmbedFooter(@NotNull String text, @Nullable String icon) {
        text(text);
        icon(icon);
    }

    public EmbedFooter(@NotNull JsonElement jsonElement) {
        JsonObject json = jsonElement.getAsJsonObject();
        text(json.get("text").getAsString());
        JsonElement icon_url = json.get("icon_url");
        if (icon_url != null) icon(icon_url.getAsString());
    }

    @Contract(value = "_ -> this", mutates = "this")
    public @NotNull EmbedFooter text(@NotNull String text) {
        requireNonNull(text, "Embed footer text cannot be null!");
        if (text.length() > MAX_TEXT_LENGTH) {
            throw new IllegalArgumentException("Embed footer text too long, " + text.length() + " > " + MAX_TEXT_LENGTH);
        }

        this.text = text;
        return this;
    }

    @Override
    public @NotNull JsonElement toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("text", text());
        if (icon() != null) json.addProperty("icon_url", icon());
        return json;
    }
}
