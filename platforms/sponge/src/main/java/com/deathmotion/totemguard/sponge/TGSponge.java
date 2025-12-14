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

package com.deathmotion.totemguard.sponge;

import com.google.inject.Inject;
import lombok.Getter;
import org.spongepowered.api.Server;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.builtin.jvm.Plugin;

import java.nio.file.Path;

@Plugin("totemguard")
public class TGSponge {

    private final PluginContainer pluginContainer;

    @Getter
    private final TGSpongePlatform tg;

    @Inject
    public TGSponge(PluginContainer pluginContainer, @ConfigDir(sharedRoot = false) Path configDirectory) {
        this.pluginContainer = pluginContainer;
        this.tg = new TGSpongePlatform(configDirectory);
    }

    @Listener
    public void onServerStart(final StartedEngineEvent<Server> event) {
        tg.commonOnInitialize();
        tg.commonOnEnable();
    }

    @Listener
    public void onServerStop(final StoppingEngineEvent<Server> event) {
        tg.commonOnDisable();
    }
}