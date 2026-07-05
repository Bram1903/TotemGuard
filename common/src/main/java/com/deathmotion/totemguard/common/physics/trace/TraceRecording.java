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

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.config.view.ConfigView;
import com.deathmotion.totemguard.common.physics.area.AreaBounds;
import com.deathmotion.totemguard.common.physics.ground.GroundFacts;
import com.deathmotion.totemguard.common.physics.input.PlayerInput;
import com.deathmotion.totemguard.common.physics.medium.MediumSample;
import com.deathmotion.totemguard.common.physics.preset.PhysicsDebugLevel;
import com.deathmotion.totemguard.common.physics.collision.ContactReport;
import com.deathmotion.totemguard.common.physics.verdict.PhysicsVerdict;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.world.block.BlockReader;
import org.jetbrains.annotations.Nullable;

public final class TraceRecording {

    private final TGPlayer player;
    private final TraceFrame frame = new TraceFrame();
    private final TraceDump dumper = new TraceDump();
    private TickRecorder recorder;
    private long tickCounter;

    public TraceRecording(TGPlayer player) {
        this.player = player;
    }

    public @Nullable TickRecorder recorder() {
        return recorder;
    }

    public void reset() {
        if (recorder != null) recorder.clear();
        tickCounter = 0;
    }

    public boolean dumpNow(String cause) {
        if (recorder == null) return false;
        return dumper.dump(player, recorder, cause);
    }

    public void record(ConfigView view,
                       @Nullable ContactReport contact, @Nullable MediumSample sample,
                       @Nullable GroundFacts ground, @Nullable PlayerInput input,
                       AreaBounds bounds, PhysicsVerdict verdict,
                       BlockReader reader, double buffer, double engineFall) {
        tickCounter++;
        PhysicsDebugLevel level = view.physicsDebugLevel();
        if (!level.recording()) return;
        if (recorder == null) recorder = new TickRecorder();

        frame.tick = tickCounter;
        frame.obsX = verdict.observedX();
        frame.obsY = verdict.observedY();
        frame.obsZ = verdict.observedZ();
        frame.centerX = bounds.centerX();
        frame.centerZ = bounds.centerZ();
        frame.radius = bounds.radius();
        frame.ceiling = bounds.ceiling();
        frame.floor = bounds.floor() - bounds.descentSlack();
        frame.horizontalExcess = verdict.horizontalExcess();
        frame.ascentExcess = verdict.ascentExcess();
        frame.descentExcess = verdict.descentExcess();
        frame.phaseExcess = verdict.phaseExcess();
        frame.outcome = (byte) verdict.outcome().ordinal();
        frame.reason = verdict.declineReason() == null ? -1 : (byte) verdict.declineReason().ordinal();
        frame.breach = verdict.breach() == null ? -1 : (byte) verdict.breach().ordinal();
        frame.medium = (byte) verdict.medium().ordinal();
        frame.ground = (byte) verdict.ground().ordinal();
        frame.flags = flags(contact, sample, ground, input, verdict);
        frame.supportGap = contact != null ? Math.min(contact.nearestSupportGap(), 9.999) : 0.0;
        frame.ceilingClearance = contact != null ? contact.ceilingClearance() : 0.0;
        frame.reads = reader.readsThisTick();
        frame.misses = reader.missesThisTick();
        frame.uncertainHits = reader.uncertainHitsThisTick();
        frame.buffer = buffer;
        frame.engineFall = engineFall;
        frame.mitigation = (byte) ((verdict.mitigation().triggered() ? 1 : 0)
                | (verdict.mitigation().setbackIssued() ? 2 : 0)
                | (verdict.mitigation().setbackSkipped() ? 4 : 0)
                | (verdict.mitigation().inventoryClosed() ? 8 : 0));
        recorder.record(frame);

        if (level == PhysicsDebugLevel.TRACE) {
            TGPlatform.getInstance().getLogger().info(
                    "[PhysicsTrace] " + player.getUser().getName() + " " + TraceFormatter.format(frame));
        }
        if (verdict.mitigation().triggered() || verdict.fall().violation()) {
            dumper.dump(player, recorder, verdict.breach() != null ? verdict.breach().name() : "fall");
        }
    }

    private static int flags(@Nullable ContactReport contact, @Nullable MediumSample sample,
                             @Nullable GroundFacts ground, @Nullable PlayerInput input,
                             PhysicsVerdict verdict) {
        int flags = 0;
        if (input != null) {
            if (input.sprinting()) flags |= TraceFrame.FLAG_SPRINT;
            if (input.sneaking()) flags |= TraceFrame.FLAG_SNEAK;
            if (input.jumpPossible()) flags |= TraceFrame.FLAG_JUMP_POSSIBLE;
        }
        if (verdict.inventoryOpen()) flags |= TraceFrame.FLAG_INVENTORY_OPEN;
        if (contact != null) {
            if (contact.wallNear()) flags |= TraceFrame.FLAG_WALL_NEAR;
            if (contact.startOverlapping()) flags |= TraceFrame.FLAG_START_OVERLAP;
            if (contact.stepUsedHeight() > 0.0) flags |= TraceFrame.FLAG_STEP_USED;
        }
        if (sample != null) {
            if (sample.stuck()) flags |= TraceFrame.FLAG_STUCK;
            if (sample.bubbleAscent() > 0.0) flags |= TraceFrame.FLAG_BUBBLE;
        }
        if (ground != null) {
            if (ground.groundedEnd()) flags |= TraceFrame.FLAG_GROUNDED_END;
            if (ground.arrested()) flags |= TraceFrame.FLAG_ARRESTED;
        }
        if (verdict.knockbackConsumed()) flags |= TraceFrame.FLAG_ALT_CENTER;
        return flags;
    }
}
