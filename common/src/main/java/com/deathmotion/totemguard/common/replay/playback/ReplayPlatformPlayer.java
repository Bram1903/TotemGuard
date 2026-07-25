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

package com.deathmotion.totemguard.common.replay.playback;

import com.deathmotion.totemguard.common.platform.player.ManualCheckHandle;
import com.deathmotion.totemguard.common.platform.player.PlatformPlayer;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

final class ReplayPlatformPlayer implements PlatformPlayer {

    private static final String WORLD = "replay";

    @Override
    public boolean hasPermission(@NotNull String permission) {
        return false;
    }

    @Override
    public void sendMessage(@NotNull Component message) {
    }

    @Override
    public void kick(@NotNull Component reason) {
    }

    @Override
    public boolean isInSurvivalOrAdventure() {
        return true;
    }

    @Override
    public boolean isInvulnerable() {
        return false;
    }

    @Override
    public @Nullable String getWorldName() {
        return WORLD;
    }

    @Override
    public void teleport(@NotNull String worldName, double x, double y, double z, float yaw, float pitch) {
    }

    @Override
    public void stopRiding() {
    }

    @Override
    public void resetFallDistance() {
    }

    @Override
    public boolean dealFallDamage(double amount) {
        return true;
    }

    @Override
    public void beginManualCheck(@NotNull Consumer<@NotNull ManualCheckHandle> onStarted,
                                 @NotNull Runnable onDamageRefused) {
        onDamageRefused.run();
    }

    @Override
    public void resyncInventoryToClient() {
    }

    @Override
    public @Nullable String clientBrandName() {
        return null;
    }
}
