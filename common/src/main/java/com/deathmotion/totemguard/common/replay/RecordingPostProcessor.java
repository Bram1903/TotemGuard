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

import com.deathmotion.totemguard.common.replay.capture.RecordingSession;
import com.deathmotion.totemguard.common.replay.playback.ReplayResult;
import com.deathmotion.totemguard.common.replay.playback.ReplayRun;
import com.deathmotion.totemguard.common.replay.prune.PruneScan;
import com.deathmotion.totemguard.common.replay.prune.RecordingPruner;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.logging.Logger;

public final class RecordingPostProcessor {

    private RecordingPostProcessor() {
    }

    public static void run(RecordingSession session, Logger logger) {
        Path file = session.getFile();
        String name = file.getFileName().toString();
        try {
            PruneScan scan = new PruneScan();
            ReplayResult result = ReplayRun.run(file, scan);

            if (result.error() != null) {
                logger.warning("[Replay] " + name + " did not replay: " + result.error());
                return;
            }
            logCoverage(logger, name, result);
            logVerification(logger, name, result);
            logWorldCheck(logger, name, result);

            prune(session, file, name, scan, result, logger);
        } catch (Exception failure) {
            logger.warning("[Replay] post-processing " + name + " failed: " + failure);
        }
    }

    private static void prune(RecordingSession session, Path file, String name, PruneScan scan,
                              ReplayResult before, Logger logger) throws IOException {
        Set<Long> droppable = scan.droppableFrames();
        if (droppable.isEmpty()) return;

        Path backup = file.resolveSibling(file.getFileName() + ".unpruned");
        Files.copy(file, backup, StandardCopyOption.REPLACE_EXISTING);
        try {
            long dropped = RecordingPruner.prune(file, droppable);
            if (dropped == 0) return;

            ReplayResult after = ReplayRun.run(file);
            String regression = regression(before, after);
            if (regression != null) {
                Files.move(backup, file, StandardCopyOption.REPLACE_EXISTING);
                logger.warning("[Replay] " + name + " kept every frame: pruning changed the verdicts ("
                        + regression + ").");
                return;
            }

            session.applyPruneResult(dropped);
            logger.info("[Replay] " + name + " pruned " + dropped + " frames the session never went near.");
        } finally {
            Files.deleteIfExists(backup);
        }
    }

    private static @Nullable String regression(ReplayResult before, ReplayResult after) {
        if (after.error() != null) return after.error();
        if (after.verification().status() == ReplayResult.Verification.Status.DIVERGED) {
            return after.verification().field();
        }
        if (after.worldCheck().status() == ReplayResult.WorldCheck.Status.DIVERGED) {
            return "the world no longer rebuilds: " + after.worldCheck().difference();
        }
        if (before.digests().size() != after.digests().size()) {
            return "tick count (" + before.digests().size() + " before, " + after.digests().size() + " after)";
        }
        for (int i = 0; i < before.digests().size(); i++) {
            String field = before.digests().get(i).firstDifference(after.digests().get(i));
            if (field != null) return "tick " + before.digests().get(i).tick() + " " + field;
        }
        return null;
    }

    private static void logCoverage(Logger logger, String name, ReplayResult result) {
        logger.info("[Replay] " + name + " " + result.ticks() + " ticks"
                + " judged=" + result.judged()
                + " coasted=" + result.coasted()
                + " declined=" + result.declined()
                + " flags=" + result.settledFlags().size()
                + (result.warmUpFlags() == 0 ? "" : " (+" + result.warmUpFlags() + " in the warm-up)")
                + (result.truncated() ? " (truncated)" : ""));
        if (!result.vacuous()) return;
        logger.warning("[Replay] " + name + " judged no ticks at all, so it proves nothing."
                + " Top decline reasons: " + result.declineReasons());
    }

    private static void logWorldCheck(Logger logger, String name, ReplayResult result) {
        ReplayResult.WorldCheck check = result.worldCheck();
        switch (check.status()) {
            case ABSENT -> {
            }
            case MATCHED -> logger.info("[Replay] " + name + " prologue rebuilt the world exactly ("
                    + check.columnsLoaded() + " columns sampled).");
            case DIVERGED -> logger.warning("[Replay] " + name
                    + " retained prologue rebuilt a different world: " + check.difference());
            case UNSETTLED -> logger.warning("[Replay] " + name
                    + " world never settled, so the prologue could not be checked.");
            case VACUOUS -> logger.warning("[Replay] " + name
                    + " prologue was not checked: " + check.difference());
        }
    }

    private static void logVerification(Logger logger, String name, ReplayResult result) {
        ReplayResult.Verification verification = result.verification();
        switch (verification.status()) {
            case MATCHED -> logger.info("[Replay] " + name + " verified "
                    + verification.compared() + "/" + verification.total() + " ticks identical to capture.");
            case DIVERGED -> logger.warning("[Replay] " + name + " diverged from capture at tick "
                    + verification.divergentTick() + " (" + verification.field() + ").");
            case SKIPPED -> logger.info("[Replay] " + name + " verification skipped: " + verification.skipReason());
        }
    }
}
