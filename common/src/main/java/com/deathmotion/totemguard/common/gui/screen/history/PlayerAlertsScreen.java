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

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.cache.CacheCodecs;
import com.deathmotion.totemguard.common.cache.CacheKeys;
import com.deathmotion.totemguard.common.cache.CacheRepositoryImpl;
import com.deathmotion.totemguard.common.database.model.AlertRecord;
import com.deathmotion.totemguard.common.gui.*;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Paginated alerts history for one player.
 *
 * <p>Without a {@code checkName} filter this is the "recent firehose" view —
 * every check mixed together, newest first. With a filter applied only that
 * one check's alerts appear, and the footer picks up a "Clear filter" button
 * so the operator can return to the full view in one click.</p>
 *
 * <p>The database query runs off the main thread during
 * {@link #onOpen(GuiSession)}; while it's in-flight the screen shows a single
 * "Loading…" tile so the player never stares at filler squares.</p>
 */
public final class PlayerAlertsScreen extends GuiScreen {

    static final int PAGE_SIZE = 21;
    // Multiple staff viewing the same profile on a busy server shouldn't each
    // fan out independent DB round-trips — 2 minutes is short enough that new
    // alerts still surface quickly and long enough that pagination hops don't
    // repeatedly hit the database.
    private static final Duration HISTORY_TTL = Duration.ofMinutes(2);
    // Content slots in a 6-row GUI: rows 1..3 of the middle 7 columns.
    // That gives us PAGE_SIZE = 21 alert tiles per page.
    private static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private final UUID targetId;
    private final String targetName;
    private final int page;
    private final @Nullable String checkName;

    private volatile @Nullable List<AlertRecord> loaded;
    private volatile int totalCount = -1;
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

    /**
     * Resolves a stored protocol version to its friendly release name when
     * PacketEvents knows about it, falling back to {@code protocol#N} so old
     * clients we haven't mapped still show something meaningful.
     */
    private static String formatClientVersion(Integer protocol) {
        if (protocol == null) return null;
        try {
            ClientVersion cv = ClientVersion.getById(protocol);
            if (cv != null && cv != ClientVersion.UNKNOWN) {
                return cv.getReleaseName();
            }
        } catch (Throwable ignored) {
            // fall through to raw protocol
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

        // When the database is disabled or unreachable we don't schedule any
        // JDBC work — the screen renders an "offline" tile directly so nothing
        // ends up in the server log.
        if (!platform.getDatabaseRepository().isConnected()) {
            this.loaded = List.of();
            this.totalCount = 0;
            this.offline = true;
            return;
        }

        platform.getScheduler().runAsyncTask(() -> {
            CacheRepositoryImpl cache = platform.getCacheRepository();
            String pageKey = CacheKeys.alertHistoryPage(targetId, page, checkName);
            String countKey = CacheKeys.alertHistoryCount(targetId, checkName);

            List<AlertRecord> cachedPage = cache.get(pageKey, CacheCodecs.ALERT_RECORDS);
            Integer cachedCount = cache.get(countKey, CacheCodecs.INT);
            if (cachedPage != null && cachedCount != null) {
                this.loaded = cachedPage;
                this.totalCount = cachedCount;
                platform.getGuiManager().refresh(session.viewerId());
                return;
            }

            try {
                List<AlertRecord> rows;
                int total;
                if (checkName == null) {
                    rows = platform.getDatabaseRepository()
                            .findAlertsByPlayer(targetId, PAGE_SIZE, page * PAGE_SIZE);
                    total = platform.getDatabaseRepository().countAlertsByPlayer(targetId);
                } else {
                    rows = platform.getDatabaseRepository()
                            .findAlertsByPlayerAndCheck(targetId, checkName, PAGE_SIZE, page * PAGE_SIZE);
                    total = platform.getDatabaseRepository()
                            .countAlertsByPlayerAndCheck(targetId, checkName);
                }
                this.loaded = rows;
                this.totalCount = total;
                cache.put(pageKey, rows, CacheCodecs.ALERT_RECORDS, HISTORY_TTL);
                cache.put(countKey, total, CacheCodecs.INT, HISTORY_TTL);
            } catch (Exception ex) {
                // A connection drop between the isConnected() check and the
                // query is expected and not worth a stack trace — anything
                // else is a real bug and we want the details.
                if (!platform.getDatabaseRepository().isConnected()) {
                    this.loaded = List.of();
                    this.totalCount = 0;
                    this.offline = true;
                } else {
                    this.loaded = List.of();
                    this.totalCount = 0;
                    this.loadError = ex.getMessage();
                    platform.getLogger().log(Level.WARNING,
                            "Failed to load alert history for " + targetId, ex);
                }
            }
            platform.getGuiManager().refresh(session.viewerId());
        });
    }

    @Override
    public GuiRenderResult render(GuiSession session) {
        String title = checkName == null
                ? "Alerts: " + targetName
                : "Alerts [" + checkName + "]: " + targetName;

        GuiRenderResult.Builder builder = GuiRenderResult.builder(6,
                Component.text(title, NamedTextColor.GOLD));
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

        List<AlertRecord> rows = this.loaded;

        if (rows == null) {
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
                    Component.text("Failed to load alerts", NamedTextColor.RED),
                    List.of(
                            Component.text("Check the server log for details.", NamedTextColor.GRAY),
                            Component.text(loadError, NamedTextColor.DARK_RED)
                    )
            ));
            return builder.build();
        }

        if (rows.isEmpty() && page == 0) {
            builder.set(22, buildEmptyTile());
            renderFooter(builder);
            return builder.build();
        }

        for (int i = 0; i < rows.size() && i < CONTENT_SLOTS.length; i++) {
            AlertRecord record = rows.get(i);
            builder.set(CONTENT_SLOTS[i], buildAlertTile(record));
        }

        renderFooter(builder);
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

    private ItemStack buildAlertTile(AlertRecord record) {
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

    private void renderFooter(GuiRenderResult.Builder builder) {
        int total = Math.max(0, totalCount);
        int pages = Math.max(1, (int) Math.ceil(total / (double) PAGE_SIZE));
        boolean hasPrev = page > 0;
        boolean hasNext = (page + 1) < pages;

        if (hasPrev) {
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

        if (hasNext) {
            builder.set(50, GuiItems.simple(
                    ItemTypes.ARROW,
                    Component.text("Next page", NamedTextColor.GOLD),
                    List.of(Component.text("Page " + (page + 2), NamedTextColor.GRAY))
            ), ctx -> ctx.replace(new PlayerAlertsScreen(targetId, targetName, page + 1, checkName)));
        }

        renderFilterButton(builder);
    }

    /**
     * Slot 53 holds the filter affordance. On the unfiltered feed it opens
     * the check picker (so {@code back} still returns to the full feed). On a
     * filtered feed "Clear filter" pops back to the full feed, and "Change
     * filter" replaces with the picker to avoid stack growth.
     */
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
