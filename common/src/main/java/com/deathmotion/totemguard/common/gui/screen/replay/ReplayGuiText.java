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

import com.deathmotion.totemguard.common.gui.GuiText;
import com.deathmotion.totemguard.common.physics.preset.PhysicsDebugContext;
import com.deathmotion.totemguard.common.replay.RecordingSummary;
import com.deathmotion.totemguard.common.replay.ReplayText;
import com.deathmotion.totemguard.common.replay.format.RecordingLabel;
import com.deathmotion.totemguard.common.util.Palette;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ReplayGuiText {

    private ReplayGuiText() {
    }

    public static TextColor labelColor(RecordingLabel label) {
        if (label == null) return Palette.CONNECTIVE;
        return switch (label) {
            case LEGIT -> Palette.SUCCESS;
            case CHEAT -> Palette.DANGER;
            case AUTO -> Palette.WARN;
            case SCRATCH -> Palette.CONNECTIVE;
        };
    }

    public static ItemType labelItem(RecordingLabel label) {
        if (label == null) return ItemTypes.PAPER;
        return switch (label) {
            case LEGIT -> ItemTypes.WRITTEN_BOOK;
            case CHEAT -> ItemTypes.WRITABLE_BOOK;
            case AUTO -> ItemTypes.CLOCK;
            case SCRATCH -> ItemTypes.PAPER;
        };
    }

    public static String flags(RecordingSummary summary) {
        if (summary.flagCounts().isEmpty()) return String.valueOf(summary.flags());
        StringBuilder text = new StringBuilder(String.valueOf(summary.flags())).append(" (");
        boolean first = true;
        for (Map.Entry<String, Integer> entry : summary.flagCounts().entrySet()) {
            if (!first) text.append(", ");
            text.append(entry.getKey()).append(' ').append(entry.getValue());
            first = false;
        }
        return text.append(')').toString();
    }

    public static String tags(List<String> tags) {
        return tags.isEmpty() ? "none" : String.join(", ", tags);
    }

    public static String traces(List<String> tags) {
        List<String> contexts = new ArrayList<>();
        for (String tag : tags) {
            PhysicsDebugContext context = PhysicsDebugContext.parseOne(tag);
            if (context != null && !contexts.contains(context.name())) contexts.add(context.name());
        }
        return contexts.isEmpty() ? "none" : String.join(", ", contexts);
    }

    public static List<Component> details(RecordingSummary summary) {
        List<Component> lore = new ArrayList<>();
        lore.add(GuiText.line("Recorded", ReplayText.age(summary.startEpochMillis()) + " ago"));
        lore.add(GuiText.line("Player", summary.playerName()));
        lore.add(GuiText.line("Client", summary.clientName() + " (protocol " + summary.clientProtocol() + ")"));
        lore.add(GuiText.line("Server", "protocol " + summary.serverProtocol()
                + (summary.supportsEndTick() ? ", tick-end model" : ", legacy tick model")));
        lore.add(GuiText.line("Length", summary.length() + "  " + ReplayText.size(summary.bytes())
                + (summary.truncated() ? "  TRUNCATED" : "")));

        lore.add(Component.empty());
        lore.add(GuiText.line("Label", summary.label().id() + "/" + summary.scenario()));
        lore.add(GuiText.line("Tags", tags(summary.tags())));
        lore.add(GuiText.line("Traces", traces(summary.tags())));
        if (!summary.note().isBlank()) lore.add(GuiText.line("Note", summary.note()));
        if (!summary.marks().isEmpty()) lore.add(GuiText.line("Marks", String.join(", ", summary.marks())));

        lore.add(Component.empty());
        lore.add(GuiText.line("Preset", summary.preset()
                + (summary.observeOnly() ? ", observe-only" : ", mitigation live")));
        lore.add(GuiText.line("Built by", summary.pluginVersion()
                + (summary.gitHash().isEmpty() ? "" : " @" + summary.gitHash())));
        lore.add(GuiText.line("Via table", summary.viaTable() ? "present" : "none, client matches server"));

        if (!summary.sealed()) {
            lore.add(Component.empty());
            lore.add(Component.text("No end frame, this recording was never closed.", Palette.WARN));
            return lore;
        }

        lore.add(Component.empty());
        lore.add(GuiText.line("Frames", summary.frames() + " kept"
                + (summary.droppedFrames() > 0 ? ", " + summary.droppedFrames() + " dropped" : "")
                + (summary.pruned() ? ", " + summary.prunedFrames() + " pruned" : "")));
        lore.add(GuiText.line("Ticks", summary.judgedTicks() + " judged, " + summary.coastedTicks()
                + " coasted, " + summary.declinedTicks() + " declined"));
        lore.add(GuiText.line("Flags", flags(summary)));

        if (summary.degraded()) {
            lore.add(Component.empty());
            lore.add(Component.text("Frames were dropped, this recording will fail at replay time.",
                    Palette.DANGER));
        }
        return lore;
    }
}
