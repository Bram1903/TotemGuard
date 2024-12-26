/*
 *  This source is part of TotemGuard, found at https://github.com/Bram1903/TotemGuard
 *
 *  Copyright (C) 2024 Bram
 *
 *  This program is free software: you may redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, visit <http://www.gnu.org/licenses/>.
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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

@CheckData(name = "AutoTotemA", description = "Monitors usage timings")
public class AutoTotemA extends Check implements BukkitEventCheck {

    private Long lastTotemUse;
    private Long lastClickTime;

    public AutoTotemA(TotemPlayer playerData) {
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
            if (lastClickTime != null && lastTotemUse != null) {
                evaluateSuspicion(lastClickTime);
            }
            return;
        }

        // Clicking an item that’s a Totem
        if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.TOTEM_OF_UNDYING) {
            lastClickTime = System.currentTimeMillis();
        }
    }

    /**
     * Validates whether the behavior is suspicious by comparing relevant time intervals.
     */
    private void evaluateSuspicion(long clickMoment) {
        long now = System.currentTimeMillis();
        long timeSinceTotemUse = Math.abs(now - lastTotemUse);
        long timeSinceClick = Math.abs(now - clickMoment);
        long adjustedTotemTime = Math.abs(timeSinceTotemUse - player.getKeepAlivePing());

        var config = TotemGuard.getInstance().getConfigManager().getChecks().getAutoTotemA();

        // If both the time after click and time since totem usage are under the configured thresholds, flag.
        if (timeSinceClick <= config.getClickTimeDifference() && timeSinceTotemUse <= config.getNormalCheckTimeMs()) {
            fail(buildAlertMessage(timeSinceTotemUse, adjustedTotemTime, timeSinceClick));
        }

        lastTotemUse = null;
    }

    /**
     * Retrieves a string for the item in the player's main hand (or "Empty Hand").
     */
    private String describeMainHand() {
        Material mainHandItem = player.bukkitPlayer.getInventory().getItemInMainHand().getType();
        return mainHandItem == Material.AIR ? "Empty Hand" : mainHandItem.toString();
    }

    /**
     * Constructs the alert message using placeholders from the check's configured message.
     */
    private Component buildAlertMessage(long rawTotemDiff, long pingAdjustedDiff, long rawClickDiff) {
        return Component.text()
                .append(Component.text("Totem Time: ", color.getX()))
                .append(Component.text(rawTotemDiff + "ms", color.getY()))
                .append(Component.newline())
                .append(Component.text("Real Totem Time: ", color.getX()))
                .append(Component.text(pingAdjustedDiff + "ms", color.getY()))
                .append(Component.newline())
                .append(Component.text("Click Difference: ", color.getX()))
                .append(Component.text(rawClickDiff + "ms", color.getY()))
                .append(Component.newline())
                .append(Component.text("Main Hand: ", color.getX()))
                .append(Component.text(describeMainHand(), color.getY()))
                .append(Component.newline())
                .append(Component.text("States: ", color.getX()))
                .append(Component.text(gatherStates(), color.getY()))
                .build();
    }

    /**
     * Gathers and returns a comma-separated list of the player's current activity states, or "None" if empty.
     */
    private String gatherStates() {
        StringBuilder currentStates = new StringBuilder();

        if (player.bukkitPlayer.isSprinting()) currentStates.append("Sprinting, ");
        if (player.bukkitPlayer.isSneaking()) currentStates.append("Sneaking, ");
        if (player.bukkitPlayer.isBlocking()) currentStates.append("Blocking, ");

        if (!currentStates.isEmpty()) {
            // Remove the last ", "
            currentStates.setLength(currentStates.length() - 2);
        } else {
            currentStates.append("None");
        }

        return currentStates.toString();
    }
}