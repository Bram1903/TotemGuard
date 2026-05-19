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

package com.deathmotion.totemguard.common.database.util;

import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DebugTemplate {

    public static final String ARG_DELIMITER = "";

    private static final int MAX_TEMPLATE_BYTES = 255;
    private static final int MAX_ARGS_BYTES = 64;
    private static final int MAX_ARGS = 8;

    private static final Pattern NUMERIC = Pattern.compile("-?\\d+\\.\\d+|-?\\d+");
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{(\\d+)}");

    private DebugTemplate() {
    }

    public static @Nullable Compiled compile(@Nullable String raw) {
        if (raw == null) return null;
        if (raw.isEmpty()) return new Compiled("", null);

        Matcher matcher = NUMERIC.matcher(raw);
        if (!matcher.find()) {
            return new Compiled(truncate(raw, MAX_TEMPLATE_BYTES), null);
        }

        StringBuilder template = new StringBuilder(raw.length());
        StringBuilder argsJoined = new StringBuilder();
        int cursor = 0;
        int argIndex = 0;

        do {
            template.append(raw, cursor, matcher.start());
            if (argIndex >= MAX_ARGS) {
                template.append(matcher.group());
            } else {
                template.append('{').append(argIndex).append('}');
                if (argIndex > 0) argsJoined.append(ARG_DELIMITER);
                argsJoined.append(matcher.group());
                argIndex++;
            }
            cursor = matcher.end();
        } while (matcher.find());
        template.append(raw, cursor, raw.length());

        String compiledTemplate = truncate(template.toString(), MAX_TEMPLATE_BYTES);
        String compiledArgs = argIndex == 0 ? null : truncate(argsJoined.toString(), MAX_ARGS_BYTES);
        return new Compiled(compiledTemplate, compiledArgs);
    }

    public static @Nullable Compiled precompiled(@Nullable String template, @Nullable Object... args) {
        if (template == null) return null;
        String compiledTemplate = truncate(template, MAX_TEMPLATE_BYTES);
        if (args == null || args.length == 0) return new Compiled(compiledTemplate, null);

        StringBuilder joined = new StringBuilder();
        int count = Math.min(args.length, MAX_ARGS);
        for (int i = 0; i < count; i++) {
            if (i > 0) joined.append(ARG_DELIMITER);
            joined.append(args[i] == null ? "" : args[i].toString());
        }
        return new Compiled(compiledTemplate, truncate(joined.toString(), MAX_ARGS_BYTES));
    }

    public static @Nullable String render(@Nullable String template, @Nullable String args) {
        if (template == null) return null;
        if (template.isEmpty() || args == null || args.isEmpty()) return template;

        String[] parts = args.split(ARG_DELIMITER, -1);
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuilder out = new StringBuilder(template.length() + args.length());
        int cursor = 0;
        while (matcher.find()) {
            out.append(template, cursor, matcher.start());
            int idx = Integer.parseInt(matcher.group(1));
            if (idx >= 0 && idx < parts.length) {
                out.append(parts[idx]);
            } else {
                out.append(matcher.group());
            }
            cursor = matcher.end();
        }
        out.append(template, cursor, template.length());
        return out.toString();
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }

    public record Compiled(String template, @Nullable String args) {
    }
}
