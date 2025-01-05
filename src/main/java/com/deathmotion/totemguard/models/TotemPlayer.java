/*
 *  This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 *  Copyright (C) 2024 Bram and contributors
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.deathmotion.totemguard.models;

import com.deathmotion.totemguard.api.interfaces.AbstractCheck;
import com.deathmotion.totemguard.api.interfaces.TotemUser;
import com.deathmotion.totemguard.checks.impl.misc.ClientBrand;
import com.deathmotion.totemguard.manager.CheckManager;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.User;
import org.bukkit.entity.Player;

import java.util.UUID;

public class TotemPlayer implements TotemUser {
    public final CheckManager checkManager;
    public final TotemData totemData;
    public DigAndPickupState digAndPickupState;

    public final UUID uniqueId;
    public final User user;
    public Player bukkitPlayer;

    public TotemPlayer(User user) {
        this.uniqueId = user.getUUID();
        this.user = user;

        checkManager = new CheckManager(this);
        totemData = new TotemData();
        digAndPickupState = new DigAndPickupState();
    }

    public void reload() {
        // reload all checks
        for (AbstractCheck value : checkManager.allChecks.values()) value.reload();
    }

    public String getBrand() {
        return checkManager.getPacketCheck(ClientBrand.class).getBrand();
    }

    @Override
    public String getName() {
        return user.getName();
    }

    @Override
    public UUID getUniqueId() {
        return uniqueId;
    }

    @Override
    public String getVersionName() {
        return user.getClientVersion().getReleaseName();
    }

    @Override
    public int getKeepAlivePing() {
        if (bukkitPlayer == null) return -1;
        return PacketEvents.getAPI().getPlayerManager().getPing(bukkitPlayer);
    }
}
