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

package com.deathmotion.totemguard.common.commands.impl;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.commands.Command;
import com.deathmotion.totemguard.common.platform.sender.Sender;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.inventory.InventoryConstants;
import com.deathmotion.totemguard.common.player.inventory.PacketInventory;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.jetbrains.annotations.NotNull;

public class InventoryCommand implements Command {

    private static Component labeledSlot(String label, int slot, PacketInventory inv) {
        return Component.text(label + ": ", NamedTextColor.GRAY)
                .append(formatItem(inv.getItem(slot), NamedTextColor.WHITE))
                .append(Component.text(" (slot " + slot + ")", NamedTextColor.DARK_GRAY))
                .append(Component.newline());
    }

    private static Component formatItem(ItemStack item, NamedTextColor color) {
        if (item == null || item.isEmpty()) {
            return Component.text("Empty", NamedTextColor.DARK_GRAY);
        }

        String typeName = item.getType().getName().getKey();
        int amount = item.getAmount();

        return Component.text(typeName + " x" + amount, color);
    }

    @Override
    public void register(CommandManager<Sender> manager) {
        manager.command(
                manager.commandBuilder("totemguard", "tg")
                        .literal("inventory")
                        .handler(this::handleInventoryTest)
        );
    }

    private void handleInventoryTest(@NotNull CommandContext<Sender> context) {
        if (!context.sender().isPlayer()) {
            context.sender().sendMessage(Component.text("You must be a player to use this command!", NamedTextColor.RED));
            return;
        }

        TGPlayer player = TGPlatform.getInstance().getPlayerRepository().getPlayer(context.sender().getUniqueId());
        if (player == null) {
            context.sender().sendMessage(Component.text("Your player data could not be found in the player repository", NamedTextColor.RED));
            return;
        }

        PacketInventory inv = player.getInventory();

        Component msg = Component.empty()
                .append(Component.text("Packet Inventory Overview", NamedTextColor.GREEN, TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.text("Selected hotbar index: ", NamedTextColor.GRAY))
                .append(Component.text(String.valueOf(inv.getSelectedHotbarIndex()), NamedTextColor.YELLOW))
                .append(Component.text(" (container slot ", NamedTextColor.GRAY))
                .append(Component.text(String.valueOf(inv.getMainHandSlot()), NamedTextColor.YELLOW))
                .append(Component.text(")", NamedTextColor.GRAY))
                .append(Component.newline())
                .append(Component.text("Carried: ", NamedTextColor.GRAY))
                .append(formatItem(inv.getCarriedItem(), NamedTextColor.WHITE))
                .append(Component.newline())
                .append(Component.text("────────────────────────────", NamedTextColor.DARK_GRAY))
                .append(Component.newline())
                .append(Component.newline());

        msg = msg.append(Component.text("Hands", NamedTextColor.AQUA, TextDecoration.BOLD)).append(Component.newline());
        msg = msg.append(Component.text("  Main hand: ", NamedTextColor.GRAY))
                .append(formatItem(inv.getMainHandItem(), NamedTextColor.WHITE))
                .append(Component.newline());
        msg = msg.append(Component.text("  Offhand:   ", NamedTextColor.GRAY))
                .append(formatItem(inv.getOffhandItem(), NamedTextColor.WHITE))
                .append(Component.newline())
                .append(Component.newline());

        msg = msg.append(Component.text("Armor", NamedTextColor.AQUA, TextDecoration.BOLD)).append(Component.newline());
        msg = msg.append(labeledSlot("  Helmet", InventoryConstants.SLOT_HELMET, inv));
        msg = msg.append(labeledSlot("  Chest", InventoryConstants.SLOT_CHESTPLATE, inv));
        msg = msg.append(labeledSlot("  Legs", InventoryConstants.SLOT_LEGGINGS, inv));
        msg = msg.append(labeledSlot("  Boots", InventoryConstants.SLOT_BOOTS, inv));
        msg = msg.append(Component.newline());

        msg = msg.append(Component.text("Crafting", NamedTextColor.AQUA, TextDecoration.BOLD)).append(Component.newline());
        msg = msg.append(labeledSlot("  Result", InventoryConstants.SLOT_CRAFT_RESULT, inv));
        msg = msg.append(labeledSlot("  Slot 1", InventoryConstants.SLOT_CRAFT_1, inv));
        msg = msg.append(labeledSlot("  Slot 2", InventoryConstants.SLOT_CRAFT_2, inv));
        msg = msg.append(labeledSlot("  Slot 3", InventoryConstants.SLOT_CRAFT_3, inv));
        msg = msg.append(labeledSlot("  Slot 4", InventoryConstants.SLOT_CRAFT_4, inv));
        msg = msg.append(Component.newline());

        msg = msg.append(Component.text("Hotbar", NamedTextColor.AQUA, TextDecoration.BOLD)).append(Component.newline());
        int selected = inv.getSelectedHotbarIndex();
        for (int i = 0; i < 9; i++) {
            int slot = InventoryConstants.HOTBAR_START + i;
            boolean isSelected = (i == selected);

            msg = msg.append(Component.text("  [" + i + "] ", NamedTextColor.GRAY))
                    .append(isSelected
                            ? Component.text("▶ ", NamedTextColor.GOLD)
                            : Component.text("  ", NamedTextColor.DARK_GRAY))
                    .append(formatItem(inv.getItem(slot), isSelected ? NamedTextColor.YELLOW : NamedTextColor.WHITE))
                    .append(Component.text(" (slot " + slot + ")", NamedTextColor.DARK_GRAY))
                    .append(Component.newline());
        }
        msg = msg.append(Component.newline());

        msg = msg.append(Component.text("Inventory", NamedTextColor.AQUA, TextDecoration.BOLD))
                .append(Component.newline());

        for (int row = 0; row < 3; row++) {
            int base = InventoryConstants.ITEMS_START + (row * 9);

            msg = msg.append(Component.text("  Row " + (row + 1), NamedTextColor.GRAY))
                    .append(Component.newline());

            for (int col = 0; col < 9; col++) {
                int slot = base + col;

                msg = msg.append(Component.text("    Slot " + slot + ": ", NamedTextColor.DARK_GRAY))
                        .append(formatItem(inv.getItem(slot), NamedTextColor.WHITE))
                        .append(Component.newline());
            }
        }

        context.sender().sendMessage(msg);
    }
}
