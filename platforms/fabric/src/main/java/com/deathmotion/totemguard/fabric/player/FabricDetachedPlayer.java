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

import com.deathmotion.totemguard.common.platform.player.DetachedPlayer;
import com.deathmotion.totemguard.common.platform.player.WorldAnchor;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

final class FabricDetachedPlayer implements DetachedPlayer {

    private final ServerPlayer player;
    private final MinecraftServer server;
    private final AtomicBoolean released = new AtomicBoolean();

    private final ServerLevel level;
    private final Vec3 home;
    private final float yaw;
    private final float pitch;
    private final GameType gameMode;
    private final boolean invulnerable;

    private FabricDetachedPlayer(ServerPlayer player, MinecraftServer server) {
        this.player = player;
        this.server = server;
        this.level = player.level();
        this.home = player.position();
        this.yaw = player.getYRot();
        this.pitch = player.getXRot();
        this.gameMode = player.gameMode.getGameModeForPlayer();
        this.invulnerable = player.isInvulnerable();
    }

    static DetachedPlayer detach(ServerPlayer player, MinecraftServer server) {
        FabricDetachedPlayer detached = new FabricDetachedPlayer(player, server);
        server.execute(detached::park);
        return detached;
    }

    private void park() {
        if (player.hasDisconnected()) return;
        player.closeContainer();
        player.setGameMode(GameType.SPECTATOR);
        player.setInvulnerable(true);
        ClientboundPlayerInfoRemovePacket removal =
                new ClientboundPlayerInfoRemovePacket(List.of(player.getUUID()));
        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            if (other != player) other.connection.send(removal);
        }
    }

    @Override
    public @Nullable WorldAnchor anchor() {
        ChunkPos at = player.chunkPosition();
        return new WorldAnchor(level.dimension().identifier().toString(),
                at.x(), at.z(), server.getPlayerList().getViewDistance(), gameMode.name());
    }

    @Override
    public void hideFrom(@NotNull UUID joined) {
        if (released.get()) return;
        server.execute(() -> {
            ServerPlayer other = server.getPlayerList().getPlayer(joined);
            if (other == null || other == player) return;
            other.connection.send(new ClientboundPlayerInfoRemovePacket(List.of(player.getUUID())));
        });
    }

    @Override
    public void reattach(@NotNull Runnable whenBack) {
        if (!released.compareAndSet(false, true)) return;
        restoreParked();
        server.execute(() -> {
            restore();
            whenBack.run();
        });
    }

    private void restoreParked() {
        try {
            player.setGameMode(gameMode);
            player.setInvulnerable(invulnerable);
        } catch (RuntimeException ignored) {
        }
    }

    private void restore() {
        if (player.hasDisconnected()) return;


        ClientboundPlayerInfoUpdatePacket listing =
                ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(player));
        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            if (other != player) other.connection.send(listing);
        }
        player.connection.send(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(
                server.getPlayerList().getPlayers()));

        restoreParked();
        player.teleportTo(level, home.x(), home.y(), home.z(), Set.<Relative>of(), yaw, pitch, true);
        resendColumns();
        player.containerMenu.sendAllDataToRemote();
        player.connection.send(new ClientboundSetDefaultSpawnPositionPacket(level.getRespawnData()));
        player.resetSentInfo();
    }

    private void resendColumns() {
        int radius = server.getPlayerList().getViewDistance();
        ChunkPos center = player.chunkPosition();
        player.connection.send(new ClientboundSetChunkCacheRadiusPacket(radius));
        player.connection.send(new ClientboundSetChunkCacheCenterPacket(center.x(), center.z()));
        for (int x = center.x() - radius; x <= center.x() + radius; x++) {
            for (int z = center.z() - radius; z <= center.z() + radius; z++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(x, z);
                if (chunk == null) continue;
                player.connection.send(new ClientboundLevelChunkWithLightPacket(
                        chunk, level.getLightEngine(), null, null));
            }
        }
    }
}
