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

package com.deathmotion.totemguard.common.features.antivpn;

import com.deathmotion.totemguard.common.features.antivpn.adapters.IPRiskAntiVPNAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

public final class AntiVPNProviders {

    private static final List<AntiVPNAdapter> ADAPTERS = List.of(
            new IPRiskAntiVPNAdapter()
    );

    private AntiVPNProviders() {
    }

    public static @Nullable AntiVPNAdapter byName(@NotNull String name) {
        for (AntiVPNAdapter adapter : ADAPTERS) {
            if (adapter.getName().equalsIgnoreCase(name)) return adapter;
        }
        return null;
    }

    public static @NotNull String availableNames() {
        return ADAPTERS.stream()
                .map(AntiVPNAdapter::getName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.joining(", "));
    }
}
