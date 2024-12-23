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

import com.deathmotion.totemguard.api.interfaces.TotemUser;
import com.deathmotion.totemguard.checks.impl.misc.ClientBrand;
import com.deathmotion.totemguard.manager.CheckManager;
import com.github.retrooper.packetevents.protocol.player.User;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Getter
public class TotemPlayer implements TotemUser {
    public final CheckManager checkManager;

    private final UUID uniqueId;
    private final User user;
    @Nullable
    public Player bukkitPlayer;

    public TotemPlayer(User user) {
        this.uniqueId = user.getUUID();
        this.user = user;

        checkManager = new CheckManager(this);
    }

    public void pollData() {
        if (uniqueId != null && this.bukkitPlayer == null) {
            this.bukkitPlayer = Bukkit.getPlayer(uniqueId);
        }
    }

    public String getBrand() {
        return checkManager.getPacketCheck(ClientBrand.class).getBrand();
    }

    @Override
    public String getName() {
        return user.getName();
    }

    @Override
    public String getVersionName() {
        return user.getClientVersion().getReleaseName();
    }
}
