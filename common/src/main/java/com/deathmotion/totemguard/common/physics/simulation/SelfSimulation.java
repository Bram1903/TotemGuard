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
import com.deathmotion.totemguard.common.physics.collision.*;
import com.deathmotion.totemguard.common.physics.control.ControlEnvelope;
import com.deathmotion.totemguard.common.physics.fall.FallTracker;
import com.deathmotion.totemguard.common.physics.ground.GroundFacts;
import com.deathmotion.totemguard.common.physics.ground.GroundResolver;
import com.deathmotion.totemguard.common.physics.ground.GroundState;
import com.deathmotion.totemguard.common.physics.hover.HoverDetector;
import com.deathmotion.totemguard.common.physics.medium.*;
import com.deathmotion.totemguard.common.physics.medium.model.LandModel;
import com.deathmotion.totemguard.common.physics.mitigation.MitigationTracker;
import com.deathmotion.totemguard.common.physics.phase.EmbedExemptions;
import com.deathmotion.totemguard.common.physics.phase.PhaseTracker;
import com.deathmotion.totemguard.common.physics.prescan.DeclineCheck;
import com.deathmotion.totemguard.common.physics.prescan.TrustTracker;
import com.deathmotion.totemguard.common.physics.preset.PhysicsPreset;
import com.deathmotion.totemguard.common.physics.push.EntityPushTracker;
import com.deathmotion.totemguard.common.physics.push.KnockbackTracker;
import com.deathmotion.totemguard.common.physics.push.PistonWindow;
import com.deathmotion.totemguard.common.physics.push.RiptideWindow;
import com.deathmotion.totemguard.common.physics.rules.*;
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
    private static final int HOVER_SETBACK_LIMIT = 2;
    private static final double GLIDE_PRESERVE_GAP = 0.1;
    private static final double GROUND_CLAIM_FALSE_GAP = 0.02;
    private static final double VERTICAL_DUAL_GAP = 0.05;
    private static final double STEP_FROM_FALL_EPS = 1.0e-4;
    private static final int STUCK_SETTLE_SCANS = 3;
    private static final int PENDING_SPAWN_LIMIT = 12;

    private final EngineActor actor;
    private final Data data;
    private final WorldMirror world;
    private final BlockReader reader;
    private final EngineContext context;
    private final VersionGates gates;

    private final CarriedHypotheses carried = new CarriedHypotheses();
    private final AreaBounds[] boundsSlots = new AreaBounds[CarriedHypotheses.CAPACITY];
    private final JudgedExcess[] excessSlots = new JudgedExcess[CarriedHypotheses.CAPACITY];
    private final MotionArea[] pendingSpawnAreas = new MotionArea[PENDING_SPAWN_LIMIT];
    private final CarriedHypotheses.Kind[] pendingSpawnKinds = new CarriedHypotheses.Kind[PENDING_SPAWN_LIMIT];
    private final ResidualCarry carry = new ResidualCarry();
    private final ColliderBuffer colliders = new ColliderBuffer();
    private final CollisionSweep sweep = new CollisionSweep();
    private final ContactReport contact = new ContactReport();
    private final GroundResolver groundResolver = new GroundResolver();
    private final SupportingBlockTracker supportTracker;
    private final MediumSample sample = new MediumSample();
    private final StuckFactor stuckFactor = new StuckFactor();
    private final BubbleLift bubble = new BubbleLift();
    private final KnockbackTracker knockback;
    private final PistonWindow pistons;
    private final RiptideWindow riptide;
    private final PhaseTracker phase = new PhaseTracker();
    private final EmbedExemptions exemptions = new EmbedExemptions();
    private final PlayerBody body;
    private final GlideExitRule glideExit = new GlideExitRule();
    private final RiptideGlideRule riptideGlide = new RiptideGlideRule();
    private final BedBounceRule bedBounce = new BedBounceRule();
    private final BounceRiseRule bounceRise = new BounceRiseRule();
    private final TrustTracker trust = new TrustTracker();
    private final TickGate gate = new TickGate();
    private final TickContext ctx = new TickContext();
    private final HoverDetector hover = new HoverDetector();
    private final MitigationTracker mitigation;
    private final FallTracker fall;
    private final TraceRecording trace;
    private int pendingSpawnCount;
    private long spawnBits;
    private AreaBounds chosenBounds;
    private int chosenSlot;
    private boolean initialized;
    private boolean previousClaimedGround = true;
    private boolean previousIsFlying;
    private boolean previousPowderSnowSwept;
    private int stuckSettleScans;
    private double lastSupportGap = Double.MAX_VALUE;
    private GameMode lastGameMode;
    private boolean scannedThisTick;
    private boolean doubleMoveThisTick;
    private boolean honeySlideActive;
    private GroundFacts groundThisTick;
    private ControlEnvelope inputThisTick;
    private MediumModel mediumThisTick;
    private double preStepCarriedX, preStepCarriedZ, preStepCarriedFloor, preStepCarriedCeil;

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

    private static int floor(double value) {
        return (int) Math.floor(value);
    }

    public void onFlying() {
        ConfigView view = context.view();
        if (!view.physicsEngineEnabled()) {
            disengage();
            return;
        }
        PhysicsPreset preset = view.physicsPreset();
        MovementData movement = data.getMovementData();
        world.entities().advance();
        reader.resetCounters();

        Location current = movement.getCurrent();
        Location previous = movement.getPrevious();
        double dx = current.getX() - previous.getX();
        double dy = current.getY() - previous.getY();
        double dz = current.getZ() - previous.getZ();
        trust.countFlying(movement.isLastFlyingWasTeleportResync());

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

        scannedThisTick = false;
        doubleMoveThisTick = false;
        honeySlideActive = false;
        groundThisTick = null;
        inputThisTick = null;
        mediumThisTick = null;
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

            double observedSpeed = ClientMath.horizontalDistance(dx, dz);
            gate.evaluateSelf(data, world, reader, actor, movement, current, dx, dy, dz, observedSpeed);
            if (gate.kind() == TickGate.Kind.DECLINE) {
                switch (gate.carriedMode()) {
                    case REST -> carried.collapse(MotionArea.rest());
                    case REST_JUMP_CEILING -> carried.collapse(new MotionArea(0.0, 0.0, 0.0, 0.0, jumpCeiling()));
                    case FROZEN -> {
                        double gravity = data.getAttributeData().gravity();
                        double jumpCeiling = jumpCeiling();
                        carried.mapInPlace(area -> gate.frozen(area, gravity, jumpCeiling));
                    }
                    case KEEP -> {
                    }
                }
                decline(gate.reason(), dx, dy, dz, gate.reseed(), current, half, height);
                return;
            }
            if (gate.kind() == TickGate.Kind.FLAG) {
                flagDetection(gate.breach(), dx, dy, dz, gate.horizontalExcess(), gate.verticalExcess());
                return;
            }

            scanTick(previous, current, half, height, dx, dy, dz);
            if (reader.missesThisTick() > 0) {
                scannedThisTick = false;
                decline(DeclineReason.UNLOADED, dx, dy, dz, true, current, half, height);
                return;
            }
            GroundFacts ground = groundResolver.resolve(dy, contact, sample.fluid(),
                    data.getAttributeData().stepHeight(), carried.minFloorVy(), data.isSneaking(),
                    supportTracker);
            groundThisTick = ground;
            bubble.observe(sample.bubbleAscent());
            body.mediums().water().advanceEntryWindow(sample.fluid(), ground.wasFluid());
            stuckFactor.advanceWindow(sample.stuckAlongPath());

            TrustTracker.Trust trustKind = trust.classify(movement.isLastFlyingPositionChanged(),
                    movement.isLastFlyingWasDuplicate(), gates.endTick(),
                    data.getTeleportData().lastPacketWasTeleport(), preset.doubleMoveGraceTicks());
            boolean doubleMove = trustKind == TrustTracker.Trust.COAST_DOUBLE
                    || trustKind == TrustTracker.Trust.JUDGED_DOUBLE;
            doubleMoveThisTick = doubleMove;
            ControlEnvelope input = body.control().build(movement, contact, ground, sample.fluid(),
                    dx, dy, dz, doubleMove, sample.stuckVertical(), previousPowderSnowSwept);
            inputThisTick = input;
            MediumModel medium = body.medium(sample, false);
            mediumThisTick = medium;

            switch (trustKind) {
                case TRUSTED, TRUSTED_ZERO, JUDGED_DOUBLE ->
                        judgeTick(dx, dy, dz, observedSpeed, medium, input, ground, preset);
                case WITHHELD -> coastTick(true, dx, dy, dz, medium, input, ground, preset);
                case COAST_DOUBLE -> coastTick(false, dx, dy, dz, medium, input, ground, preset);
            }
        } finally {
            observeTail(view, preset, dy);
            previousClaimedGround = movement.isOnGround();
            previousIsFlying = data.isFlying();
            previousPowderSnowSwept = scannedThisTick && sample.powderSnowSwept();
            stuckSettleScans = scannedThisTick ? stuckSettleScans + 1 : 0;
            data.decayFlyChangeGrace();
        }
    }

    public void rewriteGroundClaim(PacketReceiveEvent event) {
        if (!scannedThisTick || groundThisTick == null) return;
        if (verdict.outcome() != TickOutcome.JUDGED) return;
        boolean claimed = data.getMovementData().isOnGround();
        double gap = contact.nearestSupportGap();
        boolean rewriteTo;
        if (claimed && !groundThisTick.groundedEnd() && gap > GROUND_CLAIM_FALSE_GAP) {
            rewriteTo = false;
        } else if (!claimed && groundThisTick.groundedEnd()
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
            if (verdict.mitigation().triggered() || verdict.fall().violation()) {
                verdict = verdict.withOutcome(MitigationOutcome.NONE, FallFinding.NONE, verdict.improperSprint());
            }
            return;
        }
        onSilentTick();
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
        supportTracker.invalidate();
        hover.reset();
        bubble.reset();
        stuckFactor.reset();
        body.mediums().reset();
        carry.clear();
        groundResolver.clearWindows();
        body.control().clear();
        phase.clear();
        exemptions.clear();
        bedBounce.reset();
        bounceRise.disarm();
        body.pose().clearHistory();
        verdict = PhysicsVerdict.INITIAL;
    }

    public void reset() {
        initialized = false;
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

    private void onSilentTick() {
        if (!initialized) return;
        if (data.isDead()) return;
        if (!world.readiness().ready()) return;
        ConfigView view = context.view();
        if (!view.physicsEngineEnabled()) return;
        if (data.getTeleportData().hasPendingTeleport()) return;
        MovementData movement = data.getMovementData();
        if (!movement.isCameraIsSelf()) return;
        Location current = movement.getCurrent();
        if (DeclineCheck.check(data) != null) return;
        PhysicsPreset preset = view.physicsPreset();

        reader.resetCounters();
        double half = body.halfWidth();
        double height = body.height();
        scannedThisTick = false;
        honeySlideActive = false;
        groundThisTick = null;
        inputThisTick = null;
        mediumThisTick = null;
        scanTick(current, current, half, height, 0.0, 0.0, 0.0);
        if (reader.missesThisTick() > 0) return;
        GroundFacts ground = groundResolver.resolve(0.0, contact, sample.fluid(),
                data.getAttributeData().stepHeight(), carried.minFloorVy(), data.isSneaking(),
                supportTracker);
        groundThisTick = ground;
        supportTracker.invalidate();
        ControlEnvelope input = body.control().build(movement, contact, ground, sample.fluid(),
                0.0, 0.0, 0.0, false, sample.stuckVertical(), previousPowderSnowSwept);
        inputThisTick = input;
        MediumModel medium = body.medium(sample, true);
        mediumThisTick = medium;

        observeAbsentKnockback(medium, input, ground, preset);
        coastArea(medium, input, ground, preset);

        boolean airborne = airborneNow(ground, preset);
        if (hover.observe(airborne, preset.hoverGraceTicks(), HOVER_SETBACK_LIMIT)) {
            verdict = buildVerdict(TickOutcome.COASTED, null, BoundBreach.HOVER,
                    0.0, 0.0, 0.0, 0.0, 0.0, HOVER_EXCESS, 0.0, 0.0, input, ground);
        } else {
            verdict = buildVerdict(TickOutcome.COASTED, DeclineReason.WITHHELD, null,
                    0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, input, ground);
        }

        previousPowderSnowSwept = scannedThisTick && sample.powderSnowSwept();
        observeTail(view, preset, 0.0);
    }

    private void scanTick(Location previous, Location current, double half, double height,
                          double dx, double dy, double dz) {
        ShapeQuery query = shapeQuery(current.getY());
        double minX = Math.min(previous.getX(), current.getX()) - half - HARVEST_HORIZONTAL_MARGIN;
        double maxX = Math.max(previous.getX(), current.getX()) + half + HARVEST_HORIZONTAL_MARGIN;
        double minY = Math.min(previous.getY(), current.getY()) - HARVEST_DOWN_MARGIN;
        double maxY = Math.max(previous.getY(), current.getY()) + height + HARVEST_UP_MARGIN;
        double minZ = Math.min(previous.getZ(), current.getZ()) - half - HARVEST_HORIZONTAL_MARGIN;
        double maxZ = Math.max(previous.getZ(), current.getZ()) + half + HARVEST_HORIZONTAL_MARGIN;
        ColliderCollector.fill(colliders, reader, world.entities(), query, exemptions,
                data.getPistonData(),
                minX, minY, minZ, maxX, maxY, maxZ);
        BorderColliders.fill(colliders, world.border(), previous.getX(), previous.getZ(), half,
                minX, minY, minZ, maxX, maxY, maxZ);
        pistons.setPlayerBox(minX, minY, minZ, maxX, maxY, maxZ);
        sweep.resolve(colliders, contact,
                previous.getX(), previous.getY(), previous.getZ(),
                half, height, dx, dy, dz,
                data.getAttributeData().stepHeight(), groundResolver.lastGroundedEnd());
        TraitSampler.sample(reader, contact, current.getX(), current.getY(), current.getZ(), half);
        MediumScan.sample(reader, sample,
                !data.isFlying(), world.dimension().dimensionType() != null
                        && world.dimension().dimensionType().isUltraWarm(),
                gates.modernFluidPush(), data.getEffectData().hasWeaving(),
                !previousIsFlying && stuckSettleScans >= STUCK_SETTLE_SCANS,
                previous.getX() - half, previous.getY(), previous.getZ() - half,
                previous.getX() + half, previous.getY() + height, previous.getZ() + half,
                Math.min(previous.getX(), current.getX()) - half, Math.min(previous.getY(), current.getY()),
                Math.min(previous.getZ(), current.getZ()) - half,
                Math.max(previous.getX(), current.getX()) + half,
                Math.max(previous.getY(), current.getY()) + height,
                Math.max(previous.getZ(), current.getZ()) + half);
        scannedThisTick = true;
    }

    private void judgeTick(double dx, double dy, double dz, double observedSpeed,
                           MediumModel medium, ControlEnvelope input,
                           GroundFacts ground, PhysicsPreset preset) {
        hover.onJudged();
        Location end = data.getMovementData().getCurrent();
        double trackHalf = body.halfWidth();
        supportTracker.update(colliders, reader, ground.groundedEnd(), contact.nearestSupportGap(),
                contact.supportApproximate(),
                end.getX() - trackHalf, end.getZ() - trackHalf,
                end.getX() + trackHalf, end.getZ() + trackHalf,
                end.getX(), end.getY(), end.getZ(), dx, dz);
        spawnBits = 0L;
        pendingSpawnCount = 0;
        spawnKnockbackHypothesis(medium, input);
        MotionArea preStep = carried.union();
        preStepCarriedX = preStep.centerX();
        preStepCarriedZ = preStep.centerZ();
        preStepCarriedFloor = preStep.floorVy();
        preStepCarriedCeil = preStep.ceilVy();
        boolean landMedium = sample.landMedium();

        boolean residualCarryWidened = carry.horizontal() > 0.0;
        boolean landModel = medium == body.mediums().land();
        double riseFloor = bounceRise.required(riseTainted(landModel), input,
                data.getAttributeData().jumpStrength() * ground.startJumpMin() + input.jumpBoostPower());
        bedBounce.prepare(landMedium);
        double arrestCap = landMedium && dy <= -preset.verticalFlagEpsilon()
                ? Math.max(0.0, contact.nearestSupportGap() - preset.verticalNoisePad())
                : -1.0;

        Location current = data.getMovementData().getCurrent();
        double half = body.halfWidth();
        double height = body.lastHeight();

        int chosen = 0;
        double best = Double.MAX_VALUE;
        boolean knockbackWidened = false;
        boolean riptideWidened = false;
        boolean pistonInfluence = false;
        double entityPushInfluence = 0.0;
        boolean offerBounceAlt = false;
        boolean boostApplied = false;
        GroundFacts claimAirGround = null;
        double boostStuckScale = sample.stuck() && !sample.fluid() ? sample.stuckHorizontal() : 1.0;
        for (int slot = 0; slot < CarriedHypotheses.CAPACITY; slot++) {
            if (!carried.live(slot)) continue;
            AreaBounds slotBounds = boundsSlots[slot];
            MotionArea area = carried.area(slot);
            GroundFacts slotGround = ground;
            if (carried.kind(slot) == CarriedHypotheses.Kind.AIR_REGIME && !previousClaimedGround
                    && medium == body.mediums().land()) {
                if (claimAirGround == null) claimAirGround = airRegimeView(ground);
                slotGround = claimAirGround;
            }
            ctx.fill(preset, sample, input, slotGround, contact, medium, slotBounds);
            AreaExpander.grow(area, ctx, stuckFactor, bubble, carry);
            knockback.apply(slotBounds, preset.knockbackPad(),
                    carried.kind(slot) == CarriedHypotheses.Kind.KNOCKBACK_SET);
            boolean slotKnockback = slotBounds.hasAltCenter();
            riptide.apply(slotBounds, input);
            boolean slotRiptide = slotBounds.hasAltCenter() && !slotKnockback;
            boolean slotPiston = pistons.apply(slotBounds);
            glideExit.widenForExit(medium, data, body.mediums(), input, ground, area, slotBounds);
            riptideGlide.offer(medium, data, body.mediums(), input, ground, contact, area, slotBounds);
            double slotPush = EntityPushTracker.apply(slotBounds, world.entities(),
                    Math.min(current.getX() - dx, current.getX()) - half, Math.min(current.getY() - dy, current.getY()),
                    Math.min(current.getZ() - dz, current.getZ()) - half,
                    Math.max(current.getX() - dx, current.getX()) + half,
                    Math.max(current.getY() - dy, current.getY()) + height,
                    Math.max(current.getZ() - dz, current.getZ()) + half,
                    half, height);
            boolean slotBounce = bedBounce.applyTo(slotBounds,
                    slotBounds.centerX() - area.centerX(), slotBounds.centerZ() - area.centerZ());
            boolean slotBoost = SprintBoostRule.apply(input, slotBounds, boostStuckScale);
            if (riseFloor > 0.0) slotBounds.riseFloor(riseFloor);
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
        if (landMedium && !ground.groundedStart() && !ground.groundedEnd()
                && dy > preset.verticalFlagEpsilon()
                && lastSupportGap >= 0.0
                && dy + lastSupportGap <= input.stepHeight() + STEP_FROM_FALL_EPS
                && lastSupportGap <= -preStepCarriedFloor + STEP_FROM_FALL_EPS) {
            stepFromFall = contact.nearestSupportGap() <= preset.verticalNoisePad()
                    || sweep.flushTopAt(colliders, current.getY(),
                    current.getX() - half, current.getZ() - half,
                    current.getX() + half, current.getZ() + half);
        }
        boolean stepped = contact.stepUsedHeight() > 0.0
                || (dy > preset.verticalFlagEpsilon() && ground.groundedEnd())
                || stepFromFall;
        if (stepped && horizontalExcess > 0.0) {
            horizontalExcess = Math.max(0.0, horizontalExcess - preset.stepNoiseSlack());
        }
        double ascentExcess = excess.ascent();
        if (stepFromFall) ascentExcess = 0.0;

        double phaseExcess;
        if (landMedium) {
            phaseExcess = phase.excess(contact.horizontalCrossingDepth(), contact.embedDepth(),
                    observedSpeed, true, preset);
        } else {
            phase.invalidateEmbed();
            phaseExcess = 0.0;
        }

        consumeKnockback(knockbackHypothesisChosen, excess, preset);
        boolean knockbackTainted = contact.collidedX() || contact.collidedZ() || contact.wallNear()
                || contact.startOverlapping() || contact.stepUsedHeight() > 0.0 || stepFromFall
                || sample.stuck() || !landMedium || data.isFlying()
                || pistonInfluence
                || data.getGlideData().riptideActive() || data.isSpinAttacking()
                || offerBounceAlt
                || data.getMitigationService().setbackPending();
        knockback.observeRequirement(dx, dz, requirementReach(chosenSlotBounds), knockbackTainted,
                preset.horizontalFlagEpsilon());

        double descentExcess = excess.descent();
        BoundBreach breach = classify(horizontalExcess, ascentExcess, descentExcess, phaseExcess, preset);
        if (breach == BoundBreach.DESCENT_FLOOR && riseFloor > 0.0) breach = BoundBreach.BOUNCE_RISE;

        boolean flyGraceSuppressed = false;
        if (breach != null && data.isCanFly() && data.getFlyChangeGrace() > 0
                && (breach == BoundBreach.HORIZONTAL_DISK || breach == BoundBreach.ASCENT
                || breach == BoundBreach.DESCENT_FLOOR || breach == BoundBreach.BOUNCE_RISE)) {
            breach = null;
            horizontalExcess = 0.0;
            ascentExcess = 0.0;
            descentExcess = 0.0;
            flyGraceSuppressed = true;
        }

        boolean sneakHeld = data.isSneaking();
        boolean sneakEdge = SneakEdgeRule.protectsCarry(sneakHeld, ground, landMedium, dy, observedSpeed,
                chosenSlotBounds, contact, preset);
        boolean preserveGrace = gate.inPreserveGrace();
        boolean carryPredicted = stepped || preserveGrace || sneakEdge;

        honeySlideActive = landMedium && HoneySlideRule.slidePossible(reader,
                gates.modernBlockEffects(), dy, ground.groundedEnd(), input.gravity(),
                current.getX(), current.getY(), current.getZ(), half, height);

        if (sample.stuck() && !sample.fluid()) {
            double climbVy = input.powderSnowClimb()
                    ? medium.advanceVertical(LandModel.POWDER_SNOW_CLIMB, input) : 0.0;
            carried.collapse(climbVy > 0.0 ? MotionArea.seeded(0.0, 0.0, climbVy) : MotionArea.rest());
            pendingSpawnCount = 0;
            bounceRise.disarm();
        } else {
            double frictionMax = medium.frictionMax(input, ground);
            double speedFactor = effectiveSpeedFactor();
            for (int slot = 0; slot < CarriedHypotheses.CAPACITY; slot++) {
                if (!carried.live(slot)) continue;
                boolean slotSneakEdge = slot == chosen ? sneakEdge
                        : SneakEdgeRule.protectsCarry(sneakHeld, ground, landMedium, dy, observedSpeed,
                        boundsSlots[slot], contact, preset);
                boolean slotCarry = stepped || preserveGrace || slotSneakEdge;
                carried.area(slot, advance(boundsSlots[slot], carried.area(slot), excessSlots[slot],
                        carried.kind(slot), slotCarry, stepFromFall, medium, input, ground, preset,
                        frictionMax, speedFactor, dx, dy, dz));
            }
            if (previousClaimedGround) {
                if (chosen != 0 && carried.live(chosen)
                        && carried.kind(chosen) == CarriedHypotheses.Kind.AIR_REGIME) {
                    carried.area(0, carried.area(chosen));
                }
                carried.killKind(CarriedHypotheses.Kind.AIR_REGIME);
            }
            spawnWallZero(chosen);
            spawnGlideExit(chosen, chosenSlotBounds, medium, frictionMax, speedFactor);
            spawnAirRegime(chosenSlotBounds, medium, input, ground, stepped, stepFromFall,
                    frictionMax, speedFactor);
            flushPendingSpawns();
            carried.mergeConverged();
            bedBounce.arm(contact, chosenSlotBounds);
            bounceRise.arm(landModel, doubleMoveThisTick, fullPose(), sneakHeld, sample.stuck(),
                    honeySlideActive, supportTracker.bounceCertain(), dy, preStepCarriedCeil,
                    carried.minFloorVy(), contact.ceilingClearanceAny());
        }
        gate.tickPreserveGrace();

        long widenings = spawnBits;
        if (carried.pollOverflowed()) widenings |= TraceFrame.HYPOTHESIS_OVERFLOW;
        if (knockbackWidened) widenings |= TraceFrame.WIDENED_KNOCKBACK;
        if (riptideWidened) widenings |= TraceFrame.WIDENED_RIPTIDE;
        if (pistonInfluence) widenings |= TraceFrame.WIDENED_PISTON;
        if (entityPushInfluence > 0.0) widenings |= TraceFrame.WIDENED_ENTITY_PUSH;
        if (sample.stuck() && !sample.fluid()) widenings |= TraceFrame.WIDENED_STUCK;
        if (sample.bubbleAscent() > 0.0) widenings |= TraceFrame.WIDENED_BUBBLE;
        if (offerBounceAlt) widenings |= TraceFrame.WIDENED_BED_BOUNCE;
        if (riseFloor > 0.0) widenings |= TraceFrame.PINNED_BOUNCE_RISE;
        if (honeySlideActive) widenings |= TraceFrame.WIDENED_HONEY_SLIDE;
        if (boostApplied) widenings |= TraceFrame.WIDENED_BOOST_SEGMENT;
        if (carryPredicted) widenings |= TraceFrame.WIDENED_STEP_CARRY;
        if (stepFromFall) widenings |= TraceFrame.STEP_FROM_FALL;
        if (sneakEdge) widenings |= TraceFrame.WIDENED_SNEAK_EDGE;
        if (residualCarryWidened) widenings |= TraceFrame.WIDENED_RESIDUAL_CARRY;
        if (input.claimedInputExact() && input.horizontalInput()) widenings |= TraceFrame.INPUT_EXACT;
        if (flyGraceSuppressed) widenings |= TraceFrame.FLY_TRANSITION;
        trace.contributors(widenings);

        boolean phaseBreach = breach == BoundBreach.PHASE_CROSS || breach == BoundBreach.PHASE_EMBED;
        carry.store(phaseBreach ? 0.0 : horizontalExcess,
                breach == BoundBreach.DESCENT_FLOOR || breach == BoundBreach.BOUNCE_RISE ? 0.0 : ascentExcess,
                false, preset.residualCarryCap());

        verdict = buildVerdict(TickOutcome.JUDGED, null, breach,
                dx, dy, dz, horizontalExcess, ascentExcess, descentExcess, phaseExcess,
                (excess.altCenterUsed() || knockbackHypothesisChosen) && breach == null ? 1.0 : 0.0,
                input, ground);
    }

    private void coastTick(boolean withheld, double dx, double dy, double dz,
                           MediumModel medium, ControlEnvelope input, GroundFacts ground, PhysicsPreset preset) {
        supportTracker.invalidate();
        if (withheld) observeAbsentKnockback(medium, input, ground, preset);
        coastArea(medium, input, ground, preset);
        gate.tickPreserveGrace();

        double phaseExcess = 0.0;
        if (!withheld && sample.landMedium()) {
            phaseExcess = phase.excess(contact.horizontalCrossingDepth(), contact.embedDepth(),
                    ClientMath.horizontalDistance(dx, dz), false, preset);
        }
        boolean airborne = withheld && airborneNow(ground, preset);
        boolean hovering = hover.observe(airborne, preset.hoverGraceTicks(), HOVER_SETBACK_LIMIT);

        if (phaseExcess > preset.horizontalFlagEpsilon()) {
            BoundBreach breach = contact.horizontalCrossingDepth() > preset.phaseCrossTolerance()
                    ? BoundBreach.PHASE_CROSS : BoundBreach.PHASE_EMBED;
            verdict = buildVerdict(TickOutcome.COASTED, null, breach,
                    dx, dy, dz, phaseExcess, 0.0, 0.0, phaseExcess, 0.0, input, ground);
        } else if (hovering) {
            verdict = buildVerdict(TickOutcome.COASTED, null, BoundBreach.HOVER,
                    dx, dy, dz, 0.0, HOVER_EXCESS, 0.0, 0.0, 0.0, input, ground);
        } else {
            verdict = buildVerdict(TickOutcome.COASTED,
                    withheld ? DeclineReason.WITHHELD : DeclineReason.DOUBLE_MOVE, null,
                    dx, dy, dz, 0.0, 0.0, 0.0, 0.0, 0.0, input, ground);
        }
        carry.clear();
    }

    private MotionArea advance(AreaBounds bounds, MotionArea area, JudgedExcess excess,
                               CarriedHypotheses.Kind kind, boolean carryPredicted, boolean stepFromFall,
                               MediumModel medium, ControlEnvelope input,
                               GroundFacts ground, PhysicsPreset preset,
                               double frictionMax, double speedFactor,
                               double dx, double dy, double dz) {
        boolean airRegime = kind == CarriedHypotheses.Kind.AIR_REGIME && !previousClaimedGround
                && medium == body.mediums().land();
        if (airRegime) {
            frictionMax = LandModel.computeModifiedFriction(LandModel.AIR_FRICTION, input.airDragModifier());
        }
        AreaAdvancer.clampObserved(bounds, dx, dy, dz, excess.altCenterUsed(), preset.modelDriftSlack());
        double anchor = (ground.groundedEnd() || stepFromFall) && !airRegime ? 0.0 : bounds.legalVy();
        double advancedVy = medium.advanceVertical(anchor, input);
        double advancedFloorVy = CeilingFlushRule.advanceFloor(anchor, advancedVy, medium, input, bounds, contact);
        advancedFloorVy = ClimbExitRule.carryFloor(advancedFloorVy, medium, body.mediums(), bounds, input);
        boolean intervalOverridden = false;
        if (honeySlideActive) {
            double honeyVy = HoneySlideRule.carriedVy(gates.modernBlockEffects(), input.gravity());
            if (honeyVy > advancedVy) {
                advancedVy = honeyVy;
                intervalOverridden = true;
            }
        }
        if (input.powderSnowClimb()) {
            double climbVy = medium.advanceVertical(LandModel.POWDER_SNOW_CLIMB, input);
            if (climbVy > advancedVy) {
                advancedVy = climbVy;
                intervalOverridden = true;
            }
        }

        MotionArea next = AreaAdvancer.next(bounds.legalX(), bounds.legalZ(), frictionMax,
                speedFactor, advancedFloorVy, advancedVy);

        if (carryPredicted) {
            double accel = medium.accelBound(input, ground);
            double coastX = area.centerX();
            double coastZ = area.centerZ();
            double coastSlack = area.slack() + accel;
            if (input.sprintJump()) {
                coastX += input.boostDirX() * MediumModel.SPRINT_JUMP_BOOST;
                coastZ += input.boostDirZ() * MediumModel.SPRINT_JUMP_BOOST;
                coastSlack += input.boostSpread();
            }
            queueSpawn(CarriedHypotheses.Kind.STEP_TRACK, TraceFrame.SPAWN_STEP,
                    new MotionArea(coastX * frictionMax, coastZ * frictionMax,
                            coastSlack * frictionMax, advancedFloorVy, advancedVy));
        }

        if (medium == body.mediums().glide() && body.mediums().glide().dualActive()) {
            double glideX = bounds.legalX() * speedFactor;
            double glideZ = bounds.legalZ() * speedFactor;
            double freeFallShrink = ClientMath.horizontalDistance(glideX, glideZ) * (1.0 - frictionMax);
            next = new MotionArea(glideX, glideZ, next.slack() + freeFallShrink,
                    body.mediums().land().advanceVertical(bounds.legalVy(), input), bounds.legalVy());
            intervalOverridden = true;
        }

        if (medium == body.mediums().glide() && contact.nearestSupportGap() <= GLIDE_PRESERVE_GAP) {
            double exitFloor = body.mediums().land().advanceVertical(bounds.floor(), input);
            if (exitFloor < next.floorVy()) {
                next = new MotionArea(next.centerX(), next.centerZ(), next.slack(),
                        exitFloor, next.ceilVy());
                intervalOverridden = true;
            }
        }

        if (ground.bounced() && area.floorVy() < 0.0 && contact.supportBounce() > 0.0) {
            double reflected = BounceRule.reflectMax(gates.restitutionBounce(), contact,
                    area.floorVy(), input.gravity(), LandModel.verticalDrag(input));
            double advancedCeil = medium.advanceVertical(reflected, input);
            next = new MotionArea(next.centerX(), next.centerZ(), next.slack(),
                    bounceFloor(advancedCeil, area, medium, input), advancedCeil);
            intervalOverridden = true;
        }

        next = AreaAdvancer.zeroClamp(next, gates.jointHorizontalZeroing());

        if (!intervalOverridden && next.ceilVy() - next.floorVy() > VERTICAL_DUAL_GAP) {
            queueSpawn(CarriedHypotheses.Kind.SPARE, TraceFrame.SPAWN_VERTICAL_DUAL,
                    new MotionArea(next.centerX(), next.centerZ(), next.slack(),
                            next.floorVy(), next.floorVy()));
            next = new MotionArea(next.centerX(), next.centerZ(), next.slack(),
                    next.ceilVy(), next.ceilVy());
        }

        return next;
    }

    private double bounceFloor(double advancedCeil, MotionArea area, MediumModel medium, ControlEnvelope input) {
        if (!supportTracker.bounceCertain() || area.ceilVy() >= 0.0) return advancedCeil;
        double least = BounceRule.reflectMin(gates.restitutionBounce(), supportTracker.bounceFactor(),
                supportTracker.bounceBed(), area.ceilVy(), input.gravity());
        if (least <= 0.0) return medium.advanceVertical(0.0, input);
        return Math.min(advancedCeil, medium.advanceVertical(least, input));
    }

    private void spawnWallZero(int chosen) {
        if (contact.stepUsedHeight() > 0.0) return;
        boolean penetration = contact.collidedX() || contact.collidedZ();
        if (!penetration && !contact.wallNear()) return;
        if (gates.endTick() && !data.getMovementData().isHorizontalCollision()) return;
        MotionArea advanced = carried.area(chosen);
        double centerX = advanced.centerX();
        double centerZ = advanced.centerZ();
        if (penetration) {
            double zeroedX = contact.collidedX() ? 0.0 : centerX;
            double zeroedZ = contact.collidedZ() ? 0.0 : centerZ;
            if (zeroedX != centerX || zeroedZ != centerZ) {
                queueSpawn(CarriedHypotheses.Kind.SPARE, TraceFrame.SPAWN_COLLIDE_ZERO,
                        new MotionArea(zeroedX, zeroedZ, advanced.slack(),
                                advanced.floorVy(), advanced.ceilVy()));
            }
            return;
        }
        if (Math.abs(centerX) > 1.0e-9) {
            queueSpawn(CarriedHypotheses.Kind.SPARE, TraceFrame.SPAWN_COLLIDE_ZERO,
                    new MotionArea(0.0, centerZ, advanced.slack(),
                            advanced.floorVy(), advanced.ceilVy()));
        }
        if (Math.abs(centerZ) > 1.0e-9) {
            queueSpawn(CarriedHypotheses.Kind.SPARE, TraceFrame.SPAWN_COLLIDE_ZERO,
                    new MotionArea(centerX, 0.0, advanced.slack(),
                            advanced.floorVy(), advanced.ceilVy()));
        }
    }

    private void spawnKnockbackHypothesis(MediumModel medium, ControlEnvelope input) {
        ExternalVelocityData external = data.getExternalVelocityData();
        if (!external.isActive() || !external.hasSet()) return;
        double slack = external.slack();
        double rawLow = external.y() - slack;
        double rawHigh = external.y() + slack;
        double advancedLow = medium.advanceVertical(rawLow, input);
        double advancedHigh = medium.advanceVertical(rawHigh, input);
        double floor = Math.min(rawLow, Math.min(advancedLow, advancedHigh));
        double ceiling = Math.max(rawHigh, Math.max(advancedLow, advancedHigh));
        carried.spawn(CarriedHypotheses.Kind.KNOCKBACK_SET,
                new MotionArea(external.x(), external.z(), slack, floor, ceiling));
        spawnBits |= TraceFrame.SPAWN_KNOCKBACK;
    }

    private void spawnAirRegime(AreaBounds chosenSlotBounds, MediumModel medium, ControlEnvelope input,
                                GroundFacts ground, boolean stepped, boolean stepFromFall,
                                double frictionMax, double speedFactor) {
        if (!stepped || (!ground.groundedEnd() && !stepFromFall)) return;
        if (medium != body.mediums().land()) return;
        boolean claimedGround = data.getMovementData().isOnGround();
        if (claimedGround && previousClaimedGround) return;
        if (!claimedGround && !input.jumpPossible()) return;
        double carryFriction = (previousClaimedGround ? frictionMax
                : LandModel.computeModifiedFriction(LandModel.AIR_FRICTION, input.airDragModifier()))
                * speedFactor;
        double fallVy = medium.advanceVertical(0.0, input);
        double jumpVy = medium.advanceVertical(input.jumpTakeoff(), input);
        queueSpawn(CarriedHypotheses.Kind.AIR_REGIME, TraceFrame.SPAWN_AIR_REGIME,
                new MotionArea(chosenSlotBounds.legalX() * carryFriction, chosenSlotBounds.legalZ() * carryFriction,
                        0.0, fallVy, Math.max(fallVy, jumpVy)));
    }

    private void spawnGlideExit(int chosen, AreaBounds chosenSlotBounds, MediumModel medium,
                                double frictionMax, double speedFactor) {
        if (medium != body.mediums().land() || !data.getGlideData().justExited()) return;
        double airDecay = LandModel.AIR_FRICTION * speedFactor;
        if (airDecay <= frictionMax * speedFactor) return;
        MotionArea advanced = carried.area(chosen);
        queueSpawn(CarriedHypotheses.Kind.GLIDE_EXIT, TraceFrame.SPAWN_GLIDE_EXIT,
                new MotionArea(chosenSlotBounds.legalX() * airDecay, chosenSlotBounds.legalZ() * airDecay,
                        0.0, advanced.floorVy(), advanced.ceilVy()));
    }

    private void queueSpawn(CarriedHypotheses.Kind kind, long bit, MotionArea area) {
        if (pendingSpawnCount >= PENDING_SPAWN_LIMIT) return;
        pendingSpawnKinds[pendingSpawnCount] = kind;
        pendingSpawnAreas[pendingSpawnCount] = area;
        pendingSpawnCount++;
        spawnBits |= bit;
    }

    private void flushPendingSpawns() {
        for (int i = 0; i < pendingSpawnCount; i++) {
            carried.spawn(pendingSpawnKinds[i], pendingSpawnAreas[i]);
            pendingSpawnAreas[i] = null;
        }
        pendingSpawnCount = 0;
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

    private double requirementReach(AreaBounds chosenSlotBounds) {
        for (int slot = 0; slot < CarriedHypotheses.CAPACITY; slot++) {
            if (carried.live(slot) && carried.kind(slot) == CarriedHypotheses.Kind.KNOCKBACK_SET) {
                return boundsSlots[slot].radius();
            }
        }
        return chosenSlotBounds.radius();
    }

    private void coastArea(MediumModel medium, ControlEnvelope input, GroundFacts ground, PhysicsPreset preset) {
        if (sample.stuck() && !sample.fluid()) {
            carried.collapse(MotionArea.rest());
            carry.clear();
            bounceRise.disarm();
            return;
        }
        double accel = medium.accelBound(input, ground);
        double frictionMax = medium.frictionMax(input, ground);
        boolean glide = medium.kind() == MediumKind.GLIDE;
        int first = -1;
        for (int slot = 0; slot < CarriedHypotheses.CAPACITY; slot++) {
            if (!carried.live(slot)) continue;
            if (first < 0) first = slot;
            AreaBounds slotBounds = boundsSlots[slot];
            MotionArea area = carried.area(slot);
            ctx.fill(preset, sample, input, ground, contact, medium, slotBounds);
            AreaExpander.grow(area, ctx, stuckFactor, bubble, carry);
            knockback.apply(slotBounds, preset.knockbackPad(),
                    carried.kind(slot) == CarriedHypotheses.Kind.KNOCKBACK_SET);
            riptide.apply(slotBounds, input);
            pistons.apply(slotBounds);
            if (glide) {
                carried.area(slot, new MotionArea(slotBounds.centerX(), slotBounds.centerZ(),
                        area.slack() + accel, slotBounds.floor(), slotBounds.ceiling()));
            } else {
                boolean airRegime = carried.kind(slot) == CarriedHypotheses.Kind.AIR_REGIME
                        && !previousClaimedGround && medium == body.mediums().land();
                double slotFriction = airRegime
                        ? LandModel.computeModifiedFriction(LandModel.AIR_FRICTION, input.airDragModifier())
                        : frictionMax;
                boolean grounded = ground.groundedEnd() && !airRegime;
                double floorSource = grounded ? 0.0 : area.floorVy();
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
        bounceRise.disarm();
    }

    private void decline(DeclineReason reason, double dx, double dy, double dz, boolean reseed,
                         Location current, double half, double height) {
        if (reseed) {
            carried.collapse(reason == DeclineReason.SLEEPING
                    ? wakeSeed(dx, dy, dz)
                    : MotionArea.seeded(dx, dz, dy));
        }
        chosenSlot = 0;
        supportTracker.invalidate();
        trust.clearDoubleMoveStreak();
        hover.onDeclined();
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
        }
        carry.clear();
        bedBounce.reset();
        bounceRise.disarm();
        verdict = buildVerdict(TickOutcome.DECLINED, reason, null,
                dx, dy, dz, 0.0, 0.0, 0.0, 0.0, 0.0, null, null);
    }

    private MotionArea wakeSeed(double dx, double dy, double dz) {
        double advancedVy = (dy - data.getAttributeData().gravity()) * MotionDefaults.VERTICAL_DRAG;
        return new MotionArea(dx, dz, 0.0, Math.min(dy, advancedVy), Math.max(dy, advancedVy));
    }

    private void flagDetection(BoundBreach breach, double dx, double dy, double dz,
                               double horizontalExcess, double verticalExcess) {
        hover.onDeclined();
        supportTracker.invalidate();
        body.control().improperSprint(false);
        carried.collapse(MotionArea.seeded(dx, dz, dy));
        chosenSlot = 0;
        carry.clear();
        bedBounce.reset();
        bounceRise.disarm();
        verdict = buildVerdict(TickOutcome.JUDGED, null, breach,
                dx, dy, dz, Math.max(0.0, horizontalExcess), Math.max(0.0, verticalExcess), 0.0, 0.0,
                0.0, null, null);
    }

    private void observeAbsentKnockback(MediumModel medium, ControlEnvelope input,
                                        GroundFacts ground, PhysicsPreset preset) {
        ExternalVelocityData external = data.getExternalVelocityData();
        if (!external.isActive() || !external.hasSet()) return;
        Location at = data.getMovementData().getCurrent();
        double half = body.halfWidth();
        double height = body.height();
        int pushers = world.entities().countPushableNear(
                at.getX() - half, at.getY(), at.getZ() - half,
                at.getX() + half, at.getY() + height, at.getZ() + half,
                half, height);
        boolean tainted = sample.stuck() || !sample.landMedium() || data.isFlying()
                || contact.wallNear() || contact.startOverlapping()
                || pushers > 0
                || data.getPistonData().isActive()
                || data.getGlideData().riptideActive() || data.isSpinAttacking()
                || data.getMitigationService().setbackPending();
        double threshold = gates.modernMovementThreshold()
                ? MovementData.DUPLICATE_THRESHOLD_MODERN
                : MovementData.DUPLICATE_THRESHOLD_LEGACY;
        MotionArea continuation = carried.union(CarriedHypotheses.Kind.KNOCKBACK_SET);
        double reach = medium.accelBound(input, ground)
                + ClientMath.horizontalDistance(continuation.centerX(), continuation.centerZ())
                + continuation.slack()
                + external.slack() + preset.knockbackPad() + threshold;
        knockback.observeRequirement(0.0, 0.0, reach, tainted, preset.horizontalFlagEpsilon());
    }

    private void observeTail(ConfigView view, PhysicsPreset preset, double dy) {
        double ignoredKnockback = knockback.pollIgnored();
        if (ignoredKnockback > preset.horizontalFlagEpsilon()
                && verdict.breach() == null
                && (verdict.outcome() == TickOutcome.JUDGED
                || verdict.outcome() == TickOutcome.COASTED)) {
            verdict = verdict.withKnockbackBreach(ignoredKnockback);
        }
        knockback.finishTick();

        fall.observe(verdict.outcome(), verdict.declineReason(), verdict.breach(),
                dy, data.getMovementData().isOnGround(), honeySlideActive,
                scannedThisTick ? sample : null, groundThisTick, scannedThisTick ? contact : null, view);

        boolean offense = verdict.breach() != null;
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
        double safeGap = scannedThisTick ? Math.min(contact.nearestSupportGap(), 99.0) : 0.0;
        boolean safeAirborne = scannedThisTick && groundThisTick != null
                && airborneNow(groundThisTick, preset);
        mitigation.observe(view, preset, offense, excess, inventoryMove, trustedPosition,
                dy, safeGap, safeAirborne);
        if (verdict.breach() == BoundBreach.HOVER && mitigation.setbackIssuedThisTick()) {
            hover.onSetbackIssued();
        }
        verdict = verdict.withOutcome(mitigation.outcome(), fall.finding(), body.control().improperSprint());

        if (scannedThisTick) {
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

        if (scannedThisTick) lastSupportGap = contact.nearestSupportGap();
        trace.hypotheses(chosenSlot, carried.liveCount());
        trace.record(view, scannedThisTick ? contact : null, scannedThisTick ? sample : null,
                groundThisTick, inputThisTick, chosenBounds, verdict, reader, mitigation.buffer(), fall.engineFall(),
                preStepCarriedX, preStepCarriedZ, preStepCarriedFloor, preStepCarriedCeil);
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
                scannedThisTick && mediumThisTick != null ? mediumThisTick.kind() : MediumKind.LAND,
                ground != null ? ground.start() : GroundState.AMBIGUOUS,
                data.isOpenInventory(),
                impulseUsed > 0.0,
                body.control().improperSprint(),
                MitigationOutcome.NONE,
                FallFinding.NONE);
    }

    private boolean airborneNow(GroundFacts ground, PhysicsPreset preset) {
        return sample.landMedium() && !ground.groundedEnd()
                && !data.isFlying()
                && contact.nearestSupportGap() > preset.hoverMinGap()
                && !contact.startOverlapping();
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
        double raw = supportTracker.speedCertain()
                ? supportTracker.speedFactor()
                : contact.supportSpeedFactor();
        if (raw >= 1.0) return 1.0;
        if (!gates.speedFactorOnCenter()) return 1.0;
        double efficiency = data.getAttributeData().movementEfficiency();
        return raw + efficiency * (1.0 - raw);
    }

    private boolean riseTainted(boolean landModel) {
        return !landModel || data.isFlying() || doubleMoveThisTick
                || sample.stuck() || sample.pushed() || sample.bubbleAscent() > 0.0
                || data.getExternalVelocityData().isActive()
                || data.getPistonData().isActive()
                || data.getGlideData().riptideActive() || data.isSpinAttacking()
                || data.getMitigationService().setbackPending();
    }

    private boolean fullPose() {
        return body.height() >= MotionDefaults.STANDING_HEIGHT * data.getAttributeData().scale();
    }

    private double jumpCeiling() {
        return data.getAttributeData().jumpStrength()
                + (data.getEffectData().hasJumpBoost()
                ? 0.1 * (data.getEffectData().jumpBoostAmplifier() + 1) : 0.0);
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
