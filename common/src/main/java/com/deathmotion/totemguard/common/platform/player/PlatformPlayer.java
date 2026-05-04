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

    /**
     * Starts a manual-check window on the target's region thread:
     * <ol>
     *   <li>snapshots inventory, cursor, held-slot, health, food, saturation, and active potion effects,</li>
     *   <li>forces enough damage to pop the offhand totem — this is what drives the natural totem animation,
     *       sound, and regen/absorption buffs, so the target experiences it as real combat damage.</li>
     * </ol>
     *
     * <p>If the damage is actually applied, invokes {@code onStarted} with a handle whose
     * {@link ManualCheckHandle#restore()} method reverts the full snapshot — including stripping
     * the regen/fire-resist/absorption buffs the totem granted, so the check leaves no visible trace.</p>
     *
     * <p>If the damage was refused (e.g. a damage-cap plugin or anticheat cancels the
     * {@code EntityDamageEvent}), {@code onDamageRefused} is invoked instead, the snapshot is
     * discarded untouched, and no handle is produced.</p>
     */
    void beginManualCheck(@NotNull Consumer<@NotNull ManualCheckHandle> onStarted,
                          @NotNull Runnable onDamageRefused);
}
