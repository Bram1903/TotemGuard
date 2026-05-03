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

import com.deathmotion.totemguard.common.platform.player.PlatformPlayer;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RealtimeAlertRoster {

    private final ConcurrentHashMap<UUID, AlertSubscription> subscriptions = new ConcurrentHashMap<>();

    public void put(@NotNull UUID viewerUuid, @NotNull PlatformPlayer viewer,
                    @NotNull AlertFilter filter, @Nullable String displayLabel) {
        subscriptions.put(viewerUuid, new AlertSubscription(viewerUuid, viewer, filter, displayLabel));
    }

    public @Nullable AlertSubscription remove(@NotNull UUID viewerUuid) {
        return subscriptions.remove(viewerUuid);
    }

    public @Nullable AlertSubscription get(@NotNull UUID viewerUuid) {
        return subscriptions.get(viewerUuid);
    }

    public @NotNull List<AlertSubscription> matching(@Nullable UUID violatorUuid) {
        List<AlertSubscription> matches = new ArrayList<>();
        for (AlertSubscription sub : subscriptions.values()) {
            if (sub.filter().accept(violatorUuid)) matches.add(sub);
        }
        return matches;
    }

    public void deliver(@Nullable UUID violatorUuid, @NotNull Component message) {
        for (AlertSubscription sub : subscriptions.values()) {
            if (!sub.filter().accept(violatorUuid)) continue;
            sub.viewer().sendMessage(message);
        }
    }

    public @NotNull List<AlertSubscription> removeAllTargeting(@NotNull UUID target) {
        List<AlertSubscription> removed = new ArrayList<>();
        subscriptions.entrySet().removeIf(entry -> {
            if (entry.getValue().filter() instanceof AlertFilter.Violator violator
                    && violator.target().equals(target)) {
                removed.add(entry.getValue());
                return true;
            }
            return false;
        });
        return removed;
    }
}
