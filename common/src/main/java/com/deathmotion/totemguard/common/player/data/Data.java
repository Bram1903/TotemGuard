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

package com.deathmotion.totemguard.common.player.data;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.inventory.enums.Issuer;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Data {

    private final TGPlayer player;
    private final TGPlatform platform;
    private final TeleportData teleportData;
    private final InputData inputData;
    private final MovementData movementData;
    private final WorldEntityData worldEntityData;

    private GameMode gameMode;
    private boolean sprinting;
    private boolean sneaking;
    private boolean canFly;
    private boolean isFlying;

    @Setter(AccessLevel.NONE)
    private boolean openInventory;
    @Setter(AccessLevel.NONE)
    private Issuer lastInventoryIssuer = Issuer.CLIENT;

    private boolean verifiedOpenInventory;
    private boolean pendingOpenInventory;
    private long inventoryOpenedAt;

    private boolean serverOpenedInventoryThisTick;
    private boolean clientOpenedInventoryThisTick;

    private boolean inventoryMitigated;
    private boolean inventoryMitigatedThisTick;

    private volatile boolean sendingBundlePacket;

    public Data(TGPlayer player) {
        this.player = player;
        this.platform = TGPlatform.getInstance();
        this.teleportData = new TeleportData();
        this.inputData = new InputData();
        this.movementData = new MovementData();
        this.worldEntityData = new WorldEntityData();
    }

    public void setOpenInventory(boolean openInventory, Issuer issuer) {
        boolean changed = this.openInventory != openInventory;
        this.openInventory = openInventory;
        this.lastInventoryIssuer = issuer;

        if (!openInventory) {
            this.verifiedOpenInventory = false;
            this.pendingOpenInventory = false;
        }

        if (!changed) return;

        if (openInventory) {
            this.inventoryOpenedAt = System.currentTimeMillis();
        }
        platform.getGuiManager().refreshMonitor(player.getUuid());

        boolean serverInitiated = issuer == Issuer.SERVER;
        if (openInventory) {
            platform.getEventBus().getUserInventoryOpen().fire(player, serverInitiated);
        } else {
            platform.getEventBus().getUserInventoryClose().fire(player, serverInitiated);
        }
    }

    public long getInventoryOpenDurationMs() {
        if (!openInventory || inventoryOpenedAt == 0L) return 0L;
        return System.currentTimeMillis() - inventoryOpenedAt;
    }

    public void setVerifiedOpenInventory() {
        verifiedOpenInventory = true;
        pendingOpenInventory = true;
    }

    public void applyPendingOpenInventory() {
        if (!pendingOpenInventory) {
            return;
        }

        pendingOpenInventory = false;
        if (!openInventory) clientOpenedInventoryThisTick = true;
        setOpenInventory(true, Issuer.CLIENT);
    }
}
