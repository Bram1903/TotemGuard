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

package com.deathmotion.totemguard.velocity.sender;

import com.deathmotion.totemguard.common.platform.sender.Sender;
import com.deathmotion.totemguard.common.platform.sender.SenderFactory;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import org.incendo.cloud.SenderMapper;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class VelocitySenderFactory extends SenderFactory<CommandSource> implements SenderMapper<CommandSource, Sender> {

    @Override
    protected String getName(final CommandSource sender) {
        if (sender instanceof Player) {
            return ((Player) sender).getUsername();
        }
        return Sender.CONSOLE_NAME;
    }

    @Override
    protected UUID getUniqueId(final CommandSource sender) {
        if (sender instanceof Player) {
            return ((Player) sender).getUniqueId();
        }
        return Sender.CONSOLE_UUID;
    }

    @Override
    protected void sendMessage(final CommandSource sender, final String message) {
        sender.sendPlainMessage(message);
    }

    @Override
    protected void sendMessage(final CommandSource sender, final Component message) {
        sender.sendMessage(message);
    }

    @Override
    protected boolean hasPermission(final CommandSource sender, final String node) {
        return sender.hasPermission(node);
    }

    @Override
    protected boolean hasPermission(final CommandSource sender, final String node, final boolean defaultIfUnset) {
        return sender.hasPermission(node);
    }

    @Override
    protected void performCommand(final CommandSource sender, final String command) {
        throw new UnsupportedOperationException("performCommand is not implemented for Velocity");
    }

    @Override
    protected boolean isConsole(final CommandSource sender) {
        return sender instanceof ConsoleCommandSource;
    }

    @Override
    protected boolean isPlayer(final CommandSource sender) {
        return sender instanceof Player;
    }

    @Override
    public @NotNull Sender map(final @NotNull CommandSource base) {
        return this.wrap(base);
    }

    @Override
    public @NotNull CommandSource reverse(final @NotNull Sender mapped) {
        return this.unwrap(mapped);
    }
}
