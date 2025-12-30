/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2025 Bram and contributors
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

package com.deathmotion.totemguard.common.check;

import com.deathmotion.totemguard.api.check.Check;
import com.deathmotion.totemguard.api.check.CheckCategory;
import com.deathmotion.totemguard.api.event.impl.TGFlagEvent;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.inventory.PacketInventory;
import com.deathmotion.totemguard.common.reload.Reloadable;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

public class CheckImpl implements Check, Reloadable {

    protected final TGPlayer player;
    protected final PacketInventory inventory;
    protected final Buffer buffer;

    @Getter
    private final String name;
    @Getter
    private final String description;
    @Getter
    private final CheckCategory category;
    @Getter
    private final boolean experimental;

    @Getter
    private boolean enabled = true;
    @Getter
    private int violations;

    public CheckImpl(TGPlayer player) {
        this.player = player;
        this.inventory = player.getInventory();
        this.buffer = new Buffer();

        final Class<?> checkClass = this.getClass();
        if (!checkClass.isAnnotationPresent(CheckData.class)) {
            throw new IllegalStateException("Check class " + checkClass.getName() + " is missing the @CheckData annotation!");
        }
        final CheckData checkData = checkClass.getAnnotation(CheckData.class);

        this.name = checkData.name().isBlank() ? checkClass.getSimpleName() : checkData.name();
        this.description = checkData.description();
        this.category = checkData.category();
        this.experimental = checkData.experimental();

        load();
    }

    @Override
    public void reload() {
        load();
    }

    public void load() {
        // Load check settings from config
    }

    protected boolean fail() {
        return fail(null);
    }

    protected boolean fail(@Nullable String debug) {
        if (!shouldFail()) return false;
        violations++;

        // TODO: Replace this with proper flag logic
        TGPlatform.getInstance().getLogger().info("Player " + player.getName() + " failed " + name + " VL: " + getViolations() + (debug != null ? " | Debug: " + debug : ""));
        return true;
    }

    protected boolean shouldFail() {
        TGFlagEvent event = new TGFlagEvent(player, this);
        event = (TGFlagEvent) TGPlatform.getInstance().getEventRepository().post(event);

        return !event.isCancelled();
    }
}
