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
import com.deathmotion.totemguard.api3.event.impl.TGUserFlagEvent;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.cache.data.CheckSnapshot;
import com.deathmotion.totemguard.common.check.annotations.CheckData;
import com.deathmotion.totemguard.common.check.annotations.RequiresTickEnd;
import com.deathmotion.totemguard.common.event.api.impl.TGUserFlagEventImpl;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.data.Data;
import com.deathmotion.totemguard.common.player.inventory.InventoryConstants;
import com.deathmotion.totemguard.common.player.inventory.PacketInventory;
import com.deathmotion.totemguard.common.punishment.PunishmentCommand;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class CheckImpl implements Check {

    public final TGPlayer player;

    protected final Data data;
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
    private final boolean requiresTickEnd;
    @Getter
    protected boolean punishable;
    protected boolean mitigate;
    @Getter
    private boolean enabled;
    @Getter
    private int violations;

    @Getter
    private int maxViolations;

    @Getter
    private List<PunishmentCommand> punishCommands = new ArrayList<>();

    public CheckImpl(TGPlayer player) {
        this.player = player;
        this.data = player.getData();
        this.inventory = player.getInventory();
        this.buffer = new Buffer();
        this.platform = TGPlatform.getInstance();

        final Class<?> checkClass = this.getClass();
        if (!checkClass.isAnnotationPresent(CheckData.class)) {
            throw new IllegalStateException("Check class " + checkClass.getName() + " is missing the @CheckData annotation!");
        }
        final CheckData checkData = checkClass.getAnnotation(CheckData.class);
        this.requiresTickEnd = checkClass.isAnnotationPresent(RequiresTickEnd.class);

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
        CheckOptions checkOptions = new CheckOptions(name);

        enabled = checkOptions.isEnabled();
        punishable = checkOptions.isPunishable();
        mitigate = checkOptions.isMitigate();
        maxViolations = checkOptions.getMaxViolations();
        punishCommands = checkOptions.getPunishCommands();
    }

    protected boolean fail() {
        return fail(null);
    }

    protected boolean fail(@Nullable String debug) {
        if (!shouldFail(debug)) return false;
        violations++;

        //TGPlatform.getInstance().getLogger().info("Player " + player.getName() + " failed " + name + " VL: " + getViolations() + (debug != null ? " | Debug: " + debug : ""));
        TGPlatform.getInstance().getAlertRepository().alert(this, violations, debug);
        return true;
    }

    protected void failInventory(@Nullable String debug) {
        if (!fail(debug)) {
            return;
        }

        if (mitigate && data.isOpenInventory() && !data.isInventoryMitigated()) {
            data.setInventoryMitigated(true);
            player.getUser().sendPacket(InventoryConstants.SERVER_CLOSE_WINDOW);
        }
    }

    protected boolean shouldFail(@Nullable String debug) {
        TGUserFlagEvent event = TGPlatform.getInstance().getEventRepository().post(new TGUserFlagEventImpl(player, this, debug));
        return !event.isCancelled();
    }

    public void clearViolations() {
        violations = 0;
    }

    @Override
    public boolean requiresTickEnd() {
        return requiresTickEnd;
    }

    @Override
    public boolean isHeuristic() {
        return false;
    }

    public CheckSnapshot getSnapshot() {
        return new CheckSnapshot(name, buffer.get(), violations);
    }

    public void applySnapshot(CheckSnapshot checkSnapshot) {
        violations = checkSnapshot.violations();
        buffer.set(checkSnapshot.buffer());
    }
}
