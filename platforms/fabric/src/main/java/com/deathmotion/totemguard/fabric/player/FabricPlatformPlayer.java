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

import com.deathmotion.totemguard.common.platform.player.ManualCheckHandle;
import com.deathmotion.totemguard.common.platform.player.PlatformPlayer;
import com.deathmotion.totemguard.fabric.FabricServerHolder;
import lombok.Getter;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.kyori.adventure.platform.modcommon.MinecraftServerAudiences;
import net.kyori.adventure.text.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.GameType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.function.Consumer;

public class FabricPlatformPlayer implements PlatformPlayer {

    @Getter
    private final ServerPlayer fabricPlayer;

    public FabricPlatformPlayer(ServerPlayer fabricPlayer) {
        this.fabricPlayer = fabricPlayer;
    }

    private static ServerLevel findLevel(MinecraftServer server, String worldName) {
        for (ServerLevel level : server.getAllLevels()) {
            if (level.dimension().identifier().toString().equals(worldName)) return level;
        }
        return null;
    }

    @Override
    public boolean hasPermission(@NotNull String permission) {
        return Permissions.check(fabricPlayer, permission, PermissionLevel.GAMEMASTERS);
    }

    @Override
    public void sendMessage(@NotNull Component message) {
        MinecraftServer server = resolveServer();
        if (server == null) return;
        MinecraftServerAudiences.of(server).audience(fabricPlayer).sendMessage(message);
    }

    @Override
    public void kick(@NotNull Component reason) {
        MinecraftServer server = resolveServer();
        if (server == null) return;
        fabricPlayer.connection.disconnect(MinecraftServerAudiences.of(server).asNative(reason));
    }

    @Override
    public boolean isInSurvivalOrAdventure() {
        GameType mode = fabricPlayer.gameMode.getGameModeForPlayer();
        return mode == GameType.SURVIVAL || mode == GameType.ADVENTURE;
    }

    @Override
    public boolean isInvulnerable() {
        return fabricPlayer.isInvulnerable();
    }

    @Override
    public String getWorldName() {
        return fabricPlayer.level().dimension().identifier().toString();
    }

    @Override
    public void teleport(@NotNull String worldName, double x, double y, double z, float yaw, float pitch) {
        MinecraftServer server = resolveServer();
        if (server == null) return;
        server.execute(() -> {
            ServerLevel destination = findLevel(server, worldName);
            if (destination == null) destination = fabricPlayer.level();
            fabricPlayer.teleportTo(destination, x, y, z, Set.<Relative>of(), yaw, pitch, true);
        });
    }

    @Override
    public void beginManualCheck(@NotNull Consumer<@NotNull ManualCheckHandle> onStarted, @NotNull Runnable onDamageRefused) {
        onDamageRefused.run();
    }

    @Override
    public void resyncInventoryToClient() {
        MinecraftServer server = resolveServer();
        if (server == null) return;
        server.execute(() -> {
            if (fabricPlayer.hasDisconnected()) return;
            fabricPlayer.containerMenu.sendAllDataToRemote();
        });
    }

    @Override
    public @Nullable String clientBrandName() {
        return null;
    }

    private MinecraftServer resolveServer() {
        MinecraftServer cached = FabricServerHolder.server();
        return cached != null ? cached : fabricPlayer.level().getServer();
    }
}
