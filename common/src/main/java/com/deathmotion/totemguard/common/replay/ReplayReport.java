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

package com.deathmotion.totemguard.common.replay;

import com.deathmotion.totemguard.api.config.key.ConfigKey;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.config.key.MessagesKeys;
import com.deathmotion.totemguard.common.replay.playback.ReplayResult;
import net.kyori.adventure.text.Component;

import java.util.Map;
import java.util.function.Consumer;

public final class ReplayReport {

    private ReplayReport() {
    }

    public static void send(Consumer<Component> sink, String name, ReplayResult result) {
        if (result.error() != null) {
            tell(sink, MessagesKeys.REPLAY_RUN_FAILED, Map.of("tg_error", result.error()));
            return;
        }

        tell(sink, MessagesKeys.REPLAY_RUN_FINISHED, Map.of(
                "tg_file", name,
                "tg_ticks", result.ticks(),
                "tg_judged", result.judged(),
                "tg_coasted", result.coasted(),
                "tg_declined", result.declined(),
                "tg_flags", result.flags().size(),
                "tg_elapsed", result.elapsedMillis() + "ms"));

        if (result.truncated()) {
            tell(sink, MessagesKeys.REPLAY_RUN_TRUNCATED, Map.of());
        }

        ReplayResult.Verification verification = result.verification();
        switch (verification.status()) {
            case MATCHED -> tell(sink, MessagesKeys.REPLAY_VERIFY_MATCHED, Map.of(
                    "tg_matched", verification.compared(), "tg_total", verification.total()));
            case DIVERGED -> tell(sink, MessagesKeys.REPLAY_VERIFY_DIVERGED, Map.of(
                    "tg_tick", verification.divergentTick(),
                    "tg_field", String.valueOf(verification.field())));
            case SKIPPED -> tell(sink, MessagesKeys.REPLAY_VERIFY_SKIPPED,
                    Map.of("tg_reason", String.valueOf(verification.skipReason())));
        }

        ReplayResult.FlagCheck flagCheck = result.flagCheck();
        switch (flagCheck.status()) {
            case ABSENT -> {
            }
            case MATCHED -> tell(sink, MessagesKeys.REPLAY_FLAGS_MATCHED,
                    Map.of("tg_flags", flagCheck.recorded()));
            case DIVERGED -> tell(sink, MessagesKeys.REPLAY_FLAGS_DIVERGED, Map.of(
                    "tg_recorded", flagCheck.recorded(),
                    "tg_replayed", flagCheck.replayed(),
                    "tg_difference", String.valueOf(flagCheck.difference())));
        }

        ReplayResult.WorldCheck worldCheck = result.worldCheck();
        switch (worldCheck.status()) {
            case ABSENT -> {
            }
            case MATCHED -> tell(sink, MessagesKeys.REPLAY_WORLD_MATCHED,
                    Map.of("tg_columns", worldCheck.columnsLoaded()));
            case DIVERGED -> tell(sink, MessagesKeys.REPLAY_WORLD_DIVERGED,
                    Map.of("tg_difference", String.valueOf(worldCheck.difference())));
            case UNSETTLED -> tell(sink, MessagesKeys.REPLAY_WORLD_UNSETTLED, Map.of());
            case VACUOUS -> tell(sink, MessagesKeys.REPLAY_WORLD_VACUOUS,
                    Map.of("tg_difference", String.valueOf(worldCheck.difference())));
        }

        if (result.vacuous()) {
            tell(sink, MessagesKeys.REPLAY_VACUOUS,
                    Map.of("tg_reasons", String.valueOf(result.declineReasons())));
        }
    }

    private static void tell(Consumer<Component> sink, ConfigKey<String> key, Map<String, Object> extras) {
        sink.accept(TGPlatform.getInstance().getMessageService().getComponent(key, extras));
    }
}
