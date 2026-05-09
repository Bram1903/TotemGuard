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

package com.deathmotion.totemguard.common.gui.screen.player;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.config.key.MessagesKeys;
import com.deathmotion.totemguard.common.features.monitor.MonitorRepository;
import com.deathmotion.totemguard.common.features.monitor.MonitorSnapshot;
import com.deathmotion.totemguard.common.gui.*;
import com.deathmotion.totemguard.common.message.MessageService;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.inventory.InventoryConstants;
import com.deathmotion.totemguard.common.util.Palette;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import net.kyori.adventure.text.Component;

import java.util.*;

public final class PlayerMonitorScreen extends GuiScreen {

    public static final String PERMISSION = "TotemGuard.Gui.Monitor";

    private final UUID targetId;
    private final String fallbackName;
    private volatile @org.jetbrains.annotations.Nullable UserProfile cachedRemoteProfile;

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
    public void onOpen(GuiSession session) {
        TGPlatform platform = TGPlatform.getInstance();
        MonitorRepository monitor = platform.getMonitorRepository();
        if (monitor != null) monitor.openLocalMonitor(targetId);

        if (platform.getPlayerRepository().getPlayer(targetId) == null
                && platform.getNetworkPresenceRepository() != null) {
            platform.getScheduler().runAsyncTask(() -> {
                try {
                    this.cachedRemoteProfile = platform.getNetworkPresenceRepository().loadProfile(targetId);
                } catch (Exception ignored) {
                } finally {
                    if (session.currentScreen() == this) {
                        platform.getGuiManager().refresh(session.viewerId());
                    }
                }
            });
        }
    }

    @Override
    public void onClose(GuiSession session) {
        MonitorRepository monitor = TGPlatform.getInstance().getMonitorRepository();
        if (monitor != null) monitor.closeLocalMonitor(targetId);
    }

    @Override
    public GuiRenderResult render(GuiSession session) {
        TGPlatform platform = TGPlatform.getInstance();
        MessageService messages = platform.getMessageService();

        TGPlayer local = platform.getPlayerRepository().getPlayer(targetId);
        MonitorRepository monitor = platform.getMonitorRepository();
        MonitorSnapshot snapshot = local != null
                ? MonitorSnapshot.captureFrom(local,
                platform.getNetworkPresenceRepository().getLocalServerName())
                : (monitor != null ? monitor.lastSnapshot(targetId) : null);

        String targetName = snapshot != null ? snapshot.playerName() : fallbackName;

        GuiRenderResult.Builder builder = GuiRenderResult.builder(6,
                GuiTitle.of(messages.getString(MessagesKeys.GUI_MONITOR_TITLE, Map.of("tg_player", targetName))));

        if (session.viewerId().equals(targetId)) {
            builder.fillEmpty(GuiItems.filler());
            builder.set(0, singleExitButton(session, messages), ctx -> {
                if (session.hasParent()) {
                    ctx.back();
                    return;
                }
                ctx.close();
            });
            builder.set(13, GuiItems.simple(
                    ItemTypes.BARRIER,
                    messages.getComponent(MessagesKeys.GUI_MONITOR_SELF_DISABLED_TITLE),
                    List.of(messages.getComponent(MessagesKeys.GUI_MONITOR_SELF_DISABLED_LORE))
            ));
            return builder.build();
        }

        if (snapshot == null) {
            builder.set(0, singleExitButton(session, messages), ctx -> {
                if (session.hasParent()) {
                    ctx.back();
                    return;
                }
                ctx.close();
            });
            builder.set(8, emptyPane("Player"));
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

        UserProfile profile;
        if (local != null) {
            profile = local.getUser().getProfile();
        } else {
            UserProfile cached = this.cachedRemoteProfile;
            profile = cached != null ? cached : new UserProfile(snapshot.targetUuid(), snapshot.playerName());
        }

        builder.set(0, singleExitButton(session, messages), ctx -> {
            if (session.hasParent()) {
                ctx.back();
                return;
            }
            ctx.close();
        });

        builder.set(8, GuiItems.playerHead(
                profile,
                Component.text(snapshot.playerName(), Palette.SUCCESS),
                List.of(
                        GuiText.line("Client version", snapshot.clientVersion()),
                        GuiText.line("Brand", snapshot.clientBrand()),
                        GuiText.line("Selected hotbar", String.valueOf(snapshot.selectedHotbarIndex())),
                        GuiText.line("Server", snapshot.serverName())
                )
        ), ctx -> {
            if (local != null) ctx.open(new PlayerProfileScreen(local));
        });

        builder.set(1, displaySlot(
                snapshot.mainHandItem(),
                "Main Hand",
                List.of(
                        GuiText.line("Packet slot", String.valueOf(snapshot.mainHandSlot())),
                        GuiText.line("Summary", GuiText.itemSummary(snapshot.mainHandItem()))
                ),
                ItemTypes.WHITE_STAINED_GLASS_PANE
        ));
        builder.set(2, displaySlot(
                snapshot.offhandItem(),
                "Offhand",
                List.of(
                        GuiText.line("Packet slot", String.valueOf(InventoryConstants.SLOT_OFFHAND)),
                        GuiText.line("Summary", GuiText.itemSummary(snapshot.offhandItem()))
                ),
                ItemTypes.WHITE_STAINED_GLASS_PANE
        ));
        builder.set(3, displaySlot(
                snapshot.carriedItem(),
                "Carried Item",
                List.of(
                        GuiText.line("Updated slot", String.valueOf(snapshot.carriedItemSlot())),
                        GuiText.line("Summary", GuiText.itemSummary(snapshot.carriedItem()))
                ),
                ItemTypes.WHITE_STAINED_GLASS_PANE
        ));
        builder.set(4, GuiItems.simple(
                ItemTypes.PAPER,
                messages.getComponent(MessagesKeys.GUI_MONITOR_PACKET_STATE_TITLE),
                List.of(
                        GuiText.line("Last issuer", snapshot.lastIssuer()),
                        GuiText.line("Selected hotbar", String.valueOf(snapshot.selectedHotbarIndex())),
                        GuiText.line("Main hand slot", String.valueOf(snapshot.mainHandSlot()))
                )
        ));

        builder.set(5, GuiItems.simple(
                snapshot.inventoryOpen() ? ItemTypes.GREEN_WOOL : ItemTypes.RED_WOOL,
                Component.text(snapshot.inventoryOpen() ? "Inventory Open" : "Inventory Closed", Palette.BRAND),
                List.of(GuiText.status("Inventory open", snapshot.inventoryOpen()))
        ));

        builder.set(6, GuiItems.simple(
                ItemTypes.COMPARATOR,
                messages.getComponent(MessagesKeys.GUI_MONITOR_LATENCY_TITLE),
                List.of(
                        GuiText.line("Transaction ping", String.valueOf(snapshot.transactionPing())),
                        GuiText.line("KeepAlive ping", String.valueOf(snapshot.keepAlivePing())),
                        GuiText.line("Pending tx", String.valueOf(snapshot.pendingTransactionCount()))
                )
        ));

        builder.set(7, GuiItems.simple(
                ItemTypes.BOOK,
                messages.getComponent(MessagesKeys.GUI_MONITOR_CLIENT_TITLE),
                List.of(
                        GuiText.line("Client version", snapshot.clientVersion()),
                        GuiText.line("Brand", snapshot.clientBrand()),
                        messages.getComponent(MessagesKeys.GUI_MONITOR_HEAD_TOOLTIP)
                )
        ));

        for (int slot = 9; slot <= 13; slot++) {
            builder.set(slot, separatorPane());
        }

        builder.set(14, displaySlot(
                snapshot.itemAt(InventoryConstants.SLOT_HELMET),
                "Helmet",
                List.of(GuiText.line("Packet slot", String.valueOf(InventoryConstants.SLOT_HELMET))),
                ItemTypes.WHITE_STAINED_GLASS_PANE
        ));
        builder.set(15, displaySlot(
                snapshot.itemAt(InventoryConstants.SLOT_CHESTPLATE),
                "Chestplate",
                List.of(GuiText.line("Packet slot", String.valueOf(InventoryConstants.SLOT_CHESTPLATE))),
                ItemTypes.WHITE_STAINED_GLASS_PANE
        ));
        builder.set(16, displaySlot(
                snapshot.itemAt(InventoryConstants.SLOT_LEGGINGS),
                "Leggings",
                List.of(GuiText.line("Packet slot", String.valueOf(InventoryConstants.SLOT_LEGGINGS))),
                ItemTypes.WHITE_STAINED_GLASS_PANE
        ));
        builder.set(17, displaySlot(
                snapshot.itemAt(InventoryConstants.SLOT_BOOTS),
                "Boots",
                List.of(GuiText.line("Packet slot", String.valueOf(InventoryConstants.SLOT_BOOTS))),
                ItemTypes.WHITE_STAINED_GLASS_PANE
        ));

        copyRange(builder, snapshot, InventoryConstants.ITEMS_START, 18, 27);
        copyRange(builder, snapshot, InventoryConstants.HOTBAR_START, 45, 9);

        return builder.build();
    }

    private void copyRange(GuiRenderResult.Builder builder, MonitorSnapshot snapshot, int sourceStart, int targetStart, int amount) {
        for (int index = 0; index < amount; index++) {
            ItemStack item = snapshot.itemAt(sourceStart + index);
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

    private ItemStack emptyPane(String label, List<Component> lore, ItemType type) {
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
        return GuiItems.filler(ItemTypes.WHITE_STAINED_GLASS_PANE);
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
