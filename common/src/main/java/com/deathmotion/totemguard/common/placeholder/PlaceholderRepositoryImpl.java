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

package com.deathmotion.totemguard.common.placeholder;

import com.deathmotion.totemguard.api3.check.Check;
import com.deathmotion.totemguard.api3.placeholder.PlaceholderContext;
import com.deathmotion.totemguard.api3.placeholder.PlaceholderHolder;
import com.deathmotion.totemguard.api3.placeholder.PlaceholderRepository;
import com.deathmotion.totemguard.api3.user.TGUser;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.check.CheckImpl;
import com.deathmotion.totemguard.common.placeholder.engine.InternalContext;
import com.deathmotion.totemguard.common.placeholder.engine.PlaceholderEngine;
import com.deathmotion.totemguard.common.placeholder.holder.HolderRegistry;
import com.deathmotion.totemguard.common.placeholder.holder.InternalPlaceholderHolder;
import com.deathmotion.totemguard.common.placeholder.holder.impl.CheckPlaceholders;
import com.deathmotion.totemguard.common.placeholder.holder.impl.PlatformPlaceholders;
import com.deathmotion.totemguard.common.placeholder.holder.impl.PlayerPlaceholders;
import com.deathmotion.totemguard.common.player.TGPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PlaceholderRepositoryImpl implements PlaceholderRepository {

    private final TGPlatform platform;

    private final HolderRegistry<InternalPlaceholderHolder> internalHolders = new HolderRegistry<>();
    private final HolderRegistry<PlaceholderHolder> apiHolders = new HolderRegistry<>();

    private volatile PlaceholderEngine engine;

    public PlaceholderRepositoryImpl() {
        this.platform = TGPlatform.getInstance();

        internalHolders.register(new PlatformPlaceholders());
        internalHolders.register(new PlayerPlaceholders());
        internalHolders.register(new CheckPlaceholders());

        rebuildEngine();
    }

    public boolean registerInternal(@NotNull InternalPlaceholderHolder holder) {
        boolean changed = internalHolders.register(holder);
        if (changed) rebuildEngine();
        return changed;
    }

    public boolean unregisterInternal(@NotNull InternalPlaceholderHolder holder) {
        boolean changed = internalHolders.unregister(holder);
        if (changed) rebuildEngine();
        return changed;
    }

    @Override
    public boolean registerHolder(@NotNull PlaceholderHolder holder) {
        boolean changed = apiHolders.register(holder);
        if (changed) rebuildEngine();
        return changed;
    }

    @Override
    public boolean unregisterHolder(@NotNull PlaceholderHolder holder) {
        boolean changed = apiHolders.unregister(holder);
        if (changed) rebuildEngine();
        return changed;
    }

    private void rebuildEngine() {
        this.engine = new PlaceholderEngine(
                internalHolders.snapshot(),
                apiHolders.snapshot()
        );
    }

    @Override
    public @NotNull String replace(@NotNull String message, @NotNull PlaceholderContext context) {
        TGPlayer player = adaptUser(context.user());
        CheckImpl internalCheck = adaptCheck(context.check());
        return engine.replace(
                message,
                new InternalContext(platform, player, internalCheck),
                context
        );
    }

    public @NotNull String replace(@NotNull String message,
                                   @Nullable TGPlayer player,
                                   @Nullable CheckImpl check,
                                   @NotNull Map<String, Object> extras) {
        return engine.replace(
                message,
                new InternalContext(platform, player, check),
                new PlaceholderContext(player, check, extras)
        );
    }

    public @NotNull String replace(@NotNull String message,
                                   @Nullable TGPlayer player,
                                   @Nullable CheckImpl check) {
        return replace(message, player, check, Map.of());
    }

    @Override
    public @NotNull Set<String> registeredKeys() {
        return engine.registeredKeys();
    }

    @Override
    public @NotNull Set<String> registeredPatterns() {
        return engine.registeredPatterns();
    }

    private @Nullable TGPlayer adaptUser(@Nullable TGUser user) {
        if (user == null) return null;
        if (user instanceof TGPlayer player) return player;

        UUID uuid = user.getUuid();
        return platform.getPlayerRepository().getPlayer(uuid);
    }

    private @Nullable CheckImpl adaptCheck(@Nullable Check check) {
        return check instanceof CheckImpl impl ? impl : null;
    }
}
