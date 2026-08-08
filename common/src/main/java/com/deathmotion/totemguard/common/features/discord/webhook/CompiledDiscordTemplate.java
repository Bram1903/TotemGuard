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
 *
 * Originally adapted from GrimAC (https://github.com/GrimAnticheat/Grim).
 */

package com.deathmotion.totemguard.common.features.discord.webhook;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record CompiledDiscordTemplate(Segment[] segments) {

    public static final char BACKTICK_REPLACEMENT = 'ʼ';
    private static final Pattern PLACEHOLDER = Pattern.compile("%([a-zA-Z0-9_]+)%");

    public static CompiledDiscordTemplate compile(@NotNull String template) {
        return compile(template, true);
    }

    public static CompiledDiscordTemplate compilePlain(@NotNull String template) {
        return compile(template, false);
    }

    private static CompiledDiscordTemplate compile(@NotNull String template, boolean markdown) {
        List<Segment> parts = new ArrayList<>();
        Matcher m = PLACEHOLDER.matcher(template);
        MarkdownContext ctx = MarkdownContext.NORMAL;
        int lastEnd = 0;

        while (m.find()) {
            String gap = template.substring(lastEnd, m.start());
            if (!gap.isEmpty()) parts.add(new Literal(gap));

            EscapeMode mode;
            if (markdown) {
                ctx = advanceContext(ctx, gap);
                mode = switch (ctx) {
                    case NORMAL -> EscapeMode.FULL_MARKDOWN;
                    case INLINE_CODE,
                         CODE_BLOCK -> EscapeMode.CODE_SPAN;
                };
            } else {
                mode = EscapeMode.NONE;
            }
            parts.add(new Placeholder(m.group(1), mode));
            lastEnd = m.end();
        }

        if (lastEnd < template.length()) {
            parts.add(new Literal(template.substring(lastEnd)));
        }
        return new CompiledDiscordTemplate(parts.toArray(Segment[]::new));
    }

    private static String escape(String value, EscapeMode mode) {
        if (mode == EscapeMode.NONE) return value;
        if (mode == EscapeMode.CODE_SPAN) return escapeCodeSpan(value);
        return escapeMarkdown(value);
    }

    public static String escapeMarkdown(String s) {
        if (s == null || s.isEmpty()) return s;
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '`' -> sb.append("\\`");
                case '*' -> sb.append("\\*");
                case '_' -> sb.append("\\_");
                case '~' -> sb.append("\\~");
                case '|' -> sb.append("\\|");
                case '[' -> sb.append("\\[");
                case ']' -> sb.append("\\]");
                case '(' -> sb.append("\\(");
                case ')' -> sb.append("\\)");
                case ':' -> sb.append("\\:");
                case '<' -> sb.append("\\<");
                case '#' -> sb.append("\\#");
                case '>' -> sb.append("\\>");
                case '-' -> sb.append("\\-");
                case '.' -> sb.append("\\.");
                case '\n' -> sb.append("\\n");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    public static String escapeCodeSpan(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.replace('`', BACKTICK_REPLACEMENT);
    }

    private static MarkdownContext advanceContext(MarkdownContext ctx, String text) {
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (ctx == MarkdownContext.NORMAL) {
                if (c == '\\' && i + 1 < text.length()) {
                    i += 2;
                    continue;
                }
                if (c == '`') {
                    if (i + 2 < text.length()
                            && text.charAt(i + 1) == '`'
                            && text.charAt(i + 2) == '`') {
                        ctx = MarkdownContext.CODE_BLOCK;
                        i += 3;
                        continue;
                    }
                    ctx = MarkdownContext.INLINE_CODE;
                }
            } else if (ctx == MarkdownContext.INLINE_CODE) {
                if (c == '`') ctx = MarkdownContext.NORMAL;
            } else {
                if (c == '`'
                        && i + 2 < text.length()
                        && text.charAt(i + 1) == '`'
                        && text.charAt(i + 2) == '`') {
                    ctx = MarkdownContext.NORMAL;
                    i += 3;
                    continue;
                }
            }
            i++;
        }
        return ctx;
    }

    public String render(@NotNull Function<String, @Nullable String> resolver) {
        StringBuilder sb = new StringBuilder(segments.length * 32);
        for (Segment seg : segments) {
            if (seg instanceof Literal l) {
                sb.append(l.text);
            } else if (seg instanceof Placeholder p) {
                String val = resolver.apply(p.key);
                if (val != null) {
                    sb.append(escape(val, p.mode));
                } else {
                    sb.append('%').append(p.key).append('%');
                }
            }
        }
        return sb.toString();
    }

    private enum MarkdownContext {
        NORMAL,
        INLINE_CODE,
        CODE_BLOCK
    }

    public enum EscapeMode {
        FULL_MARKDOWN,
        CODE_SPAN,
        NONE
    }

    public sealed interface Segment permits Literal, Placeholder {
    }

    public record Literal(String text) implements Segment {
    }

    public record Placeholder(String key, EscapeMode mode) implements Segment {
    }
}
