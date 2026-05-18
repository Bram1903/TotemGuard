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

package com.deathmotion.totemguard.paper.testplugin.listeners;

import com.deathmotion.totemguard.api.event.events.TGUserInventoryCloseEvent;
import com.deathmotion.totemguard.api.event.events.TGUserInventoryOpenEvent;
import com.deathmotion.totemguard.api.event.events.TGUserQuitEvent;
import com.deathmotion.totemguard.api.user.TGUser;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

/**
 * Listens to a user's inventory open and close events and logs a short
 * status line each time. Tracks per-user open counts and the timestamp of
 * the last open so the close handler can report how long the inventory
 * stayed open. Demonstrates keeping listener state in a dedicated class
 * and cleaning it up on quit so the maps do not grow forever.
 */
public final class InventoryActivityListener {

    private final Logger logger;
    private final ConcurrentMap<UUID, Long> openedAt = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Integer> openCounts = new ConcurrentHashMap<>();

    public InventoryActivityListener(Logger logger) {
        this.logger = logger;
    }

    public void onOpen(TGUserInventoryOpenEvent event) {
        TGUser user = event.getUser();
        int count = openCounts.merge(user.getUuid(), 1, Integer::sum);
        openedAt.put(user.getUuid(), System.currentTimeMillis());
        logger.info("[inventory] " + user.getName() + " opened by "
                + (event.isServerInitiated() ? "server" : "client")
                + " (session count: " + count + ")");
    }

    public void onClose(TGUserInventoryCloseEvent event) {
        TGUser user = event.getUser();
        Long openTs = openedAt.remove(user.getUuid());
        String duration = openTs == null ? "" : " after " + (System.currentTimeMillis() - openTs) + "ms";
        logger.info("[inventory] " + user.getName() + " closed by "
                + (event.isServerInitiated() ? "server" : "client") + duration);
    }

    /**
     * Drops cached state for a user that just left. Without this the
     * per-UUID maps would keep growing for the life of the JVM.
     */
    public void onQuit(TGUserQuitEvent event) {
        UUID uuid = event.getUser().getUuid();
        openedAt.remove(uuid);
        openCounts.remove(uuid);
    }
}
