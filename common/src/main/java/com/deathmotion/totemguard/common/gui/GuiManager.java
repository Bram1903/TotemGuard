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

package com.deathmotion.totemguard.common.gui;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.event.internal.impl.InventoryChangedEvent;
import com.deathmotion.totemguard.common.platform.player.PlatformUser;
import com.deathmotion.totemguard.common.platform.player.PlatformUserCreation;
import com.deathmotion.totemguard.common.platform.sender.Sender;
import com.deathmotion.totemguard.common.player.inventory.InventoryConstants;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import com.github.retrooper.packetevents.wrapper.play.server.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public final class GuiManager {

    private static final int MAX_WINDOW_ID = 100;

    private final TGPlatform platform;
    private final ConcurrentMap<UUID, GuiSession> sessions = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, GuiViewerInventory> viewerInventories = new ConcurrentHashMap<>();
    private final ConcurrentMap<GuiSubscriptionKey, Set<UUID>> subscriptions = new ConcurrentHashMap<>();
    private final AtomicInteger nextWindowId = new AtomicInteger(1);

    public GuiManager() {
        this.platform = TGPlatform.getInstance();
        this.platform.getEventRepository().subscribeInternal(InventoryChangedEvent.class, this::handleInventoryChanged);
    }

    private GuiViewerInventory inventoryFor(UUID viewerId) {
        return viewerInventories.computeIfAbsent(viewerId, ignored -> new GuiViewerInventory());
    }

    public boolean open(Sender sender, GuiScreen screen) {
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(screen, "screen");

        if (!sender.isPlayer()) {
            return false;
        }

        User user = PacketEvents.getAPI().getPlayerManager().getUser(sender.getNativeSender());
        if (user == null || user.getUUID() == null) {
            return false;
        }

        String required = screen.requiredPermission();
        if (required != null && !sender.hasPermission(required)) {
            sender.sendMessage(deniedMessage(required));
            return false;
        }

        UUID viewerId = user.getUUID();
        close(viewerId, false);

        GuiSession session = new GuiSession(viewerId, user, allocateWindowId());
        session.push(screen);
        screen.onOpen(session);
        sessions.put(viewerId, session);
        render(session, true);
        return true;
    }

    public void pushScreen(UUID viewerId, GuiScreen screen) {
        GuiSession session = sessions.get(viewerId);
        if (session == null || !isInteractive(session)) {
            return;
        }
        if (!checkPermission(viewerId, screen)) {
            return;
        }

        session.push(screen);
        screen.onOpen(session);
        render(session, true);
    }

    public void replaceScreen(UUID viewerId, GuiScreen screen) {
        GuiSession session = sessions.get(viewerId);
        if (session == null || !isInteractive(session)) {
            return;
        }
        if (!checkPermission(viewerId, screen)) {
            return;
        }

        GuiScreen current = session.currentScreen();
        if (current != null) {
            session.pop();
            current.onClose(session);
        }

        session.push(screen);
        screen.onOpen(session);
        render(session, true);
    }

    /**
     * Resolves the viewer as a {@link PlatformUser} and checks the screen's
     * permission. Sends a denial message on failure so the click doesn't look
     * like a no-op.
     */
    private boolean checkPermission(UUID viewerId, GuiScreen screen) {
        String required = screen.requiredPermission();
        if (required == null) return true;

        PlatformUserCreation creation = platform.getPlatformUserFactory().create(viewerId);
        if (creation == null) return false;

        PlatformUser viewer = creation.getPlatformUser();
        if (viewer.hasPermission(required)) return true;

        viewer.sendMessage(deniedMessage(required));
        return false;
    }

    private static Component deniedMessage(String permission) {
        return Component.text("You lack the permission ", NamedTextColor.RED)
                .append(Component.text(permission, NamedTextColor.YELLOW))
                .append(Component.text(" to open that screen.", NamedTextColor.RED));
    }

    public void back(UUID viewerId) {
        GuiSession session = sessions.get(viewerId);
        if (session == null || !isInteractive(session)) {
            return;
        }

        if (session.stackSize() <= 1) {
            close(viewerId, true);
            return;
        }

        GuiScreen current = session.pop();
        current.onClose(session);
        render(session, true);
    }

    public void refresh(UUID viewerId) {
        GuiSession session = sessions.get(viewerId);
        if (session == null || !isInteractive(session)) {
            return;
        }

        render(session, false);
    }

    public void close(UUID viewerId, boolean sendClosePacket) {
        GuiSession session = sessions.get(viewerId);
        if (session == null) {
            return;
        }

        if (sendClosePacket) {
            requestClientClose(session);
            return;
        }

        finalizeClose(viewerId, session);
    }

    public void shutdown() {
        for (UUID viewerId : List.copyOf(sessions.keySet())) {
            close(viewerId, false);
        }
    }

    public int activeSessionCount() {
        return sessions.size();
    }

    public boolean isGuiWindow(User user, int windowId) {
        if (user == null || user.getUUID() == null) {
            return false;
        }

        GuiSession session = sessions.get(user.getUUID());
        return session != null && isTracked(session) && session.windowId() == windowId;
    }

    public boolean hasSession(User user) {
        if (user == null || user.getUUID() == null) {
            return false;
        }

        GuiSession session = sessions.get(user.getUUID());
        return session != null && isTracked(session);
    }

    public int windowSize(User user) {
        if (user == null || user.getUUID() == null) {
            return -1;
        }

        GuiSession session = sessions.get(user.getUUID());
        if (session == null || !isTracked(session)) {
            return -1;
        }

        GuiRenderResult render = session.currentRender();
        return render == null ? -1 : render.size();
    }

    public void trackWindowItems(User user, int windowId, List<ItemStack> items, ItemStack carried) {
        if (user == null || user.getUUID() == null) {
            return;
        }

        GuiViewerInventory inventory = inventoryFor(user.getUUID());

        if (windowId == InventoryConstants.PLAYER_WINDOW_ID) {
            inventory.applyPlayerWindowItems(items, carried);
            return;
        }

        if (!isGuiWindow(user, windowId)) {
            int topSize = items.size() >= 36 ? items.size() - 36 : -1;
            inventory.setOpenWindow(windowId, topSize);
        }

        inventory.applyContainerWindowItems(items, carried);
    }

    public void trackPlayerSlot(User user, int slot, ItemStack item) {
        if (user == null || user.getUUID() == null) {
            return;
        }

        inventoryFor(user.getUUID()).applySlot(slot, item);
    }

    public void trackWindowSlot(User user, int windowId, int slot, ItemStack item) {
        if (user == null || user.getUUID() == null || windowId < 0) {
            return;
        }

        GuiViewerInventory inventory = inventoryFor(user.getUUID());

        if (windowId == InventoryConstants.PLAYER_WINDOW_ID) {
            inventory.applySlot(slot, item);
            return;
        }

        int mappedSlot = isGuiWindow(user, windowId)
                ? mapContainerSlotToPlayerSlot(windowSize(user), slot)
                : inventory.mapContainerSlotToPlayerSlot(windowId, slot);
        if (mappedSlot >= 0) {
            inventory.applySlot(mappedSlot, item);
        }
    }

    public void trackPlayerCursor(User user, ItemStack item) {
        if (user == null || user.getUUID() == null) {
            return;
        }

        inventoryFor(user.getUUID()).applyCursor(item);
    }

    public void trackClientWindowClick(User user, WrapperPlayClientClickWindow packet) {
        if (user == null || user.getUUID() == null || packet == null) {
            return;
        }

        GuiViewerInventory inventory = inventoryFor(user.getUUID());

        int sourceSlot = mapViewerSlot(user, inventory, packet.getWindowId(), packet.getSlot());
        int targetSlot = mapSwapTargetSlot(packet);

        boolean sourceKnown = sourceSlot >= 0 && inventory.isKnown(sourceSlot);
        boolean targetKnown = targetSlot >= 0 && inventory.isKnown(targetSlot);
        ItemStack previousSource = sourceKnown ? inventory.item(sourceSlot) : ItemStack.EMPTY;
        ItemStack previousTarget = targetKnown ? inventory.item(targetSlot) : ItemStack.EMPTY;

        inventory.applyCursor(copyItem(packet.getCarriedItemStack()));

        Set<Integer> updatedSlots = new HashSet<>();
        packet.getSlots().ifPresent(slots -> {
            for (Map.Entry<Integer, ItemStack> entry : slots.entrySet()) {
                int mappedSlot = mapViewerSlot(user, inventory, packet.getWindowId(), entry.getKey());
                if (mappedSlot < 0) {
                    continue;
                }

                inventory.applySlot(mappedSlot, entry.getValue());
                updatedSlots.add(mappedSlot);
            }
        });

        if (packet.getWindowClickType() != WrapperPlayClientClickWindow.WindowClickType.SWAP
                || sourceSlot < 0
                || targetSlot < 0
                || sourceSlot == targetSlot) {
            return;
        }

        if (!updatedSlots.contains(sourceSlot) && targetKnown) {
            inventory.applySlot(sourceSlot, previousTarget);
        }

        if (!updatedSlots.contains(targetSlot) && sourceKnown) {
            inventory.applySlot(targetSlot, previousSource);
        }
    }

    public void trackCreativeInventoryAction(User user, int slot, ItemStack item) {
        if (user == null || user.getUUID() == null) {
            return;
        }

        inventoryFor(user.getUUID()).applySlot(slot, item);
    }

    public void resyncTopInventory(User user) {
        if (user == null || user.getUUID() == null) {
            return;
        }

        GuiSession session = sessions.get(user.getUUID());
        if (session == null || !isInteractive(session)) {
            return;
        }

        GuiRenderResult render = session.currentRender();
        if (render == null) {
            return;
        }

        int stateId = session.nextStateId();
        scheduleRender(session, render, () -> {
            for (int slot = 0; slot < render.size(); slot++) {
                session.user().sendPacket(new WrapperPlayServerSetSlot(
                        session.windowId(),
                        stateId,
                        slot,
                        render.item(slot)
                ));
            }
            session.appliedRender(render);
        });
    }

    public void restoreViewerInventory(User user) {
        restoreViewerInventory(user, ItemStack.EMPTY);
    }

    public void restoreViewerInventory(User user, ItemStack fallbackCursor) {
        if (user == null || user.getUUID() == null) {
            return;
        }

        GuiSession session = sessions.get(user.getUUID());
        if (session == null || !isInteractive(session)) {
            return;
        }

        restoreViewerInventory(session, false, fallbackCursor);
    }

    public void resyncWindow(User user, ItemStack fallbackCursor) {
        if (user == null || user.getUUID() == null) {
            return;
        }

        GuiSession session = sessions.get(user.getUUID());
        if (session == null || !isInteractive(session)) {
            return;
        }

        restoreViewerInventory(session, true, fallbackCursor);
    }

    public void handleWindowClick(User user, WrapperPlayClientClickWindow packet) {
        if (user == null || user.getUUID() == null) {
            return;
        }

        GuiSession session = sessions.get(user.getUUID());
        if (session == null || !isTracked(session) || session.windowId() != packet.getWindowId()) {
            return;
        }

        if (session.closeRequested()) {
            resendClosePacket(session);
            return;
        }

        GuiRenderResult render = session.currentRender();
        GuiClickContext context = new GuiClickContext(this, session, packet);

        if (render != null) {
            int slot = packet.getSlot();
            if (slot >= 0 && slot < render.size()) {
                GuiClickAction action = render.clickAction(slot);
                if (action != null) {
                    try {
                        action.accept(context);
                    } catch (Exception exception) {
                        platform.getLogger().log(Level.WARNING, "Failed to process GUI click action", exception);
                    }
                }
            }
        }

        ItemStack fallbackCursor = fallbackCursor(packet);
        session.pendingCursorFallback(fallbackCursor);

        if (context.rendered()) {
            return;
        }

        session.clearPendingCursorFallback();
        resync(session, fallbackCursor);
    }

    public void handleWindowClose(User user, int windowId) {
        if (user == null || user.getUUID() == null) {
            return;
        }

        GuiSession session = sessions.get(user.getUUID());
        if (session == null || !isTracked(session) || session.windowId() != windowId) {
            return;
        }

        finalizeClose(user.getUUID(), session);
    }

    public void handleExternalInventoryOpen(User user, int windowId, int topSize) {
        if (user == null || user.getUUID() == null) {
            return;
        }

        if (!isGuiWindow(user, windowId)) {
            inventoryFor(user.getUUID()).setOpenWindow(windowId, topSize);
        }

        finalizeClose(user.getUUID(), sessions.get(user.getUUID()));
    }

    public void handleInventoryClosePacket(User user, int windowId) {
        if (user == null || user.getUUID() == null) {
            return;
        }

        GuiViewerInventory inventory = viewerInventories.get(user.getUUID());
        if (inventory != null && !isGuiWindow(user, windowId)) {
            inventory.closeWindow(windowId);
        }

        finalizeClose(user.getUUID(), sessions.get(user.getUUID()));
    }

    public void handleUserDisconnect(UUID uuid) {
        if (uuid == null) {
            return;
        }

        close(uuid, false);
        viewerInventories.remove(uuid);
        closeSubscribers(GuiSubscriptionKey.monitor(uuid));
    }

    public void refreshMonitor(UUID uuid) {
        if (uuid == null) {
            return;
        }

        refreshSubscribers(GuiSubscriptionKey.monitor(uuid));
    }

    private void handleInventoryChanged(InventoryChangedEvent event) {
        refreshMonitor(event.getPlayer().getUuid());
    }

    private void refreshSubscribers(GuiSubscriptionKey key) {
        Set<UUID> viewerIds = subscriptions.get(key);
        if (viewerIds == null || viewerIds.isEmpty()) {
            return;
        }

        for (UUID viewerId : List.copyOf(viewerIds)) {
            refresh(viewerId);
        }
    }

    private void closeSubscribers(GuiSubscriptionKey key) {
        Set<UUID> viewerIds = subscriptions.get(key);
        if (viewerIds == null || viewerIds.isEmpty()) {
            return;
        }

        for (UUID viewerId : List.copyOf(viewerIds)) {
            close(viewerId, true);
        }
    }

    private void render(GuiSession session, boolean forceReopen) {
        if (!isInteractive(session)) {
            return;
        }

        GuiScreen screen = session.currentScreen();
        if (screen == null) {
            close(session.viewerId(), true);
            return;
        }

        final GuiRenderResult render;
        try {
            render = screen.render(session);
        } catch (Exception exception) {
            platform.getLogger().log(Level.WARNING, "Failed to render GUI screen " + screen.getClass().getName(), exception);
            close(session.viewerId(), true);
            return;
        }

        applyRender(session, screen, render, forceReopen);
    }

    private void applyRender(GuiSession session, GuiScreen screen, GuiRenderResult nextRender, boolean forceReopen) {
        if (!isInteractive(session)) {
            return;
        }

        GuiRenderResult previousRender = session.appliedRender();
        session.currentRender(nextRender);
        updateSubscriptions(session, screen.subscriptionKeys());

        boolean reopen = forceReopen
                || previousRender == null
                || previousRender.rows() != nextRender.rows()
                || !Objects.equals(previousRender.title(), nextRender.title());

        int stateId = session.nextStateId();
        ItemStack cursorFallback = session.consumePendingCursorFallback();

        if (reopen) {
            scheduleRender(session, nextRender, () -> {
                sendOpenWindow(session, nextRender);
                sendWindowItems(session, stateId, nextRender, cursorFallback);
                session.user().sendPacket(new WrapperPlayServerSetCursorItem(currentCursor(session, cursorFallback)));
                session.appliedRender(nextRender);
            });
            return;
        }

        int changedSlots = countChangedSlots(previousRender, nextRender);
        if (changedSlots == 0) {
            if (cursorFallback == null) {
                return;
            }

            scheduleRender(session, nextRender, () -> {
                session.user().sendPacket(new WrapperPlayServerSetCursorItem(currentCursor(session, cursorFallback)));
                session.appliedRender(nextRender);
            });
            return;
        }

        scheduleRender(session, nextRender, () -> {
            if (changedSlots > (nextRender.size() / 2)) {
                sendWindowItems(session, stateId, nextRender, cursorFallback);
                if (cursorFallback != null) {
                    session.user().sendPacket(new WrapperPlayServerSetCursorItem(currentCursor(session, cursorFallback)));
                }
                session.appliedRender(nextRender);
                return;
            }

            for (int slot = 0; slot < nextRender.size(); slot++) {
                if (!previousRender.item(slot).equals(nextRender.item(slot))) {
                    session.user().sendPacket(new WrapperPlayServerSetSlot(
                            session.windowId(),
                            stateId,
                            slot,
                            nextRender.item(slot)
                    ));
                }
            }

            if (cursorFallback != null) {
                session.user().sendPacket(new WrapperPlayServerSetCursorItem(currentCursor(session, cursorFallback)));
            }

            session.appliedRender(nextRender);
        });
    }

    private void resync(GuiSession session, ItemStack fallbackCursor) {
        GuiRenderResult render = session.currentRender();
        if (render == null || !isInteractive(session)) {
            return;
        }

        restoreViewerInventory(session, true, fallbackCursor);
    }

    private void sendOpenWindow(GuiSession session, GuiRenderResult render) {
        ServerVersion version = PacketEvents.getAPI().getServerManager().getVersion();
        WrapperPlayServerOpenWindow openWindow;

        if (version.isNewerThanOrEquals(ServerVersion.V_1_14)) {
            openWindow = new WrapperPlayServerOpenWindow(session.windowId(), render.rows() - 1, render.title());
        } else {
            openWindow = new WrapperPlayServerOpenWindow(
                    session.windowId(),
                    "minecraft:chest",
                    render.title(),
                    render.size(),
                    0
            );
        }

        session.user().sendPacket(openWindow);
    }

    private void sendWindowItems(GuiSession session, int stateId, GuiRenderResult render, ItemStack fallbackCursor) {
        GuiViewerInventory inventory = viewerInventories.get(session.viewerId());
        session.user().sendPacket(new WrapperPlayServerWindowItems(
                session.windowId(),
                stateId,
                buildWindowItems(render, inventory),
                currentCursor(session, fallbackCursor)
        ));
    }

    private void updateSubscriptions(GuiSession session, Set<GuiSubscriptionKey> nextKeys) {
        Set<GuiSubscriptionKey> immutableKeys = Set.copyOf(nextKeys);
        removeSubscriptions(session, session.subscriptionKeys());

        session.subscriptionKeys(immutableKeys);
        if (immutableKeys.isEmpty()) {
            return;
        }

        for (GuiSubscriptionKey key : immutableKeys) {
            subscriptions
                    .computeIfAbsent(key, ignored -> ConcurrentHashMap.newKeySet())
                    .add(session.viewerId());
        }
    }

    private void removeSubscriptions(GuiSession session, Set<GuiSubscriptionKey> keys) {
        if (keys == null || keys.isEmpty()) {
            return;
        }

        for (GuiSubscriptionKey key : keys) {
            subscriptions.computeIfPresent(key, (ignored, viewerIds) -> {
                viewerIds.remove(session.viewerId());
                return viewerIds.isEmpty() ? null : viewerIds;
            });
        }
    }

    private void schedule(GuiSession session, Runnable action) {
        platform.getScheduler().runAsyncTask(() -> {
            if (!isTracked(session)) {
                return;
            }

            try {
                action.run();
            } catch (Exception exception) {
                platform.getLogger().log(Level.WARNING, "Failed to send GUI packet(s)", exception);
            }
        });
    }

    private void scheduleRender(GuiSession session, GuiRenderResult expectedRender, Runnable action) {
        schedule(session, () -> {
            if (session.currentRender() != expectedRender) {
                return;
            }

            action.run();
        });
    }

    private void restoreViewerInventory(GuiSession session, boolean includeTop, ItemStack fallbackCursor) {
        if (!isInteractive(session)) {
            return;
        }

        GuiRenderResult render = session.currentRender();
        if (render == null) {
            return;
        }

        GuiViewerInventory inventory = viewerInventories.get(session.viewerId());
        int stateId = session.nextStateId();

        scheduleRender(session, render, () -> {
            if (includeTop) {
                for (int slot = 0; slot < render.size(); slot++) {
                    session.user().sendPacket(new WrapperPlayServerSetSlot(
                            session.windowId(),
                            stateId,
                            slot,
                            render.item(slot)
                    ));
                }
                session.appliedRender(render);
            }

            syncViewerRange(session, stateId, render.size(), inventory, InventoryConstants.ITEMS_START, 27);
            syncViewerRange(session, stateId, render.size() + 27, inventory, InventoryConstants.HOTBAR_START, 9);
            syncHiddenViewerSlots(session, inventory);
            session.user().sendPacket(new WrapperPlayServerSetCursorItem(currentCursor(session, fallbackCursor)));
        });
    }

    private List<ItemStack> buildWindowItems(GuiRenderResult render, GuiViewerInventory inventory) {
        List<ItemStack> items = new ArrayList<>(render.size() + 36);

        for (int slot = 0; slot < render.size(); slot++) {
            items.add(render.item(slot));
        }

        appendViewerRange(items, inventory, InventoryConstants.ITEMS_START, 27);
        appendViewerRange(items, inventory, InventoryConstants.HOTBAR_START, 9);
        return items;
    }

    private void appendViewerRange(List<ItemStack> items, GuiViewerInventory inventory, int sourceStart, int amount) {
        for (int index = 0; index < amount; index++) {
            int slot = sourceStart + index;
            if (inventory == null || !inventory.isKnown(slot)) {
                items.add(ItemStack.EMPTY);
                continue;
            }

            items.add(inventory.item(slot));
        }
    }

    private void syncViewerRange(GuiSession session, int stateId, int targetStart, GuiViewerInventory inventory, int sourceStart, int amount) {
        if (inventory == null) {
            return;
        }

        for (int index = 0; index < amount; index++) {
            int sourceSlot = sourceStart + index;
            if (!inventory.isKnown(sourceSlot)) {
                continue;
            }

            session.user().sendPacket(new WrapperPlayServerSetSlot(
                    session.windowId(),
                    stateId,
                    targetStart + index,
                    inventory.item(sourceSlot)
            ));
        }
    }

    private void syncHiddenViewerSlots(GuiSession session, GuiViewerInventory inventory) {
        if (inventory == null) {
            return;
        }

        syncPlayerInventorySlot(session, inventory, InventoryConstants.SLOT_HELMET);
        syncPlayerInventorySlot(session, inventory, InventoryConstants.SLOT_CHESTPLATE);
        syncPlayerInventorySlot(session, inventory, InventoryConstants.SLOT_LEGGINGS);
        syncPlayerInventorySlot(session, inventory, InventoryConstants.SLOT_BOOTS);
        syncPlayerInventorySlot(session, inventory, InventoryConstants.SLOT_OFFHAND);
    }

    private void syncPlayerInventorySlot(GuiSession session, GuiViewerInventory inventory, int slot) {
        if (!inventory.isKnown(slot)) {
            return;
        }

        session.user().sendPacket(new WrapperPlayServerSetSlot(
                InventoryConstants.PLAYER_WINDOW_ID,
                0,
                slot,
                inventory.item(slot)
        ));
    }

    private ItemStack currentCursor(GuiSession session, ItemStack fallbackCursor) {
        GuiViewerInventory inventory = viewerInventories.get(session.viewerId());
        if (inventory != null && inventory.isCursorKnown()) {
            return inventory.cursor();
        }

        return copyItem(fallbackCursor);
    }

    private ItemStack fallbackCursor(WrapperPlayClientClickWindow packet) {
        ItemStack carriedItem = copyItem(packet.getCarriedItemStack());
        if (!carriedItem.isEmpty()) {
            return carriedItem;
        }

        Map<Integer, ItemStack> changedSlots = packet.getSlots().orElse(Map.of());
        if (changedSlots.isEmpty()) {
            return ItemStack.EMPTY;
        }

        int slot = packet.getSlot();
        ItemStack clickedItem = changedSlots.get(slot);
        if (clickedItem != null && !clickedItem.isEmpty()) {
            return copyItem(clickedItem);
        }

        for (ItemStack changedItem : changedSlots.values()) {
            if (changedItem != null && !changedItem.isEmpty()) {
                return copyItem(changedItem);
            }
        }

        return ItemStack.EMPTY;
    }

    private ItemStack copyItem(ItemStack item) {
        return item == null || item.isEmpty() ? ItemStack.EMPTY : item.copy();
    }

    private int mapViewerSlot(User user, GuiViewerInventory inventory, int windowId, int slot) {
        if (slot < 0) {
            return -1;
        }

        if (windowId == InventoryConstants.PLAYER_WINDOW_ID) {
            return slot;
        }

        if (isGuiWindow(user, windowId)) {
            return mapContainerSlotToPlayerSlot(windowSize(user), slot);
        }

        return inventory.mapContainerSlotToPlayerSlot(windowId, slot);
    }

    private int mapSwapTargetSlot(WrapperPlayClientClickWindow packet) {
        if (packet.getWindowClickType() != WrapperPlayClientClickWindow.WindowClickType.SWAP) {
            return -1;
        }

        int button = packet.getButton();
        if (button >= 0 && button <= 8) {
            return InventoryConstants.HOTBAR_START + button;
        }

        return button == 40 ? InventoryConstants.SLOT_OFFHAND : -1;
    }

    private int mapContainerSlotToPlayerSlot(int topSize, int containerSlot) {
        if (topSize < 0) {
            return -1;
        }

        int relativeSlot = containerSlot - topSize;
        if (relativeSlot < 0) {
            return -1;
        }

        if (relativeSlot < 27) {
            return InventoryConstants.ITEMS_START + relativeSlot;
        }

        if (relativeSlot < 36) {
            return InventoryConstants.HOTBAR_START + (relativeSlot - 27);
        }

        return -1;
    }

    private boolean isInteractive(GuiSession session) {
        return isTracked(session) && !session.closeRequested();
    }

    private boolean isTracked(GuiSession session) {
        return session != null
                && !session.closed()
                && sessions.get(session.viewerId()) == session;
    }

    private void requestClientClose(GuiSession session) {
        if (!isTracked(session)) {
            return;
        }

        if (!session.closeRequested()) {
            beginClose(session);
        }

        resendClosePacket(session);
    }

    private void resendClosePacket(GuiSession session) {
        schedule(session, () -> {
            session.user().sendPacket(new WrapperPlayServerSetCursorItem(currentCursor(session, ItemStack.EMPTY)));
            session.user().sendPacket(new WrapperPlayServerCloseWindow(session.windowId()));
        });
    }

    private void beginClose(GuiSession session) {
        if (session.closeRequested()) {
            return;
        }

        session.closeRequested(true);
        removeSubscriptions(session, session.subscriptionKeys());

        while (session.stackSize() > 0) {
            GuiScreen screen = session.pop();
            screen.onClose(session);
        }
    }

    private void finalizeClose(UUID viewerId, GuiSession session) {
        if (session == null || !isTracked(session)) {
            return;
        }

        beginClose(session);
        session.closed(true);
        sessions.remove(viewerId, session);
    }

    private int countChangedSlots(GuiRenderResult previousRender, GuiRenderResult nextRender) {
        int changed = 0;
        for (int slot = 0; slot < nextRender.size(); slot++) {
            if (!previousRender.item(slot).equals(nextRender.item(slot))) {
                changed++;
            }
        }
        return changed;
    }

    private int allocateWindowId() {
        int current = nextWindowId.getAndUpdate(previous -> previous >= MAX_WINDOW_ID ? 1 : previous + 1);
        return Math.max(1, current);
    }
}
