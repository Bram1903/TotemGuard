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
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.replay.RecordingLibrary;
import com.deathmotion.totemguard.common.replay.ReplayService;
import com.deathmotion.totemguard.common.replay.ReplayText;
import com.deathmotion.totemguard.common.replay.capture.ArmedRecording;
import com.deathmotion.totemguard.common.replay.capture.RecordingSession;
import com.deathmotion.totemguard.common.replay.format.RecordingLabel;
import com.deathmotion.totemguard.common.util.Palette;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ReplaySetupScreen extends ReplayScreen {

    private final ReplayDraft draft;
    private volatile String targetName;
    private volatile boolean shadow;

    private ReplaySetupScreen(String targetName, ReplayDraft draft) {
        this.targetName = targetName;
        this.draft = draft;
    }

    public static ReplaySetupScreen forViewer(UUID viewerId) {
        TGPlayer viewer = TGPlatform.getInstance().getPlayerRepository().getPlayer(viewerId);
        String name = viewer == null ? "" : viewer.getName();
        return forPlayer(name);
    }

    public static ReplaySetupScreen forPlayer(String name) {
        return new ReplaySetupScreen(name,
                new ReplayDraft(RecordingLabel.LEGIT, RecordingLibrary.scenarioFrom(name), List.of(), ""));
    }

    @Override
    public GuiRenderResult render(GuiSession session) {
        MessageService messages = messages();
        GuiRenderResult.Builder builder = GuiRenderResult.builder(4,
                GuiTitle.of(messages.getString(MessagesKeys.GUI_REPLAY_SETUP_TITLE)));
        builder.fillEmpty(GuiItems.filler());

        backButton(builder, 0, ReplayHubScreen::new);

        TGPlayer target = findTarget();
        ReplayService service = service();
        RecordingSession running = service == null || target == null ? null : service.sessionOf(target);
        ArmedRecording armed = service == null || target == null ? null : service.armOf(target.getName());

        renderTarget(builder, messages, target);

        if (running != null) {
            renderRunning(builder, messages, running, target);
            return builder.build();
        }
        if (armed != null) {
            renderArmed(builder, messages, armed, target);
            return builder.build();
        }

        renderMode(builder, messages);
        renderLabel(builder, messages);
        renderScenario(builder, messages);
        renderTags(builder, messages);
        renderNote(builder, messages);
        renderStart(builder, messages, target, session.viewerId());

        return builder.build();
    }

    private void renderRunning(GuiRenderResult.Builder builder, MessageService messages,
                               RecordingSession running, TGPlayer target) {
        long clock = target.getClock().nanos();
        builder.set(13, GuiItems.simple(
                ItemTypes.REDSTONE,
                messages.getComponent(MessagesKeys.GUI_REPLAY_SETUP_BUSY_TITLE),
                List.of(
                        messages.getComponent(MessagesKeys.GUI_REPLAY_SETUP_BUSY_LORE_1),
                        Component.empty(),
                        GuiText.line("Recording", running.getLabel().id() + "/" + running.getScenario()),
                        GuiText.line("Elapsed", ReplayText.elapsed(running.elapsedNanos(clock))),
                        GuiText.line("Size", ReplayText.size(running.byteCount())),
                        GuiText.line("Flags", String.valueOf(running.flagCount()))
                )
        ));

        builder.set(31, GuiItems.simple(
                ItemTypes.BARRIER,
                messages.getComponent(MessagesKeys.GUI_REPLAY_SETUP_STOP_TITLE),
                List.of(messages.getComponent(MessagesKeys.GUI_REPLAY_SETUP_STOP_LORE))
        ), context -> {
            ReplayService service = service();
            if (service == null || !service.stop(target, "stopped from the menu")) {
                context.playSound(GuiSounds.DENIED);
                return;
            }
            context.refresh();
            context.playSound(GuiSounds.CLOSE);
        });
    }

    private void renderArmed(GuiRenderResult.Builder builder, MessageService messages,
                             ArmedRecording armed, TGPlayer target) {
        builder.set(13, GuiItems.simple(
                ItemTypes.CLOCK,
                messages.getComponent(MessagesKeys.GUI_REPLAY_SETUP_ARMED_TITLE),
                List.of(
                        messages.getComponent(MessagesKeys.GUI_REPLAY_SETUP_ARMED_LORE_1),
                        Component.empty(),
                        GuiText.line("Armed", armed.label().id() + "/" + armed.scenario()),
                        GuiText.line("Mode", armed.shadow() ? "shadow, no kick" : "record, waiting for the rejoin"),
                        GuiText.line("Tags", ReplayGuiText.tags(armed.tags()))
                )
        ));

        builder.set(31, GuiItems.simple(
                ItemTypes.BARRIER,
                messages.getComponent(MessagesKeys.GUI_REPLAY_SETUP_CANCEL_TITLE),
                List.of(messages.getComponent(MessagesKeys.GUI_REPLAY_SETUP_CANCEL_LORE))
        ), context -> {
            ReplayService service = service();
            if (service == null || !service.cancel(target.getName())) {
                context.playSound(GuiSounds.DENIED);
                return;
            }
            context.refresh();
            context.playSound(GuiSounds.BACK);
        });
    }

    private @Nullable TGPlayer findTarget() {
        if (targetName.isBlank()) return null;
        for (TGPlayer player : TGPlatform.getInstance().getPlayerRepository().getPlayers()) {
            if (targetName.equalsIgnoreCase(player.getName())) return player;
        }
        return null;
    }

    private void renderTarget(GuiRenderResult.Builder builder, MessageService messages,
                              @Nullable TGPlayer target) {
        List<Component> lore = List.of(
                messages.getComponent(MessagesKeys.GUI_REPLAY_SETUP_TARGET_LORE_1),
                messages.getComponent(MessagesKeys.GUI_REPLAY_SETUP_TARGET_LORE_2),
                Component.empty(),
                GuiText.line("Player", targetName.isBlank() ? "none" : targetName),
                GuiText.line("Online", target == null ? "no" : "yes"));

        if (target != null) {
            builder.set(10, GuiItems.playerHead(target.getUser().getProfile(),
                    Component.text(target.getName(), Palette.VALUE), lore), this::promptTarget);
            return;
        }

        builder.set(10, GuiItems.simple(ItemTypes.PLAYER_HEAD,
                messages.getComponent(MessagesKeys.GUI_REPLAY_SETUP_TARGET_TITLE), lore), this::promptTarget);
    }

    private void promptTarget(GuiClickContext context) {
        if (context.rightClick()) {
            TGPlayer viewer = TGPlatform.getInstance().getPlayerRepository()
                    .getPlayer(context.session().viewerId());
            if (viewer != null) retarget(viewer.getName());
            context.refresh();
            context.playSound(GuiSounds.FILTER);
            return;
        }
        context.prompt(promptFor("the player to record"), this, text -> {
            retarget(text);
            return this;
        });
    }

    private void retarget(String name) {
        String previous = RecordingLibrary.scenarioFrom(targetName);
        this.targetName = name;
        if (draft.scenario().equals(previous)) draft.scenario(name);
    }

    private void renderMode(GuiRenderResult.Builder builder, MessageService messages) {
        builder.set(12, GuiItems.simple(
                shadow ? ItemTypes.SPYGLASS : ItemTypes.REDSTONE,
                messages.getComponent(shadow
                        ? MessagesKeys.GUI_REPLAY_SETUP_MODE_SHADOW_TITLE
                        : MessagesKeys.GUI_REPLAY_SETUP_MODE_RECORD_TITLE),
                List.of(
                        messages.getComponent(shadow
                                ? MessagesKeys.GUI_REPLAY_SETUP_MODE_SHADOW_LORE_1
                                : MessagesKeys.GUI_REPLAY_SETUP_MODE_RECORD_LORE_1),
                        messages.getComponent(shadow
                                ? MessagesKeys.GUI_REPLAY_SETUP_MODE_SHADOW_LORE_2
                                : MessagesKeys.GUI_REPLAY_SETUP_MODE_RECORD_LORE_2),
                        Component.empty(),
                        messages.getComponent(MessagesKeys.GUI_STATUS_CLICK_TO_SWITCH)
                )
        ), context -> {
            this.shadow = !this.shadow;
            context.refresh();
            context.playSound(GuiSounds.FILTER);
        });
    }

    private void renderLabel(GuiRenderResult.Builder builder, MessageService messages) {
        builder.set(14, GuiItems.simple(
                ReplayGuiText.labelItem(draft.label()),
                messages.getComponent(MessagesKeys.GUI_REPLAY_LABEL_TITLE),
                List.of(
                        messages.getComponent(MessagesKeys.GUI_REPLAY_LABEL_LORE_1),
                        Component.empty(),
                        GuiText.line("Label", draft.label().id()),
                        messages.getComponent(MessagesKeys.GUI_STATUS_CLICK_TO_SWITCH)
                )
        ), context -> {
            draft.cycleLabel();
            context.refresh();
            context.playSound(GuiSounds.FILTER);
        });
    }

    private void renderScenario(GuiRenderResult.Builder builder, MessageService messages) {
        builder.set(16, GuiItems.simple(
                ItemTypes.NAME_TAG,
                messages.getComponent(MessagesKeys.GUI_REPLAY_SETUP_SCENARIO_TITLE),
                List.of(
                        messages.getComponent(MessagesKeys.GUI_REPLAY_SETUP_SCENARIO_LORE_1),
                        Component.empty(),
                        GuiText.line("Name", draft.scenario().isBlank() ? "none" : draft.scenario()),
                        messages.getComponent(MessagesKeys.GUI_STATUS_CLICK_TO_TYPE)
                )
        ), context -> context.prompt(promptFor("the recording name"), this, text -> {
            draft.scenario(text);
            return this;
        }));
    }

    private void renderTags(GuiRenderResult.Builder builder, MessageService messages) {
        builder.set(20, GuiItems.simple(
                ItemTypes.BOOK,
                messages.getComponent(MessagesKeys.GUI_REPLAY_TAGS_TITLE),
                List.of(
                        messages.getComponent(MessagesKeys.GUI_REPLAY_TAGS_LORE_1),
                        Component.empty(),
                        GuiText.line("Tags", ReplayGuiText.tags(draft.tags())),
                        GuiText.line("Traces", ReplayGuiText.traces(draft.tags())),
                        Component.empty(),
                        messages.getComponent(MessagesKeys.GUI_STATUS_CLICK_TO_OPEN)
                )
        ), context -> context.open(new ReplayTagsScreen(draft, this, 0)));
    }

    private void renderNote(GuiRenderResult.Builder builder, MessageService messages) {
        builder.set(24, GuiItems.simple(
                ItemTypes.WRITABLE_BOOK,
                messages.getComponent(MessagesKeys.GUI_REPLAY_NOTE_TITLE),
                List.of(
                        messages.getComponent(MessagesKeys.GUI_REPLAY_NOTE_LORE_1),
                        messages.getComponent(MessagesKeys.GUI_REPLAY_NOTE_LORE_2),
                        Component.empty(),
                        GuiText.line("Note", draft.note().isBlank() ? "none" : draft.note())
                )
        ), context -> {
            if (context.rightClick()) {
                draft.note("");
                context.refresh();
                context.playSound(GuiSounds.FILTER);
                return;
            }
            context.prompt(promptFor("the note"), this, text -> {
                draft.note(text);
                return this;
            });
        });
    }

    private void renderStart(GuiRenderResult.Builder builder, MessageService messages,
                             @Nullable TGPlayer target, UUID viewerId) {
        boolean ready = target != null && RecordingLibrary.validScenario(draft.scenario());
        builder.set(31, GuiItems.simple(
                ready ? ItemTypes.LIME_DYE : ItemTypes.LIGHT_GRAY_DYE,
                messages.getComponent(MessagesKeys.GUI_REPLAY_SETUP_START_TITLE),
                List.of(messages.getComponent(ready
                        ? (shadow
                        ? MessagesKeys.GUI_REPLAY_SETUP_START_SHADOW_LORE
                        : MessagesKeys.GUI_REPLAY_SETUP_START_RECORD_LORE)
                        : (target == null
                        ? MessagesKeys.GUI_REPLAY_SETUP_START_NO_TARGET_LORE
                        : MessagesKeys.GUI_REPLAY_SETUP_START_NO_NAME_LORE)))
        ), context -> {
            if (!ready) {
                context.playSound(GuiSounds.DENIED);
                return;
            }
            start(context, viewerId);
        });
    }

    private void start(GuiClickContext context, UUID viewerId) {
        ReplayService service = service();
        TGPlayer target = findTarget();
        if (service == null || target == null) {
            context.playSound(GuiSounds.DENIED);
            return;
        }
        if (service.sessionOf(target) != null || service.armOf(target.getName()) != null) {
            context.refresh();
            context.playSound(GuiSounds.DENIED);
            return;
        }

        TGPlatform platform = TGPlatform.getInstance();
        PlatformPlayer viewer = platform.getPlatformPlayerFactory().create(viewerId);
        context.close();

        platform.getScheduler().runAsyncTask(() -> {
            if (shadow) {
                service.shadow(target, draft.scenario(), draft.note(), draft.tags());
            } else {
                service.arm(target, draft.label(), draft.scenario(), draft.note(), draft.tags());
            }

            if (viewer == null || target.getUuid().equals(viewerId)) return;
            viewer.sendMessage(messages().getComponent(shadow
                            ? MessagesKeys.REPLAY_SHADOW_QUEUED
                            : MessagesKeys.REPLAY_ARM_REQUESTED,
                    Map.of("tg_player", target.getName(),
                            "tg_label", draft.label().id(),
                            "tg_scenario", draft.scenario())));
        });
    }
}
