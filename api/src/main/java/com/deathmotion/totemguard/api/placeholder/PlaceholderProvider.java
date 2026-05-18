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
import java.util.List;

/**
 * Optional capability for holders to expose their supported keys for introspection.
 */
public interface PlaceholderProvider {

    /**
     * Exact placeholder keys this holder resolves, without surrounding '%'.
     */
    @NotNull
    Collection<String> keys();

    /**
     * Dynamic key patterns (prefix or regex) the holder supports. Example,
     * {@code "myplugin_stat_*"} or {@code "myplugin_stat:<id>"}.
     */
    default @NotNull Collection<String> patterns() {
        return List.of();
    }
}
