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

package com.deathmotion.totemguard.common;

import com.deathmotion.totemguard.api3.TotemGuardAPI;
import com.deathmotion.totemguard.api3.alert.AlertRepository;
import com.deathmotion.totemguard.api3.config.ConfigRepository;
import com.deathmotion.totemguard.api3.event.EventRepository;
import com.deathmotion.totemguard.api3.placeholder.PlaceholderRepository;
import com.deathmotion.totemguard.api3.user.UserRepository;
import com.deathmotion.totemguard.api3.versioning.TGVersion;
import com.deathmotion.totemguard.common.util.TGVersions;
import org.jetbrains.annotations.NotNull;

public final class TGPlatformAPI implements TotemGuardAPI {

    private final TGPlatform platform;

    public TGPlatformAPI() {
        this.platform = TGPlatform.getInstance();
    }

    @Override
    public @NotNull TGVersion getVersion() {
        return TGVersions.CURRENT;
    }

    @Override
    public @NotNull EventRepository getEventRepository() {
        return platform.getEventRepository();
    }

    @Override
    public @NotNull ConfigRepository getConfigRepository() {
        return platform.getConfigRepository();
    }

    @Override
    public @NotNull UserRepository getUserRepository() {
        return platform.getPlayerRepository();
    }

    @Override
    public @NotNull PlaceholderRepository getPlaceholderRepository() {
        return platform.getPlaceholderRepository();
    }

    @Override
    public @NotNull AlertRepository getAlertRepository() {
        return platform.getAlertRepository();
    }
}
