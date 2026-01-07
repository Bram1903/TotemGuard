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

package com.deathmotion.totemguard.common.placeholder.engine;

import com.deathmotion.totemguard.api.check.Check;
import com.deathmotion.totemguard.api.placeholder.PlaceholderContext;
import com.deathmotion.totemguard.api.placeholder.PlaceholderHolder;
import com.deathmotion.totemguard.api.user.TGUser;
import com.deathmotion.totemguard.common.placeholder.holder.InternalPlaceholderHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PlaceholderEngine {

    private static final Pattern PLACEHOLDER = Pattern.compile("%([a-zA-Z0-9_]+)%");

    private final List<InternalPlaceholderHolder> internalHolders;
    private final List<PlaceholderHolder> apiHolders;

    public PlaceholderEngine(@NotNull List<InternalPlaceholderHolder> internalHolders,
                             @NotNull List<PlaceholderHolder> apiHolders) {
        this.internalHolders = List.copyOf(internalHolders);
        this.apiHolders = List.copyOf(apiHolders);
    }

    public @NotNull String replace(@NotNull String message,
                                   @NotNull InternalContext internalCtx,
                                   @Nullable TGUser apiUser,
                                   @Nullable Check apiCheck,
                                   @NotNull Map<String, Object> extras) {

        if (message.isEmpty() || message.indexOf('%') == -1) {
            return message;
        }

        PlaceholderContext apiCtx = new PlaceholderContext(apiUser, apiCheck, extras);

        Matcher matcher = PLACEHOLDER.matcher(message);
        StringBuilder out = new StringBuilder(message.length());

        while (matcher.find()) {
            String key = matcher.group(1);

            String replacement = resolveExtra(key, extras);
            if (replacement == null) {
                replacement = resolveInternal(key, internalCtx);
            }
            if (replacement == null) {
                replacement = resolveApi(key, apiCtx);
            }

            if (replacement == null) {
                replacement = matcher.group(0);
            }

            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(out);
        return out.toString();
    }

    private @Nullable String resolveExtra(String key, Map<String, Object> extras) {
        Object value = extras.get(key);
        return value != null ? value.toString() : null;
    }

    private @Nullable String resolveInternal(String key, InternalContext ctx) {
        for (InternalPlaceholderHolder holder : internalHolders) {
            String replacement = holder.resolve(key, ctx);
            if (replacement != null) return replacement;
        }
        return null;
    }

    private @Nullable String resolveApi(String key, PlaceholderContext ctx) {
        for (PlaceholderHolder holder : apiHolders) {
            String replacement = holder.resolve(key, ctx);
            if (replacement != null) return replacement;
        }
        return null;
    }
}

