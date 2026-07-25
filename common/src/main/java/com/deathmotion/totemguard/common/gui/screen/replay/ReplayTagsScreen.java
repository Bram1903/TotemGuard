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
import com.deathmotion.totemguard.common.physics.preset.PhysicsDebugContext;
import com.deathmotion.totemguard.common.replay.RecordingIndex;
import com.deathmotion.totemguard.common.replay.ReplayService;
import com.deathmotion.totemguard.common.util.Palette;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import net.kyori.adventure.text.Component;

import java.util.*;

public final class ReplayTagsScreen extends ReplayScreen {

    private static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private final ReplayDraft draft;
    private final GuiScreen parent;
    private final int page;

    public ReplayTagsScreen(ReplayDraft draft, GuiScreen parent, int page) {
        this.draft = draft;
        this.parent = parent;
        this.page = Math.max(0, page);
    }

    @Override
    public void onOpen(GuiSession session) {
        ReplayService service = service();
        if (service == null) return;
        service.index().refreshed().thenRun(() ->
                TGPlatform.getInstance().getGuiManager().refresh(session.viewerId()));
    }

    private List<String> vocabulary() {
        Set<String> names = new LinkedHashSet<>();
        for (ReplayTagCatalog.Tag tag : ReplayTagCatalog.known()) names.add(tag.name());
        names.addAll(draft.tags());
        ReplayService service = service();
        if (service != null) {
            for (RecordingIndex.Entry entry : service.index().current()) names.addAll(entry.tags());
        }
        return List.copyOf(names);
    }

    @Override
    public GuiRenderResult render(GuiSession session) {
        MessageService messages = messages();
        GuiRenderResult.Builder builder = GuiRenderResult.builder(6,
                GuiTitle.of(messages.getString(MessagesKeys.GUI_REPLAY_TAGS_SCREEN_TITLE)));
        builder.fillEmpty(GuiItems.filler());

        backButton(builder, 0, () -> parent);

        List<String> vocabulary = vocabulary();
        int pages = Math.max(1, (vocabulary.size() + CONTENT_SLOTS.length - 1) / CONTENT_SLOTS.length);
        int from = Math.min(page * CONTENT_SLOTS.length, vocabulary.size());
        int to = Math.min(from + CONTENT_SLOTS.length, vocabulary.size());

        for (int index = from; index < to; index++) {
            String tag = vocabulary.get(index);
            builder.set(CONTENT_SLOTS[index - from], tile(tag, messages), context -> {
                draft.toggleTag(tag);
                context.refresh();
                context.playSound(draft.hasTag(tag) ? GuiSounds.ENTER : GuiSounds.BACK);
            });
        }

        builder.set(45, GuiItems.simple(
                ItemTypes.NAME_TAG,
                messages.getComponent(MessagesKeys.GUI_REPLAY_TAG_CUSTOM_TITLE),
                List.of(
                        messages.getComponent(MessagesKeys.GUI_REPLAY_TAG_CUSTOM_LORE_1),
                        Component.empty(),
                        messages.getComponent(MessagesKeys.GUI_STATUS_CLICK_TO_TYPE)
                )
        ), context -> context.prompt(promptFor("a tag to add"), this, text -> {
            draft.toggleTag(text);
            return this;
        }));

        if (!draft.tags().isEmpty()) {
            builder.set(46, GuiItems.simple(
                    ItemTypes.BARRIER,
                    messages.getComponent(MessagesKeys.GUI_REPLAY_TAG_CLEAR_TITLE),
                    List.of(messages.getComponent(MessagesKeys.GUI_REPLAY_TAG_CLEAR_LORE))
            ), context -> {
                draft.clearTags();
                context.refresh();
                context.playSound(GuiSounds.BACK);
            });
        }

        if (page > 0) {
            builder.set(48, GuiItems.simple(
                    ItemTypes.ARROW,
                    messages.getComponent(MessagesKeys.GUI_BTN_PREVIOUS_PAGE_TITLE),
                    List.of(Component.text("Page " + page, Palette.CONNECTIVE))
            ), context -> context.replace(new ReplayTagsScreen(draft, parent, page - 1)));
        }

        builder.set(49, GuiItems.simple(
                ItemTypes.BOOK,
                Component.text("Page " + (page + 1) + " of " + pages, Palette.BRAND),
                List.of(
                        GuiText.line("Selected", ReplayGuiText.tags(draft.tags())),
                        GuiText.line("Traces", ReplayGuiText.traces(draft.tags()))
                )
        ));

        if (page + 1 < pages) {
            builder.set(50, GuiItems.simple(
                    ItemTypes.ARROW,
                    messages.getComponent(MessagesKeys.GUI_BTN_NEXT_PAGE_TITLE),
                    List.of(Component.text("Page " + (page + 2), Palette.CONNECTIVE))
            ), context -> context.replace(new ReplayTagsScreen(draft, parent, page + 1)));
        }

        builder.set(53, GuiItems.simple(
                ItemTypes.LIME_DYE,
                messages.getComponent(MessagesKeys.GUI_REPLAY_TAG_DONE_TITLE),
                List.of(messages.getComponent(MessagesKeys.GUI_REPLAY_TAG_DONE_LORE))
        ), context -> leave(context, parent));

        return builder.build();
    }

    private ItemStack tile(String tag, MessageService messages) {
        ReplayTagCatalog.Tag known = ReplayTagCatalog.describe(tag);
        boolean enabled = draft.hasTag(tag);
        PhysicsDebugContext context = PhysicsDebugContext.parseOne(tag);

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(known.description(), Palette.CONNECTIVE));
        lore.add(Component.empty());
        lore.add(context == null
                ? messages.getComponent(MessagesKeys.GUI_REPLAY_TAG_NO_TRACE)
                : messages.getComponent(MessagesKeys.GUI_REPLAY_TAG_TRACE,
                Map.of("tg_trace", context.name())));
        lore.add(Component.empty());
        lore.add(messages.getComponent(enabled
                ? MessagesKeys.GUI_REPLAY_TAG_ON
                : MessagesKeys.GUI_REPLAY_TAG_OFF));

        return GuiItems.simple(
                enabled ? known.item() : ItemTypes.LIGHT_GRAY_DYE,
                Component.text(tag, enabled ? Palette.SUCCESS : Palette.CAPTION),
                lore);
    }
}
