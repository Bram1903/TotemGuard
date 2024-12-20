/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2024 Bram and contributors
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

package com.deathmotion.totemguard.manager;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.checks.ICheck;
import com.deathmotion.totemguard.checks.impl.badpackets.BadPacketsA;
import com.deathmotion.totemguard.checks.impl.badpackets.BadPacketsB;
import com.deathmotion.totemguard.checks.impl.badpackets.BadPacketsC;
import com.deathmotion.totemguard.checks.impl.totem.*;
import com.deathmotion.totemguard.commands.totemguard.CheckCommand;
import com.deathmotion.totemguard.commands.totemguard.ManualBanCommand;
import com.deathmotion.totemguard.listeners.TotemProcessor;
import com.deathmotion.totemguard.models.checks.CheckRecord;
import com.deathmotion.totemguard.packetlisteners.UserTracker;
import com.deathmotion.totemguard.util.MessageService;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.google.common.collect.ImmutableList;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class CheckManager {
    private final TotemGuard plugin;
    private final AlertManager alertManager;
    private final UserTracker userTracker;
    private final MessageService messageService;

    private final List<ICheck> checks;

    public CheckManager(TotemGuard plugin) {
        this.plugin = plugin;
        this.alertManager = plugin.getAlertManager();
        this.userTracker = plugin.getUserTracker();
        this.messageService = plugin.getMessageService();

        TotemProcessor.init(plugin);

        this.checks = ImmutableList.of(
                new AutoTotemA(plugin),
                new AutoTotemB(plugin),
                new AutoTotemC(plugin),
                new AutoTotemD(plugin),
                new AutoTotemE(plugin),
                new AutoTotemF(plugin),
                new BadPacketsA(plugin),
                new BadPacketsC(plugin),
                BadPacketsB.getInstance(plugin),
                CheckCommand.getInstance(plugin),
                ManualBanCommand.getInstance(plugin)
        );

        registerPacketListeners();

        long resetInterval = calculateResetInterval();
        FoliaScheduler.getAsyncScheduler().runAtFixedRate(plugin, (o) -> resetData(), resetInterval, resetInterval);
    }

    private long calculateResetInterval() {
        return plugin.getConfigManager().getSettings().getResetViolationsInterval() * 60L * 20L;
    }

    public void resetData() {
        checks.forEach(ICheck::resetData);
        TotemProcessor.getInstance().resetData();
        userTracker.clearTotemData();
        alertManager.sendAlert(messageService.getPrefix().append(Component.text("All flag counts have been reset.", NamedTextColor.GREEN)));
    }

    public void resetData(UUID uuid) {
        checks.forEach(check -> check.resetData(uuid));
        TotemProcessor.getInstance().resetData(uuid);
    }

    public List<CheckRecord> getViolations() {
        return checks.stream()
                .map(ICheck::getViolations)
                .filter(checkRecord -> !checkRecord.violations().isEmpty()) // Ensure only non-empty violations are included
                .collect(Collectors.toList());
    }

    private void registerPacketListeners() {
        checks.stream()
                .filter(check -> check instanceof PacketListener)
                .map(PacketListener.class::cast)
                .forEach(listener -> PacketEvents.getAPI().getEventManager().registerListener(listener, PacketListenerPriority.NORMAL));
    }
}