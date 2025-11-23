/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2025 Bram and contributors
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

package com.deathmotion.totemguard.checks.impl.autototem;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.checks.Check;
import com.deathmotion.totemguard.checks.CheckData;
import com.deathmotion.totemguard.checks.type.BukkitEventCheck;
import com.deathmotion.totemguard.models.TotemPlayer;
import com.deathmotion.totemguard.util.MathUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

@CheckData(name = "AutoTotemH", description = "Consistent click standard deviation")
public class AutoTotemH extends Check implements BukkitEventCheck {

    private final ConcurrentLinkedDeque<Long> clickDifferences = new ConcurrentLinkedDeque<>();

    private long lastTotemUse = -1L;
    private long lastClickTime = -1L;

    private int lowStandardDeviationCount = 0;

    public AutoTotemH(TotemPlayer playerData) {
        super(playerData);
    }

    @Override
    public void onBukkitEvent(Event event) {
        if (event instanceof EntityResurrectEvent) {
            handleEntityResurrection();
        }
        if (event instanceof InventoryClickEvent invClickEvent) {
            handleInventoryClick(invClickEvent);
        }
    }

    /**
     * Records the moment a totem is used (if conditions are met).
     */
    private void handleEntityResurrection() {
        PlayerInventory playerInventory = player.bukkitPlayer.getInventory();

        // Ensure the main hand doesn't already have a Totem, and at least 2 Totems exist in the inventory
        if (playerInventory.getItemInMainHand().getType() == Material.TOTEM_OF_UNDYING) return;
        if (!playerInventory.containsAtLeast(new ItemStack(Material.TOTEM_OF_UNDYING), 2)) return;

        lastTotemUse = System.currentTimeMillis();
    }

    /**
     * Tracks inventory clicks to capture the time of a Totem click.
     */
    private void handleInventoryClick(InventoryClickEvent event) {
        // Moving Totem to off-hand
        if (event.getRawSlot() == 45 && event.getCursor().getType() == Material.TOTEM_OF_UNDYING) {

            if (lastClickTime != -1 && lastTotemUse != -1) {

                double now = System.currentTimeMillis();
                var settings = TotemGuard.getInstance().getConfigManager().getChecks().getAutoTotemH();

                // Must be within allowed time window between totem use
                if (now - lastTotemUse < settings.getMinCheckTime()) {

                    long diff = Math.abs(System.currentTimeMillis() - lastClickTime);
                    clickDifferences.add(diff);

                    // Keep last 20 entries
                    while (clickDifferences.size() > 20) {
                        clickDifferences.poll();
                    }

                    evaluateSuspicion();
                }
            }

            lastTotemUse = -1;
            return;
        }

        // Clicking an item that’s a Totem
        if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.TOTEM_OF_UNDYING) {
            lastClickTime = System.currentTimeMillis();
        }
    }

    /**
     * Validates whether the behavior is suspicious by comparing relevant time difference intervals.
     */
    private void evaluateSuspicion() {
        if (clickDifferences.size() < 5) return;

        double standardDeviation = MathUtil.getStandardDeviation(clickDifferences);
        double mean = MathUtil.getMean(clickDifferences);

        var settings = TotemGuard.getInstance().getConfigManager().getChecks().getAutoTotemH();

        if (standardDeviation >= settings.getStandardDeviationThreshold()) return;
        if (mean >= settings.getMeanThreshold()) return;

        lowStandardDeviationCount++;

        if (lowStandardDeviationCount >= settings.getConsecutiveLowSDCount()) {
            lowStandardDeviationCount = 0;
            fail(createComponent(standardDeviation, mean));
        }
    }

    private Component createComponent(double standardDeviation, double mean) {
        return Component.text()
            .append(Component.text("Click SD: ", color.getX()))
            .append(Component.text(MathUtil.trim(2, standardDeviation) + "ms", color.getY()))
            .append(Component.newline())
            .append(Component.text("Mean: ", color.getX()))
            .append(Component.text(MathUtil.trim(2, mean) + "ms", color.getY()))
            .build();
    }
}
