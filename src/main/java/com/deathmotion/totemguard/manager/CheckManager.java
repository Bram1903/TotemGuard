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
import com.deathmotion.totemguard.config.Settings;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.UserDisconnectEvent;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.List;
import java.util.UUID;

public class CheckManager implements PacketListener {
    private final TotemGuard plugin;
    private final AlertManager alertManager;
    private final List<ICheck> checks;

    public CheckManager(TotemGuard plugin) {
        this.plugin = plugin;
        this.alertManager = plugin.getAlertManager();
        this.checks = List.of(
                new AutoTotemA(plugin),
                new AutoTotemB(plugin),
                new BadPacketsA(plugin),
                new ManualTotemA(plugin)
        );

        PacketEvents.getAPI().getEventManager().registerListener(this, PacketListenerPriority.LOW);
        registerPacketListeners();

        long resetInterval = plugin.getConfigManager().getSettings().getResetViolationsInterval() * 60L * 20L;
        FoliaScheduler.getAsyncScheduler().runAtFixedRate(plugin, (o) -> resetData(), resetInterval, resetInterval);
    }

    @Override
    public void onUserDisconnect(UserDisconnectEvent event) {
        UUID userUUID = event.getUser().getUUID();
        if (userUUID == null) return;

        resetData(userUUID);
    }

    public void resetData() {
        for (ICheck check : checks) {
            check.resetData();
        }

        final Settings settings = plugin.getConfigManager().getSettings();
        Component resetComponent = Component.text()
                .append(LegacyComponentSerializer.legacyAmpersand().deserialize(settings.getPrefix()))
                .append(Component.text("All flag counts have been reset.", NamedTextColor.GREEN))
                .build();

        alertManager.sendAlert(resetComponent);
    }

    public void resetData(UUID uuid) {
        for (ICheck check : checks) {
            check.resetData(uuid);
        }
    }

    private void registerPacketListeners() {
        for (ICheck check : checks) {
            if (check instanceof PacketListener) {
                PacketEvents.getAPI().getEventManager().registerListener((PacketListener) check, PacketListenerPriority.NORMAL);
            }
        }
    }
}
