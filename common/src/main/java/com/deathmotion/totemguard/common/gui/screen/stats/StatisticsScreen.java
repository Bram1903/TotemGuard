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

import com.deathmotion.totemguard.api3.result.ResultError;
import com.deathmotion.totemguard.api3.stats.StatsSnapshot;
import com.deathmotion.totemguard.api3.stats.StatsWindow;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.gui.*;
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
        return "TotemGuardV3.Gui.Statistics";
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
        GuiRenderResult.Builder builder = GuiRenderResult.builder(3,
                GuiTitle.of("TotemGuard Statistics"));
        builder.fillEmpty(GuiItems.filler());

        if (session.hasParent()) {
            builder.set(SLOT_BACK, GuiItems.simple(
                    ItemTypes.ARROW,
                    Component.text("Back", Palette.BRAND),
                    List.of(Component.text("Return to the overview", Palette.CONNECTIVE))
            ), ctx -> ctx.back());
        } else {
            builder.set(SLOT_BACK, GuiItems.simple(
                    ItemTypes.BARRIER,
                    Component.text("Close", Palette.DANGER),
                    List.of(Component.text("Close this screen", Palette.CONNECTIVE))
            ), ctx -> ctx.close());
        }

        if (offline) {
            builder.set(SLOT_CURRENT_WINDOW, GuiItems.simple(
                    ItemTypes.RED_CONCRETE,
                    Component.text("Database offline", Palette.DANGER),
                    List.of(
                            Component.text("Statistics are unavailable. The database", Palette.CONNECTIVE),
                            Component.text("is disabled or currently unreachable.", Palette.CONNECTIVE)
                    )
            ));
            return builder.build();
        }

        if (loadError != null) {
            builder.set(SLOT_CURRENT_WINDOW, GuiItems.simple(
                    ItemTypes.RED_CONCRETE,
                    Component.text("Failed to load statistics", Palette.DANGER),
                    List.of(
                            Component.text("Check the server log for details.", Palette.CONNECTIVE),
                            Component.text(loadError, Palette.VALUE_ON_DANGER)
                    )
            ));
            renderFilters(builder);
            return builder.build();
        }

        builder.set(SLOT_CURRENT_WINDOW, currentWindowTile(this.loaded));
        renderFilters(builder);
        return builder.build();
    }

    private ItemStack currentWindowTile(@Nullable StatsSnapshot snapshot) {
        List<Component> lore = new ArrayList<>();
        lore.add(GuiText.line("Active window", window.label()));
        lore.add(Component.empty());
        if (snapshot == null) {
            lore.add(Component.text("Querying the database…", Palette.CONNECTIVE));
        } else {
            lore.add(GuiText.line("Alerts", String.valueOf(snapshot.alertCount())));
            lore.add(GuiText.line("Punishments", String.valueOf(snapshot.punishmentCount())));
            lore.add(GuiText.line("Unique players", String.valueOf(snapshot.uniquePlayers())));
            lore.add(GuiText.line("DB size", formatBytes(snapshot.databaseBytes())));
        }
        lore.add(Component.empty());
        lore.add(Component.text("Pick a different window below.", Palette.CAPTION));

        return GuiItems.simple(
                ItemTypes.CLOCK,
                Component.text("Current window", Palette.BRAND),
                lore
        );
    }

    private void renderFilters(GuiRenderResult.Builder builder) {
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
                            ? List.of(Component.text("Currently selected", Palette.SUCCESS))
                            : List.of(Component.text("Click to switch ▶", Palette.CAPTION))
            );

            if (active) {
                builder.set(slot, item);
            } else {
                builder.set(slot, item, ctx -> ctx.replace(new StatisticsScreen(option)));
            }
        }
    }
}
