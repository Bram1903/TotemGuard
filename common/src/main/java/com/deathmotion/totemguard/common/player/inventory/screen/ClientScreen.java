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

package com.deathmotion.totemguard.common.player.inventory.screen;

import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.data.Data;
import com.deathmotion.totemguard.common.player.data.ping.PingData;
import com.deathmotion.totemguard.common.player.inventory.InventoryConstants;
import com.deathmotion.totemguard.common.player.inventory.enums.Issuer;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.LongConsumer;

public final class ClientScreen {

    private final TGPlayer player;
    private final Data data;
    private final PingData pingData;
    private final Deque<ScreenChange> pending = new ArrayDeque<>();

    @Getter
    private int windowId = InventoryConstants.PLAYER_WINDOW_ID;
    private boolean playerScreen;
    private boolean displaced;
    @Getter
    private int revision;
    @Getter
    private int selectsLanded;
    @Getter
    private int lastLandedSelect = -1;
    @Getter
    private int sprintRaisesLanded;
    private int playerMenuSize = -1;
    private int containerSize = -1;

    public ClientScreen(TGPlayer player) {
        this.player = player;
        this.data = player.getData();
        this.pingData = player.getPingData();
    }

    public void serverOpened(PacketSendEvent event, int windowId, @Nullable LongConsumer then) {
        queue(event, ScreenChange.Kind.OPEN, windowId, then, true);
    }

    public void serverClosed(PacketSendEvent event, @Nullable LongConsumer then) {
        queue(event, ScreenChange.Kind.CLOSE, InventoryConstants.PLAYER_WINDOW_ID, then, true);
    }

    public void serverDisplaced(PacketSendEvent event) {
        queue(event, ScreenChange.Kind.DISPLACE, 0, null, true);
    }

    // The client never answers a ping written ahead of JOIN_GAME
    public void serverJoined(PacketSendEvent event) {
        queue(event, ScreenChange.Kind.RESPAWN, 0, null, false);
    }

    public void serverRespawned(PacketSendEvent event) {
        queue(event, ScreenChange.Kind.RESPAWN, 0, null, true);
    }

    public void serverSelected(PacketSendEvent event, int slot, @Nullable LongConsumer then) {
        queue(event, ScreenChange.Kind.SELECT, slot, then, true);
    }

    public void serverRaisedSprint(PacketSendEvent event) {
        queue(event, ScreenChange.Kind.SPRINT, 0, null, false);
    }

    public void serverFilled(int windowId, int size) {
        if (windowId == InventoryConstants.PLAYER_WINDOW_ID) {
            playerMenuSize = size;
        } else if (windowId == this.windowId) {
            containerSize = size;
        }
    }

    public void clientClicked(int clickedWindowId) {
        applyThroughOpen(clickedWindowId);
        if (clickedWindowId != windowId) return;
        shows(windowId, playerScreen || clickedWindowId == InventoryConstants.PLAYER_WINDOW_ID, false, Issuer.CLIENT);
    }

    public void clientClosed(int closedWindowId) {
        if (closedWindowId != InventoryConstants.PLAYER_WINDOW_ID) {
            applyThroughOpen(closedWindowId);
        }
        shows(InventoryConstants.PLAYER_WINDOW_ID, false, false, Issuer.CLIENT);
    }

    public int menuSize(int clickedWindowId) {
        if (clickedWindowId != windowId) return -1;
        return clickedWindowId == InventoryConstants.PLAYER_WINDOW_ID ? playerMenuSize : containerSize;
    }

    public boolean mayHold(int clickedWindowId) {
        if (clickedWindowId == windowId) return true;

        long confirmed = pingData.getConfirmedTransactionOrdinal();
        for (ScreenChange change : pending) {
            if (!change.possiblyApplied(confirmed)) return false;
            if (change.kind() == ScreenChange.Kind.OPEN && change.value() == clickedWindowId) return true;
            if (change.closes() && clickedWindowId == InventoryConstants.PLAYER_WINDOW_ID) return true;
        }
        return false;
    }

    public ScreenState state() {
        if (displaced) return ScreenState.DISPLACED;
        if (windowId != InventoryConstants.PLAYER_WINDOW_ID) return ScreenState.CONTAINER;
        return playerScreen ? ScreenState.INFERRED : ScreenState.NONE;
    }

    public boolean certainlyOpen() {
        return state().open() && !anyPossiblyApplied(true);
    }

    public boolean certainlyClosed() {
        ScreenState state = state();
        return !state.open() && !state.unknown() && !anyPossiblyApplied(false);
    }

    public boolean certainlyContainer() {
        return state() == ScreenState.CONTAINER && !anyPossiblyApplied(true);
    }

    public boolean selectPossiblyApplied(int slot) {
        long confirmed = pingData.getConfirmedTransactionOrdinal();
        for (ScreenChange change : pending) {
            if (!change.possiblyApplied(confirmed)) return false;
            if (change.kind() == ScreenChange.Kind.SELECT && change.value() == slot) return true;
        }
        return false;
    }

    public boolean sprintRaisePossiblyApplied() {
        long confirmed = pingData.getConfirmedTransactionOrdinal();
        for (ScreenChange change : pending) {
            if (!change.possiblyApplied(confirmed)) return false;
            if (change.kind() == ScreenChange.Kind.SPRINT) return true;
        }
        return false;
    }

    public boolean openPossiblyApplied() {
        long confirmed = pingData.getConfirmedTransactionOrdinal();
        for (ScreenChange change : pending) {
            if (!change.possiblyApplied(confirmed)) return false;
            if (change.kind() == ScreenChange.Kind.OPEN) return true;
        }
        return false;
    }

    private boolean anyPossiblyApplied(boolean closing) {
        long confirmed = pingData.getConfirmedTransactionOrdinal();
        for (ScreenChange change : pending) {
            if (!change.possiblyApplied(confirmed)) return false;
            if (closing ? change.closes() : change.kind() == ScreenChange.Kind.OPEN) return true;
        }
        return false;
    }

    private void queue(PacketSendEvent event, ScreenChange.Kind kind, int value, @Nullable LongConsumer then, boolean pingFirst) {
        if (event.isCancelled()) return;
        if (pingFirst) player.getLatencyHandler().pingBefore(event);

        ScreenChange change = new ScreenChange(kind, value, pingData.getSentTransactionOrdinal());
        pending.addLast(change);
        player.getLatencyHandler().compensate(event, timestamp -> {
            applyThrough(change);
            if (then != null) then.accept(timestamp);
        }, () -> pending.remove(change));
    }

    private void applyThroughOpen(int openedWindowId) {
        long confirmed = pingData.getConfirmedTransactionOrdinal();
        ScreenChange target = null;
        for (ScreenChange change : pending) {
            if (!change.possiblyApplied(confirmed)) break;
            if (change.kind() == ScreenChange.Kind.OPEN && change.value() == openedWindowId) {
                target = change;
                break;
            }
        }
        if (target != null) applyThrough(target);
    }

    private void applyThrough(ScreenChange target) {
        if (!containsExactly(target)) return;

        ScreenChange change;
        do {
            change = pending.pollFirst();
            apply(change);
        } while (change != target);
    }

    private boolean containsExactly(ScreenChange target) {
        for (ScreenChange change : pending) {
            if (change == target) return true;
        }
        return false;
    }

    private void apply(ScreenChange change) {
        switch (change.kind()) {
            case OPEN -> {
                containerSize = -1;
                shows(change.value(), false, false, Issuer.SERVER);
            }
            case CLOSE, RESPAWN -> shows(InventoryConstants.PLAYER_WINDOW_ID, false, false, Issuer.SERVER);
            case DISPLACE -> shows(windowId, false, true, Issuer.SERVER);
            case SELECT -> {
                selectsLanded++;
                lastLandedSelect = change.value();
            }
            case SPRINT -> sprintRaisesLanded++;
        }
    }

    private void shows(int windowId, boolean playerScreen, boolean displaced, Issuer issuer) {
        if (this.windowId == windowId && this.playerScreen == playerScreen && this.displaced == displaced) return;

        this.windowId = windowId;
        this.playerScreen = playerScreen;
        this.displaced = displaced;
        revision++;
        data.setOpenInventory(state().open(), issuer);
    }
}
