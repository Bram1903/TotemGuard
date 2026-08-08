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

package com.deathmotion.totemguard.paper.player;

import com.deathmotion.totemguard.common.platform.player.PlatformPlayer;
import com.deathmotion.totemguard.common.platform.player.PlatformPlayerFactory;
import com.deathmotion.totemguard.paper.scheduler.PaperScheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class PaperPlatformPlayerFactory implements PlatformPlayerFactory {

    private final Plugin plugin;
    private final PaperScheduler scheduler;

    public PaperPlatformPlayerFactory(Plugin plugin, PaperScheduler scheduler) {
        this.plugin = plugin;
        this.scheduler = scheduler;
    }

    @Override
    public @Nullable PlatformPlayer create(@NotNull UUID uuid) {
        Player nativePlayer = Bukkit.getPlayer(uuid);
        if (nativePlayer == null) return null;
        return new PaperPlatformPlayer(nativePlayer, plugin, scheduler);
    }
}
