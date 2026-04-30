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

package com.deathmotion.totemguard.common.commands.impl;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.commands.AbstractCommand;
import com.deathmotion.totemguard.common.platform.sender.Sender;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.inventory.InventoryConstants;
import com.deathmotion.totemguard.common.player.inventory.PacketInventory;
import com.deathmotion.totemguard.common.util.Palette;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.jetbrains.annotations.NotNull;

/**
 * Snapshot-only inventory dump. Output is hardcoded against {@link Palette} on purpose —
 * this is a developer-facing tool, not user-configurable surface.
 */
public final class InventoryCommand extends AbstractCommand {

    private static Component labeledSlot(String label, int slot, PacketInventory inv) {
        return Component.text(label + ": ", Palette.LABEL)
                .append(formatItem(inv.getItem(slot), Palette.PARCH_50))
                .append(Component.text(" (slot " + slot + ")", Palette.CAPTION))
                .append(Component.newline());
    }

    private static Component formatItem(ItemStack item, TextColor color) {
        if (item == null || item.isEmpty()) {
            return Component.text("Empty", Palette.CAPTION);
        }

        String typeName = item.getType().getName().getKey();
        int amount = item.getAmount();

        return Component.text(typeName + " x" + amount, color);
    }

    @Override
    public void register(@NonNull CommandManager<Sender> manager) {
        manager.command(
                base(manager)
                        .literal("inventory")
                        .permission(perm("inventory"))
                        .handler(this::handleInventoryTest)
        );
    }

    private void handleInventoryTest(@NotNull CommandContext<Sender> context) {
        final Sender sender = context.sender();
        if (!requirePlayer(sender)) {
            return;
        }

        TGPlayer player = TGPlatform.getInstance().getPlayerRepository().getPlayer(sender.getUniqueId());
        if (player == null) {
            sender.sendMessage(Component.text("Your player data could not be found in the player repository", Palette.DANGER));
            return;
        }

        PacketInventory inv = player.getInventory();

        Component msg = Component.empty()
                .append(Component.text("Packet Inventory Overview", Palette.BRAND, TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.text("Open Inventory: ", Palette.LABEL))
                .append(
                        Component.text(
                                player.getData().isOpenInventory() ? "Yes" : "No",
                                player.getData().isOpenInventory() ? Palette.SUCCESS : Palette.DANGER
                        )
                )
                .append(Component.newline())
                .append(Component.text("Selected hotbar index: ", Palette.LABEL))
                .append(Component.text(String.valueOf(inv.getSelectedHotbarIndex()), Palette.VALUE))
                .append(Component.text(" (container slot ", Palette.CONNECTIVE))
                .append(Component.text(String.valueOf(inv.getMainHandSlot()), Palette.VALUE))
                .append(Component.text(")", Palette.CONNECTIVE))
                .append(Component.newline())
                .append(Component.text("Carried: ", Palette.LABEL))
                .append(formatItem(inv.getCarriedItem().getCurrentItem(), Palette.PARCH_50))
                .append(Component.newline())
                .append(Component.text("────────────────────────────", Palette.CAPTION))
                .append(Component.newline())
                .append(Component.newline());

        msg = msg.append(Component.text("Hands", Palette.BRAND, TextDecoration.BOLD)).append(Component.newline());
        msg = msg.append(Component.text("  Main hand: ", Palette.LABEL))
                .append(formatItem(inv.getMainHandItem(), Palette.PARCH_50))
                .append(Component.newline());
        msg = msg.append(Component.text("  Offhand:   ", Palette.LABEL))
                .append(formatItem(inv.getOffhandItem(), Palette.PARCH_50))
                .append(Component.newline())
                .append(Component.newline());

        msg = msg.append(Component.text("Armor", Palette.BRAND, TextDecoration.BOLD)).append(Component.newline());
        msg = msg.append(labeledSlot("  Helmet", InventoryConstants.SLOT_HELMET, inv));
        msg = msg.append(labeledSlot("  Chest", InventoryConstants.SLOT_CHESTPLATE, inv));
        msg = msg.append(labeledSlot("  Legs", InventoryConstants.SLOT_LEGGINGS, inv));
        msg = msg.append(labeledSlot("  Boots", InventoryConstants.SLOT_BOOTS, inv));
        msg = msg.append(Component.newline());

        msg = msg.append(Component.text("Crafting", Palette.BRAND, TextDecoration.BOLD)).append(Component.newline());
        msg = msg.append(labeledSlot("  Result", InventoryConstants.SLOT_CRAFT_RESULT, inv));
        msg = msg.append(labeledSlot("  Slot 1", InventoryConstants.SLOT_CRAFT_1, inv));
        msg = msg.append(labeledSlot("  Slot 2", InventoryConstants.SLOT_CRAFT_2, inv));
        msg = msg.append(labeledSlot("  Slot 3", InventoryConstants.SLOT_CRAFT_3, inv));
        msg = msg.append(labeledSlot("  Slot 4", InventoryConstants.SLOT_CRAFT_4, inv));
        msg = msg.append(Component.newline());

        msg = msg.append(Component.text("Hotbar", Palette.BRAND, TextDecoration.BOLD)).append(Component.newline());
        int selected = inv.getSelectedHotbarIndex();
        for (int i = 0; i < 9; i++) {
            int slot = InventoryConstants.HOTBAR_START + i;
            boolean isSelected = (i == selected);

            msg = msg.append(Component.text("  [" + i + "] ", Palette.CAPTION))
                    .append(isSelected
                            ? Component.text("▶ ", Palette.BRAND)
                            : Component.text("  ", Palette.CAPTION))
                    .append(formatItem(inv.getItem(slot), isSelected ? Palette.VALUE : Palette.PARCH_50))
                    .append(Component.text(" (slot " + slot + ")", Palette.CAPTION))
                    .append(Component.newline());
        }
        msg = msg.append(Component.newline());

        msg = msg.append(Component.text("Inventory", Palette.BRAND, TextDecoration.BOLD))
                .append(Component.newline());

        for (int row = 0; row < 3; row++) {
            int base = InventoryConstants.ITEMS_START + (row * 9);

            msg = msg.append(Component.text("  Row " + (row + 1), Palette.LABEL))
                    .append(Component.newline());

            for (int col = 0; col < 9; col++) {
                int slot = base + col;

                msg = msg.append(Component.text("    Slot " + slot + ": ", Palette.CAPTION))
                        .append(formatItem(inv.getItem(slot), Palette.PARCH_50))
                        .append(Component.newline());
            }
        }

        context.sender().sendMessage(msg);
    }
}
