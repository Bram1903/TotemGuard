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

package com.deathmotion.totemguard.fabric.sender;

import com.deathmotion.totemguard.common.platform.sender.Sender;
import com.deathmotion.totemguard.common.platform.sender.SenderFactory;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.kyori.adventure.platform.modcommon.MinecraftServerAudiences;
import net.kyori.adventure.text.Component;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionLevel;

import java.util.UUID;

public final class FabricSenderFactory extends SenderFactory<CommandSourceStack> {

    @Override
    protected String getName(CommandSourceStack source) {
        return source.isPlayer() ? source.getTextName() : Sender.CONSOLE_NAME;
    }

    @Override
    protected UUID getUniqueId(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        return player != null ? player.getUUID() : Sender.CONSOLE_UUID;
    }

    @Override
    protected void sendMessage(CommandSourceStack source, String message) {
        sendMessage(source, Component.text(message));
    }

    @Override
    protected void sendMessage(CommandSourceStack source, Component message) {
        MinecraftServer server = source.getServer();
        if (server == null) return;
        MinecraftServerAudiences.of(server).audience(source).sendMessage(message);
    }

    @Override
    protected boolean hasPermission(CommandSourceStack source, String node) {
        // Routes through fabric-permissions-api: if a permission backend (LuckPerms,
        // etc.) is installed, it answers; otherwise we fall back to GAMEMASTERS
        // (op level 2), which lines up with how vanilla gates moderation commands.
        return Permissions.check(source, node, PermissionLevel.GAMEMASTERS);
    }

    @Override
    protected boolean hasPermission(CommandSourceStack source, String node, boolean defaultIfUnset) {
        // The boolean overload of `Permissions.check` already encodes the
        // "default when unset" semantics, use it directly so we don't have to
        // map true/false to op-level proxies.
        return Permissions.check(source, node, defaultIfUnset);
    }

    @Override
    protected void performCommand(CommandSourceStack source, String command) {
        MinecraftServer server = source.getServer();
        if (server == null) return;
        server.getCommands().performPrefixedCommand(source, command);
    }

    @Override
    protected boolean isConsole(CommandSourceStack source) {
        return !source.isPlayer();
    }

    @Override
    protected boolean isPlayer(CommandSourceStack source) {
        return source.isPlayer();
    }
}
