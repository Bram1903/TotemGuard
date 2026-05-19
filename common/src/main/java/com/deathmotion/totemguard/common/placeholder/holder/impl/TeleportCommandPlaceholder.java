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

package com.deathmotion.totemguard.common.placeholder.holder.impl;

import com.deathmotion.totemguard.api.config.ConfigFile;
import com.deathmotion.totemguard.api.placeholder.PlaceholderContext;
import com.deathmotion.totemguard.api.placeholder.PlaceholderHolder;
import com.deathmotion.totemguard.api.placeholder.PlaceholderProvider;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.config.key.ConfigKeys;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public final class TeleportCommandPlaceholder implements PlaceholderHolder, PlaceholderProvider {

    public static final String KEY = "tg_teleport_command";
    public static final String DEFAULT_TEMPLATE = "/tg teleport %tg_player%";

    // Recursion guard: an operator who configured `teleport.command: "%tg_teleport_command%"`
    // would otherwise infinite-loop. The literal placeholder text gets emitted instead.
    private static final ThreadLocal<Boolean> RESOLVING = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private static String template() {
        try {
            String configured = TGPlatform.getInstance().getConfigRepository()
                    .config(ConfigFile.CONFIG)
                    .getString(ConfigKeys.TELEPORT_COMMAND);
            if (configured == null || configured.isBlank()) return DEFAULT_TEMPLATE;
            return configured;
        } catch (Exception ex) {
            return DEFAULT_TEMPLATE;
        }
    }

    @Override
    public @Nullable String resolve(@NotNull String key, @NotNull PlaceholderContext context) {
        if (!KEY.equals(key)) return null;
        if (RESOLVING.get()) return "%" + KEY + "%";

        String template = template();
        RESOLVING.set(Boolean.TRUE);
        try {
            return TGPlatform.getInstance().getPlaceholderRepository().replace(template, context);
        } finally {
            RESOLVING.set(Boolean.FALSE);
        }
    }

    @Override
    public @NotNull Collection<String> keys() {
        return List.of(KEY);
    }
}
