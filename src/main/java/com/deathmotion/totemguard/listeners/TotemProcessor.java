/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2024 Bram and contributors
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

package com.deathmotion.totemguard.listeners;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.models.TotemPlayer;
import com.deathmotion.totemguard.models.events.TotemCycleEvent;
import com.deathmotion.totemguard.packetlisteners.UserTracker;
import com.deathmotion.totemguard.util.datastructure.TotemData;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import lombok.Getter;
import org.bukkit.Bukkit;
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

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TotemProcessor implements Listener {
    @Getter
    private static TotemProcessor instance;

    private final TotemGuard plugin;
    private final UserTracker userTracker;

    private final ConcurrentHashMap<UUID, Long> totemUsage = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Boolean> expectingReEquip = new ConcurrentHashMap<>();

    private TotemProcessor(TotemGuard plugin) {
        this.plugin = plugin;
        this.userTracker = plugin.getUserTracker();

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public static void init(TotemGuard plugin) {
        if (instance == null) {
            instance = new TotemProcessor(plugin);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTotemUse(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.getInventory().getItemInMainHand().getType() == Material.TOTEM_OF_UNDYING) return;

        if (!player.getInventory().containsAtLeast(new ItemStack(Material.TOTEM_OF_UNDYING), 2)) {
            return;
        }

        totemUsage.put(player.getUniqueId(), System.currentTimeMillis());
        expectingReEquip.put(player.getUniqueId(), true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!expectingReEquip.getOrDefault(player.getUniqueId(), false)) return;

        ItemStack currentItem = event.getCurrentItem();
        if (currentItem == null || currentItem.getType() != Material.TOTEM_OF_UNDYING) return;

        expectingReEquip.put(player.getUniqueId(), false);

        handleTotemEvent(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (!expectingReEquip.getOrDefault(player.getUniqueId(), false)) return;
        if (event.getOffHandItem().getType() != Material.TOTEM_OF_UNDYING) return;

        expectingReEquip.put(player.getUniqueId(), false);

        handleTotemEvent(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID playerId = player.getUniqueId();

        expectingReEquip.remove(playerId);
        totemUsage.remove(playerId);
    }

    private void handleTotemEvent(Player player) {
        FoliaScheduler.getAsyncScheduler().runNow(plugin, (o) -> {
            UUID playerId = player.getUniqueId();

            long currentTime = System.currentTimeMillis();
            Long totemUseTime = totemUsage.get(playerId);

            TotemPlayer totemPlayer = userTracker.getTotemPlayer(playerId).orElse(null);
            if (totemPlayer == null) return;

            long interval = Math.abs(currentTime - totemUseTime);
            TotemData totemData = totemPlayer.totemData();
            totemData.addInterval(interval);

            TotemCycleEvent event = new TotemCycleEvent(player, totemPlayer);
            Bukkit.getPluginManager().callEvent(event);
        });
    }

    public void resetData() {
        totemUsage.clear();
        expectingReEquip.clear();
    }

    public void resetData(UUID uuid) {
        totemUsage.remove(uuid);
        expectingReEquip.remove(uuid);
    }
}
