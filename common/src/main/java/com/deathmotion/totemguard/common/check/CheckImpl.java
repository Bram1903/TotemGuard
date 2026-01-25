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

package com.deathmotion.totemguard.common.check;

import com.deathmotion.totemguard.api3.check.Check;
import com.deathmotion.totemguard.api3.check.CheckType;
import com.deathmotion.totemguard.api3.event.impl.TGFlagEvent;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.check.annotations.CheckData;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.inventory.PacketInventory;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

public class CheckImpl implements Check {

    public final TGPlayer player;

    protected final PacketInventory inventory;
    protected final Buffer buffer;
    protected final TGPlatform platform;

    @Getter
    private final String name;
    @Getter
    private final String description;
    @Getter
    private final CheckType type;
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
        this.platform = TGPlatform.getInstance();

        final Class<?> checkClass = this.getClass();
        if (!checkClass.isAnnotationPresent(CheckData.class)) {
            throw new IllegalStateException("Check class " + checkClass.getName() + " is missing the @CheckData annotation!");
        }
        final CheckData checkData = checkClass.getAnnotation(CheckData.class);

        this.name = checkData.name().isBlank() ? checkClass.getSimpleName() : checkData.name();
        this.description = checkData.description();
        this.type = checkData.type();
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
        if (!shouldFail(debug)) return false;
        violations++;

        TGPlatform.getInstance().getLogger().info("Player " + player.getName() + " failed " + name + " VL: " + getViolations() + (debug != null ? " | Debug: " + debug : ""));
        TGPlatform.getInstance().getAlertRepository().alert(this, violations, debug);
        return true;
    }

    protected boolean shouldFail(@Nullable String debug) {
        TGFlagEvent event = new TGFlagEvent(player, this, debug);
        event = (TGFlagEvent) TGPlatform.getInstance().getEventRepository().post(event);

        return !event.isCancelled();
    }
}
