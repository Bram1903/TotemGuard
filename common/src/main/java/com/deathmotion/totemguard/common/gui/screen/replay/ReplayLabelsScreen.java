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

import com.deathmotion.totemguard.api.config.key.ConfigKey;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.config.key.MessagesKeys;
import com.deathmotion.totemguard.common.gui.*;
import com.deathmotion.totemguard.common.message.MessageService;
import com.deathmotion.totemguard.common.replay.RecordingIndex;
import com.deathmotion.totemguard.common.replay.ReplayService;
import com.deathmotion.totemguard.common.replay.ReplayText;
import com.deathmotion.totemguard.common.replay.format.RecordingLabel;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

public final class ReplayLabelsScreen extends ReplayScreen {

    private static final int[] LABEL_SLOTS = {10, 12, 14};

    @Override
    public void onOpen(GuiSession session) {
        ReplayService service = service();
        if (service == null) return;
        service.index().refreshed().thenRun(() ->
                TGPlatform.getInstance().getGuiManager().refresh(session.viewerId()));
    }

    @Override
    public GuiRenderResult render(GuiSession session) {
        MessageService messages = messages();
        GuiRenderResult.Builder builder = GuiRenderResult.builder(3,
                GuiTitle.of(messages.getString(MessagesKeys.GUI_REPLAY_LABELS_TITLE)));
        builder.fillEmpty(GuiItems.filler());

        backButton(builder, 0, ReplayHubScreen::new);

        ReplayService service = service();
        List<RecordingIndex.Entry> all = service == null ? List.of() : service.index().current();

        RecordingLabel[] labels = RecordingLabel.values();
        for (int index = 0; index < labels.length && index < LABEL_SLOTS.length; index++) {
            RecordingLabel label = labels[index];
            builder.set(LABEL_SLOTS[index], tile(messages, all, label),
                    context -> context.open(new ReplayLibraryScreen(label, 0, "")));
        }

        builder.set(16, GuiItems.simple(
                ItemTypes.CHEST,
                messages.getComponent(MessagesKeys.GUI_REPLAY_LABEL_ALL_TITLE),
                counted(messages, MessagesKeys.GUI_REPLAY_LABEL_ALL_LORE, all)
        ), context -> context.open(new ReplayLibraryScreen(null, 0, "")));

        return builder.build();
    }

    private ItemStack tile(MessageService messages, List<RecordingIndex.Entry> all,
                           RecordingLabel label) {
        List<RecordingIndex.Entry> matched = new ArrayList<>();
        for (RecordingIndex.Entry entry : all) {
            if (entry.label() == label) matched.add(entry);
        }

        return GuiItems.simple(
                ReplayGuiText.labelItem(label),
                Component.text(label.id(), ReplayGuiText.labelColor(label)),
                counted(messages, loreKey(label), matched));
    }

    private List<Component> counted(MessageService messages, ConfigKey<String> loreKey,
                                    List<RecordingIndex.Entry> matched) {
        long bytes = 0L;
        long newest = 0L;
        for (RecordingIndex.Entry entry : matched) {
            bytes += entry.bytes();
            newest = Math.max(newest, entry.modifiedMillis());
        }

        List<Component> lore = new ArrayList<>();
        lore.add(messages.getComponent(loreKey));
        lore.add(Component.empty());
        lore.add(GuiText.line("Recordings", String.valueOf(matched.size())));
        lore.add(GuiText.line("Total size", ReplayText.size(bytes)));
        if (newest > 0L) lore.add(GuiText.line("Newest", ReplayText.age(newest) + " old"));
        lore.add(Component.empty());
        lore.add(messages.getComponent(MessagesKeys.GUI_STATUS_CLICK_TO_BROWSE));
        return lore;
    }

    private ConfigKey<String> loreKey(RecordingLabel label) {
        return switch (label) {
            case LEGIT -> MessagesKeys.GUI_REPLAY_LABEL_LEGIT_LORE;
            case CHEAT -> MessagesKeys.GUI_REPLAY_LABEL_CHEAT_LORE;
            case SCRATCH -> MessagesKeys.GUI_REPLAY_LABEL_SCRATCH_LORE;
        };
    }
}
