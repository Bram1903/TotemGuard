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

import com.deathmotion.totemguard.api.stats.StatsWindow;
import com.deathmotion.totemguard.api.versioning.TGAPIVersions;
import com.deathmotion.totemguard.api.versioning.TGVersion;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.check.CheckManagerImpl;
import com.deathmotion.totemguard.common.config.key.MessagesKeys;
import com.deathmotion.totemguard.common.gui.*;
import com.deathmotion.totemguard.common.gui.screen.stats.StatisticsScreen;
import com.deathmotion.totemguard.common.message.MessageService;
import com.deathmotion.totemguard.common.network.NetworkPresenceRepository;
import com.deathmotion.totemguard.common.util.TGVersions;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import net.kyori.adventure.text.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class TotemGuardInfoScreen extends GuiScreen {

    public static final String PERMISSION = "TotemGuard.Gui.Info";
    private static final DateTimeFormatter BUILD_TIME_FORMAT = DateTimeFormatter
            .ofPattern("dd-MM-yyyy HH:mm", Locale.ROOT)
            .withZone(ZoneId.systemDefault());

    private static ItemStack buildServicesTile(TGPlatform platform) {
        MessageService messages = platform.getMessageService();
        boolean dbConnected = platform.getDatabaseRepository().isConnected();
        boolean dbEnabled = platform.getDatabaseRepository().isEnabled();
        boolean redisConnected = platform.getRedisRepository().isConnected();
        boolean redisEnabled = platform.getRedisRepository().isEnabled();

        List<Component> lore = new ArrayList<>();
        lore.add(messages.getComponent(MessagesKeys.GUI_INFO_SERVICES_LORE_1));
        lore.add(Component.empty());
        lore.add(serviceLine(messages, "Database", dbEnabled, dbConnected));
        lore.add(serviceLine(messages, "Redis", redisEnabled, redisConnected));

        NetworkPresenceRepository presence = platform.getNetworkPresenceRepository();
        if (redisEnabled && redisConnected && presence != null) {
            int fleetSize = presence.fleetSize();
            int trackedPlayers = presence.trackedPlayerCount();
            lore.add(Component.empty());
            lore.add(messages.getComponent(
                    MessagesKeys.GUI_INFO_NETWORK_BACKENDS,
                    Map.of("tg_count", fleetSize)
            ));
            lore.add(messages.getComponent(
                    MessagesKeys.GUI_INFO_NETWORK_PLAYERS,
                    Map.of("tg_count", trackedPlayers)
            ));
        }

        return GuiItems.simple(
                ItemTypes.ENDER_CHEST,
                messages.getComponent(MessagesKeys.GUI_INFO_SERVICES_TITLE),
                lore
        );
    }

    private static Component serviceLine(MessageService messages, String label, boolean enabled, boolean connected) {
        Component status;
        if (!enabled) {
            status = messages.getComponent(MessagesKeys.GUI_STATUS_DISABLED);
        } else if (connected) {
            status = messages.getComponent(MessagesKeys.GUI_STATUS_CONNECTED);
        } else {
            status = messages.getComponent(MessagesKeys.GUI_STATUS_DISCONNECTED);
        }
        return GuiText.line(label, "").append(status);
    }

    private static ItemStack buildInformationTile(TGPlatform platform) {
        MessageService messages = platform.getMessageService();
        TGVersion pluginVersion = TGVersions.CURRENT;
        Instant buildTime = TGVersions.BUILD_TIMESTAMP;
        int checkCount = CheckManagerImpl.knownCheckCount();

        List<Component> lore = new ArrayList<>();
        lore.add(messages.getComponent(MessagesKeys.GUI_INFO_SECTION_VERSION));
        lore.add(GuiText.line("Plugin", pluginVersion.toDisplayString()));
        lore.add(GuiText.line("API", TGAPIVersions.CURRENT.toDisplayString()));
        lore.add(GuiText.line("Build time", BUILD_TIME_FORMAT.format(buildTime)));

        lore.add(Component.empty());
        lore.add(messages.getComponent(MessagesKeys.GUI_INFO_SECTION_PLATFORM));
        lore.add(GuiText.line("Implementation", platform.getPlatform().displayName()));
        lore.add(GuiText.line("Build", platform.getPlatformVersion()));
        lore.add(GuiText.line("Checks registered", checkCount > 0 ? String.valueOf(checkCount) : "Pending first join"));

        if (pluginVersion.snapshot() && pluginVersion.snapshotCommit() != null) {
            lore.add(Component.empty());
            lore.add(messages.getComponent(MessagesKeys.GUI_INFO_SECTION_DEV_BUILD));
            lore.add(GuiText.line("Commit", pluginVersion.snapshotCommit()));
        }

        return GuiItems.simple(
                ItemTypes.TOTEM_OF_UNDYING,
                messages.getComponent(MessagesKeys.GUI_INFO_INFORMATION_TITLE),
                lore
        );
    }

    private static ItemStack buildStatisticsTile(boolean dbReady) {
        MessageService messages = TGPlatform.getInstance().getMessageService();
        List<Component> lore = new ArrayList<>();
        lore.add(messages.getComponent(MessagesKeys.GUI_INFO_STATISTICS_LORE_1));
        lore.add(messages.getComponent(MessagesKeys.GUI_INFO_STATISTICS_LORE_2));
        lore.add(Component.empty());

        if (dbReady) {
            lore.add(messages.getComponent(MessagesKeys.GUI_STATUS_CLICK_TO_BROWSE));
            return GuiItems.simple(
                    ItemTypes.WRITABLE_BOOK,
                    messages.getComponent(MessagesKeys.GUI_INFO_STATISTICS_TITLE),
                    lore
            );
        }
        lore.add(messages.getComponent(MessagesKeys.GUI_INFO_STATISTICS_OFFLINE_1));
        lore.add(messages.getComponent(MessagesKeys.GUI_INFO_STATISTICS_OFFLINE_2));
        return GuiItems.simple(
                ItemTypes.WRITTEN_BOOK,
                messages.getComponent(MessagesKeys.GUI_INFO_STATISTICS_DISABLED),
                lore
        );
    }

    @Override
    public String requiredPermission() {
        return PERMISSION;
    }

    @Override
    public GuiRenderResult render(GuiSession session) {
        TGPlatform platform = TGPlatform.getInstance();
        MessageService messages = platform.getMessageService();

        GuiRenderResult.Builder builder = GuiRenderResult.builder(4,
                GuiTitle.of(messages.getString(MessagesKeys.GUI_INFO_TITLE)));
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
                messages.getComponent(MessagesKeys.GUI_BTN_CLOSE_TITLE),
                List.of(messages.getComponent(MessagesKeys.GUI_BTN_CLOSE_LORE))
        ), ctx -> ctx.close());

        return builder.build();
    }
}
