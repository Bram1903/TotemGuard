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

package com.deathmotion.totemguard.paper.event;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.player.TGPlayer;
import org.bukkit.PortalType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEnterEvent;

public final class NetherPortalListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onPortalEnter(EntityPortalEnterEvent event) {
        if (event.getPortalType() != PortalType.NETHER) return;
        if (!(event.getEntity() instanceof Player player)) return;
        TGPlayer tgPlayer = TGPlatform.getInstance().getPlayerRepository().getPlayer(player.getUniqueId());
        if (tgPlayer == null) return;
        tgPlayer.getData().markNetherPortalContact();
    }
}
