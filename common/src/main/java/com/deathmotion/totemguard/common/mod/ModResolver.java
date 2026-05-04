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

import com.deathmotion.totemguard.api.mod.DetectedMod;
import com.deathmotion.totemguard.api.mod.ModAction;
import com.deathmotion.totemguard.api.mod.ModSeverity;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.check.impl.mods.Mod;
import com.deathmotion.totemguard.common.event.api.impl.TGModDetectionResolvedEventImpl;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.punishment.PunishmentCommand;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public final class ModResolver {

    private final TGPlatform platform;
    private final ModKickThenBanTracker kickThenBanTracker;
    private final ModLogAlertTracker logAlertTracker;

    public ModResolver(TGPlatform platform,
                       ModKickThenBanTracker kickThenBanTracker,
                       ModLogAlertTracker logAlertTracker) {
        this.platform = platform;
        this.kickThenBanTracker = kickThenBanTracker;
        this.logAlertTracker = logAlertTracker;
    }

    private static String friendlyName(ModAction action) {
        return switch (action) {
            case NONE -> "";
            case KICK -> "Kick";
            case KICK_THEN_BAN -> "Kick → Ban";
            case BAN -> "Ban";
        };
    }

    private static String styledLabel(ModAction action) {
        return switch (action) {
            case NONE -> "";
            case KICK -> " &#B8AC8F[&#FBAF00K&#B8AC8F]";
            case KICK_THEN_BAN -> " &#B8AC8F[&#FBAF00KB&#B8AC8F]";
            case BAN -> " &#B8AC8F[&#D4452CB&#B8AC8F]";
        };
    }

    public void resolve(@NotNull ModSession session) {
        Set<DetectedMod> mods = session.snapshotDetected();
        runResolution(session.player(), session.snapshot(), mods, false, action -> session.markResolvedAction(action));
    }

    public void resolveLate(@NotNull ModSession session, @NotNull DetectedMod late) {
        runResolution(session.player(), session.snapshot(), Set.of(late), true, action -> {
        });
    }

    public void resolveUnresponsive(@NotNull ModSession session) {
        TGPlayer player = session.player();
        ModRegistry.Snapshot snapshot = session.snapshot();
        ModTranslationDetector detector = session.translationDetector();

        Mod modCheck = player.getCheckManager().getManualCheck(Mod.class);
        if (modCheck == null || !modCheck.isEnabled()) return;

        Map<String, Object> extras = new HashMap<>();
        extras.put("tg_mod_list", "");
        extras.put("tg_mod_count", 0);
        extras.put("tg_mod_overflow_count", 0);
        extras.put("tg_mod_action", friendlyName(ModAction.KICK));
        extras.put("tg_mod_action_short", ModAction.KICK.shortLabel());
        extras.put("tg_mod_action_label", styledLabel(ModAction.KICK));
        extras.put("tg_mod_probes_sent", detector.sentCount());
        extras.put("tg_mod_probes_answered", detector.answeredCount());
        extras.put("tg_mod_probes_missing", detector.sentCount() - detector.answeredCount());

        String debug = "Unresponsive: " + detector.answeredCount() + "/" + detector.sentCount() + " probes answered";
        if (!modCheck.reportFlag(debug, extras)) return;

        platform.getPunishmentRepository().punishWith(
                modCheck,
                List.of(snapshot.unresponsiveKickCommand()),
                debug,
                extras
        );
    }

    private void runResolution(TGPlayer player,
                               ModRegistry.Snapshot snapshot,
                               Set<DetectedMod> mods,
                               boolean late,
                               java.util.function.Consumer<ModAction> actionSink) {
        Mod modCheck = player.getCheckManager().getManualCheck(Mod.class);
        if (modCheck == null || !modCheck.isEnabled()) return;
        if (mods.isEmpty()) {
            actionSink.accept(ModAction.NONE);
            return;
        }

        Set<String> warnedSet = kickThenBanTracker.activeWarning(player.getUuid());
        ModAction action = decideAction(mods, warnedSet);
        actionSink.accept(action);

        Set<DetectedMod> alertMods = mods;
        if (action == ModAction.NONE) {
            Set<String> alreadyLogged = logAlertTracker.alreadyLogged(player.getUuid());
            if (!alreadyLogged.isEmpty()) {
                Set<DetectedMod> filtered = new LinkedHashSet<>();
                for (DetectedMod mod : mods) {
                    if (!alreadyLogged.contains(mod.id())) filtered.add(mod);
                }
                if (filtered.isEmpty()) return;
                alertMods = filtered;
            }
        }

        TGModDetectionResolvedEventImpl event = new TGModDetectionResolvedEventImpl(player, alertMods, action, late);
        platform.getEventRepository().post(event);
        if (event.isCancelled()) return;

        ModListRendering rendering = ModListRendering.build(alertMods, snapshot);
        Map<String, Object> extras = buildExtras(alertMods, action, rendering);
        String debug = buildDebug(rendering, late);

        if (!modCheck.reportFlag(debug, extras)) return;

        switch (action) {
            case NONE -> rememberLogged(player.getUuid(), alertMods);
            case KICK, KICK_THEN_BAN ->
                    dispatchPunishment(player, alertMods, modCheck, snapshot.kickCommand(), snapshot, debug, extras, false);
            case BAN ->
                    dispatchPunishment(player, alertMods, modCheck, snapshot.banCommand(), snapshot, debug, extras, true);
        }
    }

    private void rememberLogged(UUID uuid, Set<DetectedMod> mods) {
        Set<String> ids = new LinkedHashSet<>(mods.size());
        for (DetectedMod mod : mods) ids.add(mod.id());
        logAlertTracker.markLogged(uuid, ids);
    }

    private Map<String, Object> buildExtras(Set<DetectedMod> mods, ModAction action, ModListRendering rendering) {
        Map<String, Object> extras = new HashMap<>();
        extras.put("tg_mod_list", rendering.list());
        extras.put("tg_mod_count", mods.size());
        extras.put("tg_mod_overflow_count", rendering.overflowCount());
        extras.put("tg_mod_action", friendlyName(action));
        extras.put("tg_mod_action_short", action.shortLabel());
        extras.put("tg_mod_action_label", styledLabel(action));
        extras.put("tg_mod", mods.iterator().next().id());
        return extras;
    }

    private String buildDebug(ModListRendering rendering, boolean late) {
        return late ? rendering.list() + " (late)" : rendering.list();
    }

    private ModAction decideAction(Set<DetectedMod> mods, Set<String> warnedSet) {
        ModSeverity highest = null;
        boolean overlapsWarning = false;

        for (DetectedMod mod : mods) {
            if (highest == null || severityRank(mod.severity()) > severityRank(highest)) {
                highest = mod.severity();
            }
            if (warnedSet.contains(mod.id())) overlapsWarning = true;
        }

        if (highest == null) return ModAction.NONE;

        return switch (highest) {
            case LOG -> ModAction.NONE;
            case KICK -> ModAction.KICK;
            case BAN -> ModAction.BAN;
            case KICK_THEN_BAN -> overlapsWarning ? ModAction.BAN : ModAction.KICK_THEN_BAN;
        };
    }

    private int severityRank(ModSeverity severity) {
        return switch (severity) {
            case LOG -> 0;
            case KICK -> 1;
            case KICK_THEN_BAN -> 2;
            case BAN -> 3;
        };
    }

    private void dispatchPunishment(TGPlayer player,
                                    Set<DetectedMod> mods,
                                    Mod modCheck,
                                    PunishmentCommand command,
                                    ModRegistry.Snapshot snapshot,
                                    String debug,
                                    Map<String, Object> extras,
                                    boolean ban) {
        platform.getPunishmentRepository().punishWith(modCheck, List.of(command), debug, extras);

        if (ban) {
            kickThenBanTracker.clear(player.getUuid());
        } else if (anyKickThenBan(mods)) {
            Set<String> warned = new LinkedHashSet<>();
            for (DetectedMod mod : mods) {
                if (mod.severity() == ModSeverity.KICK_THEN_BAN) warned.add(mod.id());
            }
            if (!warned.isEmpty()) {
                kickThenBanTracker.recordWarning(player.getUuid(), warned, snapshot.kickThenBanWindow());
            }
        }
    }

    private boolean anyKickThenBan(Set<DetectedMod> mods) {
        for (DetectedMod mod : mods) {
            if (mod.severity() == ModSeverity.KICK_THEN_BAN) return true;
        }
        return false;
    }

    private record ModListRendering(String list, int overflowCount) {

        static ModListRendering build(Set<DetectedMod> mods, ModRegistry.Snapshot snapshot) {
            int limit = Math.max(1, snapshot.modListLimit());
            List<String> ids = new ArrayList<>(mods.size());
            for (DetectedMod mod : mods) ids.add(mod.id());

            int shown = Math.min(ids.size(), limit);
            int overflow = ids.size() - shown;
            String joined = String.join(", ", ids.subList(0, shown));

            if (overflow > 0) {
                String suffix = snapshot.modListOverflowFormat()
                        .replace("%tg_mod_overflow_count%", Integer.toString(overflow));
                joined = joined + suffix;
            }
            return new ModListRendering(joined, overflow);
        }
    }
}
