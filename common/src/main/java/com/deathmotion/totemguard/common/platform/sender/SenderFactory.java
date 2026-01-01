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

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

/**
 * Factory class to make a thread-safe sender instance
 *
 * @param <T> the command sender type
 */
public abstract class SenderFactory<T> {
    protected abstract UUID getUniqueId(T sender);

    protected abstract String getName(T sender);

    protected abstract void sendMessage(T sender, String message);

    protected abstract void sendMessage(T sender, Component message);

    protected abstract boolean hasPermission(T sender, String node);

    protected abstract boolean hasPermission(T sender, String node, boolean defaultIfUnset);

    protected abstract void performCommand(T sender, String command);

    protected abstract boolean isConsole(T sender);

    protected abstract boolean isPlayer(T sender);

    protected boolean shouldSplitNewlines(T sender) {
        return isConsole(sender);
    }

    public final @NotNull Sender wrap(@NotNull T sender) {
        Objects.requireNonNull(sender, "sender");
        return new AbstractSender<>(this, sender);
    }

    @SuppressWarnings("unchecked")
    public final @NotNull T unwrap(@NotNull Sender sender) {
        Objects.requireNonNull(sender, "sender");
        return (T) sender.getNativeSender();
    }
}
