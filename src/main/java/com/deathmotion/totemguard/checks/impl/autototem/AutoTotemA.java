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

package com.deathmotion.totemguard.checks.impl.autototem;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.checks.Check;
import com.deathmotion.totemguard.checks.CheckData;
import com.deathmotion.totemguard.checks.type.BukkitEventCheck;
import com.deathmotion.totemguard.models.TotemPlayer;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

@CheckData(name = "AutoTotemA", description = "Click time difference")
public class AutoTotemA extends Check implements BukkitEventCheck {

    private Long totemUsageTime;
    private Long clickTime;

    public AutoTotemA(final TotemPlayer player) {
        super(player);
    }


    @Override
    public void onPlayerEvent(Event event) {
        if (event instanceof EntityResurrectEvent resurrectEvent) {
            onTotemUsage(resurrectEvent);
        }

        if (event instanceof InventoryClickEvent inventoryClickEvent) {
            onInventoryClick(inventoryClickEvent);
        }
    }

    private void onTotemUsage(EntityResurrectEvent event) {
        if (player.bukkitPlayer.getInventory().getItemInMainHand().getType() == Material.TOTEM_OF_UNDYING) return;
        if (!player.bukkitPlayer.getInventory().containsAtLeast(new ItemStack(Material.TOTEM_OF_UNDYING), 2)) {
            return;
        }

        totemUsageTime = System.currentTimeMillis();
    }

    private void onInventoryClick(InventoryClickEvent event) {
        // If the cursor is holding a Totem of Undying and is being moved to the off-hand slot
        if (event.getRawSlot() == 45 && event.getCursor().getType() == Material.TOTEM_OF_UNDYING) {
            if (clickTime != null && totemUsageTime != null) {
                checkSuspiciousActivity(player.bukkitPlayer, clickTime);
            }
            return;
        }

        // If the clicked item is a Totem of Undying
        if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.TOTEM_OF_UNDYING) {
            clickTime = System.currentTimeMillis();
        }
    }

    private void checkSuspiciousActivity(Player player, long clickTime) {
        FoliaScheduler.getAsyncScheduler().runNow(TotemGuard.getInstance(), (o) -> {
            long currentTime = System.currentTimeMillis();
            long timeDifference = Math.abs(currentTime - totemUsageTime);
            long clickTimeDifference = Math.abs(currentTime - clickTime);
            long realTotemTime = Math.abs(timeDifference - player.getPing());

            com.deathmotion.totemguard.config.Checks.AutoTotemA checkSettings = TotemGuard.getInstance().getConfigManager().getChecks().getAutoTotemA();

            if (clickTimeDifference <= checkSettings.getClickTimeDifference() && timeDifference <= checkSettings.getNormalCheckTimeMs()) {
                fail(createComponent(timeDifference, realTotemTime, clickTimeDifference));
            }
        });
    }

    private String getMainHandItemString(Player player) {
        return player.getInventory().getItemInMainHand().getType() == Material.AIR
                ? "Empty Hand"
                : player.getInventory().getItemInMainHand().getType().toString();
    }

    private Component createComponent(long timeDifference, long realTotemTime, long clickTimeDifference) {
        return checkSettings.getCheckAlertMessage()
                .replaceText(builder -> builder
                        .matchLiteral("%totem_time%")
                        .replacement(String.valueOf(timeDifference))
                        .matchLiteral("%real_totem_time%")
                        .replacement(String.valueOf(realTotemTime))
                        .matchLiteral("%click_time_difference%")
                        .replacement(String.valueOf(clickTimeDifference))
                        .matchLiteral("%main_hand%")
                        .replacement(getMainHandItemString(player.bukkitPlayer))
                        .matchLiteral("%states%")
                        .replacement(getStatesString()));
    }

    private String getStatesString() {
        StringBuilder states = new StringBuilder();

        // Collect active player states
        if (player.bukkitPlayer.isSprinting()) {
            states.append("Sprinting, ");
        }
        if (player.bukkitPlayer.isSneaking()) {
            states.append("Sneaking, ");
        }
        if (player.bukkitPlayer.isBlocking()) {
            states.append("Blocking, ");
        }

        // Remove the trailing comma and space if any states were added
        if (!states.isEmpty()) {
            states.setLength(states.length() - 2);
        } else {
            states.append("None");
        }

        return states.toString();
    }

}
