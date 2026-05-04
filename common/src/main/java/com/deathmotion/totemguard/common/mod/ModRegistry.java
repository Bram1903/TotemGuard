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

package com.deathmotion.totemguard.common.mod;

import com.deathmotion.totemguard.api.mod.ModSeverity;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.config.schema.ModConfig;
import com.deathmotion.totemguard.common.config.view.ModsView;
import com.deathmotion.totemguard.common.punishment.PunishmentCommand;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.*;

public final class ModRegistry {

    private static volatile Snapshot SNAPSHOT = Snapshot.empty();

    private ModRegistry() {
    }

    public static @NotNull Snapshot snapshot() {
        return SNAPSHOT;
    }

    public static void load() {
        TGPlatform platform = TGPlatform.getInstance();
        ModsView view = platform.getConfigRepository().mods();

        LinkedHashMap<String, ModDefinition> definitions = new LinkedHashMap<>();
        Map<String, String> payloadOwners = new HashMap<>();
        Map<String, String> translationOwners = new HashMap<>();

        for (ModConfig cfg : view.all().values()) {
            ModSeverity severity = parseSeverity(cfg.severity());
            if (severity == null) {
                warn("Ignoring invalid severity '" + cfg.severity() + "' for mod '" + cfg.id()
                        + "'. Valid values: LOG, KICK, BAN, KICK_THEN_BAN. Defaulting to KICK.");
                severity = ModSeverity.KICK;
            }

            Set<String> payloads = claimUnique(cfg.id(), cfg.payloads(), payloadOwners, true, "payload");
            Set<String> translations = claimUnique(cfg.id(), cfg.translations(), translationOwners, false, "translation");

            if (payloads.isEmpty() && translations.isEmpty()) {
                warn("Ignoring mod '" + cfg.id() + "' because it has no usable payloads or translations.");
                continue;
            }

            definitions.put(cfg.id(), new ModDefinition(cfg.id(), severity, payloads, translations));
        }

        SNAPSHOT = new Snapshot(
                Collections.unmodifiableMap(definitions),
                buildPayloadEntries(definitions),
                PunishmentCommand.parse("[KICK] " + view.kickCommand()),
                PunishmentCommand.parse("[BAN] " + view.banCommand()),
                PunishmentCommand.parse("[KICK] " + view.unresponsiveKickCommand()),
                Duration.ofMinutes(view.kickThenBanWindowMinutes()),
                view.modListLimit(),
                view.modListOverflowFormat()
        );
    }

    private static @Nullable ModSeverity parseSeverity(@Nullable String value) {
        if (value == null || value.isBlank()) return ModSeverity.KICK;
        try {
            return ModSeverity.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static Set<String> claimUnique(
            String modId,
            Collection<String> rawValues,
            Map<String, String> ownerIndex,
            boolean lowerCase,
            String label
    ) {
        if (rawValues.isEmpty()) return Set.of();

        LinkedHashSet<String> claimed = new LinkedHashSet<>();
        for (String raw : rawValues) {
            if (raw == null) continue;
            String trimmed = raw.trim();
            if (trimmed.isEmpty()) continue;

            String key = lowerCase ? trimmed.toLowerCase(Locale.ROOT) : trimmed;
            String owner = ownerIndex.putIfAbsent(key, modId);
            if (owner == null) {
                claimed.add(key);
            } else if (!owner.equals(modId)) {
                warn("Skipping duplicate " + label + " '" + trimmed + "' on mod '" + modId
                        + "': already declared by '" + owner + "'.");
            }
        }
        return claimed;
    }

    private static List<PayloadEntry> buildPayloadEntries(Map<String, ModDefinition> definitions) {
        List<PayloadEntry> entries = new ArrayList<>();
        for (ModDefinition def : definitions.values()) {
            for (String payload : def.payloads()) {
                entries.add(new PayloadEntry(payload, def));
            }
        }
        return List.copyOf(entries);
    }

    private static void warn(String message) {
        TGPlatform.getInstance().getLogger().warning("[mods.yml] " + message);
    }

    public record PayloadEntry(@NotNull String normalizedPayload, @NotNull ModDefinition mod) {
    }

    public record Snapshot(
            @NotNull Map<String, ModDefinition> definitions,
            @NotNull List<PayloadEntry> payloadEntries,
            @NotNull PunishmentCommand kickCommand,
            @NotNull PunishmentCommand banCommand,
            @NotNull PunishmentCommand unresponsiveKickCommand,
            @NotNull Duration kickThenBanWindow,
            int modListLimit,
            @NotNull String modListOverflowFormat
    ) {

        static Snapshot empty() {
            return new Snapshot(
                    Map.of(),
                    List.of(),
                    PunishmentCommand.parse("[KICK] kick %tg_player%"),
                    PunishmentCommand.parse("[BAN] ban %tg_player%"),
                    PunishmentCommand.parse("[KICK] kick %tg_player% Failed to verify client modifications."),
                    Duration.ofMinutes(30),
                    8,
                    " (+%tg_mod_overflow_count% more)"
            );
        }

        public @Nullable ModDefinition matchPayload(@NotNull String normalizedChannel) {
            for (PayloadEntry entry : payloadEntries) {
                if (normalizedChannel.contains(entry.normalizedPayload())) {
                    return entry.mod();
                }
            }
            return null;
        }
    }
}
