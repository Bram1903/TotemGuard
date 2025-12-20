/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2025 Bram and contributors
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

package com.deathmotion.totemguard.common.config.serializer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jspecify.annotations.NonNull;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;

public final class ComponentSerializer implements TypeSerializer<Component> {

    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    @Override
    public Component deserialize(final @NonNull Type type, final @NonNull ConfigurationNode node) {
        final String serialized = node.getString("");
        return miniMessage.deserialize(serialized);
    }

    @Override
    public void serialize(final @NonNull Type type, final Component obj, final @NonNull ConfigurationNode node) throws SerializationException {
        final String serialized = miniMessage.serialize(obj);
        node.set(serialized);
    }
}
