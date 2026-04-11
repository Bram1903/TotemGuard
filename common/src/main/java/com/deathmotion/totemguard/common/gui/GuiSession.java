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

import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.player.User;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;
import java.util.UUID;

public final class GuiSession {

    private final UUID viewerId;
    private final User user;
    private final int windowId;
    private final Deque<GuiScreen> screenStack = new ArrayDeque<>();

    private GuiRenderResult currentRender;
    private Set<GuiSubscriptionKey> subscriptionKeys = Set.of();
    private boolean closed;
    private boolean closeRequested;
    private int stateId = 1;
    private ItemStack pendingCursorFallback = ItemStack.EMPTY;
    private boolean hasPendingCursorFallback;

    public GuiSession(UUID viewerId, User user, int windowId) {
        this.viewerId = viewerId;
        this.user = user;
        this.windowId = windowId;
    }

    public synchronized UUID viewerId() {
        return viewerId;
    }

    public synchronized User user() {
        return user;
    }

    public synchronized int windowId() {
        return windowId;
    }

    public synchronized GuiRenderResult currentRender() {
        return currentRender;
    }

    public synchronized void currentRender(GuiRenderResult currentRender) {
        this.currentRender = currentRender;
    }

    public synchronized Set<GuiSubscriptionKey> subscriptionKeys() {
        return subscriptionKeys;
    }

    public synchronized void subscriptionKeys(Set<GuiSubscriptionKey> subscriptionKeys) {
        this.subscriptionKeys = subscriptionKeys;
    }

    public synchronized boolean closed() {
        return closed;
    }

    public synchronized void closed(boolean closed) {
        this.closed = closed;
    }

    public synchronized boolean closeRequested() {
        return closeRequested;
    }

    public synchronized void closeRequested(boolean closeRequested) {
        this.closeRequested = closeRequested;
    }

    public synchronized void push(GuiScreen screen) {
        screenStack.push(screen);
    }

    public synchronized GuiScreen pop() {
        return screenStack.pop();
    }

    public synchronized GuiScreen currentScreen() {
        return screenStack.peek();
    }

    public synchronized int stackSize() {
        return screenStack.size();
    }

    public synchronized boolean hasParent() {
        return screenStack.size() > 1;
    }

    public synchronized int nextStateId() {
        return stateId++;
    }

    public synchronized void pendingCursorFallback(ItemStack item) {
        this.pendingCursorFallback = item == null || item.isEmpty() ? ItemStack.EMPTY : item.copy();
        this.hasPendingCursorFallback = true;
    }

    public synchronized void clearPendingCursorFallback() {
        this.pendingCursorFallback = ItemStack.EMPTY;
        this.hasPendingCursorFallback = false;
    }

    public synchronized ItemStack consumePendingCursorFallback() {
        if (!this.hasPendingCursorFallback) {
            return null;
        }

        ItemStack fallback = this.pendingCursorFallback;
        this.pendingCursorFallback = ItemStack.EMPTY;
        this.hasPendingCursorFallback = false;
        return fallback;
    }
}
