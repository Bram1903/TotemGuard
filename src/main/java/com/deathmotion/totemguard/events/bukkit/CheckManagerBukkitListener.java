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

package com.deathmotion.totemguard.events.bukkit;


import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.models.TotemPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;

public class CheckManagerBukkitListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerEvent(Event event) {
        Player bukkitPlayer = null;

        // Handle events where the entity is a player
        if (event instanceof EntityEvent) {
            Entity entity = ((EntityEvent) event).getEntity();
            if (entity instanceof Player) {
                bukkitPlayer = (Player) entity;
            }
        }

        // Handle inventory interaction events
        else if (event instanceof InventoryInteractEvent) {
            bukkitPlayer = (Player) ((InventoryInteractEvent) event).getWhoClicked();
        }

        // If we couldn't determine the player, exit early
        if (bukkitPlayer == null) return;

        // Fetch the TotemPlayer instance
        TotemPlayer player = TotemGuard.getInstance().getPlayerDataManager().getPlayer(bukkitPlayer);
        if (player == null) return;

        // Pass the event to the player's check manager
        player.checkManager.onBukkitEvent(event);
    }
}
