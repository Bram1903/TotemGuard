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
import com.deathmotion.totemguard.config.Settings;
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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public final class AutoTotemC extends Check implements Listener {

    private final TotemGuard plugin;
    private final ConcurrentHashMap<UUID, ConcurrentLinkedDeque<Long>> totemUseTimes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, ConcurrentLinkedDeque<Long>> totemReEquipTimes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Boolean> expectingReEquip = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> consistentSDCountMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, ConcurrentLinkedDeque<Double>> sdHistoryMap = new ConcurrentHashMap<>();

    public AutoTotemC(TotemGuard plugin) {
        super(plugin, "AutoTotemC", "Impossible re-totem consistency", true);
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTotemUse(EntityResurrectEvent event) {
        if (event.getEntity() instanceof Player player &&
                player.getInventory().getItemInMainHand().getType() != Material.TOTEM_OF_UNDYING) {

            recordTotemEvent(totemUseTimes, player.getUniqueId());
            expectingReEquip.put(player.getUniqueId(), true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player &&
                event.getCurrentItem() != null &&
                event.getCurrentItem().getType() == Material.TOTEM_OF_UNDYING &&
                expectingReEquip.getOrDefault(player.getUniqueId(), false)) {

            handleReEquip(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (event.getOffHandItem().getType() == Material.TOTEM_OF_UNDYING &&
                expectingReEquip.getOrDefault(player.getUniqueId(), false)) {

            handleReEquip(player);
        }
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
        consistentSDCountMap.clear();
        sdHistoryMap.clear();
    }

    @Override
    public void resetData(UUID uuid) {
        totemUseTimes.remove(uuid);
        totemReEquipTimes.remove(uuid);
        expectingReEquip.remove(uuid);
        consistentSDCountMap.remove(uuid);
        sdHistoryMap.remove(uuid);
    }

    private void recordTotemEvent(Map<UUID, ConcurrentLinkedDeque<Long>> map, UUID playerId) {
        ConcurrentLinkedDeque<Long> deque = map.computeIfAbsent(playerId, k -> new ConcurrentLinkedDeque<>());
        deque.addLast(System.nanoTime());
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

            long[] intervals = MathUtil.calculateIntervals(useTimes, reEquipTimes);
            var settings = plugin.getConfigManager().getSettings().getChecks().getAutoTotemC();

            handleConsistentSD(player, playerId, intervals, settings);
        });
    }

    private void handleConsistentSD(Player player, UUID playerId, long[] intervals, Settings.Checks.AutoTotemC settings) {
        double currentSD = calculateStandardDeviationOfIntervals(intervals) / 1_000_000.0;  // Convert to ms

        // Get the player's SD history or create a new one
        ConcurrentLinkedDeque<Double> sdHistory = sdHistoryMap.computeIfAbsent(playerId, k -> new ConcurrentLinkedDeque<>());

        // Add the current SD to the history
        sdHistory.addLast(currentSD);
        if (sdHistory.size() > 5) {
            sdHistory.pollFirst();  // Keep the history size limited to 5
        }

        // Only proceed if we have at least two SDs to compare
        if (sdHistory.size() > 1) {
            double averageSDDifference = calculateAverageDifference(sdHistory);
            double roundedDifference = Math.round(averageSDDifference * 100.0) / 100.0;  // Round to 2 decimal places

            plugin.debug(player.getName() + " - Average SD Difference: " + roundedDifference + "ms");

            // Check if the average SD difference is below the threshold
            if (roundedDifference < settings.getConsistentSDRange()) {
                int consecutiveConsistentSDCount = consistentSDCountMap.getOrDefault(playerId, 0) + 1;
                consistentSDCountMap.put(playerId, consecutiveConsistentSDCount);

                if (consecutiveConsistentSDCount >= settings.getConsistentSDThreshold()) {
                    consistentSDCountMap.remove(playerId);
                    sdHistoryMap.remove(playerId);  // Reset history after flagging
                    flag(player, createComponent(roundedDifference), settings);
                }
            } else {
                // Reset the count if the average SD difference is above the range
                consistentSDCountMap.put(playerId, 0);
            }
        }
    }

    private double calculateAverageDifference(ConcurrentLinkedDeque<Double> sdHistory) {
        double totalDifference = 0;
        Double previousSD = null;

        for (Double sd : sdHistory) {
            if (previousSD != null) {
                totalDifference += Math.abs(sd - previousSD);  // Sum the absolute differences
            }
            previousSD = sd;
        }

        return totalDifference / (sdHistory.size() - 1);  // Average of the differences
    }

    private Component createComponent(double averageSDDifference) {
        return Component.text()
                .append(Component.text("Average SD Difference" + ": ", NamedTextColor.GRAY))
                .append(Component.text(averageSDDifference + "ms", NamedTextColor.GOLD))
                .build();
    }

    private double calculateMean(long[] intervals) {
        long sum = 0;
        for (long interval : intervals) {
            sum += interval;
        }
        return (double) sum / intervals.length;
    }

    private double calculateStandardDeviation(long[] intervals, double mean) {
        double varianceSum = 0;
        for (long interval : intervals) {
            varianceSum += Math.pow(interval - mean, 2);
        }
        return Math.sqrt(varianceSum / intervals.length);
    }

    private long calculateStandardDeviationOfIntervals(long[] intervals) {
        return Math.round(calculateStandardDeviation(intervals, calculateMean(intervals)));
    }
}
