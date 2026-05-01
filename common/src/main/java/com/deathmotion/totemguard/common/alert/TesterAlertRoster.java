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

package com.deathmotion.totemguard.common.alert;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.config.key.MessagesKeys;
import com.deathmotion.totemguard.common.message.MessageService;
import com.deathmotion.totemguard.common.platform.player.PlatformUser;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory subscription list for "tester" alerts: staff who only want to see flags
 * triggered by themselves. Permission-gated by {@code TotemGuard.Tester}; never
 * persisted and never synced over Redis (the violator is always local to the
 * receiver, by definition).
 */
public final class TesterAlertRoster {

    private final ConcurrentHashMap<UUID, PlatformUser> subscribers = new ConcurrentHashMap<>();

    public boolean contains(UUID uuid) {
        return subscribers.containsKey(uuid);
    }

    public @Nullable PlatformUser get(UUID uuid) {
        return subscribers.get(uuid);
    }

    public void enable(UUID uuid, PlatformUser user) {
        if (subscribers.putIfAbsent(uuid, user) != null) return;
        user.sendMessage(messageService().getComponent(MessagesKeys.TESTER_ENABLED));
    }

    public void disable(UUID uuid) {
        PlatformUser removed = subscribers.remove(uuid);
        if (removed == null) return;
        removed.sendMessage(messageService().getComponent(MessagesKeys.TESTER_DISABLED));
    }

    /**
     * Silent removal — used for disconnect cleanup, no toggle notification.
     */
    public void clear(UUID uuid) {
        subscribers.remove(uuid);
    }

    private MessageService messageService() {
        return TGPlatform.getInstance().getMessageService();
    }
}
