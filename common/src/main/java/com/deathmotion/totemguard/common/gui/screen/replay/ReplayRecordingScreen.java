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
import com.deathmotion.totemguard.common.platform.player.PlatformPlayer;
import com.deathmotion.totemguard.common.replay.RecordingLibrary;
import com.deathmotion.totemguard.common.replay.RecordingSummary;
import com.deathmotion.totemguard.common.replay.RecordingSummaryCache;
import com.deathmotion.totemguard.common.replay.ReplayService;
import com.deathmotion.totemguard.common.util.Palette;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ReplayRecordingScreen extends ReplayScreen {

    private final String path;
    private volatile @Nullable RecordingSummaryCache.Entry cached;

    public ReplayRecordingScreen(String path) {
        this.path = path;
    }

    @Override
    public void onOpen(GuiSession session) {
        ReplayService service = service();
        if (service == null) return;
        TGPlatform platform = TGPlatform.getInstance();
        platform.getScheduler().runAsyncTask(() -> {
            this.cached = service.summaries().require(path);
            platform.getGuiManager().refresh(session.viewerId());
        });
    }

    @Override
    public GuiRenderResult render(GuiSession session) {
        MessageService messages = messages();
        String title = messages.getString(MessagesKeys.GUI_REPLAY_RECORDING_TITLE,
                Map.of("tg_file", RecordingLibrary.display(path)));
        GuiRenderResult.Builder builder = GuiRenderResult.builder(3, GuiTitle.of(title));
        builder.fillEmpty(GuiItems.filler());

        backButton(builder, 0, ReplayLabelsScreen::new);

        RecordingSummaryCache.Entry entry = this.cached;
        if (entry == null) {
            builder.set(13, GuiItems.simple(
                    ItemTypes.CLOCK,
                    messages.getComponent(MessagesKeys.GUI_LOADING_GENERIC),
                    List.of(messages.getComponent(MessagesKeys.GUI_REPLAY_READING_DETAILS))
            ));
            return builder.build();
        }

        RecordingSummary summary = entry.summary();
        if (summary == null) {
            builder.set(13, GuiItems.simple(
                    ItemTypes.RED_CONCRETE,
                    messages.getComponent(MessagesKeys.GUI_REPLAY_UNREADABLE_TITLE),
                    List.of(
                            messages.getComponent(MessagesKeys.GUI_REPLAY_UNREADABLE_LORE),
                            Component.text(String.valueOf(entry.error()), Palette.VALUE_ON_DANGER))
            ));
            return builder.build();
        }

        builder.set(4, GuiItems.simple(
                ReplayGuiText.labelItem(summary.label()),
                Component.text(summary.display(), ReplayGuiText.labelColor(summary.label())),
                ReplayGuiText.details(summary)
        ));

        ReplayService service = service();
        boolean busy = service != null && service.playing();
        builder.set(11, GuiItems.simple(
                busy ? ItemTypes.LIGHT_GRAY_DYE : ItemTypes.SPYGLASS,
                messages.getComponent(MessagesKeys.GUI_REPLAY_RUN_TITLE),
                List.of(
                        messages.getComponent(MessagesKeys.GUI_REPLAY_RUN_LORE_1),
                        messages.getComponent(MessagesKeys.GUI_REPLAY_RUN_LORE_2),
                        Component.empty(),
                        messages.getComponent(busy
                                ? MessagesKeys.GUI_REPLAY_RUN_BUSY_LORE
                                : MessagesKeys.GUI_STATUS_CLICK_TO_RUN)
                )
        ), context -> start(context, session.viewerId()));

        builder.set(15, GuiItems.simple(
                ItemTypes.NAME_TAG,
                messages.getComponent(MessagesKeys.GUI_REPLAY_EDIT_TITLE),
                List.of(
                        messages.getComponent(MessagesKeys.GUI_REPLAY_EDIT_LORE_1),
                        Component.empty(),
                        messages.getComponent(MessagesKeys.GUI_STATUS_CLICK_TO_OPEN)
                )
        ), context -> context.open(new ReplayEditScreen(path)));

        return builder.build();
    }

    private void start(GuiClickContext context, UUID viewerId) {
        ReplayService service = service();
        if (service == null) return;

        PlatformPlayer viewer = TGPlatform.getInstance().getPlatformPlayerFactory().create(viewerId);
        if (viewer == null) return;

        context.close();
        service.run(path, viewer::sendMessage);
    }
}
