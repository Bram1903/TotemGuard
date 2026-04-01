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

import com.deathmotion.totemguard.common.player.TGPlayer;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
public class Data {

    private final TGPlayer player;

    private final Set<Integer> pendingTeleportIds = new HashSet<>();

    private GameMode gameMode;
    private float health;
    private int food;
    private float foodSaturation;
    private boolean sprinting;
    private boolean sneaking;
    private boolean canFly;
    private boolean isFlying;
    private boolean openInventory;
    private boolean lastTeleportConfirmValid;
    private volatile boolean sendingBundlePacket;

    public Data(TGPlayer player) {
        this.player = player;
    }

    public void setOpenInventory(boolean openInventory) {
        if (this.openInventory != openInventory) {
            String message = "&a[Inventory] &7" + player.getName() + " has "
                    + (openInventory ? "&aopened" : "&cclosed")
                    + " &7their inventory.";

            //TGPlatform.getInstance().getAlertRepository().broadcast(message);
        }

        this.openInventory = openInventory;
    }

    public void trackTeleport(int teleportId) {
        pendingTeleportIds.add(teleportId);
    }

    public void validateTeleportConfirm(int teleportId) {
        lastTeleportConfirmValid = pendingTeleportIds.remove(teleportId);
    }
}
