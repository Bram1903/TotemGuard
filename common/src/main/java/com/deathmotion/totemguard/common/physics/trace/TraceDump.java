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

package com.deathmotion.totemguard.common.physics.trace;

import com.deathmotion.totemguard.common.physics.EngineActor;
import com.deathmotion.totemguard.common.physics.preset.PhysicsDebugContext;

import java.util.Set;
import java.util.logging.Logger;

public final class TraceDump {

    private static final int DUMP_ROWS = 40;
    private static final long RATE_LIMIT_NANOS = 5_000_000_000L;

    private final Logger logger;
    private long lastDumpNanos;

    public TraceDump(Logger logger) {
        this.logger = logger;
    }

    public boolean dump(EngineActor actor, TickRecorder recorder, String cause, Set<PhysicsDebugContext> contexts) {
        long now = System.nanoTime();
        if (now - lastDumpNanos < RATE_LIMIT_NANOS) return false;
        lastDumpNanos = now;

        logger.info("[PhysicsTrace] " + actor.name()
                + " cause=" + cause
                + " client=" + actor.clientVersion().getReleaseName()
                + " ticks=" + recorder.size());
        recorder.forEachRecent(DUMP_ROWS, frame ->
                logger.info("[PhysicsTrace] " + TraceFormatter.format(frame, contexts)));
        return true;
    }
}
