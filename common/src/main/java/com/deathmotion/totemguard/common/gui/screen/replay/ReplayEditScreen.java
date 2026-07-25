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
import com.deathmotion.totemguard.common.platform.player.PlatformPlayer;
import com.deathmotion.totemguard.common.replay.*;
import com.deathmotion.totemguard.common.replay.capture.RecordingSession;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ReplayEditScreen extends ReplayScreen {

    private final String path;
    private volatile @Nullable RecordingSummaryCache.Entry cached;
    private volatile @Nullable ReplayDraft draft;

    public ReplayEditScreen(String path) {
        this.path = path;
    }

    @Override
    public void onOpen(GuiSession session) {
        ReplayService service = service();
        if (service == null) return;
        TGPlatform platform = TGPlatform.getInstance();
        platform.getScheduler().runAsyncTask(() -> {
            RecordingSummaryCache.Entry entry = service.summaries().require(path);
            RecordingSummary summary = entry.summary();
            if (summary != null && this.draft == null) {
                this.draft = new ReplayDraft(summary.label(), summary.scenario(),
                        summary.tags(), summary.note());
            }
            this.cached = entry;
            platform.getGuiManager().refresh(session.viewerId());
        });
    }

    @Override
    public GuiRenderResult render(GuiSession session) {
        MessageService messages = messages();
        String title = messages.getString(MessagesKeys.GUI_REPLAY_EDIT_SCREEN_TITLE,
                Map.of("tg_file", RecordingLibrary.display(path)));
        GuiRenderResult.Builder builder = GuiRenderResult.builder(3, GuiTitle.of(title));
        builder.fillEmpty(GuiItems.filler());

        backButton(builder, 0, () -> new ReplayRecordingScreen(path));

        RecordingSummaryCache.Entry entry = this.cached;
        ReplayDraft current = this.draft;
        if (entry == null || current == null) {
            builder.set(13, GuiItems.simple(
                    entry == null ? ItemTypes.CLOCK : ItemTypes.RED_CONCRETE,
                    messages.getComponent(entry == null
                            ? MessagesKeys.GUI_LOADING_GENERIC
                            : MessagesKeys.GUI_REPLAY_UNREADABLE_TITLE),
                    List.of(messages.getComponent(entry == null
                            ? MessagesKeys.GUI_REPLAY_READING_DETAILS
                            : MessagesKeys.GUI_REPLAY_UNREADABLE_LORE))
            ));
            return builder.build();
        }

        RecordingSummary summary = entry.summary();
        builder.set(4, GuiItems.simple(
                ReplayGuiText.labelItem(current.label()),
                Component.text(current.label().id() + "/" + current.scenario(),
                        ReplayGuiText.labelColor(current.label())),
                previewLore(messages, summary, current)
        ));

        builder.set(10, GuiItems.simple(
                ItemTypes.NAME_TAG,
                messages.getComponent(MessagesKeys.GUI_REPLAY_RENAME_TITLE),
                List.of(
                        messages.getComponent(MessagesKeys.GUI_REPLAY_RENAME_LORE_1),
                        Component.empty(),
                        GuiText.line("Name", current.scenario())
                )
        ), context -> context.prompt(promptFor("the new name"), this, text -> {
            current.scenario(text);
            return this;
        }));

        builder.set(12, GuiItems.simple(
                ReplayGuiText.labelItem(current.label()),
                messages.getComponent(MessagesKeys.GUI_REPLAY_LABEL_TITLE),
                List.of(
                        messages.getComponent(MessagesKeys.GUI_REPLAY_LABEL_LORE_1),
                        Component.empty(),
                        GuiText.line("Label", current.label().id()),
                        messages.getComponent(MessagesKeys.GUI_STATUS_CLICK_TO_SWITCH)
                )
        ), context -> {
            current.cycleLabel();
            context.refresh();
            context.playSound(GuiSounds.FILTER);
        });

        builder.set(14, GuiItems.simple(
                ItemTypes.BOOK,
                messages.getComponent(MessagesKeys.GUI_REPLAY_TAGS_TITLE),
                List.of(
                        messages.getComponent(MessagesKeys.GUI_REPLAY_TAGS_LORE_1),
                        Component.empty(),
                        GuiText.line("Tags", ReplayGuiText.tags(current.tags())),
                        GuiText.line("Traces", ReplayGuiText.traces(current.tags())),
                        Component.empty(),
                        messages.getComponent(MessagesKeys.GUI_STATUS_CLICK_TO_OPEN)
                )
        ), context -> context.open(new ReplayTagsScreen(current, this, 0)));

        builder.set(16, GuiItems.simple(
                ItemTypes.WRITABLE_BOOK,
                messages.getComponent(MessagesKeys.GUI_REPLAY_NOTE_TITLE),
                List.of(
                        messages.getComponent(MessagesKeys.GUI_REPLAY_NOTE_LORE_1),
                        messages.getComponent(MessagesKeys.GUI_REPLAY_NOTE_LORE_2),
                        Component.empty(),
                        GuiText.line("Note", current.note().isBlank() ? "none" : current.note())
                )
        ), context -> {
            if (context.rightClick()) {
                current.note("");
                context.refresh();
                context.playSound(GuiSounds.FILTER);
                return;
            }
            context.prompt(promptFor("the new note"), this, text -> {
                current.note(text);
                return this;
            });
        });

        boolean changed = summary != null && changed(summary, current);
        builder.set(22, GuiItems.simple(
                changed ? ItemTypes.LIME_DYE : ItemTypes.LIGHT_GRAY_DYE,
                messages.getComponent(MessagesKeys.GUI_REPLAY_SAVE_TITLE),
                List.of(messages.getComponent(changed
                        ? MessagesKeys.GUI_REPLAY_SAVE_LORE
                        : MessagesKeys.GUI_REPLAY_SAVE_UNCHANGED_LORE))
        ), context -> {
            if (!changed) {
                context.playSound(GuiSounds.DENIED);
                return;
            }
            save(context, session.viewerId(), current);
        });

        return builder.build();
    }

    private List<Component> previewLore(MessageService messages, @Nullable RecordingSummary summary,
                                        ReplayDraft current) {
        List<Component> lore = new ArrayList<>();
        lore.add(messages.getComponent(MessagesKeys.GUI_REPLAY_EDIT_PREVIEW_LORE));
        lore.add(Component.empty());
        lore.add(GuiText.line("Name", current.scenario()));
        lore.add(GuiText.line("Label", current.label().id()));
        lore.add(GuiText.line("Tags", ReplayGuiText.tags(current.tags())));
        lore.add(GuiText.line("Note", current.note().isBlank() ? "none" : current.note()));
        if (summary == null) return lore;

        lore.add(Component.empty());
        lore.add(GuiText.line("Currently", summary.display()));
        return lore;
    }

    private boolean changed(RecordingSummary summary, ReplayDraft current) {
        return summary.label() != current.label()
                || !summary.scenario().equals(RecordingLibrary.scenarioFrom(current.scenario()))
                || !summary.tags().equals(RecordingLibrary.normalizeTags(current.tags()))
                || !summary.note().equals(current.note());
    }

    private void save(GuiClickContext context, UUID viewerId, ReplayDraft current) {
        ReplayService service = service();
        if (service == null) return;

        TGPlatform platform = TGPlatform.getInstance();
        PlatformPlayer viewer = platform.getPlatformPlayerFactory().create(viewerId);
        context.close();

        platform.getScheduler().runAsyncTask(() -> {
            Path file = service.library().resolve(path);
            if (file == null) {
                tell(viewer, MessagesKeys.GUI_REPLAY_SAVE_MISSING, Map.of("tg_file", path));
                return;
            }
            for (RecordingSession running : service.active()) {
                if (!running.getFile().equals(file)) continue;
                tell(viewer, MessagesKeys.GUI_REPLAY_SAVE_RUNNING, Map.of("tg_file", path));
                return;
            }

            try {
                Path moved = RecordingEditor.apply(service.library(), file, current.label(),
                        current.scenario(), current.tags(), current.note());
                service.index().invalidate();
                service.summaries().invalidate(path);
                String relocated = service.relative(moved);
                tell(viewer, MessagesKeys.GUI_REPLAY_SAVED,
                        Map.of("tg_file", RecordingLibrary.display(relocated)));
                platform.getGuiManager().open(viewerId, new ReplayRecordingScreen(relocated));
            } catch (IOException failure) {
                tell(viewer, MessagesKeys.GUI_REPLAY_SAVE_FAILED,
                        Map.of("tg_file", path, "tg_error", String.valueOf(failure.getMessage())));
            }
        });
    }

    private void tell(@Nullable PlatformPlayer viewer, ConfigKey<String> key,
                      Map<String, Object> extras) {
        if (viewer == null) return;
        viewer.sendMessage(messages().getComponent(key, extras));
    }
}
