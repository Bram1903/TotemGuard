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

package com.deathmotion.totemguard.common.physics.simulation;

import com.deathmotion.totemguard.common.config.view.ConfigView;
import com.deathmotion.totemguard.common.physics.EngineActor;
import com.deathmotion.totemguard.common.physics.EngineContext;
import com.deathmotion.totemguard.common.physics.MotionDefaults;
import com.deathmotion.totemguard.common.physics.VersionGates;
import com.deathmotion.totemguard.common.physics.area.*;
import com.deathmotion.totemguard.common.physics.body.PlayerBody;
import com.deathmotion.totemguard.common.physics.body.SwimTracker;
import com.deathmotion.totemguard.common.physics.collision.*;
import com.deathmotion.totemguard.common.physics.control.ControlEnvelope;
import com.deathmotion.totemguard.common.physics.fall.FallTracker;
import com.deathmotion.totemguard.common.physics.ground.GroundFacts;
import com.deathmotion.totemguard.common.physics.ground.GroundResolver;
import com.deathmotion.totemguard.common.physics.ground.GroundState;
import com.deathmotion.totemguard.common.physics.medium.*;
import com.deathmotion.totemguard.common.physics.medium.model.LandModel;
import com.deathmotion.totemguard.common.physics.mitigation.MitigationTracker;
import com.deathmotion.totemguard.common.physics.phase.EmbedExemptions;
import com.deathmotion.totemguard.common.physics.phase.PhaseTracker;
import com.deathmotion.totemguard.common.physics.prescan.GroundSpoofDetector;
import com.deathmotion.totemguard.common.physics.prescan.TrustTracker;
import com.deathmotion.totemguard.common.physics.preset.PhysicsPreset;
import com.deathmotion.totemguard.common.physics.push.EntityPushTracker;
import com.deathmotion.totemguard.common.physics.push.KnockbackTracker;
import com.deathmotion.totemguard.common.physics.push.PistonWindow;
import com.deathmotion.totemguard.common.physics.push.RiptideWindow;
import com.deathmotion.totemguard.common.physics.rules.*;
import com.deathmotion.totemguard.common.physics.rules.spawn.*;
import com.deathmotion.totemguard.common.physics.silence.HoverDetector;
import com.deathmotion.totemguard.common.physics.silence.MovementSilenceTracker;
import com.deathmotion.totemguard.common.physics.silence.OwedMotionDetector;
import com.deathmotion.totemguard.common.physics.trace.TickRecorder;
import com.deathmotion.totemguard.common.physics.trace.TraceFrame;
import com.deathmotion.totemguard.common.physics.trace.TraceRecording;
import com.deathmotion.totemguard.common.physics.verdict.*;
import com.deathmotion.totemguard.common.player.data.Data;
import com.deathmotion.totemguard.common.player.data.ExternalVelocityData;
import com.deathmotion.totemguard.common.player.data.MovementData;
import com.deathmotion.totemguard.common.util.ClientMath;
import com.deathmotion.totemguard.common.world.WorldMirror;
import com.deathmotion.totemguard.common.world.block.BlockReader;
import com.deathmotion.totemguard.common.world.shape.ShapeQuery;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;

@Accessors(fluent = true)
public final class SelfSimulation {

    private static final double DEEP_FALL_POWDER_SNOW = 2.5;
    private static final double HARVEST_HORIZONTAL_MARGIN = 0.3;
    private static final double HARVEST_UP_MARGIN = 2.1;
    private static final double HARVEST_DOWN_MARGIN = 2.6;
    private static final double HOVER_EXCESS = MotionDefaults.GRAVITY;
    private static final double SILENCE_EXCESS = MotionDefaults.GRAVITY;
    private static final int HOVER_SETBACK_LIMIT = 2;
    private static final double STEP_FROM_FALL_EPS = 1.0e-4;
    private static final int STUCK_SETTLE_SCANS = 3;

    private final EngineActor actor;
    private final Data data;
    private final WorldMirror world;
    private final BlockReader reader;
    private final EngineContext context;
    private final VersionGates gates;

    private final CarriedHypotheses carried = new CarriedHypotheses();
    private final AreaBounds[] boundsSlots = new AreaBounds[CarriedHypotheses.CAPACITY];
    private final JudgedExcess[] excessSlots = new JudgedExcess[CarriedHypotheses.CAPACITY];
    private final ResidualCarry carry = new ResidualCarry();
    private final ColliderBuffer colliders = new ColliderBuffer();
    private final TraitSampler traits = new TraitSampler();
    private final CollisionSweep sweep = new CollisionSweep();
    private final ContactReport contact = new ContactReport();
    private final GroundResolver groundResolver = new GroundResolver();
    private final SupportingBlockTracker supportTracker;
    private final MediumSample sample = new MediumSample();
    private final StuckFactor stuckFactor = new StuckFactor();
    private final BubbleLift bubble = new BubbleLift();
    private final KnockbackTracker knockback;
    private final PistonWindow pistons;
    private final SqueezeOutRule squeezeOut = new SqueezeOutRule();
    private final RiptideWindow riptide;
    private final PhaseTracker phase = new PhaseTracker();
    private final GroundSpoofDetector groundSpoof = new GroundSpoofDetector();
    private final SwimTracker swim = new SwimTracker();
    private final EmbedExemptions exemptions = new EmbedExemptions();
    private final PlayerBody body;
    private final GlideExitRule glideExit = new GlideExitRule();
    private final RiptideGlideRule riptideGlide = new RiptideGlideRule();
    private final BedBounceRule bedBounce = new BedBounceRule();
    private final RiseObligation obligation = new RiseObligation();
    private final TrustTracker trust = new TrustTracker();
    private final TickGate gate = new TickGate();
    private final HoverDetector hover = new HoverDetector();
    private final OwedMotionDetector owedRiseQuiet = new OwedMotionDetector();
    private final OwedMotionDetector owedFallQuiet = new OwedMotionDetector();
    private final MovementSilenceTracker silence = new MovementSilenceTracker();
    private final EntityPushTracker push = new EntityPushTracker();
    private final TurnMomentumTracker momentum = new TurnMomentumTracker();
    private final TickState state = new TickState();
    private final TickTaints taints = new TickTaints();
    private final PreviousTick previous = new PreviousTick();
    private final SpawnQueue spawns = new SpawnQueue();
    private final AreaAdvance advanceRules;
    private final MitigationTracker mitigation;
    private final FallTracker fall;
    private final TraceRecording trace;

    private boolean initialized;
    private GameMode lastGameMode;
    private AreaBounds chosenBounds;
    private int chosenSlot;
    private int pendingTeleportQuietTicks;

    @Getter
    private PhysicsVerdict verdict = PhysicsVerdict.INITIAL;

    public SelfSimulation(EngineActor actor, Data data, WorldMirror world, EngineContext context,
                          VersionGates gates, TraceRecording trace) {
        this.actor = actor;
        this.data = data;
        this.world = world;
        this.reader = world.reader();
        this.context = context;
        this.gates = gates;
        this.supportTracker = new SupportingBlockTracker(gates.supportingBlock());
        this.body = new PlayerBody(data, actor, gates);
        this.knockback = new KnockbackTracker(data.getExternalVelocityData());
        this.pistons = new PistonWindow(data.getPistonData());
        this.riptide = new RiptideWindow(data);
        this.advanceRules = new AreaAdvance(gates, body.mediums(), supportTracker);
        this.mitigation = new MitigationTracker(data);
        this.fall = new FallTracker(data, reader);
        this.trace = trace;
        for (int slot = 0; slot < CarriedHypotheses.CAPACITY; slot++) {
            boundsSlots[slot] = new AreaBounds();
            excessSlots[slot] = JudgedExcess.NONE;
        }
        this.chosenBounds = boundsSlots[0];
    }

    private static GroundFacts airRegimeView(GroundFacts ground) {
        return new GroundFacts(GroundState.AIRBORNE, false, false, ground.arrested(),
                ground.recentlyGrounded(), ground.landingSupport(), ground.bounced(),
                ground.coyoteBlocked(), ground.wasFluid(), ground.startSlipMin(), ground.startSlipMax(),
                ground.startJumpMin(), ground.startJumpMax(), ground.supportGap());
    }

    private static double maxExcess(JudgedExcess excess) {
        return Math.max(excess.horizontal(), Math.max(excess.ascent(), excess.descent()));
    }

    public void onFlying() {
        ConfigView view = context.view();
        if (!view.physicsSimulateFlying() && data.isFlying()) {
            data.setSwimming(false);
            swim.reset();
            disengage();
            return;
        }
        PhysicsPreset preset = view.physicsPreset();
        MovementData movement = data.getMovementData();
        world.entities().advance();
        world.entities().reconcileAuthority(data.getVehicleId());
        reader.resetCounters();

        Location current = movement.getCurrent();
        Location previousLocation = movement.getPrevious();
        double dx = current.getX() - previousLocation.getX();
        double dy = current.getY() - previousLocation.getY();
        double dz = current.getZ() - previousLocation.getZ();
        trust.countFlying(movement.isLastFlyingWasTeleportResync());
        if (movement.isLastFlyingCarriedPosition()) {
            silence.onPositionPacket(System.nanoTime());
        }

        double half = body.halfWidth();
        double height = body.height();
        exemptions.prune(current.getX() - half, current.getY(), current.getZ() - half,
                current.getX() + half, current.getY() + height, current.getZ() + half);

        GameMode gameMode = data.getGameMode();
        if (gameMode != lastGameMode) {
            lastGameMode = gameMode;
            if (initialized) {
                seedEmbedExemptions(current, half, height);
                phase.seedGrace();
                phase.invalidateEmbed();
            }
        }

        previous.deltaZeroed = state.deltaZeroedDisplacement;
        state.reset(preset, sample, contact, dx, dy, dz,
                ClientMath.horizontalDistance(dx, dz), previous.claimedGround);
        try {
            if (!initialized) {
                initialized = true;
                groundResolver.seed(movement.isOnGround());
                groundResolver.displaced();
                carried.collapse(MotionArea.seeded(dx, dz, dy));
                verdict = PhysicsVerdict.INITIAL;
                phase.seedGrace();
                seedEmbedExemptions(current, half, height);
                return;
            }

            gate.evaluateSelf(data, world, reader, actor, movement, current, state.observedSpeed);
            if (gate.kind() == TickGate.Kind.DECLINE) {
                switch (gate.carriedMode()) {
                    case REST -> {
                        carried.collapse(MotionArea.rest());
                        state.deltaZeroedDisplacement = true;
                    }
                    case REST_JUMP_CEILING -> {
                        carried.collapse(new MotionArea(0.0, 0.0, 0.0, 0.0, jumpCeiling()));
                        state.deltaZeroedDisplacement = true;
                    }
                    case FROZEN -> {
                        double gravity = data.getAttributeData().gravity();
                        double jumpCeiling = jumpCeiling();
                        carried.mapInPlace(area -> gate.frozen(area, gravity, jumpCeiling));
                    }
                    case KEEP -> {
                    }
                }
                decline(gate.reason(), gate.reseed(), current, half, height);
                return;
            }
            if (gate.kind() == TickGate.Kind.FLAG) {
                flagDetection(gate.breach(), gate.horizontalExcess(), gate.verticalExcess());
                return;
            }

            scanTick(previousLocation, current, half, height, dx, dy, dz);
            if (reader.missesThisTick() > 0) {
                state.scanned = false;
                decline(DeclineReason.UNLOADED, true, current, half, height);
                return;
            }
            GroundFacts ground = groundResolver.resolve(dy, contact, sample.fluid(),
                    data.getAttributeData().stepHeight(), carried.minFloorVy(), data.isSneaking(),
                    supportTracker);
            state.ground = ground;
            double supportGap = contact.nearestSupportGap();
            if (!pistons.reaching()
                    && groundSpoof.provoked(movement.isOnGround(), ground.groundedEnd(), supportGap)) {
                flagDetection(BoundBreach.GROUNDSPOOF, 0.0,
                        Math.min(supportGap, HARVEST_DOWN_MARGIN) - GroundSpoofDetector.SUPPORT_GAP_EPS);
                return;
            }
            bubble.observe(sample.bubbleAscent());
            body.mediums().water().advanceEntryWindow(sample.fluid(), ground.wasFluid());
            stuckFactor.advanceWindow(sample.stuckAlongPath());

            TrustTracker.Trust trustKind = trust.classify(movement.isLastFlyingPositionChanged(),
                    movement.isLastFlyingWasDuplicate(), gates.endTick(),
                    data.getTeleportData().lastPacketWasTeleport(), preset.doubleMoveGraceTicks());
            state.doubleMove = trustKind == TrustTracker.Trust.COAST_DOUBLE
                    || trustKind == TrustTracker.Trust.JUDGED_DOUBLE;
            state.input = body.control().build(movement, contact, ground, sample.fluid(),
                    dx, dy, dz, state.doubleMove, sample.stuckVertical(), previous.powderSnowSwept);
            state.medium = body.medium(sample, false);
            state.landMedium = sample.landMedium();

            switch (trustKind) {
                case TRUSTED, TRUSTED_ZERO, JUDGED_DOUBLE -> judgeTick();
                case WITHHELD -> coastTick(true);
                case COAST_DOUBLE -> coastTick(false);
            }
        } finally {
            observeTail(view, preset, dy, true);
            previous.claimedGround = movement.isOnGround();
            previous.flying = data.isFlying();
            previous.powderSnowSwept = state.scanned && sample.powderSnowSwept();
            previous.stuckSettleScans = state.scanned ? previous.stuckSettleScans + 1 : 0;
            data.decayFlyChangeGrace();
        }
    }

    public void rewriteGroundClaim(PacketReceiveEvent event) {
        if (!state.scanned || state.ground == null) return;
        if (verdict.outcome() != TickOutcome.JUDGED) return;
        if (pistons.reaching()) return;
        boolean claimed = data.getMovementData().isOnGround();
        double gap = contact.nearestSupportGap();
        boolean rewriteTo;
        if (GroundSpoofDetector.claimProvablyFalse(claimed, state.ground.groundedEnd(), gap)) {
            rewriteTo = false;
        } else if (!claimed && state.ground.groundedEnd()
                && gap <= SupportingBlockTracker.SLAB_DEPTH && !contact.supportApproximate()) {
            rewriteTo = true;
        } else {
            return;
        }
        WrapperPlayClientPlayerFlying packet = new WrapperPlayClientPlayerFlying(event);
        packet.setOnGround(rewriteTo);
        event.markForReEncode(true);
    }

    public void onTickEnd() {
        boolean sawFlying = trust.onTickEnd();
        mitigation.clearTickFlags();
        if (sawFlying) {
            pendingTeleportQuietTicks = 0;
            if (verdict.mitigation().triggered() || verdict.fall().violation()) {
                verdict = verdict.withOutcome(MitigationOutcome.NONE, FallFinding.NONE, verdict.improperSprint());
            }
            return;
        }
        if (data.getTeleportData().hasPendingTeleport()) {
            pendingTeleportQuietTicks++;
        } else {
            pendingTeleportQuietTicks = 0;
        }
        if (!onSilentTick()) {
            mitigation.advancePendingSetback();
            data.getSetbackController().coastRise();
        }
    }

    public void onPong() {
        if (!initialized) return;
        ConfigView view = context.view();
        if (!view.physicsEngineSetback()) return;
        if (!gate.allowsSilent(data, world, view.physicsSimulateFlying(), data.getMovementData(),
                pendingTeleportQuietTicks)) return;
        if (!silence.probeWanted(System.nanoTime())) return;
        mitigation.probeSetback();
    }

    public void onBlockApplied(int x, int y, int z, int serverStateId) {
        if (!initialized) return;
        Location current = data.getMovementData().getCurrent();
        double half = body.halfWidth();
        double height = body.height();
        exemptions.onBlockApplied(reader, shapeQuery(current.getY()),
                current.getX() - half, current.getY(), current.getZ() - half,
                current.getX() + half, current.getY() + height, current.getZ() + half,
                x, y, z, reader.stateMap().toClientId(serverStateId));
    }

    public void onInventoryToggled() {
        clearHistory();
    }

    public void clearHistory() {
        gate.clearHistory();
        pendingTeleportQuietTicks = 0;
        groundSpoof.reset();
        state.deltaZeroedDisplacement = false;
        previous.clearZeroing();
        supportTracker.invalidate();
        hover.reset();
        owedRiseQuiet.reset();
        owedFallQuiet.reset();
        silence.reset(System.nanoTime());
        data.getSetbackController().clearRise();
        bubble.reset();
        stuckFactor.reset();
        body.mediums().reset();
        carry.clear();
        groundResolver.clearWindows();
        body.control().clear();
        phase.clear();
        exemptions.clear();
        bedBounce.reset();
        obligation.disarm();
        body.pose().clearHistory();
        momentum.invalidate();
        verdict = PhysicsVerdict.INITIAL;
    }

    public void reset() {
        initialized = false;
        push.reset();
        carried.collapse(MotionArea.rest());
        trust.reset();
        groundResolver.reset();
        supportTracker.reset();
        clearHistory();
        mitigation.reset();
        fall.reset();
        trace.reset();
        data.getExternalVelocityData().reset();
        data.getPistonData().reset();
        data.getEffectData().reset();
        data.getGlideData().reset();
        data.getFireworkData().reset();
        data.getVehicleData().reset();
        data.getUseItemData().reset();
    }

    public @Nullable TickRecorder recorder() {
        return trace.recorder();
    }

    public boolean dumpTrace(String cause) {
        return trace.dumpNow(cause);
    }

    public MitigationTracker mitigation() {
        return mitigation;
    }

    public FallTracker fallTracker() {
        return fall;
    }

    private boolean onSilentTick() {
        if (!initialized) return false;
        ConfigView view = context.view();
        MovementData movement = data.getMovementData();
        if (!gate.allowsSilent(data, world, view.physicsSimulateFlying(), movement,
                pendingTeleportQuietTicks)) return false;
        Location current = movement.getCurrent();
        PhysicsPreset preset = view.physicsPreset();

        reader.resetCounters();
        double half = body.halfWidth();
        double height = body.height();
        boolean deltaZeroed = state.deltaZeroedDisplacement;
        state.reset(preset, sample, contact, 0.0, 0.0, 0.0, 0.0, previous.claimedGround);
        state.deltaZeroedDisplacement = deltaZeroed;
        scanTick(current, current, half, height, 0.0, 0.0, 0.0);
        if (reader.missesThisTick() > 0) return false;
        GroundFacts ground = groundResolver.resolve(0.0, contact, sample.fluid(),
                data.getAttributeData().stepHeight(), carried.minFloorVy(), data.isSneaking(),
                supportTracker);
        state.ground = ground;
        supportTracker.invalidate();
        momentum.invalidate();
        state.input = body.control().build(movement, contact, ground, sample.fluid(),
                0.0, 0.0, 0.0, false, sample.stuckVertical(), previous.powderSnowSwept);
        state.medium = body.medium(sample, true);
        state.landMedium = sample.landMedium();
        taints.computeBase(state, data);
        obligation.prepare(taints.forbidsRiseObligation(), state.input,
                data.getAttributeData().jumpStrength() * ground.startJumpMin()
                        + state.input.jumpBoostPower(),
                sample, contact);

        observeAbsentKnockback();
        coastArea();

        double owedRise = state.quietOwedRise;
        double owedFall = state.quietOwedFall;
        data.getSetbackController().accumulateRise(owedRise, levitationTarget(state.input),
                contact.ceilingClearanceAny());
        double owedThreshold = Math.max(preset.verticalFlagEpsilon(), MotionDefaults.MOTION_ZERO_THRESHOLD);
        boolean risePinned = owedRiseQuiet.observe(owedRise, owedThreshold);
        boolean fallPinned = owedFallQuiet.observe(owedFall, owedThreshold);
        boolean airborne = airborneNow(ground, preset);
        boolean hovering = hover.observe(airborne, preset.hoverGraceTicks(), HOVER_SETBACK_LIMIT)
                || fallPinned;
        boolean starving = silence.clockTick();
        if (risePinned) {
            verdict = buildVerdict(TickOutcome.COASTED, null, BoundBreach.FORCED_RISE,
                    0.0, 0.0, 0.0, 0.0, 0.0, owedRise, 0.0, 0.0, state.input, ground);
        } else if (hovering) {
            verdict = buildVerdict(TickOutcome.COASTED, null, BoundBreach.HOVER,
                    0.0, 0.0, 0.0, 0.0, 0.0,
                    fallPinned ? owedFall : HOVER_EXCESS, 0.0, 0.0, state.input, ground);
        } else if (starving) {
            verdict = buildVerdict(TickOutcome.COASTED, null, BoundBreach.MOTION_SILENCE,
                    0.0, 0.0, 0.0, 0.0, 0.0, SILENCE_EXCESS, 0.0, 0.0, state.input, ground);
        } else {
            verdict = buildVerdict(TickOutcome.COASTED, DeclineReason.WITHHELD, null,
                    0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, state.input, ground);
        }

        previous.powderSnowSwept = state.scanned && sample.powderSnowSwept();
        observeTail(view, preset, 0.0, false);
        return true;
    }

    private void scanTick(Location from, Location to, double half, double height,
                          double dx, double dy, double dz) {
        ShapeQuery query = shapeQuery(to.getY());
        double minX = Math.min(from.getX(), to.getX()) - half - HARVEST_HORIZONTAL_MARGIN;
        double maxX = Math.max(from.getX(), to.getX()) + half + HARVEST_HORIZONTAL_MARGIN;
        double minY = Math.min(from.getY(), to.getY()) - HARVEST_DOWN_MARGIN;
        double maxY = Math.max(from.getY(), to.getY()) + height + HARVEST_UP_MARGIN;
        double minZ = Math.min(from.getZ(), to.getZ()) - half - HARVEST_HORIZONTAL_MARGIN;
        double maxZ = Math.max(from.getZ(), to.getZ()) + half + HARVEST_HORIZONTAL_MARGIN;
        ColliderCollector.fill(colliders, reader, world.entities(), query, exemptions,
                data.getPistonData(), -1,
                minX, minY, minZ, maxX, maxY, maxZ);
        trace.stageNearestStandable(world.entities(), to.getX(), to.getY(), to.getZ(), half);
        BorderColliders.fill(colliders, world.border(), from.getX(), from.getZ(), half,
                minX, minY, minZ, maxX, maxY, maxZ);
        pistons.setPlayerBox(
                Math.min(from.getX(), to.getX()) - half,
                Math.min(from.getY(), to.getY()),
                Math.min(from.getZ(), to.getZ()) - half,
                Math.max(from.getX(), to.getX()) + half,
                Math.max(from.getY(), to.getY()) + height,
                Math.max(from.getZ(), to.getZ()) + half);
        pistons.evaluate();
        sweep.resolve(colliders, contact,
                from.getX(), from.getY(), from.getZ(),
                half, height, dx, dy, dz,
                data.getAttributeData().stepHeight(), groundResolver.lastGroundedEnd());
        traits.sample(reader, contact, to.getX(), to.getY(), to.getZ(), half);
        MediumScan.sample(reader, sample,
                !data.isFlying(), false, world.dimension().dimensionType() != null
                        && world.dimension().dimensionType().isUltraWarm(),
                gates.modernFluidPush(), data.getEffectData().hasWeaving(),
                !previous.flying && previous.stuckSettleScans >= STUCK_SETTLE_SCANS, !previous.flying,
                from.getY() + body.lastEyeHeight(),
                from.getX() - half, from.getY(), from.getZ() - half,
                from.getX() + half, from.getY() + height, from.getZ() + half,
                Math.min(from.getX(), to.getX()) - half, Math.min(from.getY(), to.getY()),
                Math.min(from.getZ(), to.getZ()) - half,
                Math.max(from.getX(), to.getX()) + half,
                Math.max(from.getY(), to.getY()) + height,
                Math.max(from.getZ(), to.getZ()) + half);
        swim.update(data, sample);
        state.scanned = true;
    }

    private void judgeTick() {
        hover.onJudged();
        owedRiseQuiet.reset();
        owedFallQuiet.reset();
        MovementData movement = data.getMovementData();
        Location current = movement.getCurrent();
        double half = body.halfWidth();
        double height = body.lastHeight();
        GroundFacts ground = state.ground;
        ControlEnvelope input = state.input;
        MediumModel medium = state.medium;
        PhysicsPreset preset = state.preset;
        double dx = state.dx, dy = state.dy, dz = state.dz;

        supportTracker.update(colliders, reader, ground.groundedEnd(), contact.nearestSupportGap(),
                contact.supportApproximate(),
                current.getX() - half, current.getZ() - half,
                current.getX() + half, current.getZ() + half,
                current.getX(), current.getY(), current.getZ(), dx, dz);

        spawns.begin();
        KnockbackSetSpawnRule.spawn(data.getExternalVelocityData(), medium, input, carried, spawns);
        PistonLaunchSpawnRule.spawn(pistons.launchMask(), medium, input, carried, spawns);
        MotionArea preStep = carried.union();
        state.preCarriedX = preStep.centerX();
        state.preCarriedZ = preStep.centerZ();
        state.preCarriedFloor = preStep.floorVy();
        state.preCarriedCeil = preStep.ceilVy();

        boolean residualCarryWidened = carry.horizontal() > 0.0;
        taints.computeBase(state, data);
        obligation.prepare(taints.forbidsRiseObligation(), input,
                data.getAttributeData().jumpStrength() * ground.startJumpMin() + input.jumpBoostPower(),
                sample, contact);
        bedBounce.prepare(state.landMedium);
        double arrestCap = state.landMedium && dy <= -preset.verticalFlagEpsilon()
                ? Math.max(0.0, contact.nearestSupportGap() - preset.verticalNoisePad())
                : -1.0;

        if ((contact.startOverlapping() || exemptions.hasAny())
                && squeezeOut.evaluate(reader, shapeQuery(current.getY() - dy),
                current.getX() - dx, current.getY() - dy, current.getZ() - dz, half, height)) {
            SqueezeOutSpawnRule.spawn(squeezeOut, carried, spawns);
        }

        push.advance(world.entities(), medium.frictionMax(input, ground),
                Math.min(current.getX() - dx, current.getX()) - half, Math.min(current.getY() - dy, current.getY()),
                Math.min(current.getZ() - dz, current.getZ()) - half,
                Math.max(current.getX() - dx, current.getX()) + half,
                Math.max(current.getY() - dy, current.getY()) + height,
                Math.max(current.getZ() - dz, current.getZ()) + half,
                half, height);

        int chosen = 0;
        double best = Double.MAX_VALUE;
        boolean knockbackWidened = false;
        boolean riptideWidened = false;
        boolean pistonInfluence = false;
        double entityPushInfluence = 0.0;
        boolean offerBounceAlt = false;
        boolean boostApplied = false;
        boolean riseFloorPinned = false;
        GroundFacts claimAirGround = null;
        double boostStuckScale = sample.stuck() && !sample.fluid() ? sample.stuckHorizontal() : 1.0;
        boolean unobstructedFall = provenUnobstructedFall();
        boolean minFallPinned = false;
        for (int slot = 0; slot < CarriedHypotheses.CAPACITY; slot++) {
            if (!carried.live(slot)) continue;
            AreaBounds slotBounds = boundsSlots[slot];
            MotionArea area = carried.area(slot);
            GroundFacts slotGround = ground;
            if (state.airRegime(carried.kind(slot))) {
                if (claimAirGround == null) claimAirGround = airRegimeView(ground);
                slotGround = claimAirGround;
            }
            AreaExpander.grow(area, slotBounds, slotGround, state, stuckFactor, bubble, carry);
            if (unobstructedFall && carried.kind(slot) == CarriedHypotheses.Kind.MAIN) {
                slotBounds.ceiling(Math.min(slotBounds.ceiling(), area.ceilVy()));
                minFallPinned = true;
            }
            knockback.apply(slotBounds, preset.knockbackPad(),
                    carried.kind(slot) == CarriedHypotheses.Kind.KNOCKBACK_SET);
            boolean slotKnockback = slotBounds.hasAltCenter();
            riptide.apply(slotBounds, input);
            boolean slotRiptide = slotBounds.hasAltCenter() && !slotKnockback;
            boolean slotPiston = pistons.apply(slotBounds);
            glideExit.widenForExit(medium, data, body.mediums(), input, ground, area, slotBounds);
            riptideGlide.offer(medium, data, body.mediums(), input, ground, contact, area, slotBounds);
            double slotPush = push.apply(slotBounds);
            boolean slotBounce = bedBounce.applyTo(slotBounds,
                    slotBounds.centerX() - area.centerX(), slotBounds.centerZ() - area.centerZ());
            boolean slotBoost = SprintBoostRule.apply(input, slotBounds, boostStuckScale);
            double slotRise = obligation.riseFloorFor(slotBounds);
            if (slotRise > 0.0) {
                slotBounds.riseFloor(slotRise);
                riseFloorPinned = true;
            }
            if (slot == 0) {
                knockbackWidened = slotKnockback;
                riptideWidened = slotRiptide;
                pistonInfluence = slotPiston;
                entityPushInfluence = slotPush;
                offerBounceAlt = slotBounce;
                boostApplied = slotBoost;
            }
            excessSlots[slot] = AreaJudge.judge(slotBounds, dx, dy, dz, arrestCap);
            double slotExcess = maxExcess(excessSlots[slot]);
            if (slotExcess < best) {
                best = slotExcess;
                chosen = slot;
            }
        }

        JudgedExcess excess = excessSlots[chosen];
        AreaBounds chosenSlotBounds = boundsSlots[chosen];
        chosenBounds = chosenSlotBounds;
        chosenSlot = chosen;
        boolean knockbackHypothesisChosen = carried.kind(chosen) == CarriedHypotheses.Kind.KNOCKBACK_SET;
        bedBounce.onJudged(offerBounceAlt, excess);

        double horizontalExcess = excess.horizontal();
        boolean stepFromFall = false;
        if (state.landMedium && !ground.groundedStart() && !ground.groundedEnd()
                && dy > preset.verticalFlagEpsilon()
                && previous.supportGap >= 0.0
                && dy + previous.supportGap <= input.stepHeight() + STEP_FROM_FALL_EPS
                && previous.supportGap <= -state.preCarriedFloor + STEP_FROM_FALL_EPS) {
            stepFromFall = contact.nearestSupportGap() <= preset.verticalNoisePad()
                    || sweep.flushTopAt(colliders, current.getY(),
                    current.getX() - half, current.getZ() - half,
                    current.getX() + half, current.getZ() + half);
        }
        boolean stepped = contact.stepUsedHeight() > 0.0
                || (dy > preset.verticalFlagEpsilon() && ground.groundedEnd()
                && contact.stepCandidateMax() > 0.0)
                || stepFromFall;
        state.stepped = stepped;
        state.stepFromFall = stepFromFall;
        if (stepped && horizontalExcess > 0.0) {
            horizontalExcess = Math.max(0.0, horizontalExcess - preset.stepNoiseSlack());
        }
        double ascentExcess = excess.ascent();
        if (stepFromFall) ascentExcess = 0.0;

        double phaseExcess;
        if (state.landMedium && !pistonInfluence) {
            phaseExcess = phase.excess(contact.horizontalCrossingDepth(), contact.embedDepth(),
                    state.observedSpeed, true, preset);
        } else {
            phase.invalidateEmbed();
            phaseExcess = 0.0;
        }

        taints.computeJudged(pistonInfluence, stepped, stepFromFall, offerBounceAlt,
                knockbackHypothesisChosen, excess.altCenterUsed());
        consumeKnockback(knockbackHypothesisChosen, excess, preset);
        observeKnockbackRequirement(dx, dz, chosenSlotBounds, taints.forbidsKnockbackRequirement(), preset);

        double descentExcess = excess.descent();
        state.frictionMax = medium.frictionMax(input, ground);
        state.speedFactor = effectiveSpeedFactor();
        double momentumExcess = momentum.excess(state, taints.forbidsMomentum());
        boolean momentumFlagged = momentumExcess > preset.horizontalFlagEpsilon();
        double flagHorizontal = Math.max(horizontalExcess, momentumExcess);

        BoundBreach breach = classify(flagHorizontal, ascentExcess, descentExcess, phaseExcess, preset);
        if (breach == BoundBreach.DESCENT_FLOOR && chosenSlotBounds.riseFloor() > 0.0) {
            breach = obligation.breachLabel();
        }
        boolean flyGraceSuppressed = false;
        if (breach != null && data.isCanFly() && data.getFlyChangeGrace() > 0
                && (breach == BoundBreach.HORIZONTAL_DISK || breach == BoundBreach.ASCENT
                || breach == BoundBreach.DESCENT_FLOOR || breach == BoundBreach.BOUNCE_RISE
                || breach == BoundBreach.FORCED_RISE)) {
            breach = null;
            horizontalExcess = 0.0;
            flagHorizontal = 0.0;
            ascentExcess = 0.0;
            descentExcess = 0.0;
            flyGraceSuppressed = true;
        }
        if (breach == BoundBreach.FORCED_RISE || breach == BoundBreach.BOUNCE_RISE) {
            data.getSetbackController().accumulateRise(chosenSlotBounds.riseFloor(),
                    levitationTarget(input), contact.ceilingClearanceAny());
        }

        boolean sneakHeld = data.isSneaking();
        boolean sneakEdge = SneakEdgeRule.protectsCarry(sneakHeld, ground, state.landMedium, dy,
                state.observedSpeed, chosenSlotBounds, contact, preset);
        boolean preserveGrace = gate.inPreserveGrace();
        boolean carryPredicted = stepped || preserveGrace || sneakEdge;

        state.honeySlide = state.landMedium && HoneySlideRule.slidePossible(reader,
                gates.modernBlockEffects(), dy, ground.groundedEnd(), input.gravity(),
                current.getX(), current.getY(), current.getZ(), half, height);

        if (sample.stuck() && !sample.fluid()) {
            double climbVy = input.powderSnowClimb()
                    ? medium.advanceVertical(LandModel.POWDER_SNOW_CLIMB, input) : 0.0;
            carried.collapse(climbVy > 0.0 ? MotionArea.seeded(0.0, 0.0, climbVy) : MotionArea.rest());
            spawns.drop();
            obligation.disarmBounce();
        } else {
            for (int slot = 0; slot < CarriedHypotheses.CAPACITY; slot++) {
                if (!carried.live(slot)) continue;
                boolean slotSneakEdge = slot == chosen ? sneakEdge
                        : SneakEdgeRule.protectsCarry(sneakHeld, ground, state.landMedium, dy,
                        state.observedSpeed, boundsSlots[slot], contact, preset);
                boolean slotCarry = stepped || preserveGrace || slotSneakEdge;
                carried.area(slot, advanceRules.advance(boundsSlots[slot], carried.area(slot),
                        excessSlots[slot], carried.kind(slot), slotCarry, state, spawns));
            }
            if (state.previousClaimedGround) {
                if (chosen != 0 && carried.live(chosen)
                        && carried.kind(chosen) == CarriedHypotheses.Kind.AIR_REGIME) {
                    carried.area(0, carried.area(chosen));
                }
                carried.killKind(CarriedHypotheses.Kind.AIR_REGIME);
            }
            WallZeroSpawnRule.queue(contact, gates.endTick(), movement.isHorizontalCollision(),
                    carried.area(chosen), spawns);
            GlideExitSpawnRule.queue(state, data.getGlideData(), chosenSlotBounds,
                    carried.area(chosen), spawns);
            AirRegimeSpawnRule.queue(state, previous.deltaZeroed, movement.isOnGround(),
                    chosenSlotBounds, spawns);
            spawns.flush(carried);
            carried.mergeConverged();
            if (pistonInfluence && contact.startOverlapping()) {
                seedEmbedExemptions(current, half, height);
            }
            bedBounce.arm(contact, chosenSlotBounds);
            obligation.armBounce(state.landModel(), state.doubleMove, fullPose(), sneakHeld,
                    sample.stuck(), state.honeySlide, supportTracker.bounceCertain(), dy,
                    state.preCarriedCeil, carried.minFloorVy(), contact.ceilingClearanceAny());
        }
        gate.tickPreserveGrace();

        long widenings = spawns.bits();
        if (carried.pollOverflowed()) widenings |= TraceFrame.HYPOTHESIS_OVERFLOW;
        if (knockbackWidened) widenings |= TraceFrame.WIDENED_KNOCKBACK;
        if (riptideWidened) widenings |= TraceFrame.WIDENED_RIPTIDE;
        if (pistonInfluence) widenings |= TraceFrame.WIDENED_PISTON;
        if (entityPushInfluence > 0.0) widenings |= TraceFrame.WIDENED_ENTITY_PUSH;
        if (sample.stuck() && !sample.fluid()) widenings |= TraceFrame.WIDENED_STUCK;
        if (sample.bubbleAscent() > 0.0) widenings |= TraceFrame.WIDENED_BUBBLE;
        if (offerBounceAlt) widenings |= TraceFrame.WIDENED_BED_BOUNCE;
        if (riseFloorPinned && obligation.bouncePinned()) widenings |= TraceFrame.PINNED_BOUNCE_RISE;
        if (riseFloorPinned && !obligation.bouncePinned()) widenings |= TraceFrame.PINNED_FORCED_RISE;
        if (state.honeySlide) widenings |= TraceFrame.WIDENED_HONEY_SLIDE;
        if (boostApplied) widenings |= TraceFrame.WIDENED_BOOST_SEGMENT;
        if (minFallPinned) widenings |= TraceFrame.PINNED_MIN_FALL;
        if (momentumFlagged) widenings |= TraceFrame.TURN_MOMENTUM;
        if (carryPredicted) widenings |= TraceFrame.WIDENED_STEP_CARRY;
        if (stepFromFall) widenings |= TraceFrame.STEP_FROM_FALL;
        if (sneakEdge) widenings |= TraceFrame.WIDENED_SNEAK_EDGE;
        if (residualCarryWidened) widenings |= TraceFrame.WIDENED_RESIDUAL_CARRY;
        if (input.claimedInputExact() && input.horizontalInput()) widenings |= TraceFrame.INPUT_EXACT;
        if (flyGraceSuppressed) widenings |= TraceFrame.FLY_TRANSITION;
        trace.contributors(widenings);
        trace.taints(taints.bits());

        boolean phaseBreach = breach == BoundBreach.PHASE_CROSS || breach == BoundBreach.PHASE_EMBED;
        carry.store(phaseBreach ? 0.0 : horizontalExcess,
                breach == BoundBreach.DESCENT_FLOOR || breach == BoundBreach.BOUNCE_RISE
                        || breach == BoundBreach.FORCED_RISE ? 0.0 : ascentExcess,
                false, preset.residualCarryCap());

        verdict = buildVerdict(TickOutcome.JUDGED, null, breach,
                dx, dy, dz, flagHorizontal, ascentExcess, descentExcess, phaseExcess,
                (excess.altCenterUsed() || knockbackHypothesisChosen) && breach == null ? 1.0 : 0.0,
                input, ground);
    }

    private void coastTick(boolean withheld) {
        PhysicsPreset preset = state.preset;
        supportTracker.invalidate();
        momentum.invalidate();
        taints.computeBase(state, data);
        obligation.prepare(taints.forbidsRiseObligation(), state.input,
                data.getAttributeData().jumpStrength() * state.ground.startJumpMin()
                        + state.input.jumpBoostPower(),
                sample, contact);
        if (withheld) observeAbsentKnockback();
        coastArea();
        gate.tickPreserveGrace();

        double owedRise = withheld ? state.quietOwedRise : 0.0;
        double owedFall = withheld ? state.quietOwedFall : 0.0;
        data.getSetbackController().accumulateRise(owedRise, levitationTarget(state.input),
                contact.ceilingClearanceAny());
        double phaseExcess = 0.0;
        if (!withheld && sample.landMedium() && !pistons.reaching()) {
            phaseExcess = phase.excess(contact.horizontalCrossingDepth(), contact.embedDepth(),
                    state.observedSpeed, false, preset);
        }
        double owedThreshold = Math.max(preset.verticalFlagEpsilon(), MotionDefaults.MOTION_ZERO_THRESHOLD);
        boolean risePinned = owedRiseQuiet.observe(owedRise, owedThreshold);
        boolean fallPinned = owedFallQuiet.observe(owedFall, owedThreshold);
        boolean airborne = withheld && airborneNow(state.ground, preset);
        boolean hovering = hover.observe(airborne, preset.hoverGraceTicks(), HOVER_SETBACK_LIMIT)
                || fallPinned;
        boolean starving = silence.clockTick();

        if (phaseExcess > preset.horizontalFlagEpsilon()) {
            BoundBreach breach = contact.horizontalCrossingDepth() > preset.phaseCrossTolerance()
                    ? BoundBreach.PHASE_CROSS : BoundBreach.PHASE_EMBED;
            verdict = buildVerdict(TickOutcome.COASTED, null, breach,
                    state.dx, state.dy, state.dz, phaseExcess, 0.0, 0.0, phaseExcess, 0.0,
                    state.input, state.ground);
        } else if (risePinned) {
            verdict = buildVerdict(TickOutcome.COASTED, null, BoundBreach.FORCED_RISE,
                    state.dx, state.dy, state.dz, 0.0, 0.0, owedRise, 0.0, 0.0,
                    state.input, state.ground);
        } else if (hovering) {
            verdict = buildVerdict(TickOutcome.COASTED, null, BoundBreach.HOVER,
                    state.dx, state.dy, state.dz, 0.0,
                    fallPinned ? owedFall : HOVER_EXCESS, 0.0, 0.0, 0.0,
                    state.input, state.ground);
        } else if (starving) {
            verdict = buildVerdict(TickOutcome.COASTED, null, BoundBreach.MOTION_SILENCE,
                    state.dx, state.dy, state.dz, 0.0, SILENCE_EXCESS, 0.0, 0.0, 0.0,
                    state.input, state.ground);
        } else {
            verdict = buildVerdict(TickOutcome.COASTED,
                    withheld ? DeclineReason.WITHHELD : DeclineReason.DOUBLE_MOVE, null,
                    state.dx, state.dy, state.dz, 0.0, 0.0, 0.0, 0.0, 0.0,
                    state.input, state.ground);
        }
        carry.clear();
    }

    private void coastArea() {
        state.quietOwedRise = 0.0;
        state.quietOwedFall = 0.0;
        MediumModel medium = state.medium;
        ControlEnvelope input = state.input;
        GroundFacts ground = state.ground;
        if (sample.stuck() && !sample.fluid()) {
            carried.collapse(MotionArea.rest());
            carry.clear();
            obligation.disarmBounce();
            return;
        }
        double accel = medium.accelBound(input, ground) + pistons.horizontalReach();
        double frictionMax = medium.frictionMax(input, ground);
        boolean glide = medium.kind() == MediumKind.GLIDE;
        double owedRise = Double.MAX_VALUE;
        double maxCeiling = -Double.MAX_VALUE;
        int first = -1;
        for (int slot = 0; slot < CarriedHypotheses.CAPACITY; slot++) {
            if (!carried.live(slot)) continue;
            if (first < 0) first = slot;
            AreaBounds slotBounds = boundsSlots[slot];
            MotionArea area = carried.area(slot);
            AreaExpander.grow(area, slotBounds, ground, state, stuckFactor, bubble, carry);
            knockback.apply(slotBounds, state.preset.knockbackPad(),
                    carried.kind(slot) == CarriedHypotheses.Kind.KNOCKBACK_SET);
            riptide.apply(slotBounds, input);
            pistons.apply(slotBounds);
            double slotRise = obligation.riseFloorFor(slotBounds);
            if (slotRise > 0.0) slotBounds.riseFloor(slotRise);
            owedRise = Math.min(owedRise, slotRise);
            maxCeiling = Math.max(maxCeiling, slotBounds.ceiling());
            if (glide) {
                carried.area(slot, new MotionArea(slotBounds.centerX(), slotBounds.centerZ(),
                        area.slack() + accel, slotBounds.floor(), slotBounds.ceiling()));
            } else {
                boolean airRegime = state.airRegime(carried.kind(slot));
                double slotFriction = airRegime
                        ? LandModel.computeModifiedFriction(MotionDefaults.AIR_FRICTION, input.airDragModifier())
                        : frictionMax;
                boolean grounded = ground.groundedEnd() && !airRegime;
                double floorSource = (grounded ? 0.0 : area.floorVy()) + pistons.pushLoY();
                double ceilSource = grounded ? 0.0 : slotBounds.ceiling();
                carried.area(slot, AreaAdvancer.zeroClamp(AreaAdvancer.coast(area, accel, slotFriction,
                        medium.advanceVertical(floorSource, input),
                        medium.advanceVertical(ceilSource, input)), gates.jointHorizontalZeroing()));
            }
        }
        chosenBounds = boundsSlots[first];
        chosenSlot = first;
        carried.mergeConverged();
        carry.clear();
        bedBounce.reset();
        obligation.disarmBounce();
        state.quietOwedRise = owedRise == Double.MAX_VALUE ? 0.0 : owedRise;
        state.quietOwedFall = maxCeiling == -Double.MAX_VALUE ? 0.0 : Math.max(0.0, -maxCeiling);
    }

    private void decline(DeclineReason reason, boolean reseed, Location current, double half, double height) {
        if (reseed) {
            carried.collapse(reason == DeclineReason.SLEEPING
                    ? wakeSeed(state.dx, state.dy, state.dz)
                    : MotionArea.seeded(state.dx, state.dz, state.dy));
        }
        chosenSlot = 0;
        supportTracker.invalidate();
        trust.clearDoubleMoveStreak();
        hover.onDeclined();
        owedRiseQuiet.reset();
        owedFallQuiet.reset();
        silence.reset(System.nanoTime());
        if (reason != DeclineReason.RESYNC) data.getSetbackController().clearRise();
        body.control().onDecline();
        if (reason != DeclineReason.FAST) {
            seedEmbedExemptions(current, half, height);
        }
        if (reason == DeclineReason.RESYNC || reason == DeclineReason.TELEPORT
                || reason == DeclineReason.UNLOADED || reason == DeclineReason.LOADING
                || reason == DeclineReason.SLEEPING) {
            phase.seedGrace();
            phase.invalidateEmbed();
        }
        if (reseed || reason == DeclineReason.RESYNC || reason == DeclineReason.TELEPORT
                || reason == DeclineReason.SLEEPING) {
            groundResolver.displaced();
            push.reset();
        }
        carry.clear();
        bedBounce.reset();
        obligation.disarmBounce();
        momentum.invalidate();
        verdict = buildVerdict(TickOutcome.DECLINED, reason, null,
                state.dx, state.dy, state.dz, 0.0, 0.0, 0.0, 0.0, 0.0, null, null);
    }

    private MotionArea wakeSeed(double dx, double dy, double dz) {
        double advancedVy = (dy - data.getAttributeData().gravity()) * MotionDefaults.VERTICAL_DRAG;
        return new MotionArea(dx, dz, 0.0, Math.min(dy, advancedVy), Math.max(dy, advancedVy));
    }

    private void flagDetection(BoundBreach breach, double horizontalExcess, double verticalExcess) {
        hover.onDeclined();
        owedRiseQuiet.reset();
        owedFallQuiet.reset();
        silence.reset(System.nanoTime());
        data.getSetbackController().clearRise();
        supportTracker.invalidate();
        body.control().improperSprint(false);
        carried.collapse(MotionArea.seeded(state.dx, state.dz, state.dy));
        chosenSlot = 0;
        carry.clear();
        bedBounce.reset();
        obligation.disarmBounce();
        momentum.invalidate();
        verdict = buildVerdict(TickOutcome.JUDGED, null, breach,
                state.dx, state.dy, state.dz,
                Math.max(0.0, horizontalExcess), Math.max(0.0, verticalExcess), 0.0, 0.0,
                0.0, null, null);
    }

    private void observeAbsentKnockback() {
        ExternalVelocityData external = data.getExternalVelocityData();
        if (!external.isActive() || !external.hasSet()) return;
        Location at = data.getMovementData().getCurrent();
        double half = body.halfWidth();
        double height = body.height();
        int pushers = world.entities().countPushableNear(
                at.getX() - half, at.getY(), at.getZ() - half,
                at.getX() + half, at.getY() + height, at.getZ() + half,
                half, height);
        double threshold = gates.modernMovementThreshold()
                ? MovementData.DUPLICATE_THRESHOLD_MODERN
                : MovementData.DUPLICATE_THRESHOLD_LEGACY;
        MotionArea continuation = carried.union(CarriedHypotheses.Kind.KNOCKBACK_SET);
        double reach = state.medium.accelBound(state.input, state.ground)
                + ClientMath.horizontalDistance(continuation.centerX(), continuation.centerZ())
                + continuation.slack()
                + external.slack() + state.preset.knockbackPad() + threshold;
        knockback.observeRequirement(0.0, 0.0, external.x(), external.z(), reach,
                taints.forbidsAbsentKnockbackRequirement(pushers), state.preset.horizontalFlagEpsilon());
    }

    private void consumeKnockback(boolean knockbackHypothesisChosen, JudgedExcess excess, PhysicsPreset preset) {
        if (knockbackHypothesisChosen
                && excess.horizontal() <= preset.horizontalFlagEpsilon()
                && excess.ascent() <= preset.verticalFlagEpsilon()) {
            knockback.consumeExplained();
            return;
        }
        knockback.consumeIfExplained(excess, preset.horizontalFlagEpsilon(), preset.verticalFlagEpsilon());
    }

    private void observeKnockbackRequirement(double dx, double dz, AreaBounds chosenSlotBounds,
                                             boolean tainted, PhysicsPreset preset) {
        AreaBounds kb = knockbackSlotBounds(chosenSlotBounds);
        double centerX = kb.centerX();
        double centerZ = kb.centerZ();
        if (kb.hasAltCenter()
                && ClientMath.horizontalDistance(dx - kb.altCenterX(), dz - kb.altCenterZ())
                < ClientMath.horizontalDistance(dx - centerX, dz - centerZ)) {
            centerX = kb.altCenterX();
            centerZ = kb.altCenterZ();
        }
        knockback.observeRequirement(dx, dz, centerX, centerZ, kb.radius(), tainted,
                preset.horizontalFlagEpsilon());
    }

    private AreaBounds knockbackSlotBounds(AreaBounds chosenSlotBounds) {
        for (int slot = 0; slot < CarriedHypotheses.CAPACITY; slot++) {
            if (carried.live(slot) && carried.kind(slot) == CarriedHypotheses.Kind.KNOCKBACK_SET) {
                return boundsSlots[slot];
            }
        }
        return chosenSlotBounds;
    }

    private void observeTail(ConfigView view, PhysicsPreset preset, double dy, boolean flyingTick) {
        double ignoredKnockback = knockback.pollIgnored();
        if (ignoredKnockback > preset.horizontalFlagEpsilon()
                && verdict.breach() == null
                && (verdict.outcome() == TickOutcome.JUDGED
                || verdict.outcome() == TickOutcome.COASTED)) {
            verdict = verdict.withKnockbackBreach(ignoredKnockback);
        }
        knockback.finishTick();

        fall.observe(verdict.outcome(), verdict.declineReason(), verdict.breach(),
                dy, data.getMovementData().isOnGround(), state.honeySlide,
                state.scanned ? sample : null, state.ground, state.scanned ? contact : null, view);

        boolean offense = verdict.breach() != null && !actor.physicsBypassed();
        double excess = Math.max(Math.max(verdict.horizontalExcess(), verdict.ascentExcess()),
                Math.max(verdict.descentExcess(), verdict.phaseExcess()));
        boolean inventoryMove = data.isOpenInventory()
                && (verdict.breach() == BoundBreach.HORIZONTAL_DISK || verdict.breach() == BoundBreach.ASCENT);
        DeclineReason reason = verdict.declineReason();
        boolean trustedPosition = reason != DeclineReason.WITHHELD
                && reason != DeclineReason.DOUBLE_MOVE
                && reason != DeclineReason.TELEPORT
                && reason != DeclineReason.VEHICLE
                && reason != DeclineReason.GLIDE
                && reason != DeclineReason.SLEEPING;
        double safeGap = state.scanned ? Math.min(contact.nearestSupportGap(), 99.0) : 0.0;
        boolean safeAirborne = state.scanned && state.ground != null
                && airborneNow(state.ground, preset);
        mitigation.observe(view, preset, offense, excess, inventoryMove, trustedPosition,
                dy, safeGap, safeAirborne, flyingTick);
        if (verdict.breach() == BoundBreach.HOVER && mitigation.setbackIssuedThisTick()) {
            hover.onSetbackIssued();
        }
        verdict = verdict.withOutcome(mitigation.outcome(), fall.finding(), body.control().improperSprint());

        if (state.scanned) {
            Location poseAt = data.getMovementData().getCurrent();
            double poseHalf = body.halfWidth();
            body.pose().updateHeadroom(colliders, poseAt.getX() - poseHalf, poseAt.getY(),
                    poseAt.getZ() - poseHalf, poseAt.getX() + poseHalf, poseAt.getZ() + poseHalf);
        }

        data.getExternalVelocityData().tick();
        data.getPistonData().tick();
        data.getEffectData().tick();
        data.getGlideData().tick();
        data.getFireworkData().tick();
        data.getUseItemData().tick();

        if (state.scanned) previous.supportGap = contact.nearestSupportGap();
        trace.hypotheses(chosenSlot, carried.liveCount());
        trace.record(view, state.scanned ? contact : null, state.scanned ? sample : null,
                state.ground, state.input, chosenBounds, verdict, reader, mitigation.buffer(), fall.engineFall(),
                state.preCarriedX, state.preCarriedZ, state.preCarriedFloor, state.preCarriedCeil);
    }

    private BoundBreach classify(double horizontalExcess, double ascentExcess, double descentExcess,
                                 double phaseExcess, PhysicsPreset preset) {
        if (phaseExcess > preset.horizontalFlagEpsilon()) {
            return contact.horizontalCrossingDepth() > preset.phaseCrossTolerance()
                    ? BoundBreach.PHASE_CROSS : BoundBreach.PHASE_EMBED;
        }
        if (descentExcess > preset.verticalFlagEpsilon()) return BoundBreach.DESCENT_FLOOR;
        if (ascentExcess > preset.verticalFlagEpsilon()) return BoundBreach.ASCENT;
        if (horizontalExcess > preset.horizontalFlagEpsilon()) return BoundBreach.HORIZONTAL_DISK;
        return null;
    }

    private PhysicsVerdict buildVerdict(TickOutcome outcome, DeclineReason reason, BoundBreach breach,
                                        double dx, double dy, double dz,
                                        double horizontalExcess, double ascentExcess, double descentExcess,
                                        double phaseExcess, double impulseUsed,
                                        ControlEnvelope input, GroundFacts ground) {
        return new PhysicsVerdict(MotionStream.SELF, body.kind(), outcome, reason, breach,
                dx, dy, dz,
                horizontalExcess, ascentExcess, descentExcess, phaseExcess,
                chosenBounds.centerX(), chosenBounds.centerZ(), chosenBounds.radius(),
                chosenBounds.ceiling(), chosenBounds.judgedFloor(),
                state.scanned && state.medium != null ? state.medium.kind() : MediumKind.LAND,
                ground != null ? ground.start() : GroundState.AMBIGUOUS,
                data.isOpenInventory(),
                impulseUsed > 0.0,
                body.control().improperSprint(),
                MitigationOutcome.NONE,
                FallFinding.NONE);
    }

    private double levitationTarget(ControlEnvelope input) {
        if (input == null || !input.levitation() || input.levitationAmplifier() < 0) return 0.0;
        return MotionDefaults.LEVITATION_PER_LEVEL * (input.levitationAmplifier() + 1);
    }

    private boolean airborneNow(GroundFacts ground, PhysicsPreset preset) {
        return sample.landMedium() && !ground.groundedEnd()
                && !data.isFlying()
                && contact.nearestSupportGap() > preset.hoverMinGap()
                && !contact.startOverlapping();
    }

    private boolean provenUnobstructedFall() {
        if (taints.forbidsUnobstructedFall()) return false;
        GroundFacts ground = state.ground;
        ControlEnvelope input = state.input;
        if (ground.start() != GroundState.AIRBORNE) return false;
        if (ground.groundedEnd() || ground.recentlyGrounded() || ground.landingSupport()
                || ground.arrested() || ground.bounced() || ground.wasFluid()) return false;
        if (input.jumpPossible() || input.fluidExitHop() || input.powderSnowClimb()
                || input.levitation()) return false;
        if (!sample.landMedium() || sample.climbable() || sample.climbableUncertain()) return false;
        if (contact.supportGap() != ContactReport.NO_SUPPORT
                || contact.trailingSupportGap() != ContactReport.NO_SUPPORT) return false;
        if (contact.groundHit()) return false;
        if (push.carried() > 0.0 || exemptions.hasAny()) return false;
        if (data.getFlyChangeGrace() > 0
                || data.getGlideData().claimActive() || data.isGliding()
                || data.getTeleportData().hasPendingTeleport()) return false;
        return carried.liveCount() == 1;
    }

    private void seedEmbedExemptions(Location current, double half, double height) {
        exemptions.seedBodyOverlaps(reader, shapeQuery(current.getY()),
                current.getX() - half, current.getY(), current.getZ() - half,
                current.getX() + half, current.getY() + height, current.getZ() + half);
    }

    private ShapeQuery shapeQuery(double feetY) {
        return body.shapeQuery(feetY, fall.engineFall() > DEEP_FALL_POWDER_SNOW);
    }

    private double effectiveSpeedFactor() {
        if (data.isFlying() || data.isGliding() || data.getGlideData().claimActive()) return 1.0;
        double raw = supportTracker.speedCertain()
                ? supportTracker.speedFactor()
                : contact.supportSpeedFactor();
        if (raw >= 1.0) return 1.0;
        if (!gates.speedFactorOnCenter()) return 1.0;
        float factor = (float) raw;
        float efficiency = (float) data.getAttributeData().movementEfficiency();
        return factor + efficiency * (1.0F - factor);
    }

    private boolean fullPose() {
        return body.height() >= MotionDefaults.STANDING_HEIGHT * data.getAttributeData().scale();
    }

    private double jumpCeiling() {
        return data.getAttributeData().jumpStrength()
                + (data.getEffectData().hasJumpBoost()
                ? MotionDefaults.JUMP_BOOST_PER_LEVEL * (data.getEffectData().jumpBoostAmplifier() + 1) : 0.0);
    }

    private void disengage() {
        mitigation.reset();
        fall.reset();
        if (!initialized) return;
        initialized = false;
        carried.collapse(MotionArea.rest());
        clearHistory();
    }
}
