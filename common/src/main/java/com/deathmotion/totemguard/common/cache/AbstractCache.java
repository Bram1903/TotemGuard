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

package com.deathmotion.totemguard.common.cache;

import com.deathmotion.totemguard.common.cache.data.AlertsToggleData;
import com.deathmotion.totemguard.common.cache.data.CheckSnapshot;
import com.deathmotion.totemguard.common.cache.data.VPNData;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public interface AbstractCache {

    @Blocking
    void saveCheckSnapshot(@NotNull UUID uuid, @NotNull List<CheckSnapshot> checkSnapshots);

    @Blocking
    @Nullable
    List<CheckSnapshot> getCheckSnapshot(@NotNull UUID uuid);

    @Blocking
    void saveAlertsToggleData(@NotNull UUID uuid, @NotNull AlertsToggleData alertsToggleData);

    @Blocking
    @Nullable
    AlertsToggleData getAlertsToggleData(@NotNull UUID uuid);

    @Blocking
    void saveVPNData(@NotNull String ip, @NotNull VPNData vpnData);

    @Blocking
    @Nullable
    VPNData getVPNData(@NotNull String ip);
}
