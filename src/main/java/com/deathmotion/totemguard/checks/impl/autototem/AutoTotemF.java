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
import com.deathmotion.totemguard.config.Checks;
import com.deathmotion.totemguard.models.TotemPlayer;
import com.deathmotion.totemguard.models.impl.ValidClickTypes;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.PlayerInventory;

@CheckData(name = "AutoTotemF", description = "Invalid interaction", experimental = true)
public class AutoTotemF extends Check implements BukkitEventCheck {

    private Long inventoryClickTime;

    public AutoTotemF(TotemPlayer playerData) {
        super(playerData);
    }

    @Override
    public void onBukkitEvent(Event event) {
        if (event instanceof InventoryClickEvent) {
            handleInventoryClick((InventoryClickEvent) event);
        } else if (event instanceof PlayerInteractEvent) {
            handlePlayerInteract((PlayerInteractEvent) event);
        } else if (event instanceof InventoryCloseEvent) {
            handleInventoryClose();
        } else if (event instanceof PlayerDeathEvent) {
            handlePlayerDeath();
        }
    }

    private void handleInventoryClick(InventoryClickEvent event) {
        Inventory inventory = event.getClickedInventory();
        if (inventory == null || inventory.getType() != InventoryType.PLAYER) return;
        if (!ValidClickTypes.isClickTypeValid((event.getClick()))) return;

        if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.TOTEM_OF_UNDYING) {
            inventoryClickTime = System.currentTimeMillis();
        }
    }

    private void handlePlayerInteract(PlayerInteractEvent event) {
        if (inventoryClickTime == null) return;

        Action action = event.getAction();
        if (action == Action.PHYSICAL) return;

        checkSuspiciousActivity(action);
        inventoryClickTime = null;
    }

    private void checkSuspiciousActivity(Action action) {
        long timeDifference = Math.abs(System.currentTimeMillis() - inventoryClickTime);

        final Checks.AutoTotemF settings = TotemGuard.getInstance().getConfigManager().getChecks().getAutoTotemF();
        if (timeDifference <= settings.getTimeDifference()) {
            fail(createDetails(action, timeDifference));
        }
    }

    private void handleInventoryClose() {
        inventoryClickTime = null;
    }

    private void handlePlayerDeath() {
        inventoryClickTime = null;
    }

    private Component createDetails(Action action, Long timeDifference) {
        Component component = Component.text()
                .append(Component.text("Type: ", color.getX()))
                .append(Component.text(action.toString(), color.getY()))
                .append(Component.newline())
                .append(Component.text("Time Difference: ", color.getX()))
                .append(Component.text(timeDifference, color.getY()))
                .append(Component.text("ms", color.getY()))
                .append(Component.newline())
                .append(Component.text("Main Hand: ", color.getX()))
                .append(Component.text(getMainHandItemString(), color.getY()))
                .append(Component.newline())
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
            states.setLength(states.length() - 2);
            component = component.append(Component.text("States: ", color.getX()))
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
