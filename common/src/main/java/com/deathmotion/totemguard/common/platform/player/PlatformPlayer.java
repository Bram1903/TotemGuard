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

package com.deathmotion.totemguard.common.platform.player;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public interface PlatformPlayer {

    boolean hasPermission(@NotNull String permission);

    void sendMessage(@NotNull Component message);

    void kick(@NotNull Component reason);

    boolean isInSurvivalOrAdventure();

    boolean isInvulnerable();

    @org.jetbrains.annotations.Nullable
    String getWorldName();

    void teleport(@NotNull String worldName, double x, double y, double z, float yaw, float pitch);

    void stopRiding();

    void resetFallDistance();

    boolean dealFallDamage(double amount);

    void beginManualCheck(@NotNull Consumer<@NotNull ManualCheckHandle> onStarted,
                          @NotNull Runnable onDamageRefused);

    void resyncInventoryToClient();

    @Nullable String clientBrandName();
}
