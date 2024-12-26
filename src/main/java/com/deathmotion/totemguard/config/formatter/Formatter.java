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

package com.deathmotion.totemguard.config.formatter;

import lombok.AccessLevel;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

@SuppressWarnings("unused")
public enum Formatter {
    MINIMESSAGE(
            text -> MiniMessage.miniMessage().deserialize(text),
            "MiniMessage"
    ),
    LEGACY(
            text -> getLegacySerializer().deserialize(replaceHexColorCodes(text)),
            "Legacy Text"
    );

    @Getter(value = AccessLevel.PRIVATE)
    private final static LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
            .character('&')
            .hexCharacter('#')
            .useUnusualXRepeatedCharacterHexFormat()
            .hexColors()
            .build();
    /**
     * Name of the formatter
     */
    @Getter
    private final String name;

    /**
     * Function to apply formatting to a string
     */
    private final java.util.function.Function<String, Component> formatter;

    Formatter(@NotNull Function<String, Component> formatter, @NotNull String name) {
        this.formatter = formatter;
        this.name = name;
    }

    @NotNull
    private static String replaceHexColorCodes(@NotNull String text) {
        return text.replace('§', '&');
    }

    private static LegacyComponentSerializer getLegacySerializer() {
        return LEGACY_SERIALIZER;
    }

    /**
     * Apply formatting to a string
     *
     * @param text the string to format
     * @return the formatted string
     */
    public Component format(@NotNull String text) {
        return formatter.apply(text);
    }

    public String unformat(@NotNull Component component) {
        return switch (this) {
            case MINIMESSAGE -> MiniMessage.miniMessage().serialize(component);
            case LEGACY -> getLegacySerializer().serialize(component);
        };
    }
}