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
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

@CheckData(name = "AutoTotemG", description = "Monitors swap-hand timings")
public class AutoTotemG extends Check implements BukkitEventCheck {

    private Long lastTotemUse;
    private Long lastClickTime;

    public AutoTotemG(TotemPlayer playerData) {
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
        if (event instanceof PlayerSwapHandItemsEvent swapHandItemsEvent) {
            handleSwapHandItems(swapHandItemsEvent);
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
        // Moving Totem to hotbar
        ClickType click = event.getClick();
        if (click == ClickType.NUMBER_KEY || click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT) {
            ItemStack item = event.getCurrentItem();
            int hotbar = event.getHotbarButton();
            ItemStack hotbarItem = null;
            if (hotbar >= 0) {
                hotbarItem = player.bukkitPlayer.getInventory().getItem(hotbar);
            }
            if ((item != null && item.getType() == Material.TOTEM_OF_UNDYING) || (hotbarItem != null && hotbarItem.getType() == Material.TOTEM_OF_UNDYING)) {
                lastClickTime = System.currentTimeMillis();
            }
        }
    }

    /**
     * Tracks swap-hand actions to capture the time of a Totem swap.
     */
    private void handleSwapHandItems(PlayerSwapHandItemsEvent event) {
        // Moving Totem to off-hand
        if (event.getOffHandItem().getType() == Material.TOTEM_OF_UNDYING) {
            if (lastClickTime != null && lastTotemUse != null) {
                evaluateSuspicion();
            }
        }
    }

    /**
     * Validates whether the behavior is suspicious by comparing relevant time intervals.
     */
    private void evaluateSuspicion() {
        long now = System.currentTimeMillis();
        long timeSinceTotemUse = Math.abs(now - lastTotemUse);
        long timeSinceClick = Math.abs(now - lastClickTime);
        long adjustedTotemTime = Math.abs(timeSinceTotemUse - player.getKeepAlivePing());

        var config = TotemGuard.getInstance().getConfigManager().getChecks().getAutoTotemG();

        // If both the time after click and time since totem usage are under the configured thresholds, flag.
        if (timeSinceClick <= config.getClickToSwapTimeDifference() && timeSinceTotemUse <= config.getNormalCheckTimeMs()) {
            fail(createDetails(timeSinceTotemUse, adjustedTotemTime, timeSinceClick));
        }

        lastTotemUse = null;
    }

    private Component createDetails(long timeDifference, long realTotemTime, long clickTimeDifference) {
        Component component = Component.text()
                .append(Component.text("Totem Time: ", color.getX()))
                .append(Component.text(timeDifference + "ms", color.getY()))
                .append(Component.newline())
                .append(Component.text("Real Totem Time: ", color.getX()))
                .append(Component.text(realTotemTime + "ms", color.getY()))
                .append(Component.newline())
                .append(Component.text("Click Difference: ", color.getX()))
                .append(Component.text(clickTimeDifference + "ms", color.getY()))
                .append(Component.newline())
                .append(Component.text("Main Hand: ", color.getX()))
                .append(Component.text(getMainHandItemString(), color.getY()))
                .build();

        StringBuilder states = new StringBuilder();
        if (player.bukkitPlayer.isSprinting()) {
            states.append("Sprinting, ");
        }
        if (player.bukkitPlayer.isSneaking()) {
            states.append("Sneaking, ");
        }
        if (player.bukkitPlayer.isBlocking()) {
            states.append("Blocking, ");
        }

        // If any states are active, add them to the component
        if (!states.isEmpty()) {
            states.setLength(states.length() - 2); // Remove trailing comma and space
            component = component
                    .append(Component.newline())
                    .append(Component.text("States: ", color.getX()))
                    .append(Component.text(states.toString(), color.getY()));
        }

        return component;
    }

    private String getMainHandItemString() {
        PlayerInventory inventory = player.bukkitPlayer.getInventory();

        return inventory.getItemInMainHand().getType() == Material.AIR
                ? "Empty Hand"
                : inventory.getItemInMainHand().getType().toString();
    }
}
