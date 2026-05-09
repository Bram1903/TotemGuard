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

package com.deathmotion.totemguard.common.gui.screen.top;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.config.key.MessagesKeys;
import com.deathmotion.totemguard.common.features.session.SessionSnapshot;
import com.deathmotion.totemguard.common.features.session.SessionViolationStore;
import com.deathmotion.totemguard.common.gui.*;
import com.deathmotion.totemguard.common.message.MessageService;
import com.deathmotion.totemguard.common.util.Palette;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public final class TopCheckPickerScreen extends GuiScreen {

    private static final int PAGE_SIZE = 28;
    private static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private final int page;
    private volatile @Nullable List<CheckSummary> summaries;
    private volatile boolean localOnly;
    private volatile boolean loaded;

    public TopCheckPickerScreen(int page) {
        this.page = Math.max(0, page);
    }

    private static List<CheckSummary> aggregate(List<SessionSnapshot> snapshots) {
        Map<String, int[]> agg = new HashMap<>();
        for (SessionSnapshot snap : snapshots) {
            for (Map.Entry<String, Integer> entry : snap.violations().entrySet()) {
                int count = entry.getValue();
                if (count <= 0) continue;
                int[] cell = agg.computeIfAbsent(entry.getKey(), k -> new int[2]);
                cell[0] += count;
                cell[1] += 1;
            }
        }
        List<CheckSummary> out = new ArrayList<>(agg.size());
        for (Map.Entry<String, int[]> entry : agg.entrySet()) {
            out.add(new CheckSummary(entry.getKey(), entry.getValue()[0], entry.getValue()[1]));
        }
        out.sort((a, b) -> {
            int byTotal = Integer.compare(b.totalViolations, a.totalViolations);
            if (byTotal != 0) return byTotal;
            return a.checkName.compareToIgnoreCase(b.checkName);
        });
        return out;
    }

    @Override
    public String requiredPermission() {
        return TopViolatorsScreen.PERMISSION;
    }

    @Override
    public void onOpen(GuiSession session) {
        TGPlatform platform = TGPlatform.getInstance();
        SessionViolationStore store = platform.getSessionViolationStore();
        boolean redisAvailable = store != null && platform.getRedisRepository().isConnected();
        this.localOnly = !redisAvailable;

        platform.getScheduler().runAsyncTask(() -> {
            try {
                List<SessionSnapshot> data = redisAvailable
                        ? store.topN(TopViolatorsScreen.LIMIT)
                        : TopViolatorsScreen.collectLocalSnapshots(platform, TopViolatorsScreen.LIMIT);
                this.summaries = aggregate(data);
            } catch (Exception ex) {
                platform.getLogger().log(Level.WARNING,
                        "Failed to load check summaries: " + ex.getMessage());
                this.summaries = List.of();
            } finally {
                this.loaded = true;
                platform.getGuiManager().refresh(session.viewerId());
            }
        });
    }

    @Override
    public GuiRenderResult render(GuiSession session) {
        MessageService messages = TGPlatform.getInstance().getMessageService();
        GuiRenderResult.Builder builder = GuiRenderResult.builder(6,
                GuiTitle.of(messages.getString(MessagesKeys.GUI_TOP_CHECKS_TITLE)));
        builder.fillEmpty(GuiItems.filler());

        builder.set(0, GuiItems.simple(
                ItemTypes.ARROW,
                messages.getComponent(MessagesKeys.GUI_BTN_BACK_TITLE),
                List.of(messages.getComponent(MessagesKeys.GUI_BTN_BACK_LORE))
        ), ctx -> ctx.back());

        if (localOnly) {
            builder.set(4, GuiItems.simple(
                    ItemTypes.AMETHYST_SHARD,
                    messages.getComponent(MessagesKeys.GUI_TOP_LOCAL_ONLY_TITLE),
                    List.of(messages.getComponent(MessagesKeys.GUI_TOP_CHECKS_LOCAL_ONLY_LORE))
            ));
        }

        List<CheckSummary> all = this.summaries;
        if (!loaded || all == null) {
            builder.set(22, GuiItems.simple(
                    ItemTypes.CLOCK,
                    messages.getComponent(MessagesKeys.GUI_LOADING_GENERIC),
                    List.of(messages.getComponent(MessagesKeys.GUI_TOP_CHECKS_LOADING_LORE))
            ));
            return builder.build();
        }

        if (all.isEmpty()) {
            builder.set(22, GuiItems.simple(
                    ItemTypes.LIME_CONCRETE,
                    messages.getComponent(MessagesKeys.GUI_TOP_CHECKS_EMPTY_TITLE),
                    List.of(messages.getComponent(MessagesKeys.GUI_TOP_CHECKS_EMPTY_LORE))
            ));
            return builder.build();
        }

        int totalPages = Math.max(1, (all.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int safePage = Math.min(page, totalPages - 1);
        int from = safePage * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, all.size());

        for (int i = from; i < to; i++) {
            CheckSummary summary = all.get(i);
            builder.set(CONTENT_SLOTS[i - from], GuiItems.simple(
                    ItemTypes.PAPER,
                    Component.text(summary.checkName, Palette.BRAND),
                    List.of(
                            GuiText.line(messages.getString(MessagesKeys.GUI_TOP_CHECKS_ENTRY_TOTAL_VL_LABEL),
                                    String.valueOf(summary.totalViolations)),
                            GuiText.line(messages.getString(MessagesKeys.GUI_TOP_CHECKS_ENTRY_VIOLATORS_LABEL),
                                    String.valueOf(summary.violatorCount)),
                            Component.empty(),
                            messages.getComponent(MessagesKeys.GUI_TOP_CHECKS_ENTRY_ACTION,
                                    Map.of("tg_check", summary.checkName))
                    )
            ), ctx -> ctx.replace(new TopViolatorsScreen(0, summary.checkName)));
        }

        renderFooter(builder, safePage, totalPages, all.size(), messages);
        return builder.build();
    }

    private void renderFooter(GuiRenderResult.Builder builder, int currentPage, int totalPages, int total, MessageService messages) {
        if (currentPage > 0) {
            builder.set(48, GuiItems.simple(
                    ItemTypes.ARROW,
                    messages.getComponent(MessagesKeys.GUI_BTN_PREVIOUS_PAGE_TITLE),
                    List.of(messages.getComponent(MessagesKeys.GUI_TOP_PAGE_NUMBER,
                            Map.of("tg_page", currentPage)))
            ), ctx -> ctx.replace(new TopCheckPickerScreen(currentPage - 1)));
        }

        builder.set(49, GuiItems.simple(
                ItemTypes.PAPER,
                messages.getComponent(MessagesKeys.GUI_TOP_PAGE_SUMMARY, Map.of(
                        "tg_current", currentPage + 1,
                        "tg_total", totalPages)),
                List.of(GuiText.line(messages.getString(MessagesKeys.GUI_TOP_CHECKS_FOOTER_DISTINCT_LABEL),
                        String.valueOf(total)))
        ));

        if (currentPage < totalPages - 1) {
            builder.set(50, GuiItems.simple(
                    ItemTypes.ARROW,
                    messages.getComponent(MessagesKeys.GUI_BTN_NEXT_PAGE_TITLE),
                    List.of(messages.getComponent(MessagesKeys.GUI_TOP_PAGE_NUMBER,
                            Map.of("tg_page", currentPage + 2)))
            ), ctx -> ctx.replace(new TopCheckPickerScreen(currentPage + 1)));
        }
    }

    private record CheckSummary(String checkName, int totalViolations, int violatorCount) {
    }
}
