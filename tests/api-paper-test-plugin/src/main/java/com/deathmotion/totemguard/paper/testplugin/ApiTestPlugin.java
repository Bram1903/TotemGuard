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

package com.deathmotion.totemguard.paper.testplugin;

import com.deathmotion.totemguard.api.TotemGuard;
import com.deathmotion.totemguard.api.TotemGuardAPI;
import com.deathmotion.totemguard.api.event.EventSubscription;
import com.deathmotion.totemguard.api.event.impl.TGPluginShutdownEvent;
import com.deathmotion.totemguard.api.event.impl.TGUserFlagEvent;
import com.deathmotion.totemguard.api.event.impl.TGUserJoinEvent;
import com.deathmotion.totemguard.api.event.impl.TGUserQuitEvent;
import com.deathmotion.totemguard.paper.testplugin.events.TGFlagEventListener;
import com.deathmotion.totemguard.paper.testplugin.events.TGUserJoinEventListener;
import com.deathmotion.totemguard.paper.testplugin.events.TGUserQuitEventListener;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

@Getter
public final class ApiTestPlugin extends JavaPlugin {

    @Getter
    private static ApiTestPlugin instance;
    private final List<EventSubscription> subscriptions = new ArrayList<>();
    private volatile boolean disabled;

    public ApiTestPlugin() {
        instance = this;
    }

    @Override
    public void onEnable() {
        hook();
    }

    @Override
    public void onDisable() {
        disabled = true;
        closeSubscriptions();
    }

    private void hook() {
        TotemGuard.getAsync().thenAccept(api -> {
            if (disabled) return;
            getLogger().info("Hooked into TotemGuard version " + api.getVersion() + ".");
            subscribeAll(api);
        });
    }

    private void subscribeAll(TotemGuardAPI api) {
        subscriptions.add(api.getEventRepository().subscribe(TGUserJoinEvent.class, new TGUserJoinEventListener()));
        subscriptions.add(api.getEventRepository().subscribe(TGUserQuitEvent.class, new TGUserQuitEventListener()));
        subscriptions.add(api.getEventRepository().subscribe(TGUserFlagEvent.class, new TGFlagEventListener()));
        subscriptions.add(api.getEventRepository().subscribe(TGPluginShutdownEvent.class, this::onTotemGuardShutdown));
    }

    private void onTotemGuardShutdown(TGPluginShutdownEvent event) {
        TGPluginShutdownEvent.Reason reason = event.getReason();
        getLogger().info("TotemGuard is shutting down (reason: " + reason + "). Dropping cached subscriptions.");
        subscriptions.clear();

        if (reason == TGPluginShutdownEvent.Reason.LOADER_RESTART || reason == TGPluginShutdownEvent.Reason.UPDATE_TRIGGERED) {
            // getAsync() returns a fresh future after TotemGuard.shutdown() arms it,
            // and completes once the loader publishes the new instance.
            hook();
        }
    }

    private void closeSubscriptions() {
        for (EventSubscription subscription : subscriptions) {
            try {
                subscription.close();
            } catch (Exception ignored) {
            }
        }
        subscriptions.clear();
    }
}
