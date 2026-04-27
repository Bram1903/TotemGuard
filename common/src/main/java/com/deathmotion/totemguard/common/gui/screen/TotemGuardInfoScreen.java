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

package com.deathmotion.totemguard.common.gui.screen;

import com.deathmotion.totemguard.api3.stats.StatsWindow;
import com.deathmotion.totemguard.api3.versioning.TGAPIVersions;
import com.deathmotion.totemguard.api3.versioning.TGVersion;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.check.CheckManagerImpl;
import com.deathmotion.totemguard.common.gui.*;
import com.deathmotion.totemguard.common.gui.screen.stats.StatisticsScreen;
import com.deathmotion.totemguard.common.util.TGVersions;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class TotemGuardInfoScreen extends GuiScreen {

    private static final DateTimeFormatter BUILD_TIME_FORMAT = DateTimeFormatter
            .ofPattern("dd-MM-yyyy HH:mm", Locale.ROOT)
            .withZone(ZoneId.systemDefault());

    private static ItemStack buildServicesTile(TGPlatform platform) {
        boolean dbConnected = platform.getDatabaseRepository().isConnected();
        boolean dbEnabled = platform.getDatabaseRepository().isEnabled();
        boolean redisConnected = platform.getRedisRepository().isConnected();
        boolean redisEnabled = platform.getRedisRepository().isEnabled();
        boolean antiVpnEnabled = platform.getAntiVPNRepository().isEnabled();

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Live status of external services", NamedTextColor.GRAY));
        lore.add(Component.text("TotemGuard depends on.", NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(serviceLine("Database", dbEnabled, dbConnected));
        lore.add(serviceLine("Redis", redisEnabled, redisConnected));
        lore.add(serviceLine("Anti-VPN", antiVpnEnabled, antiVpnEnabled));

        return GuiItems.simple(
                ItemTypes.ENDER_CHEST,
                Component.text("Services", NamedTextColor.LIGHT_PURPLE),
                lore
        );
    }

    private static Component serviceLine(String label, boolean enabled, boolean connected) {
        Component status;
        if (!enabled) {
            status = Component.text("Disabled", NamedTextColor.DARK_GRAY);
        } else if (connected) {
            status = Component.text("Connected", NamedTextColor.GREEN);
        } else {
            status = Component.text("Disconnected", NamedTextColor.RED);
        }
        return Component.text(label + ": ", NamedTextColor.GRAY).append(status);
    }

    private static ItemStack buildInformationTile(TGPlatform platform) {
        TGVersion pluginVersion = TGVersions.CURRENT;
        Instant buildTime = TGVersions.BUILD_TIMESTAMP;
        int checkCount = CheckManagerImpl.knownCheckCount();

        List<Component> lore = new ArrayList<>();
        lore.add(GuiText.line("Version", pluginVersion.toString()));
        lore.add(GuiText.line("API version", TGAPIVersions.CURRENT.toString()));
        lore.add(GuiText.line("Build time", BUILD_TIME_FORMAT.format(buildTime)));
        lore.add(GuiText.line("Platform", platform.getPlatform().name()));
        lore.add(GuiText.line("Platform build", platform.getPlatformVersion()));
        lore.add(GuiText.line("Checks registered", checkCount > 0 ? String.valueOf(checkCount) : "Pending first join"));

        return GuiItems.simple(
                ItemTypes.TOTEM_OF_UNDYING,
                Component.text("Information", NamedTextColor.GREEN),
                lore
        );
    }

    private static ItemStack buildStatisticsTile(boolean dbReady) {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Aggregated alert and punishment", NamedTextColor.GRAY));
        lore.add(Component.text("counts across all players.", NamedTextColor.GRAY));
        lore.add(Component.empty());

        if (dbReady) {
            lore.add(Component.text("Click to browse ▶", NamedTextColor.DARK_GRAY));
            return GuiItems.simple(
                    ItemTypes.WRITABLE_BOOK,
                    Component.text("Statistics", NamedTextColor.AQUA),
                    lore
            );
        }
        lore.add(Component.text("Database is offline.", NamedTextColor.RED));
        lore.add(Component.text("Statistics are unavailable.", NamedTextColor.GRAY));
        return GuiItems.simple(
                ItemTypes.WRITTEN_BOOK,
                Component.text("Statistics", NamedTextColor.DARK_GRAY),
                lore
        );
    }

    @Override
    public String requiredPermission() {
        return "TotemGuardV3.Gui.Info";
    }

    @Override
    public GuiRenderResult render(GuiSession session) {
        TGPlatform platform = TGPlatform.getInstance();

        GuiRenderResult.Builder builder = GuiRenderResult.builder(4,
                GuiTitle.of("TotemGuard"));
        builder.fillEmpty(GuiItems.filler());

        builder.set(11, buildServicesTile(platform));
        builder.set(13, buildInformationTile(platform));

        boolean dbReady = platform.getDatabaseRepository().isConnected();
        if (dbReady) {
            builder.set(15, buildStatisticsTile(true),
                    ctx -> ctx.open(new StatisticsScreen(StatsWindow.ALL_TIME)));
        } else {
            builder.set(15, buildStatisticsTile(false));
        }

        builder.set(31, GuiItems.simple(
                ItemTypes.BARRIER,
                Component.text("Close", NamedTextColor.RED),
                List.of(Component.text("Close this screen", NamedTextColor.GRAY))
        ), ctx -> ctx.close());

        return builder.build();
    }
}
