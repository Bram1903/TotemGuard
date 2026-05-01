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

import com.deathmotion.totemguard.api.result.ResultError;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.config.key.MessagesKeys;
import com.deathmotion.totemguard.common.database.model.AlertCheckSummary;
import com.deathmotion.totemguard.common.gui.*;
import com.deathmotion.totemguard.common.message.MessageService;
import com.deathmotion.totemguard.common.util.Palette;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
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
        return PlayerAlertsScreen.PERMISSION;
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
        MessageService messages = TGPlatform.getInstance().getMessageService();
        GuiRenderResult.Builder builder = GuiRenderResult.builder(6,
                GuiTitle.of(messages.getString(MessagesKeys.GUI_ALERT_CHECKS_TITLE, Map.of("tg_player", targetName))));
        builder.fillEmpty(GuiItems.filler());

        builder.set(0, GuiItems.simple(
                ItemTypes.ARROW,
                messages.getComponent(MessagesKeys.GUI_BTN_BACK_TITLE),
                List.of(messages.getComponent(MessagesKeys.GUI_BTN_BACK_TO_ALERTS_LORE))
        ), ctx -> ctx.back());

        if (offline) {
            builder.set(22, GuiItems.simple(
                    ItemTypes.RED_CONCRETE,
                    messages.getComponent(MessagesKeys.GUI_ERR_DATABASE_OFFLINE),
                    List.of(messages.getComponent(MessagesKeys.GUI_ALERT_CHECKS_FILTER_UNAVAILABLE))
            ));
            return builder.build();
        }

        List<AlertCheckSummary> all = this.loaded;

        if (all == null) {
            builder.set(22, GuiItems.simple(
                    ItemTypes.CLOCK,
                    messages.getComponent(MessagesKeys.GUI_LOADING_GENERIC),
                    List.of(messages.getComponent(MessagesKeys.GUI_LOADING_QUERYING_DATABASE))
            ));
            return builder.build();
        }

        if (loadError != null) {
            builder.set(22, GuiItems.simple(
                    ItemTypes.RED_CONCRETE,
                    messages.getComponent(MessagesKeys.GUI_ERR_FAILED_LOAD_CHECKS),
                    List.of(
                            messages.getComponent(MessagesKeys.GUI_ERR_CHECK_SERVER_LOG),
                            Component.text(loadError, Palette.VALUE_ON_DANGER)
                    )
            ));
            return builder.build();
        }

        if (all.isEmpty()) {
            builder.set(22, GuiItems.simple(
                    ItemTypes.LIME_CONCRETE,
                    messages.getComponent(MessagesKeys.GUI_ALERT_CHECKS_EMPTY_TITLE),
                    List.of(messages.getComponent(MessagesKeys.GUI_ALERT_CHECKS_EMPTY_LORE))
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
                    Component.text(summary.checkName(), Palette.BRAND),
                    List.of(
                            GuiText.line("Alerts", String.valueOf(summary.alertCount())),
                            Component.empty(),
                            messages.getComponent(MessagesKeys.GUI_ALERT_CHECKS_VIEW_FILTER_HINT)
                    )
            ), ctx -> ctx.replace(new PlayerAlertsScreen(
                    targetId, targetName, 0, summary.checkName())));
        }

        renderFooter(builder, all.size(), messages);
        return builder.build();
    }

    private void renderFooter(GuiRenderResult.Builder builder, int total, MessageService messages) {
        int pages = Math.max(1, (int) Math.ceil(total / (double) PAGE_SIZE));
        boolean hasPrev = page > 0;
        boolean hasNext = (page + 1) < pages;

        if (hasPrev) {
            builder.set(48, GuiItems.simple(
                    ItemTypes.ARROW,
                    messages.getComponent(MessagesKeys.GUI_BTN_PREVIOUS_PAGE_TITLE),
                    List.of(Component.text("Page " + page, Palette.CONNECTIVE))
            ), ctx -> ctx.replace(new PlayerAlertChecksScreen(targetId, targetName, page - 1)));
        }

        builder.set(49, GuiItems.simple(
                ItemTypes.PAPER,
                Component.text("Page " + (page + 1) + " of " + pages, Palette.BRAND),
                List.of(GuiText.line("Distinct checks", String.valueOf(total)))
        ));

        if (hasNext) {
            builder.set(50, GuiItems.simple(
                    ItemTypes.ARROW,
                    messages.getComponent(MessagesKeys.GUI_BTN_NEXT_PAGE_TITLE),
                    List.of(Component.text("Page " + (page + 2), Palette.CONNECTIVE))
            ), ctx -> ctx.replace(new PlayerAlertChecksScreen(targetId, targetName, page + 1)));
        }
    }
}
