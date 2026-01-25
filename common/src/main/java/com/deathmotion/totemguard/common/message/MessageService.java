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

package com.deathmotion.totemguard.common.message;

import com.deathmotion.totemguard.api3.config.Config;
import com.deathmotion.totemguard.api3.config.ConfigFile;
import com.deathmotion.totemguard.api3.config.key.ConfigValueKey;
import com.deathmotion.totemguard.api3.reload.Reloadable;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.check.CheckImpl;
import com.deathmotion.totemguard.common.placeholder.PlaceholderRepositoryImpl;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.util.Lazy;
import com.deathmotion.totemguard.common.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;

public class MessageService implements Reloadable {

    private final TGPlatform platform;
    private final Lazy<PlaceholderRepositoryImpl> placeholder;

    private Config messages;

    public MessageService() {
        this.platform = TGPlatform.getInstance();
        this.placeholder = Lazy.of(platform::getPlaceholderRepository);
        this.messages = platform.getConfigRepository().config(ConfigFile.MESSAGES);
    }

    private static @NotNull String nullToEmpty(@Nullable String s) {
        return s == null ? "" : s;
    }

    private static @NotNull Map<String, Object> safeExtras(@Nullable Map<String, Object> extras) {
        return extras == null ? Collections.emptyMap() : extras;
    }

    @Override
    public void reload() {
        this.messages = platform.getConfigRepository().config(ConfigFile.MESSAGES);
    }

    public @NotNull String getString(@NotNull ConfigValueKey<String> key) {
        return placeholder.get().replace(nullToEmpty(messages.getString(key)));
    }

    public @NotNull Component getComponent(@NotNull ConfigValueKey<String> key) {
        return MessageUtil.formatMessage(getString(key));
    }

    public @NotNull String getString(@NotNull ConfigValueKey<String> key,
                                     @NotNull TGPlayer player) {
        return getString(key, player, null, null);
    }

    public @NotNull String getString(@NotNull ConfigValueKey<String> key,
                                     @NotNull TGPlayer player,
                                     @Nullable CheckImpl check) {
        return getString(key, player, check, null);
    }

    public @NotNull String getString(@NotNull ConfigValueKey<String> key,
                                     @NotNull TGPlayer player,
                                     @Nullable CheckImpl check,
                                     @Nullable Map<String, Object> extras) {
        String raw = nullToEmpty(messages.getString(key));
        return placeholder.get().replace(raw, player, check, safeExtras(extras));
    }

    public @NotNull Component getComponent(@NotNull ConfigValueKey<String> key,
                                           @NotNull TGPlayer player,
                                           @Nullable CheckImpl check,
                                           @Nullable Map<String, Object> extras) {
        return MessageUtil.formatMessage(getString(key, player, check, extras));
    }

    @SafeVarargs
    public final @NotNull String getJoined(@NotNull TGPlayer player,
                                           @Nullable CheckImpl check,
                                           @Nullable Map<String, Object> extras,
                                           @NotNull ConfigValueKey<String>... keys) {

        StringBuilder message = new StringBuilder();
        for (ConfigValueKey<String> key : keys) {
            message.append(nullToEmpty(messages.getString(key)));
        }

        return placeholder.get().replace(message.toString(), player, check, safeExtras(extras));
    }
}
