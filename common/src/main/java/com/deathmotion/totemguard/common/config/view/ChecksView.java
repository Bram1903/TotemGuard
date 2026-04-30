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

package com.deathmotion.totemguard.common.config.view;

import com.deathmotion.totemguard.api.config.Config;
import com.deathmotion.totemguard.api.config.ConfigSection;
import com.deathmotion.totemguard.common.config.schema.CheckConfig;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Internal typed view of {@code checks.yml}. Built on top of {@link Config} sections.
 * <p>
 * Reserved top-level keys ({@code config_version}, {@code default-punishment}) are skipped;
 * every other top-level map entry is treated as a check definition. Missing fields fall
 * back to documented defaults rather than throwing.
 */
public final class ChecksView {

    private static final String DEFAULT_PUNISHMENT_PATH = "default-punishment";
    private static final String DEFAULT_PUNISHMENT_FALLBACK = "ban %tg_player% Unfair Advantage";
    private static final Set<String> RESERVED = Set.of("config_version", DEFAULT_PUNISHMENT_PATH);

    private static final boolean DEFAULT_ENABLED = true;
    private static final boolean DEFAULT_PUNISHABLE = false;
    private static final boolean DEFAULT_MITIGATE = false;
    private static final int DEFAULT_MAX_VIOLATIONS = 1;
    private static final List<String> DEFAULT_PUNISHMENT_COMMANDS = List.of("%default_punishment%");

    private final int version;
    private final String defaultPunishment;
    private final Map<String, CheckConfig> checks;

    public ChecksView(Config config) {
        this.version = config.version();
        this.defaultPunishment = config.getString(DEFAULT_PUNISHMENT_PATH).orElse(DEFAULT_PUNISHMENT_FALLBACK);

        Map<String, CheckConfig> parsed = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : config.asMap().entrySet()) {
            if (RESERVED.contains(e.getKey())) continue;
            if (!(e.getValue() instanceof Map<?, ?>)) continue;

            Optional<ConfigSection> section = config.getSection(e.getKey());
            if (section.isEmpty()) continue;

            parsed.put(e.getKey(), parseCheck(e.getKey(), section.get()));
        }
        this.checks = Collections.unmodifiableMap(parsed);
    }

    private static CheckConfig parseCheck(String name, ConfigSection s) {
        boolean enabled = s.getBoolean("enabled").orElse(DEFAULT_ENABLED);
        boolean punishable = s.getBoolean("punishable").orElse(DEFAULT_PUNISHABLE);
        boolean mitigate = s.getBoolean("mitigate").orElse(DEFAULT_MITIGATE);
        int maxViolations = Math.max(1, s.getInt("max-violations").orElse(DEFAULT_MAX_VIOLATIONS));

        List<String> commands = s.contains("punishment-commands")
                ? List.copyOf(s.getStringList("punishment-commands"))
                : DEFAULT_PUNISHMENT_COMMANDS;

        return new CheckConfig(name, enabled, punishable, mitigate, maxViolations, commands);
    }

    public int version() {
        return version;
    }

    public @NotNull String defaultPunishment() {
        return defaultPunishment;
    }

    public @NotNull Map<String, CheckConfig> all() {
        return checks;
    }

    public @NotNull Optional<CheckConfig> get(@NotNull String name) {
        return Optional.ofNullable(checks.get(name));
    }
}
