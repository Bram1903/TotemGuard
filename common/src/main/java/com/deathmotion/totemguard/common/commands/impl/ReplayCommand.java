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

package com.deathmotion.totemguard.common.commands.impl;

import com.deathmotion.totemguard.api.config.key.ConfigKey;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.commands.AbstractCommand;
import com.deathmotion.totemguard.common.commands.CommandDefaults;
import com.deathmotion.totemguard.common.commands.suggestion.RecordingSuggestionProvider;
import com.deathmotion.totemguard.common.commands.suggestion.TGPlayerSuggestionProvider;
import com.deathmotion.totemguard.common.config.key.MessagesKeys;
import com.deathmotion.totemguard.common.gui.GuiScreen;
import com.deathmotion.totemguard.common.gui.screen.replay.ReplayGuiText;
import com.deathmotion.totemguard.common.gui.screen.replay.ReplayHubScreen;
import com.deathmotion.totemguard.common.gui.screen.replay.ReplayLabelsScreen;
import com.deathmotion.totemguard.common.gui.screen.replay.ReplaySetupScreen;
import com.deathmotion.totemguard.common.platform.sender.Sender;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.replay.*;
import com.deathmotion.totemguard.common.replay.capture.ArmedRecording;
import com.deathmotion.totemguard.common.replay.capture.RecordingSession;
import com.deathmotion.totemguard.common.replay.format.RecordingLabel;
import com.deathmotion.totemguard.common.replay.retention.RetentionBuffer;
import com.deathmotion.totemguard.common.util.Palette;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextDecoration;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.component.CommandComponent;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.flag.CommandFlag;
import org.incendo.cloud.parser.standard.EnumParser;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.SuggestionProvider;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.*;

public final class ReplayCommand extends AbstractCommand {

    private static final CommandFlag<String> NOTE = CommandFlag.<Sender>builder("note")
            .withAliases("n")
            .withComponent(StringParser.greedyFlagYieldingStringParser())
            .build();

    private static final CommandFlag<String> PLAYER = CommandFlag.<Sender>builder("player")
            .withAliases("p")
            .withComponent(CommandComponent.<Sender, String>builder("player", StringParser.stringParser())
                    .suggestionProvider(TGPlayerSuggestionProvider.localSuggestionProvider()))
            .build();

    private static final CommandFlag<String> TAGS = CommandFlag.<Sender>builder("tags")
            .withAliases("t")
            .withComponent(CommandComponent.<Sender, String>builder("tags", StringParser.stringParser())
                    .suggestionProvider(RecordingSuggestionProvider.tags()))
            .build();

    private static final int LIST_LIMIT = 15;

    @Override
    public void register(@NonNull CommandManager<Sender> manager) {
        manager.command(base(manager).literal("replay")
                .permission(perm("replay")).handler(this::hub));

        manager.command(base(manager).literal("replay").literal("help")
                .permission(perm("replay")).handler(this::usage));

        manager.command(base(manager).literal("replay").literal("record")
                .permission(perm("replay")).handler(this::setup));

        manager.command(base(manager).literal("replay").literal("record")
                .required("label", EnumParser.enumParser(RecordingLabel.class))
                .required("scenario", StringParser.greedyFlagYieldingStringParser(),
                        RecordingSuggestionProvider.scenarios())
                .flag(NOTE)
                .flag(PLAYER)
                .flag(TAGS)
                .permission(perm("replay")).handler(this::record));

        manager.command(base(manager).literal("replay").literal("shadow")
                .required("player", StringParser.stringParser(),
                        TGPlayerSuggestionProvider.localSuggestionProvider())
                .flag(NOTE)
                .flag(TAGS)
                .permission(perm("replay")).handler(this::shadow));

        manager.command(base(manager).literal("replay").literal("cancel")
                .permission(perm("replay")).handler(this::cancel));

        manager.command(base(manager).literal("replay").literal("cancel")
                .required("player", StringParser.stringParser(), armedNames())
                .permission(perm("replay")).handler(this::cancel));

        manager.command(base(manager).literal("replay").literal("stop")
                .permission(perm("replay")).handler(this::stop));

        manager.command(base(manager).literal("replay").literal("stop")
                .required("player", StringParser.stringParser(), recordingNames())
                .permission(perm("replay")).handler(this::stop));

        manager.command(base(manager).literal("replay").literal("dump")
                .permission(perm("replay")).handler(this::dump));

        manager.command(base(manager).literal("replay").literal("dump")
                .required("player", StringParser.stringParser(), trackedNames())
                .permission(perm("replay")).handler(this::dump));

        manager.command(base(manager).literal("replay").literal("mark")
                .required("text", StringParser.greedyStringParser())
                .permission(perm("replay")).handler(this::mark));

        manager.command(base(manager).literal("replay").literal("status")
                .permission(perm("replay")).handler(this::status));

        manager.command(base(manager).literal("replay").literal("status")
                .required("player", StringParser.stringParser(), trackedNames())
                .permission(perm("replay")).handler(this::status));

        manager.command(base(manager).literal("replay").literal("hud")
                .permission(perm("replay")).handler(this::hud));

        manager.command(base(manager).literal("replay").literal("hud")
                .required("player", StringParser.stringParser(), recordingNames())
                .permission(perm("replay")).handler(this::hud));

        manager.command(base(manager).literal("replay").literal("list")
                .optional("filter", StringParser.greedyStringParser(), RecordingSuggestionProvider.recordings())
                .permission(perm("replay")).handler(this::list));

        manager.command(base(manager).literal("replay").literal("inspect")
                .required("file", StringParser.greedyStringParser(), RecordingSuggestionProvider.recordings())
                .permission(perm("replay")).handler(this::inspect));

        manager.command(base(manager).literal("replay").literal("run")
                .required("file", StringParser.greedyStringParser(), RecordingSuggestionProvider.recordings())
                .permission(perm("replay")).handler(this::run));

        manager.command(base(manager).literal("replay").literal("watch")
                .permission(perm("replay")).handler(this::watchStop));

        manager.command(base(manager).literal("replay").literal("watch")
                .required("file", StringParser.greedyStringParser(), RecordingSuggestionProvider.recordings())
                .permission(perm("replay")).handler(this::watch));
    }

    private void hub(CommandContext<Sender> context) {
        Sender sender = context.sender();
        if (service() == null) {
            tell(sender, MessagesKeys.REPLAY_DISABLED, Map.of());
            usage(context);
            return;
        }
        if (!sender.isPlayer()) {
            tell(sender, MessagesKeys.GUI_REPLAY_MENU_NEEDS_PLAYER, Map.of());
            usage(context);
            return;
        }
        menu(sender, new ReplayHubScreen());
    }

    private void setup(CommandContext<Sender> context) {
        Sender sender = context.sender();
        if (service() == null) {
            tell(sender, MessagesKeys.REPLAY_DISABLED, Map.of());
            return;
        }
        if (!requirePlayer(sender)) return;
        menu(sender, ReplaySetupScreen.forPlayer(sender.getName()));
    }

    private boolean menu(Sender sender, GuiScreen screen) {
        if (TGPlatform.getInstance().getGuiManager().open(sender, screen)) return true;
        tell(sender, MessagesKeys.GUI_REPLAY_OPEN_FAILED, Map.of());
        return false;
    }

    private void usage(CommandContext<Sender> context) {
        Component message = Component.text("Replay", Palette.BRAND, TextDecoration.BOLD)
                .append(line("record <legit|cheat|scratch> <scenario> [--tags a,b] [--note …] [--player <name>]",
                        "arm, kick, then record from the rejoin"))
                .append(line("shadow <player> [--tags a,b] [--note …]",
                        "arm without kicking, keep only if something flags"))
                .append(line("cancel [player]", "drop a pending arm"))
                .append(line("stop [player]", "close and save a running recording"))
                .append(line("mark <text>", "drop a named marker into the tape"))
                .append(line("status [player]", "what is armed and what is recording"))
                .append(line("hud [player]", "show a recording on your action bar, yours or someone else's"))
                .append(line("list [filter]", "recordings on disk, newest first, filter by name or tag"))
                .append(line("inspect <file>", "everything the recording knows about itself"))
                .append(line("run <file>", "replay a recording through this build"))
                .append(line("watch <file>", "watch a recording in game, from inside it"))
                .append(line("watch", "leave the recording you are watching"))
                .append(Component.newline())
                .append(Component.text("  /" + CommandDefaults.ROOT + " replay, replay record and replay list"
                        + " open the menu when a player runs them.", Palette.CAPTION));
        context.sender().sendMessage(message);
    }

    private Component line(String usage, String description) {
        return Component.newline()
                .append(Component.text("  /" + CommandDefaults.ROOT + " replay " + usage, Palette.VALUE))
                .append(Component.newline())
                .append(Component.text("    " + description, Palette.CAPTION));
    }

    private void record(CommandContext<Sender> context) {
        Sender sender = context.sender();
        ReplayService service = service();
        if (service == null) {
            tell(sender, MessagesKeys.REPLAY_DISABLED, Map.of());
            return;
        }

        TGPlayer target = target(context, sender);
        if (target == null) return;

        String scenario = context.get("scenario");
        if (!RecordingLibrary.validScenario(scenario)) {
            tell(sender, MessagesKeys.REPLAY_SCENARIO_INVALID, Map.of("tg_scenario", scenario));
            return;
        }

        RecordingLabel label = context.get("label");
        ReplayService.ArmResult result = service.arm(target, label, scenario, note(context), tags(context));
        switch (result) {
            case REPLACED -> tell(sender, MessagesKeys.REPLAY_ARM_REPLACED,
                    Map.of("tg_player", target.getName()));
            case ARMED -> {
                if (target.getUuid().equals(sender.getUniqueId())) return;
                tell(sender, MessagesKeys.REPLAY_ARM_REQUESTED, Map.of(
                        "tg_player", target.getName(),
                        "tg_label", label.id(),
                        "tg_scenario", scenario));
            }
        }
    }

    private @Nullable TGPlayer target(CommandContext<Sender> context, Sender sender) {
        Optional<String> requested = context.flags().getValue(PLAYER);
        if (requested.isEmpty()) {
            if (!requirePlayer(sender)) return null;
            return sender.getTGPlayer();
        }

        String name = requested.get();
        TGPlayer found = TGPlayerSuggestionProvider.findPlayer(name);
        if (found == null) tell(sender, MessagesKeys.GENERAL_PLAYER_NOT_FOUND, Map.of("tg_input", name));
        return found;
    }

    private void shadow(CommandContext<Sender> context) {
        Sender sender = context.sender();
        ReplayService service = service();
        if (service == null) {
            tell(sender, MessagesKeys.REPLAY_DISABLED, Map.of());
            return;
        }

        String name = context.get("player");
        TGPlayer target = TGPlayerSuggestionProvider.findPlayer(name);
        if (target == null) {
            tell(sender, MessagesKeys.GENERAL_PLAYER_NOT_FOUND, Map.of("tg_input", name));
            return;
        }

        service.shadow(target, "shadow-" + RecordingLibrary.sanitize(name), note(context), tags(context));
        tell(sender, MessagesKeys.REPLAY_SHADOW_QUEUED, Map.of("tg_player", target.getName()));
    }

    private void cancel(CommandContext<Sender> context) {
        Sender sender = context.sender();
        ReplayService service = service();
        if (service == null) {
            tell(sender, MessagesKeys.REPLAY_DISABLED, Map.of());
            return;
        }
        String name = context.<String>optional("player").orElse(sender.getName());
        tell(sender, service.cancel(name)
                ? MessagesKeys.REPLAY_ARM_CANCELLED
                : MessagesKeys.REPLAY_ARM_NONE, Map.of());
    }

    private void stop(CommandContext<Sender> context) {
        Sender sender = context.sender();
        ReplayService service = service();
        if (service == null) {
            tell(sender, MessagesKeys.REPLAY_DISABLED, Map.of());
            return;
        }

        Optional<String> requested = context.optional("player");
        if (requested.isEmpty()) {
            if (!requirePlayer(sender)) return;
            TGPlayer player = sender.getTGPlayer();
            if (player == null || !service.stop(player, "stopped")) {
                tell(sender, MessagesKeys.REPLAY_NOT_RECORDING, Map.of());
            }
            return;
        }

        String name = requested.get();
        for (RecordingSession session : service.active()) {
            TGPlayer target = session.getPlayer();
            if (!name.equalsIgnoreCase(target.getName())) continue;
            service.stop(target, "stopped by " + sender.getName());
            tell(sender, MessagesKeys.REPLAY_STOP_REQUESTED, Map.of("tg_player", target.getName()));
            return;
        }
        tell(sender, MessagesKeys.REPLAY_NOT_RECORDING, Map.of());
    }

    private void dump(CommandContext<Sender> context) {
        Sender sender = context.sender();
        ReplayService service = service();
        if (service == null) {
            tell(sender, MessagesKeys.REPLAY_DISABLED, Map.of());
            return;
        }

        String requested = context.<String>optional("player").orElse(null);
        TGPlayer target = null;
        if (requested == null) {
            if (!requirePlayer(sender)) return;
            target = sender.getTGPlayer();
        } else {
            for (TGPlayer candidate : TGPlatform.getInstance().getPlayerRepository().getPlayers()) {
                if (requested.equalsIgnoreCase(candidate.getName())) {
                    target = candidate;
                    break;
                }
            }
        }
        if (target == null) {
            tell(sender, MessagesKeys.REPLAY_NOT_RECORDING, Map.of());
            return;
        }

        tell(sender, service.dumpRetained(target, "dump by " + sender.getName())
                        ? MessagesKeys.REPLAY_DUMP_STARTED
                        : MessagesKeys.REPLAY_DUMP_UNAVAILABLE,
                Map.of("tg_player", target.getName()));
    }

    private void mark(CommandContext<Sender> context) {
        Sender sender = context.sender();
        if (!requirePlayer(sender)) return;
        TGPlayer player = sender.getTGPlayer();
        ReplayService service = service();
        if (player == null || service == null) return;
        RecordingSession session = service.sessionOf(player);
        if (session == null) {
            tell(sender, MessagesKeys.REPLAY_NOT_RECORDING, Map.of());
            return;
        }
        String text = context.get("text");
        service.mark(player, text);
        tell(sender, MessagesKeys.REPLAY_MARKED,
                Map.of("tg_label", text, "tg_frames", session.frameCount()));
    }

    private void status(CommandContext<Sender> context) {
        Sender sender = context.sender();
        ReplayService service = service();
        if (service == null) {
            tell(sender, MessagesKeys.REPLAY_DISABLED, Map.of());
            return;
        }

        String only = context.<String>optional("player").orElse(null);
        List<RecordingSession> sessions = new ArrayList<>();
        for (RecordingSession session : service.active()) {
            if (only == null || only.equalsIgnoreCase(session.getPlayer().getName())) sessions.add(session);
        }
        List<ArmedRecording> armed = new ArrayList<>();
        for (ArmedRecording arm : service.arms()) {
            if (only == null || only.equalsIgnoreCase(arm.name())) armed.add(arm);
        }
        if (sessions.isEmpty() && armed.isEmpty()) {
            if (!retentionStatus(sender, service, only)) {
                tell(sender, MessagesKeys.REPLAY_STATUS_IDLE, Map.of());
            }
            return;
        }

        long now = System.currentTimeMillis();
        for (ArmedRecording arm : armed) {
            tell(sender, MessagesKeys.REPLAY_STATUS_ARMED, Map.of(
                    "tg_player", arm.name(),
                    "tg_label", arm.label().id(),
                    "tg_scenario", arm.scenario(),
                    "tg_seconds", arm.shadow() ? "no timeout" : arm.secondsLeft(now) + "s"));
        }
        for (RecordingSession session : sessions) {
            long clock = session.getPlayer().getClock().nanos();
            tell(sender, MessagesKeys.REPLAY_STATUS, Map.of(
                    "tg_player", session.getPlayer().getName(),
                    "tg_label", session.getLabel().id(),
                    "tg_scenario", session.getScenario(),
                    "tg_elapsed", ReplayText.elapsed(session.elapsedNanos(clock)),
                    "tg_frames", session.frameCount(),
                    "tg_size", ReplayText.size(session.byteCount()),
                    "tg_judged", session.judgedCount(),
                    "tg_flags", session.flagCount()));
        }
    }

    private boolean retentionStatus(Sender sender, ReplayService service, @Nullable String only) {
        boolean any = false;
        for (TGPlayer player : TGPlatform.getInstance().getPlayerRepository().getPlayers()) {
            if (only != null && !only.equalsIgnoreCase(player.getName())) continue;
            RetentionBuffer buffer = service.retentionOf(player);
            if (buffer == null) continue;
            any = true;
            tell(sender, MessagesKeys.REPLAY_STATUS_RETAINED, Map.of(
                    "tg_player", player.getName(),
                    "tg_elapsed", ReplayText.elapsed(buffer.retainedNanos(player.getClock().nanos())),
                    "tg_frames", buffer.retainedFrames(),
                    "tg_dropped", buffer.droppedFrames()));
        }
        return any;
    }

    private void hud(CommandContext<Sender> context) {
        Sender sender = context.sender();
        if (!requirePlayer(sender)) return;
        TGPlayer viewer = sender.getTGPlayer();
        ReplayService service = service();
        if (viewer == null || service == null) return;

        Optional<String> requested = context.optional("player");
        TGPlayer target = viewer;
        if (requested.isPresent()) {
            target = TGPlayerSuggestionProvider.findPlayer(requested.get());
            if (target == null) {
                tell(sender, MessagesKeys.GENERAL_PLAYER_NOT_FOUND, Map.of("tg_input", requested.get()));
                return;
            }
        }

        if (service.sessionOf(target) == null) {
            tell(sender, MessagesKeys.REPLAY_NOT_RECORDING, Map.of());
            return;
        }

        boolean enabled = service.toggleHud(viewer, target);
        if (target == viewer) {
            tell(sender, enabled ? MessagesKeys.REPLAY_HUD_ENABLED : MessagesKeys.REPLAY_HUD_DISABLED, Map.of());
            return;
        }
        tell(sender, enabled ? MessagesKeys.REPLAY_HUD_FOLLOWING : MessagesKeys.REPLAY_HUD_UNFOLLOWED,
                Map.of("tg_player", target.getName()));
    }

    private void list(CommandContext<Sender> context) {
        Sender sender = context.sender();
        ReplayService service = service();
        if (service == null) {
            tell(sender, MessagesKeys.REPLAY_DISABLED, Map.of());
            return;
        }
        String filter = context.<String>optional("filter").orElse("");
        if (filter.isBlank() && sender.isPlayer()) {
            menu(sender, new ReplayLabelsScreen());
            return;
        }
        service.index().refreshed().thenAccept(entries -> render(sender, filter, entries));
    }

    private void render(Sender sender, String filter, List<RecordingIndex.Entry> all) {
        List<RecordingIndex.Entry> matched = RecordingIndex.matching(all, filter);
        if (matched.isEmpty()) {
            sender.sendMessage(Component.text(all.isEmpty()
                    ? "No recordings on disk yet."
                    : "No recording matches " + filter + ".", Palette.WARN));
            return;
        }

        Component message = Component.text("Recordings", Palette.BRAND, TextDecoration.BOLD)
                .append(Component.text(" (" + matched.size() + ")", Palette.CAPTION));
        for (RecordingIndex.Entry entry : matched.subList(0, Math.min(LIST_LIMIT, matched.size()))) {
            message = message.append(Component.newline()).append(row(entry));
        }
        if (matched.size() > LIST_LIMIT) {
            message = message.append(Component.newline()).append(Component.text("  +"
                    + (matched.size() - LIST_LIMIT) + " more, narrow it with /"
                    + CommandDefaults.ROOT + " replay list <filter>", Palette.CAPTION));
        }
        sender.sendMessage(message);
    }

    private Component row(RecordingIndex.Entry entry) {
        String name = RecordingLibrary.display(entry.path());
        String command = "/" + CommandDefaults.ROOT + " replay run " + name;
        Component row = Component.text("  " + name, Palette.VALUE)
                .append(Component.text("  " + ReplayText.size(entry.bytes()), Palette.CONNECTIVE))
                .append(Component.text("  " + ReplayText.age(entry.modifiedMillis()) + " old", Palette.CAPTION));
        if (!entry.tags().isEmpty()) {
            row = row.append(Component.text("  " + String.join(" ", entry.tags()), Palette.CONNECTIVE));
        }
        return row
                .clickEvent(ClickEvent.suggestCommand(command))
                .hoverEvent(HoverEvent.showText(Component.text(command, Palette.CAPTION)));
    }

    private void run(CommandContext<Sender> context) {
        Sender sender = context.sender();
        ReplayService service = service();
        if (service == null) {
            tell(sender, MessagesKeys.REPLAY_DISABLED, Map.of());
            return;
        }

        service.run(context.get("file"), sender::sendMessage);
    }

    private void watch(CommandContext<Sender> context) {
        Sender sender = context.sender();
        ReplayService service = service();
        if (service == null) {
            tell(sender, MessagesKeys.REPLAY_DISABLED, Map.of());
            return;
        }
        if (!requirePlayer(sender)) return;
        TGPlayer viewer = sender.getTGPlayer();
        if (viewer == null) return;
        service.viewers().watch(viewer, context.get("file"), sender::sendMessage);
    }

    private void watchStop(CommandContext<Sender> context) {
        Sender sender = context.sender();
        ReplayService service = service();
        if (service == null) {
            tell(sender, MessagesKeys.REPLAY_DISABLED, Map.of());
            return;
        }
        if (!requirePlayer(sender)) return;
        TGPlayer viewer = sender.getTGPlayer();
        if (viewer == null) return;
        if (service.viewers().stop(viewer)) {
            tell(sender, MessagesKeys.REPLAY_WATCH_STOPPED, Map.of());
            return;
        }
        tell(sender, MessagesKeys.REPLAY_WATCH_NOT_WATCHING, Map.of());
    }

    private void inspect(CommandContext<Sender> context) {
        Sender sender = context.sender();
        ReplayService service = service();
        if (service == null) {
            tell(sender, MessagesKeys.REPLAY_DISABLED, Map.of());
            return;
        }

        String query = context.get("file");
        TGPlatform.getInstance().getScheduler().runAsyncTask(() -> {
            Path file = service.library().resolve(query);
            if (file == null) {
                tell(sender, MessagesKeys.REPLAY_FILE_NOT_FOUND, Map.of("tg_file", query));
                return;
            }
            RecordingSummaryCache.Entry entry = service.summaries().require(service.relative(file));
            RecordingSummary summary = entry.summary();
            if (summary == null) {
                tell(sender, MessagesKeys.REPLAY_INSPECT_UNREADABLE,
                        Map.of("tg_file", service.named(file), "tg_error", String.valueOf(entry.error())));
                return;
            }
            sender.sendMessage(inspection(summary));
        });
    }

    private Component inspection(RecordingSummary summary) {
        Component body = Component.text(summary.display(), Palette.VALUE, TextDecoration.BOLD)
                .append(row("recorded", ReplayText.age(summary.startEpochMillis()) + " ago"))
                .append(row("player", summary.playerName() + "  " + summary.playerUuid()))
                .append(row("client", summary.clientName() + " (protocol " + summary.clientProtocol() + ")"))
                .append(row("server", "protocol " + summary.serverProtocol()
                        + (summary.supportsEndTick() ? ", tick-end model" : ", legacy tick model")))
                .append(row("label", summary.label().id() + "/" + summary.scenario()))
                .append(row("tags", ReplayGuiText.tags(summary.tags())))
                .append(row("traces", ReplayGuiText.traces(summary.tags())))
                .append(row("length", summary.length() + "  " + ReplayText.size(summary.bytes())
                        + (summary.truncated() ? "  TRUNCATED" : "")))
                .append(row("preset", summary.preset()
                        + (summary.observeOnly() ? ", observe-only" : ", mitigation live")))
                .append(row("built by", summary.pluginVersion()
                        + (summary.gitHash().isEmpty() ? "" : " @" + summary.gitHash())))
                .append(row("via table", summary.viaTable() ? "present" : "none, client matches server"));

        if (!summary.note().isBlank()) body = body.append(row("note", summary.note()));
        if (!summary.marks().isEmpty()) {
            body = body.append(row("marks", String.join(", ", summary.marks())));
        }

        if (!summary.sealed()) return body;
        return body.append(row("frames", summary.frames() + " kept"
                        + (summary.droppedFrames() > 0 ? ", " + summary.droppedFrames() + " dropped" : "")
                        + (summary.pruned() ? ", " + summary.prunedFrames() + " pruned" : "")))
                .append(row("ticks", summary.judgedTicks() + " judged, " + summary.coastedTicks()
                        + " coasted, " + summary.declinedTicks() + " declined"))
                .append(row("flags", ReplayGuiText.flags(summary)));
    }

    private Component row(String label, String value) {
        return Component.text("\n  ").append(Component.text(pad(label), Palette.CAPTION))
                .append(Component.text(value, Palette.VALUE));
    }

    private String pad(String label) {
        StringBuilder padded = new StringBuilder(label);
        while (padded.length() < 10) padded.append(' ');
        return padded.toString();
    }

    private String note(CommandContext<Sender> context) {
        return context.flags().getValue(NOTE).orElse("");
    }

    private void tell(Sender sender, ConfigKey<String> key, Map<String, Object> extras) {
        sender.sendMessage(TGPlatform.getInstance().getMessageService().getComponent(key, extras));
    }

    private List<String> tags(CommandContext<Sender> context) {
        return RecordingLibrary.parseTags(context.flags().getValue(TAGS).orElse(""));
    }

    private @Nullable ReplayService service() {
        return TGPlatform.getInstance().getReplayService();
    }

    private SuggestionProvider<Sender> armedNames() {
        return SuggestionProvider.blockingStrings((context, input) -> {
            ReplayService service = service();
            if (service == null) return List.of();
            String needle = input.lastRemainingToken().toLowerCase(Locale.ROOT);
            List<String> names = new ArrayList<>();
            for (ArmedRecording arm : service.arms()) {
                if (arm.name().toLowerCase(Locale.ROOT).startsWith(needle)) names.add(arm.name());
            }
            return names;
        });
    }

    private SuggestionProvider<Sender> trackedNames() {
        return SuggestionProvider.blockingStrings((context, input) -> {
            ReplayService service = service();
            if (service == null) return List.of();
            String needle = input.lastRemainingToken().toLowerCase(Locale.ROOT);
            Set<String> names = new LinkedHashSet<>();
            for (ArmedRecording arm : service.arms()) {
                if (arm.name().toLowerCase(Locale.ROOT).startsWith(needle)) names.add(arm.name());
            }
            for (RecordingSession session : service.active()) {
                String name = session.getPlayer().getName();
                if (name != null && name.toLowerCase(Locale.ROOT).startsWith(needle)) names.add(name);
            }
            return names;
        });
    }

    private SuggestionProvider<Sender> recordingNames() {
        return SuggestionProvider.blockingStrings((context, input) -> {
            ReplayService service = service();
            if (service == null) return List.of();
            String needle = input.lastRemainingToken().toLowerCase(Locale.ROOT);
            List<String> names = new ArrayList<>();
            for (RecordingSession session : service.active()) {
                String name = session.getPlayer().getName();
                if (name != null && name.toLowerCase(Locale.ROOT).startsWith(needle)) names.add(name);
            }
            return names;
        });
    }
}
