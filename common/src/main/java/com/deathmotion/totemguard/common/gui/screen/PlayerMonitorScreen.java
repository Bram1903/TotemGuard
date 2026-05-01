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
import com.deathmotion.totemguard.common.config.key.MessagesKeys;
import com.deathmotion.totemguard.common.gui.*;
import com.deathmotion.totemguard.common.message.MessageService;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.inventory.InventoryConstants;
import com.deathmotion.totemguard.common.player.inventory.PacketInventory;
import com.deathmotion.totemguard.common.util.Palette;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import net.kyori.adventure.text.Component;

import java.util.*;

public final class PlayerMonitorScreen extends GuiScreen {

    public static final String PERMISSION = "TotemGuard.Gui.Monitor";
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
    public String requiredPermission() {
        return PERMISSION;
    }

    @Override
    public Set<GuiSubscriptionKey> subscriptionKeys() {
        return Set.of(GuiSubscriptionKey.monitor(targetId));
    }

    @Override
    public GuiRenderResult render(GuiSession session) {
        TGPlatform platform = TGPlatform.getInstance();
        MessageService messages = platform.getMessageService();
        TGPlayer target = platform.getPlayerRepository().getPlayer(targetId);
        String targetName = target != null ? target.getName() : fallbackName;

        GuiRenderResult.Builder builder = GuiRenderResult.builder(6,
                GuiTitle.of(messages.getString(MessagesKeys.GUI_MONITOR_TITLE, Map.of("tg_player", targetName))));

        if (session.viewerId().equals(targetId)) {
            builder.fillEmpty(GuiItems.filler());
            builder.set(13, GuiItems.simple(
                    ItemTypes.BARRIER,
                    messages.getComponent(MessagesKeys.GUI_MONITOR_SELF_DISABLED_TITLE),
                    List.of(messages.getComponent(MessagesKeys.GUI_MONITOR_SELF_DISABLED_LORE))
            ));
            builder.set(49, singleExitButton(session, messages), ctx -> {
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
            builder.set(8, singleExitButton(session, messages), ctx -> {
                if (session.hasParent()) {
                    ctx.back();
                    return;
                }
                ctx.close();
            });
            builder.set(13, GuiItems.simple(
                    ItemTypes.RED_CONCRETE,
                    messages.getComponent(MessagesKeys.GUI_MONITOR_UNTRACKED_TITLE, Map.of("tg_player", targetName)),
                    List.of(
                            GuiText.line("UUID", targetId.toString()),
                            messages.getComponent(MessagesKeys.GUI_MONITOR_UNTRACKED_LORE)
                    )
            ));
            return builder.build();
        }

        PacketInventory inventory = target.getInventory();

        builder.set(0, GuiItems.playerHead(
                target.getUser().getProfile(),
                Component.text(target.getName(), Palette.SUCCESS),
                List.of(
                        GuiText.line("Client version", target.getClientVersion().getReleaseName()),
                        GuiText.line("Brand", target.getClientBrand()),
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
                messages.getComponent(MessagesKeys.GUI_MONITOR_PACKET_STATE_TITLE),
                List.of(
                        GuiText.line("Last issuer", String.valueOf(inventory.getLastIssuer())),
                        GuiText.line("Selected hotbar", String.valueOf(inventory.getSelectedHotbarIndex())),
                        GuiText.line("Main hand slot", String.valueOf(inventory.getMainHandSlot()))
                )
        ));

        builder.set(5, GuiItems.simple(
                target.getData().isOpenInventory() ? ItemTypes.GREEN_WOOL : ItemTypes.RED_WOOL,
                Component.text(target.getData().isOpenInventory() ? "Inventory Open" : "Inventory Closed", Palette.BRAND),
                List.of(GuiText.status("Inventory open", target.getData().isOpenInventory()))
        ));

        builder.set(6, GuiItems.simple(
                ItemTypes.COMPARATOR,
                messages.getComponent(MessagesKeys.GUI_MONITOR_LATENCY_TITLE),
                List.of(
                        GuiText.line("Transaction ping", String.valueOf(target.getPingData().getTransactionPing())),
                        GuiText.line("KeepAlive ping", String.valueOf(target.getPingData().getKeepAlivePing())),
                        GuiText.line("Pending tx", String.valueOf(target.getPingData().getPendingTransactionCount()))
                )
        ));

        builder.set(7, GuiItems.simple(
                ItemTypes.BOOK,
                messages.getComponent(MessagesKeys.GUI_MONITOR_CLIENT_TITLE),
                List.of(
                        GuiText.line("Client version", target.getClientVersion().getReleaseName()),
                        GuiText.line("Brand", target.getClientBrand()),
                        messages.getComponent(MessagesKeys.GUI_MONITOR_HEAD_TOOLTIP)
                )
        ));

        builder.set(8, singleExitButton(session, messages), ctx -> {
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
        fullLore.add(Component.text("Empty", Palette.CONNECTIVE));
        fullLore.addAll(lore);

        return GuiItems.simple(
                type,
                Component.text(label, Palette.PARCH_50),
                fullLore
        );
    }

    private ItemStack separatorPane() {
        return GuiItems.simple(
                ItemTypes.WHITE_STAINED_GLASS_PANE,
                Component.text(" ", Palette.PARCH_50),
                List.of()
        );
    }

    private ItemStack singleExitButton(GuiSession session, MessageService messages) {
        if (session.hasParent()) {
            return GuiItems.simple(
                    ItemTypes.ARROW,
                    messages.getComponent(MessagesKeys.GUI_BTN_RETURN_TITLE),
                    List.of(messages.getComponent(MessagesKeys.GUI_BTN_RETURN_LORE))
            );
        }

        return GuiItems.simple(
                ItemTypes.BARRIER,
                messages.getComponent(MessagesKeys.GUI_BTN_CLOSE_TITLE),
                List.of(messages.getComponent(MessagesKeys.GUI_BTN_CLOSE_LORE))
        );
    }
}
