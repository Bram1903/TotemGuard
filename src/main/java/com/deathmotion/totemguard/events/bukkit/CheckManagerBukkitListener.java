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
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
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
        if (player.getInventory().getItemInMainHand().getType() != Material.TOTEM_OF_UNDYING
                && player.getInventory().containsAtLeast(new ItemStack(Material.TOTEM_OF_UNDYING), 2)) {
            totemPlayer.totemData.setLastTotemUsage(System.currentTimeMillis());
            totemPlayer.totemData.setExpectingTotemSwap(true);
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
                totemPlayer.totemData.setExpectingTotemSwap(false);
                callTotemCycleHandlers(totemPlayer);
            }
        }

        totemPlayer.checkManager.onBukkitEvent(event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        TotemPlayer totemPlayer = TotemGuard.getInstance().getPlayerDataManager().getPlayer(event.getPlayer());
        if (totemPlayer == null) return;

        if (totemPlayer.totemData.isExpectingTotemSwap()) {
            ItemStack offHand = event.getOffHandItem();
            if (offHand != null && offHand.getType() == Material.TOTEM_OF_UNDYING) {
                // Finalize the swap
                totemPlayer.totemData.setExpectingTotemSwap(false);

                // Handle the timing for totem usage
                long currentTime = System.currentTimeMillis();
                Long lastUsage = totemPlayer.totemData.getLastTotemUsage();
                if (lastUsage != null) {
                    long interval = Math.abs(currentTime - lastUsage);
                    totemPlayer.totemData.addInterval(interval);
                }

                totemPlayer.checkManager.onTotemCycleEvent();
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        TotemPlayer totemPlayer = TotemGuard.getInstance().getPlayerDataManager().getPlayer(event.getEntity());
        if (totemPlayer == null) return;

        // Reset any pending totem swap logic upon death.
        totemPlayer.totemData.setExpectingTotemSwap(false);
        totemPlayer.totemData.setLastTotemUsage(null);
    }

    private void callTotemCycleHandlers(TotemPlayer player) {
        long currentTime = System.currentTimeMillis();
        Long lastUsage = player.totemData.getLastTotemUsage();

        long interval = Math.abs(currentTime - lastUsage);
        player.totemData.addInterval(interval);

        player.checkManager.onTotemCycleEvent();
    }
}
