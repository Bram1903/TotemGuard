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

package com.deathmotion.totemguard.fabric.player;

import com.deathmotion.totemguard.common.platform.player.PlatformUser;
import com.deathmotion.totemguard.fabric.FabricServerHolder;
import lombok.Getter;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.kyori.adventure.platform.modcommon.MinecraftServerAudiences;
import net.kyori.adventure.text.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionLevel;

public class FabricPlatformUser implements PlatformUser {

    @Getter
    private final ServerPlayer fabricPlayer;

    public FabricPlatformUser(ServerPlayer fabricPlayer) {
        this.fabricPlayer = fabricPlayer;
    }

    @Override
    public boolean hasPermission(String permission) {
        return Permissions.check(fabricPlayer, permission, PermissionLevel.GAMEMASTERS);
    }

    @Override
    public void sendMessage(Component message) {
        MinecraftServer server = resolveServer();
        if (server == null) return;
        MinecraftServerAudiences.of(server).audience(fabricPlayer).sendMessage(message);
    }

    @Override
    public void kick(Component reason) {
        MinecraftServer server = resolveServer();
        if (server == null) return;
        fabricPlayer.connection.disconnect(MinecraftServerAudiences.of(server).asNative(reason));
    }

    /**
     * MC 1.21.5+ removed {@code ServerPlayer#getServer()} — the {@code server} field
     * is now private with no accessor. We pull the live MinecraftServer reference from
     * {@link FabricServerHolder} instead, which captures it on SERVER_STARTED. Falling
     * back to {@code fabricPlayer.level().getServer()} is also valid but adds a method
     * chain when the holder already has the same singleton.
     */
    private MinecraftServer resolveServer() {
        MinecraftServer cached = FabricServerHolder.server();
        return cached != null ? cached : fabricPlayer.level().getServer();
    }
}
