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
import com.deathmotion.totemguard.common.physics.area.AreaAdvancer;
import com.deathmotion.totemguard.common.physics.area.AreaExpander;
import com.deathmotion.totemguard.common.physics.area.AreaJudge;
import com.deathmotion.totemguard.common.physics.area.JudgedExcess;
import com.deathmotion.totemguard.common.physics.area.AreaBounds;
import com.deathmotion.totemguard.common.physics.area.MotionArea;
import com.deathmotion.totemguard.common.physics.area.ResidualCarry;
import com.deathmotion.totemguard.common.physics.body.PlayerBody;
import com.deathmotion.totemguard.common.physics.fall.FallTracker;
import com.deathmotion.totemguard.common.physics.prescan.TrustTracker;
import com.deathmotion.totemguard.common.physics.prescan.DeclineCheck;
import com.deathmotion.totemguard.common.physics.ground.GroundState;
import com.deathmotion.totemguard.common.physics.ground.GroundFacts;
import com.deathmotion.totemguard.common.physics.ground.GroundResolver;
import com.deathmotion.totemguard.common.physics.mitigation.MitigationTracker;
import com.deathmotion.totemguard.common.physics.hover.HoverDetector;
import com.deathmotion.totemguard.common.physics.push.KnockbackTracker;
import com.deathmotion.totemguard.common.physics.push.PistonWindow;
import com.deathmotion.totemguard.common.physics.push.EntityPushTracker;
import com.deathmotion.totemguard.common.physics.push.RiptideWindow;
import com.deathmotion.totemguard.common.physics.control.ControlEnvelope;
import com.deathmotion.totemguard.common.physics.medium.BubbleLift;
import com.deathmotion.totemguard.common.physics.medium.MediumKind;
import com.deathmotion.totemguard.common.physics.medium.MediumModel;
import com.deathmotion.totemguard.common.physics.medium.MediumSample;
import com.deathmotion.totemguard.common.physics.medium.MediumScan;
import com.deathmotion.totemguard.common.physics.medium.StuckFactor;
import com.deathmotion.totemguard.common.physics.medium.model.LandModel;
import com.deathmotion.totemguard.common.physics.phase.EmbedExemptions;
import com.deathmotion.totemguard.common.physics.phase.PhaseTracker;
import com.deathmotion.totemguard.common.physics.preset.PhysicsPreset;
import com.deathmotion.totemguard.common.physics.EngineActor;
import com.deathmotion.totemguard.common.physics.EngineContext;
import com.deathmotion.totemguard.common.physics.MotionDefaults;
import com.deathmotion.totemguard.common.physics.VersionGates;
import com.deathmotion.totemguard.common.physics.rules.BedBounceRule;
import com.deathmotion.totemguard.common.physics.rules.BounceRule;
import com.deathmotion.totemguard.common.physics.rules.CeilingFlushRule;
import com.deathmotion.totemguard.common.physics.rules.ClimbExitRule;
import com.deathmotion.totemguard.common.physics.rules.GlideExitRule;
import com.deathmotion.totemguard.common.physics.rules.HoneySlideRule;
import com.deathmotion.totemguard.common.physics.rules.RiptideGlideRule;
import com.deathmotion.totemguard.common.physics.rules.SneakEdgeRule;
import com.deathmotion.totemguard.common.physics.collision.ColliderBuffer;
import com.deathmotion.totemguard.common.physics.collision.ColliderCollector;
import com.deathmotion.totemguard.common.physics.collision.ContactReport;
import com.deathmotion.totemguard.common.physics.collision.CollisionSweep;
import com.deathmotion.totemguard.common.physics.collision.SupportingBlockTracker;
import com.deathmotion.totemguard.common.physics.collision.TraitSampler;
import com.deathmotion.totemguard.common.physics.trace.TickRecorder;
import com.deathmotion.totemguard.common.physics.trace.TraceFrame;
import com.deathmotion.totemguard.common.physics.trace.TraceRecording;
import com.deathmotion.totemguard.common.physics.verdict.BoundBreach;
import com.deathmotion.totemguard.common.physics.verdict.DeclineReason;
import com.deathmotion.totemguard.common.physics.verdict.FallFinding;
import com.deathmotion.totemguard.common.physics.verdict.MitigationOutcome;
import com.deathmotion.totemguard.common.physics.verdict.MotionStream;
import com.deathmotion.totemguard.common.physics.verdict.PhysicsVerdict;
import com.deathmotion.totemguard.common.physics.verdict.TickOutcome;
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

    private final EngineActor actor;
    private final Data data;
    private final WorldMirror world;
    private final BlockReader reader;
    private final EngineContext context;
    private final VersionGates gates;

    private final AreaBounds bounds = new AreaBounds();
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
    private final TrustTracker trust = new TrustTracker();
    private final TickGate gate = new TickGate();
    private final TickContext ctx = new TickContext();
    private final HoverDetector hover = new HoverDetector();
    private final MitigationTracker mitigation;
    private final FallTracker fall;
    private final TraceRecording trace;

    private boolean initialized;
    private MotionArea carried = MotionArea.rest();
    private GameMode lastGameMode;
    private boolean scannedThisTick;
    private boolean honeySlideActive;
    private GroundFacts groundThisTick;
    private ControlEnvelope inputThisTick;
    private MediumModel mediumThisTick;
    private double preStepCarriedX, preStepCarriedZ, preStepCarriedFloor, preStepCarriedCeil;

    @Getter
    private PhysicsVerdict verdict = PhysicsVerdict.INITIAL;

    public SelfSimulation(EngineActor actor, Data data, WorldMirror world, EngineContext context,
                          TraceRecording trace) {
        this.actor = actor;
        this.data = data;
        this.world = world;
        this.reader = world.reader();
        this.context = context;
        this.gates = new VersionGates(actor.clientVersion(), actor.supportsEndTick());
        this.supportTracker = new SupportingBlockTracker(gates.supportingBlock());
        this.body = new PlayerBody(data, actor, gates);
        this.knockback = new KnockbackTracker(data.getExternalVelocityData());
        this.pistons = new PistonWindow(data.getPistonData());
        this.riptide = new RiptideWindow(data);
        this.mitigation = new MitigationTracker(data);
        this.fall = new FallTracker(data, reader);
        this.trace = trace;
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
        honeySlideActive = false;
        groundThisTick = null;
        inputThisTick = null;
        mediumThisTick = null;
        try {
            if (!initialized) {
                initialized = true;
                groundResolver.seed(movement.isOnGround());
                groundResolver.displaced();
                carried = MotionArea.seeded(dx, dz, dy);
                verdict = PhysicsVerdict.INITIAL;
                phase.seedGrace();
                seedEmbedExemptions(current, half, height);
                return;
            }

            double observedSpeed = ClientMath.horizontalDistance(dx, dz);
            gate.evaluateSelf(data, world, reader, actor, movement, current, dx, dy, dz, observedSpeed);
            if (gate.kind() == TickGate.Kind.DECLINE) {
                switch (gate.carriedMode()) {
                    case REST -> carried = MotionArea.rest();
                    case REST_JUMP_CEILING -> carried = new MotionArea(0.0, 0.0, 0.0, 0.0, jumpCeiling());
                    case FROZEN -> carried = gate.frozen(carried, data.getAttributeData().gravity(), jumpCeiling());
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
                    data.getAttributeData().stepHeight(), carried.floorVy(), data.isSneaking(),
                    supportTracker);
            groundThisTick = ground;
            bubble.observe(sample.bubbleAscent());
            body.mediums().water().advanceEntryWindow(sample.fluid(), ground.wasFluid());
            stuckFactor.advanceWindow(sample.stuckAlongPath());

            ControlEnvelope input = body.control().build(movement, contact, ground, sample.fluid(), dx, dy, dz);
            inputThisTick = input;
            MediumModel medium = body.medium(sample, false);
            mediumThisTick = medium;

            switch (trust.classify(movement.isLastFlyingPositionChanged(), movement.isLastFlyingWasDuplicate(),
                    gates.endTick(), data.getTeleportData().lastPacketWasTeleport(),
                    preset.doubleMoveGraceTicks())) {
                case TRUSTED, TRUSTED_ZERO, JUDGED_DOUBLE ->
                        judgeTick(dx, dy, dz, observedSpeed, medium, input, ground, preset);
                case WITHHELD -> coastTick(true, dx, dy, dz, medium, input, ground, preset);
                case COAST_DOUBLE -> coastTick(false, dx, dy, dz, medium, input, ground, preset);
            }
        } finally {
            observeTail(view, preset, dy);
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
        body.pose().clearHistory();
        verdict = PhysicsVerdict.INITIAL;
    }

    public void reset() {
        initialized = false;
        carried = MotionArea.rest();
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
        if (DeclineCheck.check(data, world.border(), current.getX(), current.getZ()) != null) return;
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
                data.getAttributeData().stepHeight(), carried.floorVy(), data.isSneaking(),
                supportTracker);
        groundThisTick = ground;
        supportTracker.invalidate();
        ControlEnvelope input = body.control().build(movement, contact, ground, sample.fluid(), 0.0, 0.0, 0.0);
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
        preStepCarriedX = carried.centerX();
        preStepCarriedZ = carried.centerZ();
        preStepCarriedFloor = carried.floorVy();
        preStepCarriedCeil = carried.ceilVy();
        double carriedFloorAtStart = carried.floorVy();
        boolean landMedium = sample.landMedium();

        ctx.fill(preset, sample, input, ground, contact, medium, bounds);
        boolean residualCarryWidened = carry.horizontal() > 0.0;
        AreaExpander.grow(carried, ctx, stuckFactor, bubble, carry);
        knockback.apply(bounds, preset.knockbackPad());
        boolean knockbackWidened = bounds.hasAltCenter();
        riptide.apply(bounds, input);
        boolean riptideWidened = bounds.hasAltCenter() && !knockbackWidened;
        boolean pistonInfluence = pistons.apply(bounds);
        glideExit.widenForExit(medium, data, body.mediums(), input, ground, carried, bounds);
        riptideGlide.offer(medium, data, body.mediums(), input, ground, contact, carried, bounds);
        Location current = data.getMovementData().getCurrent();
        double half = body.halfWidth();
        double height = body.lastHeight();
        double entityPushInfluence = EntityPushTracker.apply(bounds, world.entities(),
                Math.min(current.getX() - dx, current.getX()) - half, Math.min(current.getY() - dy, current.getY()),
                Math.min(current.getZ() - dz, current.getZ()) - half,
                Math.max(current.getX() - dx, current.getX()) + half,
                Math.max(current.getY() - dy, current.getY()) + height,
                Math.max(current.getZ() - dz, current.getZ()) + half,
                half, height);

        boolean offerBounceAlt = bedBounce.offer(landMedium, bounds);

        double arrestCap = landMedium && dy <= -preset.verticalFlagEpsilon()
                ? Math.max(0.0, contact.nearestSupportGap() - preset.verticalNoisePad())
                : -1.0;
        JudgedExcess excess = AreaJudge.judge(bounds, dx, dy, dz, arrestCap);
        bedBounce.onJudged(offerBounceAlt, excess);

        double horizontalExcess = excess.horizontal();
        boolean stepped = contact.stepUsedHeight() > 0.0
                || (dy > preset.verticalFlagEpsilon() && ground.groundedEnd());
        if (stepped && horizontalExcess > 0.0) {
            horizontalExcess = Math.max(0.0, horizontalExcess - preset.stepNoiseSlack());
        }

        double phaseExcess;
        if (landMedium) {
            phaseExcess = phase.excess(contact.horizontalCrossingDepth(), contact.embedDepth(),
                    observedSpeed, true, preset);
        } else {
            phase.invalidateEmbed();
            phaseExcess = 0.0;
        }

        knockback.consumeIfExplained(excess, preset.horizontalFlagEpsilon(), preset.verticalFlagEpsilon());
        boolean knockbackTainted = contact.collidedX() || contact.collidedZ() || contact.wallNear()
                || contact.startOverlapping() || contact.stepUsedHeight() > 0.0
                || sample.stuck() || !landMedium || data.isFlying()
                || pistonInfluence
                || data.getGlideData().riptideActive() || data.isSpinAttacking()
                || offerBounceAlt
                || data.getMitigationService().setbackPending();
        knockback.observeRequirement(dx, dz, bounds.radius(), knockbackTainted,
                preset.horizontalFlagEpsilon());

        BoundBreach breach = classify(horizontalExcess, excess.ascent(), excess.descent(), phaseExcess, preset);

        boolean sneakEdge = SneakEdgeRule.protectsCarry(input, ground, landMedium, dy, observedSpeed, bounds, contact, preset);
        boolean carryPredicted = stepped || gate.inPreserveGrace() || sneakEdge;
        gate.tickPreserveGrace();

        honeySlideActive = landMedium && HoneySlideRule.slidePossible(reader,
                gates.modernBlockEffects(), dy, ground.groundedEnd(), input.gravity(),
                current.getX(), current.getY(), current.getZ(), half, height);

        advanceArea(dx, dy, dz, medium, input, ground, preset, excess, carryPredicted, carriedFloorAtStart);

        long widenings = 0L;
        if (knockbackWidened) widenings |= TraceFrame.WIDENED_KNOCKBACK;
        if (riptideWidened) widenings |= TraceFrame.WIDENED_RIPTIDE;
        if (pistonInfluence) widenings |= TraceFrame.WIDENED_PISTON;
        if (entityPushInfluence > 0.0) widenings |= TraceFrame.WIDENED_ENTITY_PUSH;
        if (sample.stuck() && !sample.fluid()) widenings |= TraceFrame.WIDENED_STUCK;
        if (sample.bubbleAscent() > 0.0) widenings |= TraceFrame.WIDENED_BUBBLE;
        if (offerBounceAlt) widenings |= TraceFrame.WIDENED_BED_BOUNCE;
        if (honeySlideActive) widenings |= TraceFrame.WIDENED_HONEY_SLIDE;
        if (bounds.hasSegment()) widenings |= TraceFrame.WIDENED_BOOST_SEGMENT;
        if (carryPredicted) widenings |= TraceFrame.WIDENED_STEP_CARRY;
        if (sneakEdge) widenings |= TraceFrame.WIDENED_SNEAK_EDGE;
        if (residualCarryWidened) widenings |= TraceFrame.WIDENED_RESIDUAL_CARRY;
        trace.contributors(widenings);

        boolean phaseBreach = breach == BoundBreach.PHASE_CROSS || breach == BoundBreach.PHASE_EMBED;
        carry.store(phaseBreach ? 0.0 : horizontalExcess,
                breach == BoundBreach.DESCENT_FLOOR ? 0.0 : excess.ascent(),
                false, preset.residualCarryCap());

        verdict = buildVerdict(TickOutcome.JUDGED, null, breach,
                dx, dy, dz, horizontalExcess, excess.ascent(), excess.descent(), phaseExcess,
                excess.altCenterUsed() && breach == null ? 1.0 : 0.0, input, ground);
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

    private void advanceArea(double dx, double dy, double dz, MediumModel medium, ControlEnvelope input,
                                 GroundFacts ground, PhysicsPreset preset, JudgedExcess excess,
                                 boolean carryPredicted, double carriedFloorAtStart) {
        if (sample.stuck() && !sample.fluid()) {
            carried = MotionArea.rest();
            return;
        }
        double frictionMax = medium.frictionMax(input, ground);
        double speedFactor = effectiveSpeedFactor();
        AreaAdvancer.clampObserved(bounds, dx, dy, dz, excess.altCenterUsed(), preset.modelDriftSlack());
        double anchor = ground.groundedEnd() ? 0.0 : bounds.legalVy();
        double advancedVy = medium.advanceVertical(anchor, input);
        double advancedFloorVy = CeilingFlushRule.advanceFloor(anchor, advancedVy, medium, input, bounds, contact);
        advancedFloorVy = ClimbExitRule.carryFloor(advancedFloorVy, medium, body.mediums(), bounds, input);
        if (honeySlideActive) {
            advancedVy = Math.max(advancedVy,
                    HoneySlideRule.carriedVy(gates.modernBlockEffects(), input.gravity()));
        }

        if (carryPredicted) {
            double accel = medium.accelBound(input, ground);
            double coastX = carried.centerX() * frictionMax;
            double coastZ = carried.centerZ() * frictionMax;
            double track = frictionMax * speedFactor;
            double trackX = bounds.legalX() * track;
            double trackZ = bounds.legalZ() * track;
            if (trackX * trackX + trackZ * trackZ > coastX * coastX + coastZ * coastZ) {
                carried = new MotionArea(trackX, trackZ, 0.0, advancedFloorVy, advancedVy);
            } else {
                carried = new MotionArea(coastX, coastZ,
                        (carried.slack() + accel) * frictionMax, advancedFloorVy, advancedVy);
            }
        } else {
            carried = AreaAdvancer.next(bounds.legalX(), bounds.legalZ(), frictionMax,
                    speedFactor, advancedFloorVy, advancedVy);
        }

        carried = glideExit.widenExitDecay(medium, body.mediums(), data, frictionMax, speedFactor, bounds, carried);

        if (medium == body.mediums().glide() && body.mediums().glide().dualActive()) {
            double glideX = bounds.legalX() * speedFactor;
            double glideZ = bounds.legalZ() * speedFactor;
            double freeFallShrink = ClientMath.horizontalDistance(glideX, glideZ) * (1.0 - frictionMax);
            carried = new MotionArea(glideX, glideZ, carried.slack() + freeFallShrink,
                    body.mediums().land().advanceVertical(bounds.legalVy(), input), bounds.legalVy());
        }

        if (medium == body.mediums().glide() && contact.nearestSupportGap() <= GLIDE_PRESERVE_GAP) {
            double exitFloor = body.mediums().land().advanceVertical(bounds.floor(), input);
            if (exitFloor < carried.floorVy()) {
                carried = new MotionArea(carried.centerX(), carried.centerZ(), carried.slack(),
                        exitFloor, carried.ceilVy());
            }
        }

        if (ground.bounced() && carriedFloorAtStart < 0.0 && contact.supportBounce() > 0.0) {
            double reflected = BounceRule.reflect(gates.restitutionBounce(), contact,
                    carriedFloorAtStart, input.gravity(), LandModel.verticalDrag(input));
            double advanced = medium.advanceVertical(reflected, input);
            carried = new MotionArea(carried.centerX(), carried.centerZ(), carried.slack(),
                    advanced, advanced);
        }

        carried = AreaAdvancer.zeroClamp(carried, gates.jointHorizontalZeroing());

        bedBounce.arm(contact, bounds);
    }

    private void coastArea(MediumModel medium, ControlEnvelope input, GroundFacts ground, PhysicsPreset preset) {
        if (sample.stuck() && !sample.fluid()) {
            carried = MotionArea.rest();
            carry.clear();
            return;
        }
        ctx.fill(preset, sample, input, ground, contact, medium, bounds);
        AreaExpander.grow(carried, ctx, stuckFactor, bubble, carry);
        knockback.apply(bounds, preset.knockbackPad());
        riptide.apply(bounds, input);
        pistons.apply(bounds);
        double accel = medium.accelBound(input, ground);
        double frictionMax = medium.frictionMax(input, ground);
        if (medium.kind() == MediumKind.GLIDE) {
            carried = new MotionArea(bounds.centerX(), bounds.centerZ(),
                    (carried.slack() + accel), bounds.floor(), bounds.ceiling());
            carry.clear();
            bedBounce.reset();
            return;
        }
        double floorSource = ground.groundedEnd() ? 0.0 : carried.floorVy();
        double ceilSource = ground.groundedEnd() ? 0.0 : bounds.ceiling();
        carried = AreaAdvancer.zeroClamp(AreaAdvancer.coast(carried, accel, frictionMax,
                medium.advanceVertical(floorSource, input),
                medium.advanceVertical(ceilSource, input)), gates.jointHorizontalZeroing());
        carry.clear();
        bedBounce.reset();
    }


    private void decline(DeclineReason reason, double dx, double dy, double dz, boolean reseed,
                         Location current, double half, double height) {
        if (reseed) carried = MotionArea.seeded(dx, dz, dy);
        supportTracker.invalidate();
        trust.clearDoubleMoveStreak();
        hover.onDeclined();
        body.control().onDecline();
        if (reason != DeclineReason.FAST) {
            seedEmbedExemptions(current, half, height);
        }
        if (reason == DeclineReason.RESYNC || reason == DeclineReason.TELEPORT
                || reason == DeclineReason.UNLOADED || reason == DeclineReason.LOADING) {
            phase.seedGrace();
            phase.invalidateEmbed();
        }
        if (reseed || reason == DeclineReason.RESYNC || reason == DeclineReason.TELEPORT) {
            groundResolver.displaced();
        }
        carry.clear();
        bedBounce.reset();
        verdict = buildVerdict(TickOutcome.DECLINED, reason, null,
                dx, dy, dz, 0.0, 0.0, 0.0, 0.0, 0.0, null, null);
    }

    private void flagDetection(BoundBreach breach, double dx, double dy, double dz,
                            double horizontalExcess, double verticalExcess) {
        hover.onDeclined();
        supportTracker.invalidate();
        body.control().improperSprint(false);
        carried = MotionArea.seeded(dx, dz, dy);
        carry.clear();
        bedBounce.reset();
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
        double reach = medium.accelBound(input, ground)
                + ClientMath.horizontalDistance(carried.centerX(), carried.centerZ())
                + carried.slack()
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

        trace.record(view, scannedThisTick ? contact : null, scannedThisTick ? sample : null,
                groundThisTick, inputThisTick, bounds, verdict, reader, mitigation.buffer(), fall.engineFall(),
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
                bounds.centerX(), bounds.centerZ(), bounds.radius(),
                bounds.ceiling(), bounds.floor() - bounds.descentSlack(),
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
        carried = MotionArea.rest();
        clearHistory();
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }
}
