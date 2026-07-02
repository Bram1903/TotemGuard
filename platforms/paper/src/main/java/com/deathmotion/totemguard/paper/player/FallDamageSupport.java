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

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

final class FallDamageSupport {

    private static final boolean AVAILABLE = detect();

    private FallDamageSupport() {
    }

    private static boolean detect() {
        try {
            Class.forName("org.bukkit.damage.DamageSource");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    static void damage(Player player, double amount) {
        if (AVAILABLE) {
            Modern.damage(player, amount);
        } else {
            Legacy.damage(player, amount);
        }
    }

    private static final class Modern {

        static void damage(Player player, double amount) {
            player.damage(amount, org.bukkit.damage.DamageSource.builder(org.bukkit.damage.DamageType.FALL).build());
        }
    }

    @SuppressWarnings("deprecation")
    private static final class Legacy {

        static void damage(Player player, double amount) {
            double reduced = amount * fallSpecificMultiplier(player);
            if (reduced <= 0.0) return;
            player.damage(reduced);
        }

        private static double fallSpecificMultiplier(Player player) {
            Enchantment featherFalling = Enchantment.getByName("PROTECTION_FALL");
            Enchantment protection = Enchantment.getByName("PROTECTION_ENVIRONMENTAL");
            int protectionEpf = 0;
            int fallEpf = 0;
            for (ItemStack piece : player.getInventory().getArmorContents()) {
                if (piece == null) continue;
                if (featherFalling != null) fallEpf += piece.getEnchantmentLevel(featherFalling) * 3;
                if (protection != null) protectionEpf += piece.getEnchantmentLevel(protection);
            }
            double target = 1.0 - Math.min(20, fallEpf + protectionEpf) / 25.0;
            double pipeline = 1.0 - Math.min(20, protectionEpf) / 25.0;
            return pipeline <= 0.0 ? 1.0 : target / pipeline;
        }
    }
}
