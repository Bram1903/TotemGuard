/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2026 Bram and contributors
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

package com.deathmotion.totemguard.paper.player;

import com.deathmotion.totemguard.paper.scheduler.PaperScheduler;
import com.deathmotion.totemguard.common.platform.player.ManualCheckHandle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

final class PaperManualCheckHandle implements ManualCheckHandle {

    private final PaperScheduler scheduler;
    private final Player player;
    private final ItemStack[] contents;
    private final ItemStack cursor;
    private final double health;
    private final int foodLevel;
    private final float saturation;
    private final Collection<PotionEffect> effects;
    private final AtomicBoolean restored = new AtomicBoolean();

    PaperManualCheckHandle(PaperScheduler scheduler, Player player, ItemStack[] contents, ItemStack cursor,
                            double health, int foodLevel, float saturation,
                            Collection<PotionEffect> effects) {
        this.scheduler = scheduler;
        this.player = player;
        this.contents = contents;
        this.cursor = cursor;
        this.health = health;
        this.foodLevel = foodLevel;
        this.saturation = saturation;
        this.effects = effects;
    }

    @Override
    public void restore() {
        if (!restored.compareAndSet(false, true)) {
            return;
        }

        scheduler.runForEntity(player, () -> {
            if (!player.isOnline()) {
                return;
            }

            // Strip the regen/fire-resistance/absorption the totem just granted so the
            // check leaves no visible trace beyond the pop animation the player already saw.
            for (PotionEffect active : player.getActivePotionEffects()) {
                PotionEffectType type = active.getType();
                player.removePotionEffect(type);
            }
            player.addPotionEffects(effects);

            player.setHealth(Math.min(health, player.getMaxHealth()));
            player.setFoodLevel(foodLevel);
            player.setSaturation(saturation);

            PlayerInventory inventory = player.getInventory();
            inventory.setContents(contents);
            player.getOpenInventory().setCursor(cursor);
        }, null);
    }
}
