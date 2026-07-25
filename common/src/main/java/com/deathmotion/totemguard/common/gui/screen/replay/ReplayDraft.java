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

import com.deathmotion.totemguard.common.replay.RecordingLibrary;
import com.deathmotion.totemguard.common.replay.format.RecordingLabel;

import java.util.ArrayList;
import java.util.List;

public final class ReplayDraft {

    private final List<String> tags = new ArrayList<>();
    private RecordingLabel label;
    private String scenario;
    private String note;

    public ReplayDraft(RecordingLabel label, String scenario, List<String> tags, String note) {
        this.label = label;
        this.scenario = scenario;
        this.note = note;
        this.tags.addAll(tags);
    }

    public RecordingLabel label() {
        return label;
    }

    public void label(RecordingLabel next) {
        this.label = next;
    }

    public void cycleLabel() {
        RecordingLabel[] labels = RecordingLabel.values();
        this.label = labels[(label.ordinal() + 1) % labels.length];
    }

    public String scenario() {
        return scenario;
    }

    public void scenario(String next) {
        this.scenario = RecordingLibrary.scenarioFrom(next);
    }

    public String note() {
        return note;
    }

    public void note(String next) {
        this.note = next == null ? "" : next;
    }

    public List<String> tags() {
        return List.copyOf(tags);
    }

    public boolean hasTag(String tag) {
        return tags.contains(tag);
    }

    public void toggleTag(String tag) {
        String clean = RecordingLibrary.sanitize(tag);
        if (!tags.remove(clean)) tags.add(clean);
    }

    public void clearTags() {
        tags.clear();
    }
}
