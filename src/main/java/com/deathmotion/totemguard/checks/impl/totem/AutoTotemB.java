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

package com.deathmotion.totemguard.checks.impl.totem;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.checks.Check;
import com.deathmotion.totemguard.util.MathUtil;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public final class AutoTotemB extends Check implements Listener {

    private final TotemGuard plugin;
    private final ConcurrentHashMap<UUID, ConcurrentLinkedDeque<Long>> totemUseTimes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, ConcurrentLinkedDeque<Long>> totemReEquipTimes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Boolean> expectingReEquip = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> lowSDCountMap = new ConcurrentHashMap<>();

    public AutoTotemB(TotemGuard plugin) {
        super(plugin, "AutoTotemB", "Impossible re-totem speed", true);
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTotemUse(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.getInventory().getItemInMainHand().getType() == Material.TOTEM_OF_UNDYING) return;

        recordTotemEvent(totemUseTimes, player.getUniqueId());
        expectingReEquip.put(player.getUniqueId(), true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!expectingReEquip.getOrDefault(player.getUniqueId(), false)) return;

        ItemStack currentItem = event.getCurrentItem();
        if (currentItem == null || currentItem.getType() != Material.TOTEM_OF_UNDYING) return;

        handleReEquip(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (!expectingReEquip.getOrDefault(player.getUniqueId(), false)) return;
        if (event.getOffHandItem().getType() != Material.TOTEM_OF_UNDYING) return;

        handleReEquip(player);
    }

    private void handleReEquip(Player player) {
        UUID playerId = player.getUniqueId();
        recordTotemEvent(totemReEquipTimes, playerId);
        expectingReEquip.put(playerId, false);
        checkPlayerConsistency(player);
    }

    @Override
    public void resetData() {
        totemUseTimes.clear();
        totemReEquipTimes.clear();
        expectingReEquip.clear();
        lowSDCountMap.clear();
    }

    @Override
    public void resetData(UUID uuid) {
        totemUseTimes.remove(uuid);
        totemReEquipTimes.remove(uuid);
        expectingReEquip.remove(uuid);
        lowSDCountMap.remove(uuid);
    }

    private void recordTotemEvent(Map<UUID, ConcurrentLinkedDeque<Long>> map, UUID playerId) {
        ConcurrentLinkedDeque<Long> deque = map.computeIfAbsent(playerId, k -> new ConcurrentLinkedDeque<>());
        deque.addLast(System.currentTimeMillis());
        if (deque.size() > 10) {
            deque.pollFirst();  // Limit to 10 events
        }
    }


    private void checkPlayerConsistency(Player player) {
        FoliaScheduler.getAsyncScheduler().runNow(plugin, (o) -> {
            UUID playerId = player.getUniqueId();

            var useTimes = totemUseTimes.get(playerId);
            var reEquipTimes = totemReEquipTimes.get(playerId);

            if (useTimes == null || reEquipTimes == null || useTimes.size() < 2 || reEquipTimes.size() < 2) {
                return;
            }

            Collection<? extends Number> intervals = MathUtil.calculateIntervals(useTimes, reEquipTimes);
            double standardDeviation = MathUtil.trim(2, MathUtil.getStandardDeviation(intervals));

            plugin.debug(player.getName() + " - SD: " + standardDeviation);

            var settings = plugin.getConfigManager().getSettings().getChecks().getAutoTotemB();

            if (standardDeviation < settings.getLowSDThreshold()) {
                int consecutiveLowSDCount = lowSDCountMap.getOrDefault(playerId, 0) + 1;
                lowSDCountMap.put(playerId, consecutiveLowSDCount);

                if (consecutiveLowSDCount >= settings.getConsistentSDThreshold()) {
                    lowSDCountMap.remove(playerId);
                    flag(player, createComponent(standardDeviation), settings);
                }
            }
        });
    }

    private Component createComponent(double sd) {
        return Component.text()
                .append(Component.text("SD" + ": ", NamedTextColor.GRAY))
                .append(Component.text(sd + "ms", NamedTextColor.GOLD))
                .build();
    }
}
