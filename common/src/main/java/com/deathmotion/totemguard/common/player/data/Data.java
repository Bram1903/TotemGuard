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

package com.deathmotion.totemguard.common.player.data;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.mitigation.MitigationService;
import com.deathmotion.totemguard.common.mitigation.SetbackController;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.inventory.enums.Issuer;
import com.deathmotion.totemguard.common.util.BoundingBox;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Data {


    private static final int NETHER_PORTAL_ABSENT_TICKS_TO_CLEAR = 3;

    private final TGPlayer player;
    private final TGPlatform platform;
    private final TeleportData teleportData;
    private final InputData inputData;
    private final MovementData movementData;
    private final PlayerAttributeData attributeData;
    private final ExternalVelocityData externalVelocityData;
    private final PistonData pistonData;
    private final EffectData effectData;
    private final GlideData glideData;
    private final FireworkData fireworkData;
    private final VehicleData vehicleData;
    private final FoodData foodData;
    private final MitigationService mitigationService;
    private final SetbackController setbackController;
    private GameMode gameMode;
    private boolean sprinting;
    private boolean sneaking;
    private boolean dead;
    private boolean canFly;
    private boolean isFlying;
    private boolean swimming;
    private boolean gliding;
    private boolean spinAttacking;
    private boolean sleeping;
    private int vehicleId = -1;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Boolean lastSprinting;

    @Setter(AccessLevel.NONE)
    private boolean redundantSprint;

    @Setter(AccessLevel.NONE)
    private boolean openInventory;

    @Setter(AccessLevel.NONE)
    private Issuer lastInventoryIssuer = Issuer.CLIENT;

    private long inventoryOpenedAt;
    private boolean serverOpenedInventoryThisTick;
    private boolean clientOpenedInventoryThisTick;
    private boolean inventoryMitigated;
    private boolean inventoryMitigatedThisTick;
    private volatile boolean sendingBundlePacket;

    @Setter(AccessLevel.NONE)
    private volatile boolean inNetherPortal;
    @Setter(AccessLevel.NONE)
    private volatile boolean netherPortalContactThisTick;
    @Setter(AccessLevel.NONE)
    private int netherPortalAbsentTicks;

    public Data(TGPlayer player) {
        this.player = player;
        this.platform = TGPlatform.getInstance();
        this.teleportData = new TeleportData();
        this.inputData = new InputData();
        this.movementData = new MovementData();
        this.attributeData = new PlayerAttributeData();
        this.externalVelocityData = new ExternalVelocityData();
        this.pistonData = new PistonData();
        this.effectData = new EffectData();
        this.glideData = new GlideData();
        this.fireworkData = new FireworkData();
        this.vehicleData = new VehicleData();
        this.foodData = new FoodData();
        this.mitigationService = new MitigationService(this);
        this.setbackController = new SetbackController(mitigationService, externalVelocityData);
    }

    public void setOpenInventory(boolean openInventory, Issuer issuer) {
        boolean changed = this.openInventory != openInventory;
        this.openInventory = openInventory;
        this.lastInventoryIssuer = issuer;

        if (!changed) return;

        player.getPhysics().onInventoryToggled();

        if (openInventory) {
            this.inventoryOpenedAt = System.currentTimeMillis();
        }
        platform.getGuiManager().refreshMonitor(player.getUuid());

        boolean serverInitiated = issuer == Issuer.SERVER;
        if (openInventory) {
            platform.getEventBus().getUserInventoryOpen().fire(player, serverInitiated);
        } else {
            platform.getEventBus().getUserInventoryClose().fire(player, serverInitiated);
        }
    }

    public long getInventoryOpenDurationMs() {
        if (!openInventory || inventoryOpenedAt == 0L) return 0L;
        return System.currentTimeMillis() - inventoryOpenedAt;
    }

    public boolean isInVehicle() {
        return vehicleId != -1;
    }

    public void recordSprinting(boolean sprinting) {
        this.redundantSprint = lastSprinting != null && lastSprinting == sprinting;
        this.lastSprinting = sprinting;
    }

    public void resetSprintTracking() {
        this.lastSprinting = null;
        this.redundantSprint = false;
    }

    public void updateNetherPortalContact() {
        BoundingBox box = BoundingBox.sweptPlayer(
                movementData.getCurrent(), movementData.getPrevious(),
                attributeData.width(), attributeData.height());
        if (player.getWorldMirror().reader().containsPortal(
                box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
            markNetherPortalContact();
        }
    }

    public void markNetherPortalContact() {
        netherPortalContactThisTick = true;
        netherPortalAbsentTicks = 0;
        inNetherPortal = true;
    }

    public void flushNetherPortalContact() {
        if (netherPortalContactThisTick) {
            netherPortalContactThisTick = false;
            netherPortalAbsentTicks = 0;
            return;
        }
        if (inNetherPortal && ++netherPortalAbsentTicks >= NETHER_PORTAL_ABSENT_TICKS_TO_CLEAR) {
            inNetherPortal = false;
            netherPortalAbsentTicks = 0;
        }
    }
}
