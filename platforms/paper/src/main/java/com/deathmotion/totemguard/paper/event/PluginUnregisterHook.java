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

package com.deathmotion.totemguard.paper.event;

import com.deathmotion.totemguard.common.event.EventBusImpl;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;

/**
 * Catches {@link PluginDisableEvent} so any third-party plugin that subscribed
 * to TotemGuard's event bus with itself as the plugin context has its handlers
 * cleared automatically when Bukkit disables it. Mirrors how
 * {@code HandlerList} drops listeners on plugin disable, so consumers that
 * forget to call {@code unregisterAll} do not leak lambdas across reloads.
 */
public final class PluginUnregisterHook implements Listener {

    private final EventBusImpl eventBus;
    private final Plugin self;

    public PluginUnregisterHook(EventBusImpl eventBus, Plugin self) {
        this.eventBus = eventBus;
        this.self = self;
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        Plugin disabled = event.getPlugin();
        if (disabled == self) return;
        eventBus.unregisterAll(disabled);
    }
}
