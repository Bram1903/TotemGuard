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

package com.deathmotion.totemguard.common.gui.screen.stats;

import com.deathmotion.totemguard.api.result.ResultError;
import com.deathmotion.totemguard.api.stats.StatsSnapshot;
import com.deathmotion.totemguard.api.stats.StatsWindow;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.config.key.MessagesKeys;
import com.deathmotion.totemguard.common.gui.*;
import com.deathmotion.totemguard.common.message.MessageService;
import com.deathmotion.totemguard.common.util.NumberFormatter;
import com.deathmotion.totemguard.common.util.Palette;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public final class StatisticsScreen extends GuiScreen {

    private static final int SLOT_BACK = 0;
    private static final int SLOT_CURRENT_WINDOW = 13;
    private static final int[] FILTER_SLOTS = {19, 21, 23, 25};

    private final StatsWindow window;

    private volatile @Nullable StatsSnapshot loaded;
    private volatile @Nullable String loadError;
    private volatile boolean offline;

    public StatisticsScreen(StatsWindow window) {
        this.window = window;
    }

    private static Component playerShareLine(String label, long subset, long unique) {
        Component line = GuiText.line(label, NumberFormatter.grouped(subset));
        if (unique <= 0) return line;
        double pct = (subset * 100.0) / unique;
        String formatted = String.format(java.util.Locale.ROOT, "%.1f%%", pct);
        return line.append(Component.text(" (" + formatted + ")", Palette.CAPTION));
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format("%.1f KB", kb);
        double mb = kb / 1024.0;
        if (mb < 1024) return String.format("%.1f MB", mb);
        double gb = mb / 1024.0;
        if (gb < 1024) return String.format("%.2f GB", gb);
        return String.format("%.2f TB", gb / 1024.0);
    }

    @Override
    public String requiredPermission() {
        return "TotemGuard.Gui.Statistics";
    }

    @Override
    public void onOpen(GuiSession session) {
        TGPlatform platform = TGPlatform.getInstance();

        if (!platform.getDatabaseRepository().isConnected()) {
            this.offline = true;
            return;
        }

        platform.getStatsRepository().snapshot(window).thenAccept(response -> {
            if (response.ok()) {
                this.loaded = response.value();
            } else if (response.error() == ResultError.DATABASE_UNAVAILABLE) {
                this.offline = true;
            } else {
                this.loadError = response.message();
                platform.getLogger().log(Level.WARNING,
                        "Failed to load statistics for " + window.id() + ": " + response.message());
            }
            platform.getGuiManager().refresh(session.viewerId());
        });
    }

    @Override
    public GuiRenderResult render(GuiSession session) {
        MessageService messages = TGPlatform.getInstance().getMessageService();
        GuiRenderResult.Builder builder = GuiRenderResult.builder(3,
                GuiTitle.of(messages.getString(MessagesKeys.GUI_STATISTICS_TITLE)));
        builder.fillEmpty(GuiItems.filler());

        if (session.hasParent()) {
            builder.set(SLOT_BACK, GuiItems.simple(
                    ItemTypes.ARROW,
                    messages.getComponent(MessagesKeys.GUI_BTN_BACK_TITLE),
                    List.of(messages.getComponent(MessagesKeys.GUI_BTN_BACK_TO_OVERVIEW_LORE))
            ), ctx -> ctx.back());
        } else {
            builder.set(SLOT_BACK, GuiItems.simple(
                    ItemTypes.BARRIER,
                    messages.getComponent(MessagesKeys.GUI_BTN_CLOSE_TITLE),
                    List.of(messages.getComponent(MessagesKeys.GUI_BTN_CLOSE_LORE))
            ), ctx -> ctx.close());
        }

        if (offline) {
            builder.set(SLOT_CURRENT_WINDOW, GuiItems.simple(
                    ItemTypes.RED_CONCRETE,
                    messages.getComponent(MessagesKeys.GUI_ERR_DATABASE_OFFLINE),
                    List.of(
                            messages.getComponent(MessagesKeys.GUI_STATISTICS_DB_LORE_1),
                            messages.getComponent(MessagesKeys.GUI_ERR_DB_UNREACHABLE)
                    )
            ));
            return builder.build();
        }

        if (loadError != null) {
            builder.set(SLOT_CURRENT_WINDOW, GuiItems.simple(
                    ItemTypes.RED_CONCRETE,
                    messages.getComponent(MessagesKeys.GUI_ERR_FAILED_LOAD_STATS),
                    List.of(
                            messages.getComponent(MessagesKeys.GUI_ERR_CHECK_SERVER_LOG),
                            Component.text(loadError, Palette.VALUE_ON_DANGER)
                    )
            ));
            renderFilters(builder, messages);
            return builder.build();
        }

        builder.set(SLOT_CURRENT_WINDOW, currentWindowTile(this.loaded, messages));
        renderFilters(builder, messages);
        return builder.build();
    }

    private ItemStack currentWindowTile(@Nullable StatsSnapshot snapshot, MessageService messages) {
        List<Component> lore = new ArrayList<>();
        lore.add(GuiText.line("Active window", window.label()));
        lore.add(Component.empty());
        if (snapshot == null) {
            lore.add(messages.getComponent(MessagesKeys.GUI_LOADING_QUERYING_DATABASE));
        } else {
            lore.add(messages.getComponent(MessagesKeys.GUI_STATISTICS_SECTION_ACTIVITY));
            lore.add(GuiText.line("Alerts", NumberFormatter.grouped(snapshot.alertCount())));
            lore.add(GuiText.line("Punishments", NumberFormatter.grouped(snapshot.punishmentCount())));

            lore.add(Component.empty());
            lore.add(messages.getComponent(MessagesKeys.GUI_STATISTICS_SECTION_PLAYERS));
            lore.add(GuiText.line("Unique", NumberFormatter.grouped(snapshot.uniquePlayers())));
            lore.add(playerShareLine("Flagged", snapshot.flaggedPlayers(), snapshot.uniquePlayers()));
            lore.add(playerShareLine("Punished", snapshot.punishedPlayers(), snapshot.uniquePlayers()));

            lore.add(Component.empty());
            lore.add(messages.getComponent(MessagesKeys.GUI_STATISTICS_SECTION_STORAGE));
            lore.add(GuiText.line("DB size", formatBytes(snapshot.databaseBytes())));
        }
        lore.add(Component.empty());
        lore.add(messages.getComponent(MessagesKeys.GUI_STATISTICS_PICK_WINDOW_LORE));

        return GuiItems.simple(
                ItemTypes.CLOCK,
                messages.getComponent(MessagesKeys.GUI_STATISTICS_CURRENT_WINDOW_TITLE),
                lore
        );
    }

    private void renderFilters(GuiRenderResult.Builder builder, MessageService messages) {
        StatsWindow[] options = {
                StatsWindow.LAST_24_HOURS,
                StatsWindow.LAST_7_DAYS,
                StatsWindow.LAST_30_DAYS,
                StatsWindow.ALL_TIME
        };

        for (int i = 0; i < options.length; i++) {
            StatsWindow option = options[i];
            int slot = FILTER_SLOTS[i];
            boolean active = option == window;

            ItemStack item = GuiItems.simple(
                    active ? ItemTypes.LIME_DYE : ItemTypes.LIGHT_GRAY_DYE,
                    Component.text(option.label(), active ? Palette.SUCCESS : Palette.CAPTION),
                    active
                            ? List.of(messages.getComponent(MessagesKeys.GUI_STATUS_CURRENTLY_SELECTED))
                            : List.of(messages.getComponent(MessagesKeys.GUI_STATUS_CLICK_TO_SWITCH))
            );

            if (active) {
                builder.set(slot, item);
            } else {
                builder.set(slot, item, ctx -> ctx.replace(new StatisticsScreen(option)));
            }
        }
    }
}
