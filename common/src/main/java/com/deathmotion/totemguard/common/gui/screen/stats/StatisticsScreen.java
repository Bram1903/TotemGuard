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
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public final class StatisticsScreen extends GuiScreen {

    private final StatsWindow window;

    private volatile @Nullable StatsSnapshot loaded;
    private volatile @Nullable String loadError;
    private volatile boolean offline;

    public StatisticsScreen(StatsWindow window) {
        this.window = window;
    }

    private static ItemStack loadingTile(String label) {
        return GuiItems.simple(
                ItemTypes.CLOCK,
                Component.text(label, NamedTextColor.YELLOW),
                List.of(Component.text("Querying the database", NamedTextColor.GRAY))
        );
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
                Component.text("TotemGuard Statistics", NamedTextColor.AQUA));
        builder.fillEmpty(GuiItems.filler());

        if (session.hasParent()) {
            builder.set(0, GuiItems.simple(
                    ItemTypes.ARROW,
                    Component.text("Back", NamedTextColor.GOLD),
                    List.of(Component.text("Return to the overview", NamedTextColor.GRAY))
            ), ctx -> ctx.back());
        } else {
            builder.set(0, GuiItems.simple(
                    ItemTypes.BARRIER,
                    Component.text("Close", NamedTextColor.RED),
                    List.of(Component.text("Close this screen", NamedTextColor.GRAY))
            ), ctx -> ctx.close());
        }

        if (offline) {
            builder.set(13, GuiItems.simple(
                    ItemTypes.RED_CONCRETE,
                    Component.text("Database offline", NamedTextColor.RED),
                    List.of(
                            Component.text("Statistics are unavailable. The database", NamedTextColor.GRAY),
                            Component.text("is disabled or currently unreachable.", NamedTextColor.GRAY)
                    )
            ));
            return builder.build();
        }

        if (loadError != null) {
            builder.set(13, GuiItems.simple(
                    ItemTypes.RED_CONCRETE,
                    Component.text("Failed to load statistics", NamedTextColor.RED),
                    List.of(
                            Component.text("Check the server log for details.", NamedTextColor.GRAY),
                            Component.text(loadError, NamedTextColor.DARK_RED)
                    )
            ));
            renderFilters(builder);
            return builder.build();
        }

        StatsSnapshot snapshot = this.loaded;

        if (snapshot == null) {
            builder.set(11, loadingTile("Total alerts"));
            builder.set(13, currentWindowTile(null));
            builder.set(15, loadingTile("Total punishments"));
            renderFilters(builder);
            return builder.build();
        }

        builder.set(11, alertsTile(snapshot.alertCount()));
        builder.set(13, currentWindowTile(snapshot));
        builder.set(15, punishmentsTile(snapshot.punishmentCount()));

        renderFilters(builder);
        return builder.build();
    }

    private ItemStack alertsTile(int count) {
        return GuiItems.simple(
                ItemTypes.PAPER,
                Component.text("Total alerts", NamedTextColor.YELLOW),
                List.of(
                        GuiText.line("Window", window.label()),
                        GuiText.line("Count", String.valueOf(count))
                )
        );
    }

    private ItemStack punishmentsTile(int count) {
        return GuiItems.simple(
                ItemTypes.IRON_AXE,
                Component.text("Total punishments", NamedTextColor.RED),
                List.of(
                        GuiText.line("Window", window.label()),
                        GuiText.line("Count", String.valueOf(count))
                )
        );
    }

    private ItemStack currentWindowTile(@Nullable StatsSnapshot snapshot) {
        List<Component> lore = new ArrayList<>();
        lore.add(GuiText.line("Active window", window.label()));
        lore.add(Component.empty());
        if (snapshot == null) {
            lore.add(Component.text("Querying the database…", NamedTextColor.GRAY));
        } else {
            lore.add(GuiText.line("Alerts", String.valueOf(snapshot.alertCount())));
            lore.add(GuiText.line("Punishments", String.valueOf(snapshot.punishmentCount())));
        }
        lore.add(Component.empty());
        lore.add(Component.text("Pick a different window below.", NamedTextColor.DARK_GRAY));

        return GuiItems.simple(
                ItemTypes.CLOCK,
                Component.text("Current window", NamedTextColor.AQUA),
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
        int[] slots = {19, 21, 23, 25};

        for (int i = 0; i < options.length; i++) {
            StatsWindow option = options[i];
            int slot = slots[i];
            boolean active = option == window;

            ItemStack item = GuiItems.simple(
                    active ? ItemTypes.LIME_DYE : ItemTypes.LIGHT_GRAY_DYE,
                    Component.text(option.label(), active ? NamedTextColor.GREEN : NamedTextColor.GRAY),
                    active
                            ? List.of(Component.text("Currently selected", NamedTextColor.DARK_GREEN))
                            : List.of(Component.text("Click to switch ▶", NamedTextColor.DARK_GRAY))
            );

            if (active) {
                builder.set(slot, item);
            } else {
                builder.set(slot, item, ctx -> ctx.replace(new StatisticsScreen(option)));
            }
        }
    }
}
