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
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.replay.RecordingLibrary;
import com.deathmotion.totemguard.common.replay.ReplayService;
import com.deathmotion.totemguard.common.replay.ReplayText;
import com.deathmotion.totemguard.common.replay.capture.ArmedRecording;
import com.deathmotion.totemguard.common.replay.capture.RecordingSession;
import com.deathmotion.totemguard.common.util.Palette;
import com.deathmotion.totemguard.common.util.ScheduledTask;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class ReplayActiveScreen extends ReplayScreen {

    private static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25
    };
    private static final long REFRESH_SECONDS = 1L;

    private volatile @Nullable ScheduledTask ticker;

    @Override
    public void onOpen(GuiSession session) {
        this.ticker = TGPlatform.getInstance().getScheduler().runAsyncTaskAtFixedRate(
                () -> TGPlatform.getInstance().getGuiManager().refresh(session.viewerId()),
                REFRESH_SECONDS, REFRESH_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    public void onClose(GuiSession session) {
        ScheduledTask running = this.ticker;
        this.ticker = null;
        if (running != null) running.cancel();
    }

    @Override
    public GuiRenderResult render(GuiSession session) {
        MessageService messages = messages();
        GuiRenderResult.Builder builder = GuiRenderResult.builder(4,
                GuiTitle.of(messages.getString(MessagesKeys.GUI_REPLAY_ACTIVE_TITLE)));
        builder.fillEmpty(GuiItems.filler());

        backButton(builder, 0, ReplayHubScreen::new);

        ReplayService service = service();
        if (service == null) return builder.build();

        builder.set(31, GuiItems.simple(
                ItemTypes.REDSTONE,
                messages.getComponent(MessagesKeys.GUI_REPLAY_START_TITLE),
                List.of(
                        messages.getComponent(MessagesKeys.GUI_REPLAY_START_LORE_1),
                        Component.empty(),
                        messages.getComponent(MessagesKeys.GUI_STATUS_CLICK_TO_OPEN)
                )
        ), context -> context.open(ReplaySetupScreen.forViewer(session.viewerId())));

        List<RecordingSession> running = new ArrayList<>(service.active());
        List<ArmedRecording> armed = new ArrayList<>(service.arms());
        if (running.isEmpty() && armed.isEmpty()) {
            builder.set(13, GuiItems.simple(
                    ItemTypes.LIGHT_GRAY_DYE,
                    messages.getComponent(MessagesKeys.GUI_REPLAY_ACTIVE_IDLE_TITLE),
                    List.of(messages.getComponent(MessagesKeys.GUI_REPLAY_ACTIVE_IDLE_LORE))
            ));
            return builder.build();
        }

        int slot = 0;
        for (RecordingSession recording : running) {
            if (slot >= CONTENT_SLOTS.length) break;
            builder.set(CONTENT_SLOTS[slot++], runningTile(service, recording, session.viewerId(), messages),
                    context -> {
                        if (context.rightClick()) {
                            toggleHud(context, session.viewerId(), recording.getPlayer());
                            return;
                        }
                        service.stop(recording.getPlayer(), "stopped from the menu");
                        context.refresh();
                        context.playSound(GuiSounds.CLOSE);
                    });
        }

        long now = System.currentTimeMillis();
        for (ArmedRecording arm : armed) {
            if (slot >= CONTENT_SLOTS.length) break;
            builder.set(CONTENT_SLOTS[slot++], armedTile(arm, now, messages), context -> {
                service.cancel(arm.name());
                context.refresh();
                context.playSound(GuiSounds.BACK);
            });
        }

        return builder.build();
    }

    private void toggleHud(GuiClickContext context, UUID viewerId, TGPlayer target) {
        ReplayService service = service();
        TGPlayer viewer = TGPlatform.getInstance().getPlayerRepository().getPlayer(viewerId);
        if (viewer == null || service == null) {
            context.playSound(GuiSounds.DENIED);
            return;
        }
        service.toggleHud(viewer, target);
        context.refresh();
        context.playSound(GuiSounds.FILTER);
    }

    private ItemStack runningTile(ReplayService service, RecordingSession recording, UUID viewerId,
                                  MessageService messages) {
        TGPlayer player = recording.getPlayer();
        long clock = player.getClock().nanos();
        TGPlayer viewer = TGPlatform.getInstance().getPlayerRepository().getPlayer(viewerId);
        boolean watching = viewer != null && service.watching(viewer, player);

        List<Component> lore = new ArrayList<>();
        lore.add(GuiText.line("Recording", recording.getLabel().id() + "/" + recording.getScenario()));
        lore.add(GuiText.line("Elapsed", ReplayText.elapsed(recording.elapsedNanos(clock))));
        lore.add(GuiText.line("Size", ReplayText.size(recording.byteCount())));
        lore.add(GuiText.line("Frames", String.valueOf(recording.frameCount())));
        lore.add(GuiText.line("Judged", String.valueOf(recording.judgedCount())));
        lore.add(GuiText.line("Flags", String.valueOf(recording.flagCount())));
        lore.add(GuiText.line("Tags", ReplayGuiText.tags(recording.getTags())));
        lore.add(GuiText.line("File", RecordingLibrary.display(recording.getFile().getFileName().toString())));
        if (recording.degraded()) {
            lore.add(Component.empty());
            lore.add(Component.text("Dropped " + recording.droppedCount() + " frames.", Palette.DANGER));
        }
        lore.add(Component.empty());
        lore.add(messages.getComponent(MessagesKeys.GUI_REPLAY_ACTIVE_STOP_HINT));
        lore.add(messages.getComponent(watching
                ? MessagesKeys.GUI_REPLAY_ACTIVE_HUD_ON_HINT
                : MessagesKeys.GUI_REPLAY_ACTIVE_HUD_OFF_HINT));

        return GuiItems.playerHead(player.getUser().getProfile(),
                Component.text(player.getName(), Palette.DANGER), lore);
    }

    private ItemStack armedTile(ArmedRecording arm, long now, MessageService messages) {
        List<Component> lore = new ArrayList<>();
        lore.add(GuiText.line("Armed", arm.label().id() + "/" + arm.scenario()));
        lore.add(GuiText.line("Mode", arm.shadow() ? "shadow, no kick" : "record, waiting for the rejoin"));
        lore.add(GuiText.line("Expires", arm.shadow() ? "never" : arm.secondsLeft(now) + "s"));
        lore.add(GuiText.line("Tags", ReplayGuiText.tags(arm.tags())));
        lore.add(Component.empty());
        lore.add(messages.getComponent(MessagesKeys.GUI_REPLAY_ACTIVE_CANCEL_HINT));

        return GuiItems.simple(ItemTypes.CLOCK,
                Component.text(arm.name(), Palette.WARN), lore);
    }
}
