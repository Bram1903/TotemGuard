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

package com.deathmotion.totemguard.testplugin.events;

import com.deathmotion.totemguard.api.events.FlagEvent;
import com.deathmotion.totemguard.testplugin.ApiTestPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class FlagEventTest implements Listener {

    private final ApiTestPlugin plugin;

    public FlagEventTest(ApiTestPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onFlagEvent(FlagEvent event) {
        plugin.getLogger().info("Flag event triggered for player " + event.getTotemUser().getName() + " with check " + event.getCheck().getCheckName());
    }
}
