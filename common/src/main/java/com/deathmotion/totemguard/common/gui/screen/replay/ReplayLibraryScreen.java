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

package com.deathmotion.totemguard.common.gui.screen.replay;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.config.key.MessagesKeys;
import com.deathmotion.totemguard.common.gui.*;
import com.deathmotion.totemguard.common.message.MessageService;
import com.deathmotion.totemguard.common.replay.*;
import com.deathmotion.totemguard.common.replay.format.RecordingLabel;
import com.deathmotion.totemguard.common.util.Palette;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ReplayLibraryScreen extends ReplayScreen {

    private static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private final @Nullable RecordingLabel label;
    private final int page;
    private final String filter;
    private volatile @Nullable List<RecordingIndex.Entry> matched;

    public ReplayLibraryScreen(@Nullable RecordingLabel label, int page, String filter) {
        this.label = label;
        this.page = Math.max(0, page);
        this.filter = filter == null ? "" : filter;
    }

    @Override
    public void onOpen(GuiSession session) {
        ReplayService service = service();
        if (service == null) {
            this.matched = List.of();
            return;
        }
        service.index().refreshed().thenAccept(all -> {
            this.matched = withLabel(RecordingIndex.matching(all, filter));
            warm(session, service);
            TGPlatform.getInstance().getGuiManager().refresh(session.viewerId());
        });
    }

    private void warm(GuiSession session, ReplayService service) {
        List<RecordingIndex.Entry> rows = rows();
        if (rows.isEmpty()) return;
        service.summaries().warm(rows, () ->
                TGPlatform.getInstance().getGuiManager().refresh(session.viewerId()));
    }

    private List<RecordingIndex.Entry> withLabel(List<RecordingIndex.Entry> entries) {
        if (label == null) return entries;
        List<RecordingIndex.Entry> kept = new ArrayList<>();
        for (RecordingIndex.Entry entry : entries) {
            if (entry.label() == label) kept.add(entry);
        }
        return kept;
    }

    private List<RecordingIndex.Entry> rows() {
        List<RecordingIndex.Entry> all = this.matched;
        if (all == null || all.isEmpty()) return List.of();
        int from = Math.min(page * CONTENT_SLOTS.length, all.size());
        int to = Math.min(from + CONTENT_SLOTS.length, all.size());
        return all.subList(from, to);
    }

    @Override
    public GuiRenderResult render(GuiSession session) {
        MessageService messages = messages();
        String title = messages.getString(MessagesKeys.GUI_REPLAY_LIBRARY_SCREEN_TITLE, Map.of(
                "tg_label", label == null ? "all" : label.id(),
                "tg_filter", filter.isBlank() ? "" : " " + filter));
        GuiRenderResult.Builder builder = GuiRenderResult.builder(6, GuiTitle.of(title));
        builder.fillEmpty(GuiItems.filler());

        backButton(builder, 0, ReplayLabelsScreen::new);
        renderFilterButtons(builder, messages);

        ReplayService service = service();
        List<RecordingIndex.Entry> all = this.matched;
        if (service == null || all == null) {
            builder.set(22, GuiItems.simple(
                    ItemTypes.CLOCK,
                    messages.getComponent(MessagesKeys.GUI_LOADING_GENERIC),
                    List.of(messages.getComponent(MessagesKeys.GUI_REPLAY_LOADING_LIBRARY))
            ));
            return builder.build();
        }

        if (all.isEmpty()) {
            builder.set(22, GuiItems.simple(
                    ItemTypes.LIGHT_GRAY_DYE,
                    messages.getComponent(filter.isBlank()
                            ? MessagesKeys.GUI_REPLAY_LIBRARY_EMPTY_TITLE
                            : MessagesKeys.GUI_REPLAY_LIBRARY_NO_MATCH_TITLE),
                    List.of(messages.getComponent(filter.isBlank()
                            ? MessagesKeys.GUI_REPLAY_LIBRARY_EMPTY_LORE
                            : MessagesKeys.GUI_REPLAY_LIBRARY_NO_MATCH_LORE))
            ));
            return builder.build();
        }

        List<RecordingIndex.Entry> rows = rows();
        for (int index = 0; index < rows.size(); index++) {
            RecordingIndex.Entry entry = rows.get(index);
            builder.set(CONTENT_SLOTS[index], tile(service, entry, messages), context -> {
                if (context.rightClick()) context.open(new ReplayEditScreen(entry.path()));
                else context.open(new ReplayRecordingScreen(entry.path()));
            });
        }

        renderFooter(builder, messages, all.size());
        return builder.build();
    }

    private void renderFilterButtons(GuiRenderResult.Builder builder, MessageService messages) {
        builder.set(45, GuiItems.simple(
                ItemTypes.HOPPER,
                messages.getComponent(MessagesKeys.GUI_REPLAY_FILTER_TITLE),
                List.of(
                        messages.getComponent(MessagesKeys.GUI_REPLAY_FILTER_LORE_1),
                        Component.empty(),
                        GuiText.line("Current", filter.isBlank() ? "none" : filter)
                )
        ), context -> context.prompt(promptFor("a name or tag to filter by"), this,
                text -> new ReplayLibraryScreen(label, 0, text)));

        if (filter.isBlank()) return;
        builder.set(46, GuiItems.simple(
                ItemTypes.BARRIER,
                messages.getComponent(MessagesKeys.GUI_REPLAY_FILTER_CLEAR_TITLE),
                List.of(messages.getComponent(MessagesKeys.GUI_REPLAY_FILTER_CLEAR_LORE))
        ), context -> context.replace(new ReplayLibraryScreen(label, 0, "")));
    }

    private void renderFooter(GuiRenderResult.Builder builder, MessageService messages, int total) {
        int pages = Math.max(1, (total + CONTENT_SLOTS.length - 1) / CONTENT_SLOTS.length);

        if (page > 0) {
            builder.set(48, GuiItems.simple(
                    ItemTypes.ARROW,
                    messages.getComponent(MessagesKeys.GUI_BTN_PREVIOUS_PAGE_TITLE),
                    List.of(Component.text("Page " + page, Palette.CONNECTIVE))
            ), context -> context.replace(new ReplayLibraryScreen(label, page - 1, filter)));
        }

        builder.set(49, GuiItems.simple(
                ItemTypes.PAPER,
                Component.text("Page " + (page + 1) + " of " + pages, Palette.BRAND),
                List.of(
                        GuiText.line("Recordings", String.valueOf(total)),
                        Component.empty(),
                        messages.getComponent(MessagesKeys.GUI_REPLAY_ROW_HINT_OPEN),
                        messages.getComponent(MessagesKeys.GUI_REPLAY_ROW_HINT_EDIT)
                )
        ));

        if (page + 1 < pages) {
            builder.set(50, GuiItems.simple(
                    ItemTypes.ARROW,
                    messages.getComponent(MessagesKeys.GUI_BTN_NEXT_PAGE_TITLE),
                    List.of(Component.text("Page " + (page + 2), Palette.CONNECTIVE))
            ), context -> context.replace(new ReplayLibraryScreen(label, page + 1, filter)));
        }
    }

    private ItemStack tile(ReplayService service, RecordingIndex.Entry entry, MessageService messages) {
        RecordingSummaryCache.Entry cached = service.summaries().peek(entry);
        Component name = Component.text(RecordingLibrary.display(entry.path()),
                ReplayGuiText.labelColor(entry.label()));

        if (cached == null) {
            return GuiItems.simple(ReplayGuiText.labelItem(entry.label()), name, List.of(
                    GuiText.line("Size", ReplayText.size(entry.bytes())),
                    GuiText.line("Age", ReplayText.age(entry.modifiedMillis())),
                    Component.empty(),
                    messages.getComponent(MessagesKeys.GUI_REPLAY_READING_DETAILS)));
        }

        RecordingSummary summary = cached.summary();
        if (summary == null) {
            return GuiItems.simple(ItemTypes.RED_CONCRETE, name, List.of(
                    messages.getComponent(MessagesKeys.GUI_REPLAY_UNREADABLE_LORE),
                    Component.text(String.valueOf(cached.error()), Palette.VALUE_ON_DANGER)));
        }

        List<Component> lore = new ArrayList<>(ReplayGuiText.details(summary));
        lore.add(Component.empty());
        lore.add(messages.getComponent(MessagesKeys.GUI_REPLAY_ROW_HINT_OPEN));
        lore.add(messages.getComponent(MessagesKeys.GUI_REPLAY_ROW_HINT_EDIT));
        return GuiItems.simple(ReplayGuiText.labelItem(summary.label()), name, lore);
    }
}
