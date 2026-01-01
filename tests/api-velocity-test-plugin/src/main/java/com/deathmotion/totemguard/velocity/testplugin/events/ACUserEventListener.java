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

package com.deathmotion.totemguard.velocity.testplugin.events;

import com.deathmotion.totemguard.api.TotemGuardAPI;
import com.deathmotion.totemguard.api.event.EventSubscription;
import com.deathmotion.totemguard.api.event.impl.TGUserJoinEvent;
import com.deathmotion.totemguard.api.event.impl.TGUserQuitEvent;
import com.deathmotion.totemguard.velocity.testplugin.ApiTestPlugin;

public class ACUserEventListener {

    private final ApiTestPlugin plugin;

    private final EventSubscription userJoinSub;
    private final EventSubscription userQuitSub;

    public ACUserEventListener(ApiTestPlugin plugin, TotemGuardAPI api) {
        this.plugin = plugin;

        this.userJoinSub = api.getEventRepository().subscribe(TGUserJoinEvent.class, this::onUserJoin);
        this.userQuitSub = api.getEventRepository().subscribe(TGUserQuitEvent.class, this::onUserQuit);
    }

    public void unregister() {
        userJoinSub.unsubscribe();
        userQuitSub.unsubscribe();
    }

    private void onUserJoin(TGUserJoinEvent event) {
        plugin.getLogger().info("User {} joined the server.", event.getUser().getName());
    }

    private void onUserQuit(TGUserQuitEvent event) {
        plugin.getLogger().info("User {} quit the server.", event.getUser().getName());
    }
}
