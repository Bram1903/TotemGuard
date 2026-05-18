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

// Copied straight from Grim / Axionize as it looks well implemented, and I am not trying to reinvent the wheel

package com.deathmotion.totemguard.common.platform.sender;

import com.deathmotion.totemguard.common.player.TGPlayer;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface Sender {

    UUID CONSOLE_UUID = new UUID(0, 0); // 00000000-0000-0000-0000-000000000000

    String CONSOLE_NAME = "Console";

    String getName();

    UUID getUniqueId();

    void sendMessage(String message);

    void sendMessage(Component message);

    boolean hasPermission(String permission);

    boolean hasPermission(String permission, boolean defaultIfUnset);

    void performCommand(String commandLine);

    boolean isConsole();

    boolean isPlayer();

    default boolean isValid() {
        return true;
    }

    @NotNull Object getNativeSender();

    @Nullable TGPlayer getTGPlayer();
}
