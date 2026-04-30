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

package com.deathmotion.totemguard.common.config.yaml;

import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.resolver.Resolver;

import java.util.regex.Pattern;

/**
 * SnakeYAML's default {@link Resolver} follows YAML 1.1, which treats {@code yes}, {@code no},
 * {@code on}, {@code off} (and their case variants) as booleans. That silently rewrites map keys
 * like {@code gui.status.yes} into {@code Boolean.TRUE}, which then lookup misses.
 *
 * <p>This resolver matches YAML 1.2 / JSON: only {@code true|false} are booleans.
 */
public final class Yaml12Resolver extends Resolver {

    private static final Pattern BOOL_1_2 = Pattern.compile("^(?:true|True|TRUE|false|False|FALSE)$");

    @Override
    protected void addImplicitResolvers() {
        addImplicitResolver(Tag.BOOL, BOOL_1_2, "tTfF");
        addImplicitResolver(Tag.INT, INT, "-+0123456789");
        addImplicitResolver(Tag.FLOAT, FLOAT, "-+0123456789.");
        addImplicitResolver(Tag.MERGE, MERGE, "<");
        addImplicitResolver(Tag.NULL, NULL, "~nN\0");
        addImplicitResolver(Tag.NULL, EMPTY, null);
        addImplicitResolver(Tag.TIMESTAMP, TIMESTAMP, "0123456789");
    }
}
