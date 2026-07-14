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

package com.deathmotion.totemguard.common.features.mods;

import net.kyori.adventure.text.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class DebugModifierKeybinds {

    static final String DEBUG_MODIFIER = "key.debug.modifier";
    static final String UNBOUND = "key.keyboard.unknown";
    static final List<String> MOVEMENT = List.of("key.forward", "key.back", "key.left", "key.right", "key.jump");

    private DebugModifierKeybinds() {
    }

    static Map<String, Component> questions() {
        Map<String, Component> questions = new LinkedHashMap<>();
        questions.put(DEBUG_MODIFIER, Component.keybind(DEBUG_MODIFIER));
        questions.put(UNBOUND, Component.translatable(UNBOUND));
        for (String key : MOVEMENT) questions.put(key, Component.keybind(key));
        return questions;
    }

    static boolean conflicts(Map<String, String> renderedByToken) {
        String debugModifier = renderedByToken.get(DEBUG_MODIFIER);
        if (debugModifier == null || debugModifier.isBlank()) return false;

        String unbound = renderedByToken.get(UNBOUND);
        if (unbound != null && debugModifier.equals(unbound)) return false;

        for (String movementKey : MOVEMENT) {
            String rendered = renderedByToken.get(movementKey);
            if (rendered != null && rendered.equals(debugModifier)) return true;
        }
        return false;
    }
}
