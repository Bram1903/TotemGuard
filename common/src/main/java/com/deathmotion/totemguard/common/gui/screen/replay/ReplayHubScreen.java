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
import com.deathmotion.totemguard.common.replay.RecordingIndex;
import com.deathmotion.totemguard.common.replay.ReplayService;
import com.deathmotion.totemguard.common.replay.ReplayText;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

public final class ReplayHubScreen extends ReplayScreen {

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
                GuiTitle.of(messages.getString(MessagesKeys.GUI_REPLAY_HUB_TITLE)));
        builder.fillEmpty(GuiItems.filler());

        ReplayService service = service();
        if (service == null) {
            closeButton(builder, 0);
            builder.set(13, GuiItems.simple(
                    ItemTypes.RED_CONCRETE,
                    messages.getComponent(MessagesKeys.GUI_REPLAY_UNAVAILABLE_TITLE),
                    List.of(messages.getComponent(MessagesKeys.GUI_REPLAY_UNAVAILABLE_LORE))
            ));
            return builder.build();
        }

        closeButton(builder, 0);

        builder.set(11, GuiItems.simple(
                ItemTypes.REDSTONE,
                messages.getComponent(MessagesKeys.GUI_REPLAY_START_TITLE),
                List.of(
                        messages.getComponent(MessagesKeys.GUI_REPLAY_START_LORE_1),
                        messages.getComponent(MessagesKeys.GUI_REPLAY_START_LORE_2),
                        Component.empty(),
                        messages.getComponent(MessagesKeys.GUI_STATUS_CLICK_TO_OPEN)
                )
        ), context -> context.open(ReplaySetupScreen.forViewer(session.viewerId())));

        builder.set(13, GuiItems.simple(
                ItemTypes.CHEST,
                messages.getComponent(MessagesKeys.GUI_REPLAY_LIBRARY_TITLE),
                libraryLore(messages, service)
        ), context -> context.open(new ReplayLabelsScreen()));

        builder.set(15, GuiItems.simple(
                ItemTypes.CLOCK,
                messages.getComponent(MessagesKeys.GUI_REPLAY_LIVE_TITLE),
                liveLore(messages, service)
        ), context -> context.open(new ReplayActiveScreen()));

        return builder.build();
    }

    private List<Component> libraryLore(MessageService messages, ReplayService service) {
        List<RecordingIndex.Entry> entries = service.index().current();
        long bytes = 0L;
        for (RecordingIndex.Entry entry : entries) bytes += entry.bytes();

        List<Component> lore = new ArrayList<>();
        lore.add(messages.getComponent(MessagesKeys.GUI_REPLAY_LIBRARY_LORE_1));
        lore.add(Component.empty());
        lore.add(GuiText.line("On disk", entries.size() + " recording(s)"));
        lore.add(GuiText.line("Total size", ReplayText.size(bytes)));
        lore.add(Component.empty());
        lore.add(messages.getComponent(MessagesKeys.GUI_STATUS_CLICK_TO_BROWSE));
        return lore;
    }

    private List<Component> liveLore(MessageService messages, ReplayService service) {
        List<Component> lore = new ArrayList<>();
        lore.add(messages.getComponent(MessagesKeys.GUI_REPLAY_LIVE_LORE_1));
        lore.add(Component.empty());
        lore.add(GuiText.line("Recording", String.valueOf(service.active().size())));
        lore.add(GuiText.line("Armed", String.valueOf(service.arms().size())));
        lore.add(Component.empty());
        lore.add(messages.getComponent(MessagesKeys.GUI_STATUS_CLICK_TO_OPEN));
        return lore;
    }
}
