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

package com.deathmotion.totemguard.velocity;

import com.deathmotion.totemguard.velocity.player.VelocityPlatformUserFactory;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.Getter;

import java.nio.file.Path;

public class TGVelocity {

    private final ProxyServer server;
    private final Path dataDirectory;

    @Getter
    private final TGVelocityPlatform tg;

    @Getter
    private final VelocityPlatformUserFactory platformUserFactory;

    @Inject
    public TGVelocity(ProxyServer server, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.dataDirectory = dataDirectory;

        this.platformUserFactory = new VelocityPlatformUserFactory(server);
        this.tg = new TGVelocityPlatform(this);
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent ignoredEvent) {
        tg.commonOnInitialize();
        tg.commonOnEnable();
    }

    @Subscribe()
    public void onProxyShutdown(ProxyShutdownEvent ignoredEvent) {
        tg.commonOnDisable();
    }
}