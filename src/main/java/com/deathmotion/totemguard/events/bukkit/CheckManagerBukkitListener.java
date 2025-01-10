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
import com.deathmotion.totemguard.models.impl.DigAndPickupState;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

public class CheckManagerBukkitListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerResurrect(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        TotemPlayer totemPlayer = TotemGuard.getInstance().getPlayerDataManager().getPlayer(player);
        if (totemPlayer == null) return;

        // If not holding a totem in main hand but still has >= 2 totems in inventory,
        // track the next time they swap another totem into their hand.
        if (player.getInventory().getItemInMainHand().getType() != Material.TOTEM_OF_UNDYING) {
            totemPlayer.totemData.setLastTotemUsage(System.currentTimeMillis());
            totemPlayer.totemData.setExpectingTotemSwap(true);
            totemPlayer.digAndPickupState = new DigAndPickupState();
        }

        totemPlayer.checkManager.onBukkitEvent(event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        TotemPlayer totemPlayer = TotemGuard.getInstance().getPlayerDataManager().getPlayer(player);
        if (totemPlayer == null) return;

        if (totemPlayer.totemData.isExpectingTotemSwap()) {
            ItemStack item = event.getCurrentItem();
            if (item != null && item.getType() == Material.TOTEM_OF_UNDYING) {
                callTotemCycleHandlers(totemPlayer);
            }
        }

        totemPlayer.checkManager.onBukkitEvent(event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        TotemPlayer totemPlayer = TotemGuard.getInstance().getPlayerDataManager().getPlayer(player);
        if (totemPlayer == null) return;

        totemPlayer.checkManager.onBukkitEvent(event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        TotemPlayer totemPlayer = TotemGuard.getInstance().getPlayerDataManager().getPlayer(event.getPlayer());
        if (totemPlayer == null) return;

        if (!totemPlayer.totemData.isExpectingTotemSwap()) return;
        if (event.getOffHandItem().getType() != Material.TOTEM_OF_UNDYING) return;

        callTotemCycleHandlers(totemPlayer);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        TotemPlayer totemPlayer = TotemGuard.getInstance().getPlayerDataManager().getPlayer(event.getEntity());
        if (totemPlayer == null) return;

        // Reset any pending totem swap logic upon death.
        totemPlayer.totemData.setExpectingTotemSwap(false);
        totemPlayer.totemData.setLastTotemUsage(null);

        totemPlayer.checkManager.onBukkitEvent(event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        TotemPlayer totemPlayer = TotemGuard.getInstance().getPlayerDataManager().getPlayer(event.getPlayer());
        if (totemPlayer == null) return;

        totemPlayer.checkManager.onBukkitEvent(event);
    }

    private void callTotemCycleHandlers(TotemPlayer player) {
        player.totemData.setExpectingTotemSwap(false);

        long currentTime = System.currentTimeMillis();
        Long lastUsage = player.totemData.getLastTotemUsage();

        long interval = Math.abs(currentTime - lastUsage);
        player.totemData.addInterval(interval);

        player.checkManager.onTotemCycleEvent();
    }
}
