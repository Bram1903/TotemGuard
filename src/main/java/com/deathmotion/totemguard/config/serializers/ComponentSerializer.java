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
    private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.builder()
            .character('&')
            .hexCharacter('#')
            .hexColors()
            .build();

    /**
     * Replace §-style color codes with '&'
     *
     * @param text the input text
     * @return the processed text
     */
    private static String replaceHexColorCodes(String text) {
        return text.replace('§', '&');
    }

    @Override
    public String serialize(Component component) {
        return miniMessage.serialize(component);
    }

    // This method has been heavily inspired by
    // https://github.com/alexdev03/UnlimitedNametags/blob/main/src/main/java/org/alexdev/unlimitednametags/config/Formatter.java
    @Override
    public Component deserialize(String string) {
        // Universal deserialization logic
        // Step 1: Convert string to legacy component
        String legacy = legacySerializer.serialize(legacySerializer.deserialize(replaceHexColorCodes(string)));

        // Step 2: Convert a legacy component to MiniMessage string
        String miniMessageString = miniMessage.serialize(legacySerializer.deserialize(legacy))
                .replace("\\<", "<")
                .replace("\\", "");

        // Step 3: Deserialize MiniMessage string into a Component
        return miniMessage.deserialize(miniMessageString);
    }
}
