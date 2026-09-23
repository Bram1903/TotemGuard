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

package com.deathmotion.totemguard.common.check.impl.inventory;

import com.deathmotion.totemguard.api.check.CheckType;
import com.deathmotion.totemguard.common.check.CheckImpl;
import com.deathmotion.totemguard.common.check.annotations.CheckData;
import com.deathmotion.totemguard.common.check.type.PacketCheck;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.data.InputData;
import com.deathmotion.totemguard.common.player.inventory.screen.ClientScreen;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientHeldItemChange;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import org.jetbrains.annotations.Nullable;

@CheckData(description = "Impossible action with open inventory", type = CheckType.INVENTORY)
public class InventoryA extends CheckImpl implements PacketCheck {

    private static final String SPRINT = "sprint";

    private final ClientScreen screen;
    private final InputData inputData;

    private boolean walkReported;
    private boolean aimReported;
    private boolean rotationReported;
    private boolean openingRotation = true;
    private int openedRevision = -1;
    private int revisionAtTickEnd;
    private int selectsAtTickEnd;
    private int sprintRaisesAtTickEnd;
    private boolean echoConsumed;

    public InventoryA(TGPlayer player) {
        super(player);
        this.screen = player.getScreen();
        this.inputData = data.getInputData();
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        final PacketTypeCommon type = event.getPacketType();

        if (type == PacketType.Play.Client.CLIENT_TICK_END) {
            tickEnded();
            return;
        }

        if (type == PacketType.Play.Client.PLAYER_ROTATION || type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
            rotated();
            return;
        }

        // ViaBackwards turns every swing of an older client into a punch, menu drops included
        if (type == PacketType.Play.Client.PUNCH) {
            if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_26_3) && screen.certainlyOpen()) {
                failInventory("swing");
            }
            return;
        }

        if (type == PacketType.Play.Client.HELD_ITEM_CHANGE) {
            if (player.supportsEndTick()) {
                slotChanged(new WrapperPlayClientHeldItemChange(event).getSlot());
            }
            return;
        }

        String action = action(type, event);
        if (action == null || !screen.certainlyOpen()) return;
        if (action.equals(SPRINT) && echoesServerSprint()) return;

        failInventory(action);
    }

    // A server write can raise the client's sprint flag, and a swimmer resting on a block keeps it and echoes it
    private boolean echoesServerSprint() {
        return screen.getSprintRaisesLanded() != sprintRaisesAtTickEnd || screen.sprintRaisePossiblyApplied();
    }

    // Polar can deliver the Input ahead of a close the client sent before it
    private void tickEnded() {
        revisionAtTickEnd = screen.getRevision();
        selectsAtTickEnd = screen.getSelectsLanded();
        sprintRaisesAtTickEnd = screen.getSprintRaisesLanded();
        echoConsumed = false;

        if (data.isInVehicle() || !inputData.walking() || !screen.certainlyOpen()) {
            walkReported = false;
            return;
        }
        if (walkReported) return;

        walkReported = true;
        failInventory("move");
    }

    // The first rotation under a new screen can still carry mouse movement from before it opened
    private void rotated() {
        boolean first = !rotationReported;
        rotationReported = true;

        if (!screen.certainlyOpen()) {
            openingRotation = true;
            aimReported = false;
            return;
        }
        if (openingRotation || screen.getRevision() != openedRevision) {
            openingRotation = false;
            openedRevision = screen.getRevision();
            return;
        }
        if (first || data.isInVehicle() || data.getGameMode() == GameMode.SPECTATOR) return;
        if (!data.getMovementData().isLastFlyingRotationChanged()) {
            aimReported = false;
            return;
        }
        if (aimReported || data.getTeleportData().hasPendingTeleport()) return;
        if (data.getRotationCredits().inFlight() || data.getRotationCredits().spend()) return;

        aimReported = true;
        failInventory("aim");
    }

    // gameMode.tick flushes a pending slot under any screen, so an earlier pick or a server select echo can land here
    private void slotChanged(int slot) {
        if (!screen.certainlyOpen()) return;
        if (screen.getRevision() != revisionAtTickEnd) return;
        if (!echoConsumed && echoesServerSelect(slot)) {
            echoConsumed = true;
            return;
        }

        failInventory("change slot {0}", slot);
    }

    private boolean echoesServerSelect(int slot) {
        boolean landedSinceTickEnd = screen.getSelectsLanded() != selectsAtTickEnd;
        return (landedSinceTickEnd && screen.getLastLandedSelect() == slot) || screen.selectPossiblyApplied(slot);
    }

    // Every other action has a vanilla sender that runs with a screen open
    private static @Nullable String action(PacketTypeCommon type, PacketReceiveEvent event) {
        if (type == PacketType.Play.Client.ATTACK) return "attack";
        if (type == PacketType.Play.Client.USE_ITEM) return "use";
        if (type == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) return "place";
        if (type == PacketType.Play.Client.INTERACT_ENTITY) {
            return new WrapperPlayClientInteractEntity(event).getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK
                    ? "attack"
                    : "interact";
        }
        if (type == PacketType.Play.Client.PLAYER_DIGGING) {
            return switch (new WrapperPlayClientPlayerDigging(event).getAction()) {
                case START_DIGGING, CHANGE_DESTROY_DIRECTION, FINISHED_DIGGING -> "dig";
                case DROP_ITEM, DROP_ITEM_STACK -> "drop";
                default -> null;
            };
        }
        if (type == PacketType.Play.Client.ENTITY_ACTION) {
            return switch (new WrapperPlayClientEntityAction(event).getAction()) {
                case START_SPRINTING -> SPRINT;
                case START_SNEAKING -> "sneak";
                case START_FLYING_WITH_ELYTRA -> "start glide";
                case OPEN_HORSE_INVENTORY -> "open horse inv";
                default -> null;
            };
        }
        return null;
    }
}
