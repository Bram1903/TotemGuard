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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.IntFunction;

public interface JsonSerializable {
    static @NotNull JsonArray serializeArray(@Nullable JsonSerializable @NotNull [] serializableArray) {
        JsonArray array = new JsonArray();

        for (JsonSerializable serializable : serializableArray) {
            array.add(serializable == null ? JsonNull.INSTANCE : serializable.toJson());
        }

        return array;
    }

    static <T extends JsonSerializable> T @NotNull [] deserializeArray(JsonArray jsonArray, IntFunction<T[]> newArray, Function<JsonElement, T> constructor) {
        T[] array = newArray.apply(jsonArray.size());

        for (int i = 0; i < jsonArray.size(); i++) {
            array[i] = constructor.apply(jsonArray.get(i));
        }

        return array;
    }

    @NotNull JsonElement toJson();
}
