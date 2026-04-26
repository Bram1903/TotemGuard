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
import lombok.Getter;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class FabricPlatformPlayer implements PlatformPlayer {

    @Getter
    private final ServerPlayer fabricPlayer;

    public FabricPlatformPlayer(ServerPlayer fabricPlayer) {
        this.fabricPlayer = fabricPlayer;
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
    public void beginManualCheck(@NotNull Consumer<@NotNull ManualCheckHandle> onStarted, @NotNull Runnable onDamageRefused) {
        onDamageRefused.run();
    }
}
