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

import com.deathmotion.totemguard.api3.history.AlertEntry;
import com.deathmotion.totemguard.api3.history.HistoryPage;
import com.deathmotion.totemguard.api3.result.ResultError;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.gui.*;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Paginated alert history, optionally filtered by check name.
 */
public final class PlayerAlertsScreen extends GuiScreen {

    private static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private final UUID targetId;
    private final String targetName;
    private final int page;
    private final @Nullable String checkName;

    private volatile @Nullable HistoryPage<AlertEntry> loaded;
    private volatile @Nullable String loadError;
    private volatile boolean offline;

    public PlayerAlertsScreen(UUID targetId, String targetName, int page) {
        this(targetId, targetName, page, null);
    }

    public PlayerAlertsScreen(UUID targetId, String targetName, int page, @Nullable String checkName) {
        this.targetId = targetId;
        this.targetName = targetName;
        this.page = Math.max(0, page);
        this.checkName = checkName;
    }

    private static String formatClientVersion(Integer protocol) {
        if (protocol == null) return null;
        try {
            ClientVersion cv = ClientVersion.getById(protocol);
            if (cv != null && cv != ClientVersion.UNKNOWN) {
                return cv.getReleaseName();
            }
        } catch (Throwable ignored) {
        }
        return "protocol " + protocol;
    }

    @Override
    public String requiredPermission() {
        return "TotemGuardV3.Gui.History.Alerts";
    }

    @Override
    public void onOpen(GuiSession session) {
        TGPlatform platform = TGPlatform.getInstance();

        if (!platform.getDatabaseRepository().isConnected()) {
            this.offline = true;
            return;
        }

        platform.getHistoryRepository().alerts(targetId, page, checkName).thenAccept(response -> {
            if (response.ok()) {
                this.loaded = response.value();
            } else if (response.error() == ResultError.DATABASE_UNAVAILABLE) {
                this.offline = true;
            } else {
                this.loadError = response.message();
                platform.getLogger().log(Level.WARNING,
                        "Failed to load alert history for " + targetId + ": " + response.message());
            }
            platform.getGuiManager().refresh(session.viewerId());
        });
    }

    @Override
    public GuiRenderResult render(GuiSession session) {
        String title = checkName == null
                ? "Alerts: " + targetName
                : "Alerts [" + checkName + "]: " + targetName;

        GuiRenderResult.Builder builder = GuiRenderResult.builder(6, GuiTitle.of(title));
        builder.fillEmpty(GuiItems.filler());

        builder.set(0, GuiItems.simple(
                ItemTypes.ARROW,
                Component.text("Back", NamedTextColor.GOLD),
                List.of(Component.text("Return to the history menu", NamedTextColor.GRAY))
        ), ctx -> ctx.back());

        if (offline) {
            builder.set(22, GuiItems.simple(
                    ItemTypes.RED_CONCRETE,
                    Component.text("Database offline", NamedTextColor.RED),
                    List.of(
                            Component.text("Alert history is unavailable — the database", NamedTextColor.GRAY),
                            Component.text("is disabled or currently unreachable.", NamedTextColor.GRAY)
                    )
            ));
            return builder.build();
        }

        if (loadError != null) {
            builder.set(22, GuiItems.simple(
                    ItemTypes.RED_CONCRETE,
                    Component.text("Failed to load alerts", NamedTextColor.RED),
                    List.of(
                            Component.text("Check the server log for details.", NamedTextColor.GRAY),
                            Component.text(loadError, NamedTextColor.DARK_RED)
                    )
            ));
            return builder.build();
        }

        HistoryPage<AlertEntry> result = this.loaded;

        if (result == null) {
            builder.set(22, GuiItems.simple(
                    ItemTypes.CLOCK,
                    Component.text("Loading…", NamedTextColor.YELLOW),
                    List.of(Component.text("Querying the database", NamedTextColor.GRAY))
            ));
            return builder.build();
        }

        List<AlertEntry> rows = result.entries();

        if (rows.isEmpty() && page == 0) {
            builder.set(22, buildEmptyTile());
            renderFooter(builder, result);
            return builder.build();
        }

        for (int i = 0; i < rows.size() && i < CONTENT_SLOTS.length; i++) {
            builder.set(CONTENT_SLOTS[i], buildAlertTile(rows.get(i)));
        }

        renderFooter(builder, result);
        return builder.build();
    }

    private ItemStack buildEmptyTile() {
        if (checkName == null) {
            return GuiItems.simple(
                    ItemTypes.LIME_CONCRETE,
                    Component.text("Clean record", NamedTextColor.GREEN),
                    List.of(Component.text("No alerts have been logged for this player.", NamedTextColor.GRAY))
            );
        }
        return GuiItems.simple(
                ItemTypes.LIME_CONCRETE,
                Component.text("No alerts for " + checkName, NamedTextColor.GREEN),
                List.of(
                        Component.text("Nothing to show for this filter.", NamedTextColor.GRAY),
                        Component.text("The player may have flagged it under", NamedTextColor.GRAY),
                        Component.text("a different check name previously.", NamedTextColor.GRAY)
                )
        );
    }

    private ItemStack buildAlertTile(AlertEntry record) {
        List<Component> lore = new ArrayList<>();
        lore.add(GuiText.line("Violations", String.valueOf(record.violations())));
        lore.add(GuiText.line("Server", record.serverName()));
        lore.add(GuiText.line("When", HistoryText.relative(record.createdAt())
                + "  (" + HistoryText.absolute(record.createdAt()) + ")"));

        String version = formatClientVersion(record.clientVersion());
        String brand = record.clientBrand();
        if (version != null || (brand != null && !brand.isBlank())) {
            lore.add(Component.empty());
            if (version != null) lore.add(GuiText.line("Client", version));
            if (brand != null && !brand.isBlank()) lore.add(GuiText.line("Brand", brand));
        }

        if (record.keepalivePing() != null || record.transactionPing() != null) {
            lore.add(Component.empty());
            if (record.keepalivePing() != null) {
                lore.add(GuiText.line("KeepAlive ping", record.keepalivePing() + " ms"));
            }
            if (record.transactionPing() != null) {
                lore.add(GuiText.line("Transaction ping", record.transactionPing() + " ms"));
            }
        }

        if (record.debug() != null && !record.debug().isEmpty()) {
            lore.add(Component.empty());
            lore.add(Component.text("Debug:", NamedTextColor.GRAY));
            lore.add(Component.text(record.debug(), NamedTextColor.WHITE));
        }

        return GuiItems.simple(
                ItemTypes.PAPER,
                Component.text(record.checkName(), NamedTextColor.YELLOW),
                lore
        );
    }

    private void renderFooter(GuiRenderResult.Builder builder, HistoryPage<AlertEntry> result) {
        int total = result.totalEntries();
        int pages = result.totalPages();

        if (result.hasPrevious()) {
            builder.set(48, GuiItems.simple(
                    ItemTypes.ARROW,
                    Component.text("Previous page", NamedTextColor.GOLD),
                    List.of(Component.text("Page " + page, NamedTextColor.GRAY))
            ), ctx -> ctx.replace(new PlayerAlertsScreen(targetId, targetName, page - 1, checkName)));
        }

        builder.set(49, GuiItems.simple(
                ItemTypes.PAPER,
                Component.text("Page " + (page + 1) + " of " + pages, NamedTextColor.AQUA),
                checkName == null
                        ? List.of(GuiText.line("Total alerts", String.valueOf(total)))
                        : List.of(
                        GuiText.line("Filter", checkName),
                        GuiText.line("Matching alerts", String.valueOf(total)))
        ));

        if (result.hasNext()) {
            builder.set(50, GuiItems.simple(
                    ItemTypes.ARROW,
                    Component.text("Next page", NamedTextColor.GOLD),
                    List.of(Component.text("Page " + (page + 2), NamedTextColor.GRAY))
            ), ctx -> ctx.replace(new PlayerAlertsScreen(targetId, targetName, page + 1, checkName)));
        }

        renderFilterButton(builder);
    }

    private void renderFilterButton(GuiRenderResult.Builder builder) {
        if (checkName == null) {
            builder.set(53, GuiItems.simple(
                    ItemTypes.HOPPER,
                    Component.text("Filter by check", NamedTextColor.AQUA),
                    List.of(
                            Component.text("Only show alerts for a specific check.", NamedTextColor.GRAY),
                            Component.empty(),
                            Component.text("Click to pick a check ▶", NamedTextColor.DARK_GRAY)
                    )
            ), ctx -> ctx.open(new PlayerAlertChecksScreen(targetId, targetName, 0)));
        } else {
            builder.set(52, GuiItems.simple(
                    ItemTypes.HOPPER,
                    Component.text("Change filter", NamedTextColor.AQUA),
                    List.of(Component.text("Pick a different check", NamedTextColor.GRAY))
            ), ctx -> ctx.replace(new PlayerAlertChecksScreen(targetId, targetName, 0)));

            builder.set(53, GuiItems.simple(
                    ItemTypes.BARRIER,
                    Component.text("Clear filter", NamedTextColor.RED),
                    List.of(Component.text("Return to all alerts", NamedTextColor.GRAY))
            ), ctx -> ctx.back());
        }
    }
}
