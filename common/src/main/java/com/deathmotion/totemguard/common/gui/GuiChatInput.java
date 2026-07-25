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

package com.deathmotion.totemguard.common.gui;

import com.deathmotion.totemguard.api.config.key.ConfigKey;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.config.key.MessagesKeys;
import com.deathmotion.totemguard.common.platform.player.PlatformPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;

public final class GuiChatInput {

    private static final long TIMEOUT_SECONDS = 60L;
    private static final String ABORT_WORD = "cancel";
    private static final int MAX_LENGTH = 128;

    private final ConcurrentMap<UUID, Prompt> pending = new ConcurrentHashMap<>();

    public static String abortWord() {
        return ABORT_WORD;
    }

    public boolean waiting(@Nullable UUID viewerId) {
        return viewerId != null && pending.containsKey(viewerId);
    }

    public void request(UUID viewerId, Consumer<String> onText, Runnable onAbort) {
        Prompt prompt = new Prompt(onText, onAbort);
        drop(viewerId);
        pending.put(viewerId, prompt);
        TGPlatform.getInstance().getScheduler().runAsyncTaskDelayed(() -> {
            if (!pending.remove(viewerId, prompt)) return;
            tell(viewerId, MessagesKeys.GUI_CHAT_INPUT_EXPIRED);
        }, TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    public boolean deliver(UUID viewerId, String text) {
        Prompt prompt = pending.remove(viewerId);
        if (prompt == null) return false;

        String trimmed = text == null ? "" : text.trim();
        if (trimmed.isEmpty() || trimmed.equalsIgnoreCase(ABORT_WORD)) {
            tell(viewerId, MessagesKeys.GUI_CHAT_INPUT_CANCELLED);
            run(prompt.onAbort());
            return true;
        }

        String capped = trimmed.length() > MAX_LENGTH ? trimmed.substring(0, MAX_LENGTH) : trimmed;
        run(() -> prompt.onText().accept(capped));
        return true;
    }

    public void drop(@Nullable UUID viewerId) {
        if (viewerId != null) pending.remove(viewerId);
    }

    public void shutdown() {
        pending.clear();
    }

    private void run(Runnable action) {
        TGPlatform platform = TGPlatform.getInstance();
        platform.getScheduler().runAsyncTask(() -> {
            try {
                action.run();
            } catch (Exception failure) {
                platform.getLogger().log(Level.WARNING, "Failed to apply chat input", failure);
            }
        });
    }

    private void tell(UUID viewerId, ConfigKey<String> key) {
        TGPlatform platform = TGPlatform.getInstance();
        PlatformPlayer viewer = platform.getPlatformPlayerFactory().create(viewerId);
        if (viewer == null) return;
        viewer.sendMessage(platform.getMessageService().getComponent(key));
    }

    private record Prompt(Consumer<String> onText, Runnable onAbort) {
    }
}
