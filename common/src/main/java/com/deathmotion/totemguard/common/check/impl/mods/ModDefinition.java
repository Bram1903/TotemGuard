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

package com.deathmotion.totemguard.common.check.impl.mods;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public record ModDefinition(
        @NotNull String id,
        @NotNull ModSeverity severity,
        @NotNull List<String> payloads,
        @NotNull List<String> translations
) {

    public ModDefinition {
        id = Objects.requireNonNull(id, "id").trim();
        if (id.isBlank()) {
            throw new IllegalArgumentException("id cannot be blank");
        }

        severity = Objects.requireNonNull(severity, "severity");
        payloads = List.copyOf(Objects.requireNonNull(payloads, "payloads"));
        translations = List.copyOf(Objects.requireNonNull(translations, "translations"));
    }

    public boolean hasPayloads() {
        return !payloads.isEmpty();
    }

    public boolean hasTranslations() {
        return !translations.isEmpty();
    }

    public boolean matchesPayload(String value) {
        if (value == null || value.isBlank() || payloads.isEmpty()) {
            return false;
        }

        for (String payload : payloads) {
            if (value.contains(payload)) {
                return true;
            }
        }

        return false;
    }
}
