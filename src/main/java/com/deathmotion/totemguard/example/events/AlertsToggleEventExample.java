/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2025 Bram and contributors
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

package com.deathmotion.totemguard.example.events;

import com.deathmotion.totemguard.api.events.AlertsToggleEvent;
import com.deathmotion.totemguard.example.TotemGuardApiExample;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class AlertsToggleEventExample implements Listener {

    private final TotemGuardApiExample plugin;

    public AlertsToggleEventExample(TotemGuardApiExample plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onAlertsToggle(AlertsToggleEvent event) {
        plugin.getLogger().info("Alerts toggled for player " + event.getPlayer().getName() + " to " + event.isEnabled());
    }
}