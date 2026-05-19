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

package com.deathmotion.totemguard.loader.command;

import java.util.ArrayList;
import java.util.List;

/**
 * Platform-neutral message model so the same strings render to Adventure on Paper
 * and Minecraft Text on Fabric. Each {@link Line} is an ordered list of coloured
 * {@link Segment}. The {@link Sink} interface is what each platform implements to
 * turn lines into native components.
 */
public final class LoaderMessage {

    private LoaderMessage() {
    }

    public static Line line(Object... colorText) {
        return Line.of(colorText);
    }

    public enum Color {
        /**
         * The "[TGLoader]" chat prefix. Always emitted as the first segment.
         */
        PREFIX,
        LABEL,
        VALUE,
        CAPTION,
        CONNECTIVE,
        SUCCESS,
        DANGER,
        DANGER_SOFT
    }

    public interface Sink {
        void send(Line line);
    }

    public record Segment(Color color, String text) {
    }

    public record Line(List<Segment> segments) {

        public static Line of(Object... colorText) {
            List<Segment> out = new ArrayList<>(colorText.length / 2 + 1);
            out.add(new Segment(Color.PREFIX, "[TGLoader] "));
            for (int i = 0; i + 1 < colorText.length; i += 2) {
                Color color = (Color) colorText[i];
                String text = String.valueOf(colorText[i + 1]);
                out.add(new Segment(color, text));
            }
            return new Line(List.copyOf(out));
        }

        public String plain() {
            StringBuilder sb = new StringBuilder();
            for (Segment s : segments) sb.append(s.text());
            return sb.toString();
        }
    }
}
