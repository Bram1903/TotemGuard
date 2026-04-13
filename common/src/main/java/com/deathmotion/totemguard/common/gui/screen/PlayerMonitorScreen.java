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

package com.deathmotion.totemguard.common.gui.screen;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.gui.*;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.inventory.InventoryConstants;
import com.deathmotion.totemguard.common.player.inventory.PacketInventory;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class PlayerMonitorScreen extends GuiScreen {

    private final UUID targetId;
    private final String fallbackName;

    public PlayerMonitorScreen(TGPlayer player) {
        this(player.getUuid(), player.getName());
    }

    public PlayerMonitorScreen(UUID targetId, String fallbackName) {
        this.targetId = targetId;
        this.fallbackName = fallbackName;
    }

    @Override
    public Set<GuiSubscriptionKey> subscriptionKeys() {
        return Set.of(GuiSubscriptionKey.monitor(targetId));
    }

    @Override
    public GuiRenderResult render(GuiSession session) {
        TGPlayer target = TGPlatform.getInstance().getPlayerRepository().getPlayer(targetId);
        String targetName = target != null ? target.getName() : fallbackName;

        GuiRenderResult.Builder builder = GuiRenderResult.builder(6, Component.text("Monitor: " + targetName, NamedTextColor.GOLD));

        if (session.viewerId().equals(targetId)) {
            builder.fillEmpty(GuiItems.filler());
            builder.set(13, GuiItems.simple(
                    ItemTypes.BARRIER,
                    Component.text("Self Monitor Disabled", NamedTextColor.RED),
                    List.of(Component.text(
                            "Monitoring your own inventory is disabled to prevent ghost item desync.",
                            NamedTextColor.GRAY
                    ))
            ));
            builder.set(49, singleExitButton(session), ctx -> {
                if (session.hasParent()) {
                    ctx.back();
                    return;
                }
                ctx.close();
            });
            return builder.build();
        }

        if (target == null) {
            builder.set(0, emptyPane("Player"));
            builder.set(8, singleExitButton(session), ctx -> {
                if (session.hasParent()) {
                    ctx.back();
                    return;
                }
                ctx.close();
            });
            builder.set(13, GuiItems.simple(
                    ItemTypes.RED_CONCRETE,
                    Component.text(targetName + " is no longer tracked", NamedTextColor.RED),
                    List.of(
                            GuiText.line("UUID", targetId.toString()),
                            Component.text("All monitor viewers are closed", NamedTextColor.GRAY)
                    )
            ));
            return builder.build();
        }

        PacketInventory inventory = target.getInventory();

        builder.set(0, GuiItems.playerHead(
                target.getUser().getProfile(),
                Component.text(target.getName(), NamedTextColor.GREEN),
                List.of(
                        GuiText.line("Client version", target.getClientVersion().getReleaseName()),
                        GuiText.line("Brand", target.getClientBrand() == null ? "Unknown" : target.getClientBrand()),
                        GuiText.line("Selected hotbar", String.valueOf(inventory.getSelectedHotbarIndex()))
                )
        ), ctx -> ctx.open(new PlayerProfileScreen(target)));

        builder.set(1, displaySlot(
                inventory.getMainHandItem(),
                "Main Hand",
                List.of(
                        GuiText.line("Packet slot", String.valueOf(inventory.getMainHandSlot())),
                        GuiText.line("Summary", GuiText.itemSummary(inventory.getMainHandItem()))
                ),
                ItemTypes.WHITE_STAINED_GLASS_PANE
        ));
        builder.set(2, displaySlot(
                inventory.getOffhandItem(),
                "Offhand",
                List.of(
                        GuiText.line("Packet slot", String.valueOf(InventoryConstants.SLOT_OFFHAND)),
                        GuiText.line("Summary", GuiText.itemSummary(inventory.getOffhandItem()))
                ),
                ItemTypes.WHITE_STAINED_GLASS_PANE
        ));
        builder.set(3, displaySlot(
                inventory.getCarriedItem().getCurrentItem(),
                "Carried Item",
                List.of(
                        GuiText.line("Updated slot", String.valueOf(inventory.getCarriedItem().getSlot())),
                        GuiText.line("Summary", GuiText.itemSummary(inventory.getCarriedItem().getCurrentItem()))
                ),
                ItemTypes.WHITE_STAINED_GLASS_PANE
        ));
        builder.set(4, GuiItems.simple(
                ItemTypes.PAPER,
                Component.text("Packet State", NamedTextColor.AQUA),
                List.of(
                        GuiText.line("Last issuer", String.valueOf(inventory.getLastIssuer())),
                        GuiText.line("Selected hotbar", String.valueOf(inventory.getSelectedHotbarIndex())),
                        GuiText.line("Main hand slot", String.valueOf(inventory.getMainHandSlot()))
                )
        ));

        builder.set(5, GuiItems.simple(
                target.getData().isOpenInventory() ? ItemTypes.GREEN_WOOL : ItemTypes.RED_WOOL,
                Component.text(target.getData().isOpenInventory() ? "Inventory Open" : "Inventory Closed", NamedTextColor.AQUA),
                List.of(GuiText.status("Inventory open", target.getData().isOpenInventory()))
        ));

        builder.set(6, GuiItems.simple(
                ItemTypes.COMPARATOR,
                Component.text("Latency", NamedTextColor.LIGHT_PURPLE),
                List.of(
                        GuiText.line("Transaction ping", String.valueOf(target.getPingData().getTransactionPing())),
                        GuiText.line("KeepAlive ping", String.valueOf(target.getPingData().getKeepAlivePing())),
                        GuiText.line("Pending tx", String.valueOf(target.getPingData().getPendingTransactionCount()))
                )
        ));

        builder.set(7, GuiItems.simple(
                ItemTypes.BOOK,
                Component.text("Client", NamedTextColor.YELLOW),
                List.of(
                        GuiText.line("Client version", target.getClientVersion().getReleaseName()),
                        GuiText.line("Brand", target.getClientBrand() == null ? "Unknown" : target.getClientBrand()),
                        Component.text("Head opens the full profile.", NamedTextColor.GRAY)
                )
        ));

        builder.set(8, singleExitButton(session), ctx -> {
            if (session.hasParent()) {
                ctx.back();
                return;
            }
            ctx.close();
        });

        for (int slot = 9; slot <= 13; slot++) {
            builder.set(slot, separatorPane());
        }

        builder.set(14, displaySlot(
                inventory.getItem(InventoryConstants.SLOT_HELMET),
                "Helmet",
                List.of(GuiText.line("Packet slot", String.valueOf(InventoryConstants.SLOT_HELMET))),
                ItemTypes.WHITE_STAINED_GLASS_PANE
        ));
        builder.set(15, displaySlot(
                inventory.getItem(InventoryConstants.SLOT_CHESTPLATE),
                "Chestplate",
                List.of(GuiText.line("Packet slot", String.valueOf(InventoryConstants.SLOT_CHESTPLATE))),
                ItemTypes.WHITE_STAINED_GLASS_PANE
        ));
        builder.set(16, displaySlot(
                inventory.getItem(InventoryConstants.SLOT_LEGGINGS),
                "Leggings",
                List.of(GuiText.line("Packet slot", String.valueOf(InventoryConstants.SLOT_LEGGINGS))),
                ItemTypes.WHITE_STAINED_GLASS_PANE
        ));
        builder.set(17, displaySlot(
                inventory.getItem(InventoryConstants.SLOT_BOOTS),
                "Boots",
                List.of(GuiText.line("Packet slot", String.valueOf(InventoryConstants.SLOT_BOOTS))),
                ItemTypes.WHITE_STAINED_GLASS_PANE
        ));

        copyRange(builder, inventory, InventoryConstants.ITEMS_START, 18, 27);
        copyRange(builder, inventory, InventoryConstants.HOTBAR_START, 45, 9);

        return builder.build();
    }

    private void copyRange(GuiRenderResult.Builder builder, PacketInventory inventory, int sourceStart, int targetStart, int amount) {
        for (int index = 0; index < amount; index++) {
            ItemStack item = inventory.getItem(sourceStart + index);
            if (item == null || item.isEmpty()) {
                continue;
            }
            builder.set(targetStart + index, item);
        }
    }

    private ItemStack displaySlot(ItemStack item, String label, List<Component> lore, ItemType emptyType) {
        if (item == null || item.isEmpty()) {
            return emptyPane(label, lore, emptyType);
        }

        return item;
    }

    private ItemStack emptyPane(String label) {
        return emptyPane(label, List.of());
    }

    private ItemStack emptyPane(String label, List<Component> lore) {
        return emptyPane(label, lore, ItemTypes.WHITE_STAINED_GLASS_PANE);
    }

    private ItemStack emptyPane(String label, List<Component> lore, com.github.retrooper.packetevents.protocol.item.type.ItemType type) {
        List<Component> fullLore = new ArrayList<>(lore.size() + 1);
        fullLore.add(Component.text("Empty", NamedTextColor.GRAY));
        fullLore.addAll(lore);

        return GuiItems.simple(
                type,
                Component.text(label, NamedTextColor.WHITE),
                fullLore
        );
    }

    private ItemStack separatorPane() {
        return GuiItems.simple(
                ItemTypes.WHITE_STAINED_GLASS_PANE,
                Component.text(" ", NamedTextColor.WHITE),
                List.of()
        );
    }

    private ItemStack singleExitButton(GuiSession session) {
        if (session.hasParent()) {
            return GuiItems.simple(
                    ItemTypes.ARROW,
                    Component.text("Return", NamedTextColor.GOLD),
                    List.of(Component.text("Return to the previous screen", NamedTextColor.GRAY))
            );
        }

        return GuiItems.simple(
                ItemTypes.BARRIER,
                Component.text("Close", NamedTextColor.RED),
                List.of(Component.text("Close this screen", NamedTextColor.GRAY))
        );
    }
}
