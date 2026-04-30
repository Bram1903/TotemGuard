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
import com.deathmotion.totemguard.api3.stats.StatsWindow;
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
 * Paginated alert history, optionally filtered by check name and time window.
 */
public final class PlayerAlertsScreen extends GuiScreen {

    private static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };
    private static final int[] WINDOW_FILTER_SLOTS = {37, 39, 41, 43};
    private static final StatsWindow[] WINDOW_OPTIONS = {
            StatsWindow.LAST_24_HOURS,
            StatsWindow.LAST_7_DAYS,
            StatsWindow.LAST_30_DAYS,
            StatsWindow.ALL_TIME
    };

    private final UUID targetId;
    private final String targetName;
    private final int page;
    private final @Nullable String checkName;
    private final StatsWindow window;

    private volatile @Nullable HistoryPage<AlertEntry> loaded;
    private volatile @Nullable String loadError;
    private volatile boolean offline;

    public PlayerAlertsScreen(UUID targetId, String targetName, int page) {
        this(targetId, targetName, page, null, StatsWindow.ALL_TIME);
    }

    public PlayerAlertsScreen(UUID targetId, String targetName, int page, @Nullable String checkName) {
        this(targetId, targetName, page, checkName, StatsWindow.ALL_TIME);
    }

    public PlayerAlertsScreen(UUID targetId, String targetName, int page,
                              @Nullable String checkName, StatsWindow window) {
        this.targetId = targetId;
        this.targetName = targetName;
        this.page = Math.max(0, page);
        this.checkName = checkName;
        this.window = window;
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

    private String buildTitle() {
        StringBuilder t = new StringBuilder("Alerts");
        if (!window.isAllTime()) t.append(' ').append(window.id());
        if (checkName != null) {
            String shown = checkName.length() > 12 ? checkName.substring(0, 11) + "…" : checkName;
            t.append(" [").append(shown).append(']');
        }
        t.append(": ").append(targetName);
        return t.toString();
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

        platform.getHistoryRepository().alerts(targetId, page, checkName, window).thenAccept(response -> {
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
        GuiRenderResult.Builder builder = GuiRenderResult.builder(6, GuiTitle.of(buildTitle()));
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
                            Component.text("Alert history is unavailable. The database", NamedTextColor.GRAY),
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
            renderWindowFilters(builder);
            return builder.build();
        }

        HistoryPage<AlertEntry> result = this.loaded;

        if (result == null) {
            builder.set(22, GuiItems.simple(
                    ItemTypes.CLOCK,
                    Component.text("Loading…", NamedTextColor.YELLOW),
                    List.of(Component.text("Querying the database", NamedTextColor.GRAY))
            ));
            renderWindowFilters(builder);
            return builder.build();
        }

        List<AlertEntry> rows = result.entries();

        if (rows.isEmpty() && page == 0) {
            builder.set(22, buildEmptyTile());
            renderWindowFilters(builder);
            renderFooter(builder, result);
            return builder.build();
        }

        for (int i = 0; i < rows.size() && i < CONTENT_SLOTS.length; i++) {
            builder.set(CONTENT_SLOTS[i], buildAlertTile(rows.get(i)));
        }

        renderWindowFilters(builder);
        renderFooter(builder, result);
        return builder.build();
    }

    private ItemStack buildEmptyTile() {
        String windowSuffix = window.isAllTime() ? "" : " in the " + window.label().toLowerCase();
        if (checkName == null) {
            return GuiItems.simple(
                    ItemTypes.LIME_CONCRETE,
                    Component.text("Clean record" + windowSuffix, NamedTextColor.GREEN),
                    List.of(Component.text("No alerts have been logged" + windowSuffix + ".", NamedTextColor.GRAY))
            );
        }
        return GuiItems.simple(
                ItemTypes.LIME_CONCRETE,
                Component.text("No alerts for " + checkName + windowSuffix, NamedTextColor.GREEN),
                List.of(
                        Component.text("Nothing to show for this filter.", NamedTextColor.GRAY),
                        Component.text("Try a different check or window.", NamedTextColor.GRAY)
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

    private void renderWindowFilters(GuiRenderResult.Builder builder) {
        for (int i = 0; i < WINDOW_OPTIONS.length; i++) {
            StatsWindow option = WINDOW_OPTIONS[i];
            int slot = WINDOW_FILTER_SLOTS[i];
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
                builder.set(slot, item, ctx ->
                        ctx.replace(new PlayerAlertsScreen(targetId, targetName, 0, checkName, option)));
            }
        }
    }

    private void renderFooter(GuiRenderResult.Builder builder, HistoryPage<AlertEntry> result) {
        int total = result.totalEntries();
        int pages = result.totalPages();

        if (result.hasPrevious()) {
            builder.set(48, GuiItems.simple(
                    ItemTypes.ARROW,
                    Component.text("Previous page", NamedTextColor.GOLD),
                    List.of(Component.text("Page " + page, NamedTextColor.GRAY))
            ), ctx -> ctx.replace(new PlayerAlertsScreen(targetId, targetName, page - 1, checkName, window)));
        }

        List<Component> footerLore = new ArrayList<>();
        footerLore.add(GuiText.line("Window", window.label()));
        if (checkName != null) footerLore.add(GuiText.line("Check filter", checkName));
        footerLore.add(GuiText.line("Matching alerts", String.valueOf(total)));

        builder.set(49, GuiItems.simple(
                ItemTypes.PAPER,
                Component.text("Page " + (page + 1) + " of " + pages, NamedTextColor.AQUA),
                footerLore
        ));

        if (result.hasNext()) {
            builder.set(50, GuiItems.simple(
                    ItemTypes.ARROW,
                    Component.text("Next page", NamedTextColor.GOLD),
                    List.of(Component.text("Page " + (page + 2), NamedTextColor.GRAY))
            ), ctx -> ctx.replace(new PlayerAlertsScreen(targetId, targetName, page + 1, checkName, window)));
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
            ), ctx -> ctx.replace(new PlayerAlertsScreen(targetId, targetName, 0, null, window)));
        }
    }
}
