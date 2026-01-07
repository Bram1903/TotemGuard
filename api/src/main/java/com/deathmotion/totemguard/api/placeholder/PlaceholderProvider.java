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

package com.deathmotion.totemguard.api.placeholder;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Optional capability: holders can expose the keys they support for introspection / documentation.
 */
public interface PlaceholderProvider {

    /**
     * @return all exact placeholder keys this holder can resolve (without surrounding '%').
     */
    @NotNull
    Collection<String> keys();

    /**
     * Optional: if your holder supports dynamic keys (prefix/regex), expose them here.
     * Example: "myplugin_stat_*" or "myplugin_stat:<id>"
     */
    default @NotNull Collection<String> patterns() {
        return java.util.List.of();
    }
}
