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

import com.deathmotion.totemguard.checks.Check;
import com.deathmotion.totemguard.checks.CheckData;
import com.deathmotion.totemguard.checks.type.BukkitEventCheck;
import com.deathmotion.totemguard.models.TotemPlayer;
import io.papermc.paper.event.player.PlayerPickItemEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;

@CheckData(name = "AutoTotemG", description = "Impossible totem pick up", experimental = true)
public class AutoTotemG extends Check implements BukkitEventCheck {

    public AutoTotemG(TotemPlayer totemPlayer) {
        super(totemPlayer);
    }

    @Override
    public void onBukkitEvent(Event event) {
        if (event instanceof PlayerPickItemEvent playerPickItemEvent) {
            onPlayerPickItem(playerPickItemEvent);
        }
    }

    public void onPlayerPickItem(PlayerPickItemEvent event) {
        Player bukkitPlayer = event.getPlayer();
        if (bukkitPlayer.getGameMode() == GameMode.CREATIVE) return;

        int sourceSlot = event.getSourceSlot();
        if (sourceSlot < 0) {
            return;
        }

        ItemStack item = bukkitPlayer.getInventory().getItem(sourceSlot);
        if (item == null) {
            return;
        }

        Material sourceItemMaterial = item.getType();
        if (sourceItemMaterial == Material.TOTEM_OF_UNDYING) {
            fail(createComponent(sourceSlot));
        }
    }

    private Component createComponent(int sourceSlot) {
        return Component.text()
                .append(Component.text("Source Slot: ", color.getX()))
                .append(Component.text(sourceSlot, color.getY()))
                .build();
    }

}
