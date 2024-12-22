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

package com.deathmotion.totemguard.config.serializers;

import de.exlll.configlib.Serializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class ComponentSerializer implements Serializer<Component, String> {

    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.legacyAmpersand();

    @Override
    public String serialize(Component component) {
        // Attempt to determine the format of the original text
        String plainText = legacySerializer.serialize(component);

        if (isLegacyFormat(plainText)) {
            return legacySerializer.serialize(component); // Retain a legacy format
        }

        return miniMessage.serialize(component); // Otherwise, use MiniMessage
    }

    @Override
    public Component deserialize(String string) {
        if (isLegacyFormat(string)) {
            return legacySerializer.deserialize(string);
        } else {
            return miniMessage.deserialize(string);
        }
    }

    private boolean isLegacyFormat(String string) {
        // Check for legacy formatting codes
        return string.contains("&") || string.contains("§");
    }
}
