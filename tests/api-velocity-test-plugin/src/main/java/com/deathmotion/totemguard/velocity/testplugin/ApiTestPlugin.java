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

package com.deathmotion.totemguard.velocity.testplugin;

import com.deathmotion.totemguard.api.TotemGuard;
import com.deathmotion.totemguard.velocity.testplugin.events.ACUserEventListener;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public final class ApiTestPlugin {

    @Getter
    private final Logger logger;

    private @Nullable ACUserEventListener acUserEventListener;

    @Inject
    public ApiTestPlugin(Logger logger) {
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent ignoredEvent) {
        TotemGuard.getAsync().thenAccept(api -> {
            logger.info("Hooked into TotemGuard version {}.", api.getVersion());

            acUserEventListener = new ACUserEventListener(this, api);
        });
    }

    @Subscribe
    public void onProxyShutdown(ProxyInitializeEvent ignoredEvent) {
        if (acUserEventListener != null) {
            acUserEventListener.unregister();
            acUserEventListener = null;
        }
    }
}
