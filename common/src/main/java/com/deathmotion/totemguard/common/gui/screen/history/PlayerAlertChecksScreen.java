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

package com.deathmotion.totemguard.common.gui.screen.history;

import com.deathmotion.totemguard.api3.result.ResultError;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.database.model.AlertCheckSummary;
import com.deathmotion.totemguard.common.gui.*;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Alphabetical list of every check this player has ever flagged.
 */
public final class PlayerAlertChecksScreen extends GuiScreen {

    static final int PAGE_SIZE = 28;
    private static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private final UUID targetId;
    private final String targetName;
    private final int page;

    private volatile @Nullable List<AlertCheckSummary> loaded;
    private volatile @Nullable String loadError;
    private volatile boolean offline;

    public PlayerAlertChecksScreen(UUID targetId, String targetName, int page) {
        this.targetId = targetId;
        this.targetName = targetName;
        this.page = Math.max(0, page);
    }

    @Override
    public String requiredPermission() {
        return "TotemGuardV3.Gui.History.Alerts";
    }

    @Override
    public void onOpen(GuiSession session) {
        TGPlatform platform = TGPlatform.getInstance();

        if (!platform.getDatabaseRepository().isConnected()) {
            this.loaded = List.of();
            this.offline = true;
            return;
        }

        platform.getHistoryRepository().alertCheckSummaries(targetId).thenAccept(response -> {
            if (response.ok()) {
                this.loaded = response.value();
            } else if (response.error() == ResultError.DATABASE_UNAVAILABLE) {
                this.loaded = List.of();
                this.offline = true;
            } else {
                this.loaded = List.of();
                this.loadError = response.message();
                platform.getLogger().log(Level.WARNING,
                        "Failed to load alert check summaries for " + targetId + ": " + response.message());
            }
            platform.getGuiManager().refresh(session.viewerId());
        });
    }

    @Override
    public GuiRenderResult render(GuiSession session) {
        GuiRenderResult.Builder builder = GuiRenderResult.builder(6,
                GuiTitle.of("Alerts by check: " + targetName));
        builder.fillEmpty(GuiItems.filler());

        builder.set(0, GuiItems.simple(
                ItemTypes.ARROW,
                Component.text("Back", NamedTextColor.GOLD),
                List.of(Component.text("Return to the full alerts feed", NamedTextColor.GRAY))
        ), ctx -> ctx.back());

        if (offline) {
            builder.set(22, GuiItems.simple(
                    ItemTypes.RED_CONCRETE,
                    Component.text("Database offline", NamedTextColor.RED),
                    List.of(Component.text("Filtering is unavailable right now.", NamedTextColor.GRAY))
            ));
            return builder.build();
        }

        List<AlertCheckSummary> all = this.loaded;

        if (all == null) {
            builder.set(22, GuiItems.simple(
                    ItemTypes.CLOCK,
                    Component.text("Loading…", NamedTextColor.YELLOW),
                    List.of(Component.text("Querying the database", NamedTextColor.GRAY))
            ));
            return builder.build();
        }

        if (loadError != null) {
            builder.set(22, GuiItems.simple(
                    ItemTypes.RED_CONCRETE,
                    Component.text("Failed to load checks", NamedTextColor.RED),
                    List.of(
                            Component.text("Check the server log for details.", NamedTextColor.GRAY),
                            Component.text(loadError, NamedTextColor.DARK_RED)
                    )
            ));
            return builder.build();
        }

        if (all.isEmpty()) {
            builder.set(22, GuiItems.simple(
                    ItemTypes.LIME_CONCRETE,
                    Component.text("Clean record", NamedTextColor.GREEN),
                    List.of(Component.text("No alerts have been logged for this player.", NamedTextColor.GRAY))
            ));
            return builder.build();
        }

        int from = Math.min(page * PAGE_SIZE, all.size());
        int to = Math.min(from + PAGE_SIZE, all.size());
        List<AlertCheckSummary> pageRows = all.subList(from, to);

        for (int i = 0; i < pageRows.size(); i++) {
            AlertCheckSummary summary = pageRows.get(i);
            builder.set(CONTENT_SLOTS[i], GuiItems.simple(
                    ItemTypes.PAPER,
                    Component.text(summary.checkName(), NamedTextColor.YELLOW),
                    List.of(
                            GuiText.line("Alerts", String.valueOf(summary.alertCount())),
                            Component.empty(),
                            Component.text("Click to view only this check ▶", NamedTextColor.DARK_GRAY)
                    )
            ), ctx -> ctx.replace(new PlayerAlertsScreen(
                    targetId, targetName, 0, summary.checkName())));
        }

        renderFooter(builder, all.size());
        return builder.build();
    }

    private void renderFooter(GuiRenderResult.Builder builder, int total) {
        int pages = Math.max(1, (int) Math.ceil(total / (double) PAGE_SIZE));
        boolean hasPrev = page > 0;
        boolean hasNext = (page + 1) < pages;

        if (hasPrev) {
            builder.set(48, GuiItems.simple(
                    ItemTypes.ARROW,
                    Component.text("Previous page", NamedTextColor.GOLD),
                    List.of(Component.text("Page " + page, NamedTextColor.GRAY))
            ), ctx -> ctx.replace(new PlayerAlertChecksScreen(targetId, targetName, page - 1)));
        }

        builder.set(49, GuiItems.simple(
                ItemTypes.PAPER,
                Component.text("Page " + (page + 1) + " of " + pages, NamedTextColor.AQUA),
                List.of(GuiText.line("Distinct checks", String.valueOf(total)))
        ));

        if (hasNext) {
            builder.set(50, GuiItems.simple(
                    ItemTypes.ARROW,
                    Component.text("Next page", NamedTextColor.GOLD),
                    List.of(Component.text("Page " + (page + 2), NamedTextColor.GRAY))
            ), ctx -> ctx.replace(new PlayerAlertChecksScreen(targetId, targetName, page + 1)));
        }
    }
}
