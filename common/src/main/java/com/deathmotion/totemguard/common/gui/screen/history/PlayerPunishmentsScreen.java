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

import com.deathmotion.totemguard.api3.punishment.PunishmentType;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.cache.CacheCodecs;
import com.deathmotion.totemguard.common.cache.CacheKeys;
import com.deathmotion.totemguard.common.cache.CacheRepositoryImpl;
import com.deathmotion.totemguard.common.database.model.PunishmentRecord;
import com.deathmotion.totemguard.common.gui.*;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Paginated punishment history — one tile per dispatched command.
 */
public final class PlayerPunishmentsScreen extends GuiScreen {

    static final int PAGE_SIZE = 21;
    private static final Duration HISTORY_TTL = Duration.ofMinutes(2);
    private static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private final UUID targetId;
    private final String targetName;
    private final int page;

    private volatile @Nullable List<PunishmentRecord> loaded;
    private volatile int totalCount = -1;
    private volatile @Nullable String loadError;
    private volatile boolean offline;

    public PlayerPunishmentsScreen(UUID targetId, String targetName, int page) {
        this.targetId = targetId;
        this.targetName = targetName;
        this.page = Math.max(0, page);
    }

    private static ItemType iconFor(PunishmentType type) {
        return switch (type) {
            case GENERIC -> ItemTypes.PAPER;
            case KICK -> ItemTypes.GOLDEN_AXE;
            case BAN -> ItemTypes.NETHERITE_AXE;
        };
    }

    private static NamedTextColor colorFor(PunishmentType type) {
        return switch (type) {
            case GENERIC -> NamedTextColor.GRAY;
            case KICK -> NamedTextColor.GOLD;
            case BAN -> NamedTextColor.RED;
        };
    }

    @Override
    public String requiredPermission() {
        return "TotemGuardV3.Gui.History.Punishments";
    }

    @Override
    public void onOpen(GuiSession session) {
        TGPlatform platform = TGPlatform.getInstance();

        if (!platform.getDatabaseRepository().isConnected()) {
            this.loaded = List.of();
            this.totalCount = 0;
            this.offline = true;
            return;
        }

        platform.getScheduler().runAsyncTask(() -> {
            CacheRepositoryImpl cache = platform.getCacheRepository();
            String pageKey = CacheKeys.punishmentHistoryPage(targetId, page);
            String countKey = CacheKeys.punishmentHistoryCount(targetId);

            List<PunishmentRecord> cachedPage = cache.get(pageKey, CacheCodecs.PUNISHMENT_RECORDS);
            Integer cachedCount = cache.get(countKey, CacheCodecs.INT);
            if (cachedPage != null && cachedCount != null) {
                this.loaded = cachedPage;
                this.totalCount = cachedCount;
                platform.getGuiManager().refresh(session.viewerId());
                return;
            }

            try {
                List<PunishmentRecord> rows = platform.getDatabaseRepository()
                        .findPunishmentsByPlayer(targetId, PAGE_SIZE, page * PAGE_SIZE);
                int total = platform.getDatabaseRepository().countPunishmentsByPlayer(targetId);
                this.loaded = rows;
                this.totalCount = total;
                cache.put(pageKey, rows, CacheCodecs.PUNISHMENT_RECORDS, HISTORY_TTL);
                cache.put(countKey, total, CacheCodecs.INT, HISTORY_TTL);
            } catch (Exception ex) {
                if (!platform.getDatabaseRepository().isConnected()) {
                    this.loaded = List.of();
                    this.totalCount = 0;
                    this.offline = true;
                } else {
                    this.loaded = List.of();
                    this.totalCount = 0;
                    this.loadError = ex.getMessage();
                    platform.getLogger().log(Level.WARNING,
                            "Failed to load punishment history for " + targetId, ex);
                }
            }
            platform.getGuiManager().refresh(session.viewerId());
        });
    }

    @Override
    public GuiRenderResult render(GuiSession session) {
        GuiRenderResult.Builder builder = GuiRenderResult.builder(6,
                Component.text("Punishments: " + targetName, NamedTextColor.GOLD));
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
                            Component.text("Punishment history is unavailable — the database", NamedTextColor.GRAY),
                            Component.text("is disabled or currently unreachable.", NamedTextColor.GRAY)
                    )
            ));
            return builder.build();
        }

        List<PunishmentRecord> rows = this.loaded;

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
                    Component.text("Failed to load punishments", NamedTextColor.RED),
                    List.of(
                            Component.text("Check the server log for details.", NamedTextColor.GRAY),
                            Component.text(loadError, NamedTextColor.DARK_RED)
                    )
            ));
            return builder.build();
        }

        if (rows.isEmpty() && page == 0) {
            builder.set(22, GuiItems.simple(
                    ItemTypes.LIME_CONCRETE,
                    Component.text("Clean record", NamedTextColor.GREEN),
                    List.of(Component.text("No punishments have been issued to this player.", NamedTextColor.GRAY))
            ));
            return builder.build();
        }

        for (int i = 0; i < rows.size() && i < CONTENT_SLOTS.length; i++) {
            builder.set(CONTENT_SLOTS[i], buildPunishmentTile(rows.get(i)));
        }

        renderFooter(builder);
        return builder.build();
    }

    private ItemStack buildPunishmentTile(PunishmentRecord record) {
        List<Component> lore = new ArrayList<>();
        lore.add(GuiText.line("Type", record.type().name()));
        lore.add(GuiText.line("Server", record.serverName()));
        lore.add(GuiText.line("Check", record.checkName()));
        lore.add(GuiText.line("When", HistoryText.relative(record.createdAt())
                + "  (" + HistoryText.absolute(record.createdAt()) + ")"));
        lore.add(Component.empty());
        lore.add(Component.text("Command:", NamedTextColor.GRAY));
        lore.add(Component.text(record.command(), NamedTextColor.WHITE));
        if (record.debug() != null && !record.debug().isEmpty()) {
            lore.add(Component.empty());
            lore.add(Component.text("Debug:", NamedTextColor.GRAY));
            lore.add(Component.text(record.debug(), NamedTextColor.WHITE));
        }

        return GuiItems.simple(
                iconFor(record.type()),
                Component.text(record.checkName() + " · " + record.type().name(), colorFor(record.type())),
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
            ), ctx -> ctx.replace(new PlayerPunishmentsScreen(targetId, targetName, page - 1)));
        }

        builder.set(49, GuiItems.simple(
                ItemTypes.PAPER,
                Component.text("Page " + (page + 1) + " of " + pages, NamedTextColor.AQUA),
                List.of(GuiText.line("Total punishments", String.valueOf(total)))
        ));

        if (hasNext) {
            builder.set(50, GuiItems.simple(
                    ItemTypes.ARROW,
                    Component.text("Next page", NamedTextColor.GOLD),
                    List.of(Component.text("Page " + (page + 2), NamedTextColor.GRAY))
            ), ctx -> ctx.replace(new PlayerPunishmentsScreen(targetId, targetName, page + 1)));
        }
    }
}
