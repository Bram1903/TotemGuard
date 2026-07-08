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

package com.deathmotion.totemguard.common.physics.fall;

import com.deathmotion.totemguard.common.config.view.ConfigView;
import com.deathmotion.totemguard.common.mitigation.MitigationService;
import com.deathmotion.totemguard.common.physics.MotionDefaults;
import com.deathmotion.totemguard.common.physics.ground.GroundFacts;
import com.deathmotion.totemguard.common.physics.medium.MediumSample;
import com.deathmotion.totemguard.common.physics.collision.ContactReport;
import com.deathmotion.totemguard.common.physics.verdict.BoundBreach;
import com.deathmotion.totemguard.common.physics.verdict.DeclineReason;
import com.deathmotion.totemguard.common.physics.verdict.FallFinding;
import com.deathmotion.totemguard.common.physics.verdict.TickOutcome;
import com.deathmotion.totemguard.common.player.data.Data;
import com.deathmotion.totemguard.common.world.block.BlockReader;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.protocol.world.states.defaulttags.BlockTags;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Set;

@Accessors(fluent = true)
public final class FallTracker {

    private static final double MIN_AVOIDED_DAMAGE = 1.0;
    private static final double MIDAIR_CLAIM_GAP = 1.0;
    private static final int LATE_CLAIM_WINDOW = 5;
    private static final double PENDING_FALL_EPS = 0.1;
    private static final double MAX_FALL_DISTANCE = 512.0;
    private static final double SUPPORT_BLOCK_OFFSET = 0.5000001;

    private static final Set<StateType> LENIENT_LANDING = Set.of(
            StateTypes.SLIME_BLOCK, StateTypes.HAY_BLOCK, StateTypes.HONEY_BLOCK,
            StateTypes.POWDER_SNOW, StateTypes.COBWEB, StateTypes.SWEET_BERRY_BUSH,
            StateTypes.POINTED_DRIPSTONE,
            StateTypes.CAULDRON, StateTypes.WATER_CAULDRON,
            StateTypes.LAVA_CAULDRON, StateTypes.POWDER_SNOW_CAULDRON);

    private final Data data;
    private final BlockReader reader;

    @Getter
    private double engineFall;
    private double serverFall;
    private double creditedDamage;
    private boolean provenSpoof;
    private int lateClaimTicks;
    private double pendingExpected;
    private boolean pendingProven;

    @Getter
    private boolean violationThisTick;
    @Getter
    private double avoidedDamage;
    @Getter
    private double fallDistance;
    @Getter
    private boolean damageApplied;

    public FallTracker(Data data, BlockReader reader) {
        this.data = data;
        this.reader = reader;
    }

    public void observe(TickOutcome outcome, DeclineReason reason, BoundBreach breach,
                        double dy, boolean claimedGround,
                        MediumSample sample, GroundFacts ground, ContactReport contact,
                        ConfigView view) {
        clearLatch();

        if (breach == BoundBreach.GROUNDSPOOF || breach == BoundBreach.FAST
                || reason == DeclineReason.FAST) {
            onUnscannedTick(dy, claimedGround, view);
            return;
        }
        if (outcome == TickOutcome.SEEDED) {
            abort();
            return;
        }
        if (outcome == TickOutcome.DECLINED) {
            onDeclined(reason);
            return;
        }
        if (sample == null || ground == null || contact == null) {
            abort();
            return;
        }
        onTick(dy, claimedGround, ground.groundedEnd(), sample, contact, view);
    }

    public FallFinding finding() {
        return new FallFinding(violationThisTick, fallDistance, avoidedDamage, damageApplied);
    }

    public void reset() {
        abort();
        clearLatch();
    }

    private void onDeclined(DeclineReason reason) {
        switch (reason) {
            case WITHHELD, DOUBLE_MOVE -> {
            }
            case RESYNC, TELEPORT -> {
                MitigationService service = data.getMitigationService();
                if (!service.setbackPending()) {
                    abort();
                } else if (reason == DeclineReason.RESYNC
                        && data.getMovementData().isLastFlyingWasTeleportResync()) {
                    onSetbackConfirm(service.pendingSetbackDy());
                }
            }
            default -> abort();
        }
    }

    private void onTick(double dy, boolean claimedGround, boolean groundedEnd,
                        MediumSample sample, ContactReport contact, ConfigView view) {
        if (bailed(sample, contact)) {
            abort();
            return;
        }

        if (claimedGround) {
            creditedDamage += fallDamage(serverFall);
            serverFall = 0.0;
            if (contact.nearestSupportGap() > MIDAIR_CLAIM_GAP && !groundedEnd) provenSpoof = true;
            resolvePending(view);
        } else if (dy < 0.0) {
            serverFall = accumulate(serverFall, dy);
        }

        if (data.isGliding() && dy > -0.5) {
            engineFall = Math.min(engineFall, 1.0);
            serverFall = Math.min(serverFall, 1.0);
        }

        if (groundedEnd) {
            if (engineFall > 0.0) {
                double expected = fallDamage(engineFall);
                if (expected >= MIN_AVOIDED_DAMAGE && !lenientLandingBlock()) {
                    if (claimedGround) {
                        resolve(expected, engineFall, provenSpoof, view);
                    } else {
                        resolvePending(view);
                        pendingExpected = expected;
                        pendingProven = provenSpoof;
                        fallDistance = engineFall;
                        lateClaimTicks = LATE_CLAIM_WINDOW;
                    }
                } else if (lateClaimTicks <= 0) {
                    creditedDamage = 0.0;
                }
                engineFall = 0.0;
                provenSpoof = false;
            }
        } else if (dy < 0.0) {
            engineFall = accumulate(engineFall, dy);
            if (dy < -PENDING_FALL_EPS) resolvePending(view);
        }

        tickLateClaim(view);
    }

    private void onUnscannedTick(double dy, boolean claimedGround, ConfigView view) {
        if (claimedGround) {
            creditedDamage += fallDamage(serverFall);
            serverFall = 0.0;
            provenSpoof = true;
            resolvePending(view);
        } else if (dy < 0.0) {
            serverFall = accumulate(serverFall, dy);
        }
        if (dy < 0.0) engineFall = accumulate(engineFall, dy);
        tickLateClaim(view);
    }

    private void onSetbackConfirm(double teleportDy) {
        serverFall = 0.0;
        if (teleportDy < 0.0) {
            engineFall = 0.0;
        } else {
            engineFall = Math.min(MAX_FALL_DISTANCE, Math.max(0.0, engineFall - teleportDy));
        }
        provenSpoof = true;
    }

    private void abort() {
        engineFall = 0.0;
        serverFall = 0.0;
        creditedDamage = 0.0;
        provenSpoof = false;
        lateClaimTicks = 0;
        pendingExpected = 0.0;
        pendingProven = false;
    }

    private void clearLatch() {
        violationThisTick = false;
        avoidedDamage = 0.0;
        damageApplied = false;
    }

    private static double accumulate(double fall, double dy) {
        return Math.min(MAX_FALL_DISTANCE, fall - dy);
    }

    private void tickLateClaim(ConfigView view) {
        if (lateClaimTicks <= 0 || --lateClaimTicks > 0) return;
        resolve(pendingExpected, fallDistance, true, view);
        pendingExpected = 0.0;
        pendingProven = false;
    }

    private void resolvePending(ConfigView view) {
        if (lateClaimTicks <= 0) return;
        lateClaimTicks = 0;
        resolve(pendingExpected, fallDistance, pendingProven, view);
        pendingExpected = 0.0;
        pendingProven = false;
    }

    private void resolve(double expected, double distance, boolean proven, ConfigView view) {
        double avoided = expected - creditedDamage;
        creditedDamage = 0.0;
        if (!proven || avoided < MIN_AVOIDED_DAMAGE) return;

        violationThisTick = true;
        avoidedDamage = avoided;
        fallDistance = distance;
        if (view.physicsEngineFallDamage()) {
            damageApplied = data.getMitigationService().dealFallDamage(avoided);
        }
    }

    private boolean bailed(MediumSample sample, ContactReport contact) {
        if (sample.fluid() || sample.climbable() || sample.stuck()) return true;
        if (contact.supportBounce() > 0.0) return true;
        if (data.getEffectData().hasSlowFalling() || data.getEffectData().hasLevitation()) return true;
        GameMode gameMode = data.getGameMode();
        return gameMode == GameMode.CREATIVE || gameMode == GameMode.SPECTATOR;
    }

    private double fallDamage(double distance) {
        if (distance <= 0.0) return 0.0;
        double jumpBoost = data.getEffectData().hasJumpBoost()
                ? data.getEffectData().jumpBoostAmplifier() + 1
                : 0.0;
        double safe = Math.max(data.getAttributeData().safeFallDistance(),
                MotionDefaults.SAFE_FALL_DISTANCE + jumpBoost);
        double over = distance - safe;
        if (over <= 0.0) return 0.0;
        return Math.ceil(over * data.getAttributeData().fallDamageMultiplier());
    }

    private boolean lenientLandingBlock() {
        Location current = data.getMovementData().getCurrent();
        double half = data.getAttributeData().width() / 2.0;
        int feetY = floor(current.getY());
        int belowY = floor(current.getY() - SUPPORT_BLOCK_OFFSET);
        for (int px = floor(current.getX() - half); px <= floor(current.getX() + half); px++) {
            for (int pz = floor(current.getZ() - half); pz <= floor(current.getZ() + half); pz++) {
                if (lenientType(reader.state(px, feetY, pz).getType())) return true;
                if (belowY != feetY && lenientType(reader.state(px, belowY, pz).getType())) return true;
            }
        }
        return false;
    }

    private static boolean lenientType(StateType type) {
        return LENIENT_LANDING.contains(type) || BlockTags.BEDS.contains(type);
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }
}
