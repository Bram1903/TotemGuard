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

package com.deathmotion.totemguard.models;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.api.interfaces.AbstractCheck;
import com.deathmotion.totemguard.api.interfaces.TotemUser;
import com.deathmotion.totemguard.checks.impl.badpackets.BadPacketsB;
import com.deathmotion.totemguard.checks.impl.badpackets.BadPacketsD;
import com.deathmotion.totemguard.checks.impl.misc.ClientBrand;
import com.deathmotion.totemguard.database.entities.DatabasePlayer;
import com.deathmotion.totemguard.manager.CheckManager;
import com.deathmotion.totemguard.models.impl.DigAndPickupState;
import com.deathmotion.totemguard.models.impl.PingData;
import com.deathmotion.totemguard.models.impl.TotemData;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.User;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class TotemPlayer implements TotemUser {
    public final CheckManager checkManager;
    public final TotemData totemData;
    public final PingData pingData;
    public final UUID uniqueId;
    public final User user;

    public Player bukkitPlayer;
    public boolean firstChunkReceived;
    public boolean delayedChecksRan;
    public boolean isUsingLunarClient;
    public DigAndPickupState digAndPickupState;

    @Nullable
    public DatabasePlayer databasePlayer;

    public boolean sendingBundlePacket;

    public TotemPlayer(User user) {
        this.uniqueId = user.getUUID();
        this.user = user;

        checkManager = new CheckManager(this);
        totemData = new TotemData();
        pingData = new PingData(this);
        digAndPickupState = new DigAndPickupState();
    }

    public void reload() {
        // reload all checks
        for (AbstractCheck value : checkManager.allChecks.values()) value.reload();
    }

    public void handlePlayerLogin(Player player) {
        this.bukkitPlayer = player;

        if (!TotemGuard.getInstance().getPlayerDataManager().shouldCheck(user, bukkitPlayer)) {
            TotemGuard.getInstance().getPlayerDataManager().remove(user);
            return;
        }

        FoliaScheduler.getAsyncScheduler().runNow(TotemGuard.getInstance(), (o -> {
            databasePlayer = TotemGuard.getInstance().getDatabaseProvider().getPlayerRepository().retrieveOrRefreshPlayer(this);
        }));
    }

    private void scheduleCheck(Runnable task) {
        long ping = Math.max(0L, getPing());
        long delay = (ping + 5L) * 5L;

        FoliaScheduler.getAsyncScheduler().runDelayed(
                TotemGuard.getInstance(),
                ignored -> task.run(),
                delay,
                TimeUnit.MILLISECONDS
        );
    }

    public void runDelayedChecks() {
        if (firstChunkReceived) return;
        firstChunkReceived = true;

        scheduleCheck(() -> {
            checkManager.getGenericCheck(BadPacketsB.class).handle();
            checkManager.triggerSignChecks();
        });
    }

    public void runExtraDelayedChecks() {
        if (!firstChunkReceived || delayedChecksRan) return;
        delayedChecksRan = true;

        scheduleCheck(() -> checkManager.getGenericCheck(BadPacketsD.class).handle());
    }

    public String getBrand() {
        return this.checkManager.getPacketCheck(ClientBrand.class).getBrand();
    }

    @Override
    public String getName() {
        return user.getName();
    }

    @Override
    public UUID getUniqueId() {
        return uniqueId;
    }

    @Override
    public String getVersionName() {
        return user.getClientVersion().getReleaseName();
    }

    @Override
    public int getKeepAlivePing() {
        if (bukkitPlayer == null) return -1;
        return PacketEvents.getAPI().getPlayerManager().getPing(bukkitPlayer);
    }

    public int getPing() {
        int transactionPing = pingData.getTransactionPing();
        if (transactionPing == -1) {
            return getKeepAlivePing();
        } else {
            return transactionPing;
        }
    }
}
