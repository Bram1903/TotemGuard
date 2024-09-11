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
import com.deathmotion.totemguard.checks.impl.manual.ManualTotemA;
import com.deathmotion.totemguard.checks.impl.totem.AutoTotemA;
import com.deathmotion.totemguard.checks.impl.totem.AutoTotemB;
import com.deathmotion.totemguard.checks.impl.totem.AutoTotemC;
import com.deathmotion.totemguard.checks.impl.totem.AutoTotemD;
import com.deathmotion.totemguard.checks.impl.totem.processor.TotemProcessor;
import com.deathmotion.totemguard.config.Settings;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.google.common.collect.ImmutableList;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.List;
import java.util.UUID;

public class CheckManager {
    private final TotemGuard plugin;
    private final AlertManager alertManager;
    private final List<ICheck> checks;

    public CheckManager(TotemGuard plugin) {
        this.plugin = plugin;
        this.alertManager = plugin.getAlertManager();

        TotemProcessor.init(plugin);

        this.checks = ImmutableList.of(
                TotemProcessor.getInstance(),
                new AutoTotemA(plugin),
                new AutoTotemB(plugin),
                new AutoTotemC(plugin),
                new AutoTotemD(plugin),
                new BadPacketsA(plugin),
                new ManualTotemA(plugin)
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

        final Settings settings = plugin.getConfigManager().getSettings();
        Component resetComponent = Component.text()
                .append(LegacyComponentSerializer.legacyAmpersand().deserialize(settings.getPrefix()))
                .append(Component.text("All flag counts have been reset.", NamedTextColor.GREEN))
                .build();

        alertManager.sendAlert(resetComponent);
    }

    public void resetData(UUID uuid) {
        checks.forEach(check -> check.resetData(uuid));
    }

    private void registerPacketListeners() {
        checks.stream()
                .filter(check -> check instanceof PacketListener)
                .map(PacketListener.class::cast)
                .forEach(listener -> PacketEvents.getAPI().getEventManager().registerListener(listener, PacketListenerPriority.NORMAL));
    }
}