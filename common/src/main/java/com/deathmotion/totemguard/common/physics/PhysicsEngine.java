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

package com.deathmotion.totemguard.common.physics;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.config.view.ConfigView;
import com.deathmotion.totemguard.common.physics.area.AreaAdvancer;
import com.deathmotion.totemguard.common.physics.area.AreaExpander;
import com.deathmotion.totemguard.common.physics.area.AreaJudge;
import com.deathmotion.totemguard.common.physics.area.JudgedExcess;
import com.deathmotion.totemguard.common.physics.area.AreaBounds;
import com.deathmotion.totemguard.common.physics.area.MotionArea;
import com.deathmotion.totemguard.common.physics.area.ResidualCarry;
import com.deathmotion.totemguard.common.physics.fall.FallTracker;
import com.deathmotion.totemguard.common.physics.prescan.TrustTracker;
import com.deathmotion.totemguard.common.physics.prescan.GroundSpoofDetector;
import com.deathmotion.totemguard.common.physics.prescan.DeclineCheck;
import com.deathmotion.totemguard.common.physics.prescan.FastDetector;
import com.deathmotion.totemguard.common.physics.prescan.TeleportFilter;
import com.deathmotion.totemguard.common.physics.ground.GroundState;
import com.deathmotion.totemguard.common.physics.ground.GroundFacts;
import com.deathmotion.totemguard.common.physics.ground.GroundResolver;
import com.deathmotion.totemguard.common.physics.mitigation.MitigationTracker;
import com.deathmotion.totemguard.common.physics.hover.HoverDetector;
import com.deathmotion.totemguard.common.physics.push.KnockbackTracker;
import com.deathmotion.totemguard.common.physics.push.PistonWindow;
import com.deathmotion.totemguard.common.physics.push.EntityPushTracker;
import com.deathmotion.totemguard.common.physics.push.RiptideWindow;
import com.deathmotion.totemguard.common.physics.input.InputResolver;
import com.deathmotion.totemguard.common.physics.input.PlayerInput;
import com.deathmotion.totemguard.common.physics.medium.BubbleLift;
import com.deathmotion.totemguard.common.physics.medium.MediumKind;
import com.deathmotion.totemguard.common.physics.medium.model.LandModel;
import com.deathmotion.totemguard.common.physics.medium.MediumModel;
import com.deathmotion.totemguard.common.physics.medium.MediumSample;
import com.deathmotion.totemguard.common.physics.medium.MediumScan;
import com.deathmotion.totemguard.common.physics.medium.GlideState;
import com.deathmotion.totemguard.common.physics.medium.MediumSelect;
import com.deathmotion.totemguard.common.physics.medium.StuckFactor;
import com.deathmotion.totemguard.common.physics.phase.EmbedExemptions;
import com.deathmotion.totemguard.common.physics.phase.PhaseTracker;
import com.deathmotion.totemguard.common.physics.preset.PhysicsPreset;
import com.deathmotion.totemguard.common.physics.collision.ColliderBuffer;
import com.deathmotion.totemguard.common.physics.collision.ColliderCollector;
import com.deathmotion.totemguard.common.physics.collision.ContactReport;
import com.deathmotion.totemguard.common.physics.collision.CollisionSweep;
import com.deathmotion.totemguard.common.physics.collision.TraitSampler;
import com.deathmotion.totemguard.common.physics.trace.TickRecorder;
import com.deathmotion.totemguard.common.physics.trace.TraceRecording;
import com.deathmotion.totemguard.common.physics.verdict.BoundBreach;
import com.deathmotion.totemguard.common.physics.verdict.DeclineReason;
import com.deathmotion.totemguard.common.physics.verdict.FallFinding;
import com.deathmotion.totemguard.common.physics.verdict.MitigationOutcome;
import com.deathmotion.totemguard.common.physics.verdict.PhysicsVerdict;
import com.deathmotion.totemguard.common.physics.verdict.TickOutcome;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.data.Data;
import com.deathmotion.totemguard.common.player.data.MovementData;
import com.deathmotion.totemguard.common.player.inventory.InventoryConstants;
import com.deathmotion.totemguard.common.util.ClientMath;
import com.deathmotion.totemguard.common.world.WorldMirror;
import com.deathmotion.totemguard.common.world.block.BlockReader;
import com.deathmotion.totemguard.common.world.shape.ShapeQuery;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.world.Location;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;

@Accessors(fluent = true)
public final class PhysicsEngine {

    private static final double DEEP_FALL_POWDER_SNOW = 2.5;
    private static final double HARVEST_HORIZONTAL_MARGIN = 0.3;
    private static final double HARVEST_UP_MARGIN = 2.1;
    private static final double HARVEST_DOWN_MARGIN = 2.6;
    private static final double PRESERVED_WARP_ACCEL = 0.2;
    private static final double HOVER_EXCESS = MotionDefaults.GRAVITY;
    private static final int HOVER_SETBACK_LIMIT = 2;
    private static final double CEILING_FLUSH_EPS = 1.0e-6;
    private static final double SUPPORT_CONTACT_EPS = 0.02;
    private static final int BOUNCE_WINDOW = 4;
    private static final double POSE_FIT_EPS = 1.0e-7;
    private static final double GLIDE_PRESERVE_GAP = 0.1;

    private final TGPlayer player;
    private final Data data;
    private final WorldMirror world;
    private final BlockReader reader;

    private final AreaBounds bounds = new AreaBounds();
    private final AreaBounds glideProbe = new AreaBounds();
    private final ResidualCarry carry = new ResidualCarry();
    private final ColliderBuffer colliders = new ColliderBuffer();
    private final CollisionSweep sweep = new CollisionSweep();
    private final ContactReport contact = new ContactReport();
    private final GroundResolver groundResolver = new GroundResolver();
    private final InputResolver inputResolver;
    private final MediumSelect mediums = new MediumSelect();
    private final MediumSample sample = new MediumSample();
    private final StuckFactor stuckFactor = new StuckFactor();
    private final BubbleLift bubble = new BubbleLift();
    private final KnockbackTracker knockback;
    private final PistonWindow pistons;
    private final RiptideWindow riptide;
    private final PhaseTracker phase = new PhaseTracker();
    private final EmbedExemptions exemptions = new EmbedExemptions();
    private final TrustTracker trust = new TrustTracker();
    private final TeleportFilter teleportFilter = new TeleportFilter();
    private final FastDetector fastDetector = new FastDetector();
    private final GroundSpoofDetector groundSpoofDetector = new GroundSpoofDetector();
    private final HoverDetector hover = new HoverDetector();
    private final MitigationTracker mitigation;
    private final FallTracker fall;
    private final TraceRecording trace;

    private boolean initialized;
    private MotionArea carried = MotionArea.rest();
    private GameMode lastGameMode;
    private boolean scannedThisTick;
    private GroundFacts groundThisTick;
    private PlayerInput inputThisTick;
    private MediumModel mediumThisTick;
    private double bounceAltCenterX;
    private double bounceAltCenterZ;
    private int bounceWindow;
    private boolean bounceAltValid;
    private boolean bounceAltUsedLast;
    private boolean glideMediumLastTick;
    private double preStepCarriedX, preStepCarriedZ, preStepCarriedFloor, preStepCarriedCeil;
    private double lastPoseHeight = MotionDefaults.STANDING_HEIGHT;
    private double lastPoseBase = MotionDefaults.STANDING_HEIGHT;
    private double lastFeetClearance = Double.MAX_VALUE;

    @Getter
    private PhysicsVerdict verdict = PhysicsVerdict.INITIAL;

    public PhysicsEngine(TGPlayer player, Data data, WorldMirror world) {
        this.player = player;
        this.data = data;
        this.world = world;
        this.reader = world.reader();
        this.inputResolver = new InputResolver(data);
        this.knockback = new KnockbackTracker(data.getExternalVelocityData());
        this.pistons = new PistonWindow(data.getPistonData());
        this.riptide = new RiptideWindow(data);
        this.mitigation = new MitigationTracker(data);
        this.fall = new FallTracker(data, reader);
        this.trace = new TraceRecording(player);
    }

    public void onFlying() {
        ConfigView view = TGPlatform.getInstance().getConfigRepository().configView();
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

        double half = data.getAttributeData().width() / 2.0;
        double height = poseHeight();
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
        groundThisTick = null;
        inputThisTick = null;
        mediumThisTick = null;
        try {
            if (!initialized) {
                initialized = true;
                groundResolver.seed(movement.isOnGround());
                carried = MotionArea.seeded(dx, dz, dy);
                verdict = PhysicsVerdict.INITIAL;
                phase.seedGrace();
                seedEmbedExemptions(current, half, height);
                return;
            }

            switch (teleportFilter.classify(movement, data.getTeleportData(), data.getMitigationService().setbackPending())) {
                case RESYNC_REST_PENDING_SETBACK -> {
                    carried = MotionArea.rest();
                    decline(DeclineReason.RESYNC, dx, dy, dz, false, current, half, height);
                    return;
                }
                case RESYNC_REST -> {
                    carried = new MotionArea(0.0, 0.0, 0.0, 0.0, jumpCeiling());
                    decline(DeclineReason.RESYNC, dx, dy, dz, false, current, half, height);
                    return;
                }
                case RESYNC_PRESERVED -> {
                    coastFrozenMomentum();
                    teleportFilter.startPreserveGrace();
                    decline(DeclineReason.RESYNC, dx, dy, dz, false, current, half, height);
                    return;
                }
                case RESYNC_OTHER -> {
                    decline(DeclineReason.RESYNC, dx, dy, dz, true, current, half, height);
                    return;
                }
                case TELEPORT_PRESERVED -> {
                    coastFrozenMomentum();
                    decline(DeclineReason.TELEPORT, dx, dy, dz, false, current, half, height);
                    return;
                }
                case TELEPORT -> {
                    decline(DeclineReason.TELEPORT, dx, dy, dz, true, current, half, height);
                    return;
                }
                case NONE -> {
                }
            }

            if (!world.readiness().ready()) {
                int feetChunkX = floor(current.getX()) >> 4;
                int feetChunkZ = floor(current.getZ()) >> 4;
                if (reader.columnLoaded(feetChunkX, feetChunkZ)) {
                    world.readiness().requestReadiness(player.getLatencyHandler());
                }
                decline(DeclineReason.LOADING, dx, dy, dz, true, current, half, height);
                return;
            }

            if (!movement.isCameraIsSelf()) {
                decline(DeclineReason.CAMERA, dx, dy, dz, true, current, half, height);
                return;
            }
            DeclineReason bail = DeclineCheck.check(data, world.border(), current.getX(), current.getZ());
            if (bail != null) {
                decline(bail, dx, dy, dz, true, current, half, height);
                return;
            }

            if (groundSpoofDetector.provoked(movement.isOnGround(), dy)) {
                flagDetection(BoundBreach.GROUNDSPOOF, dx, dy, dz,
                        0.0, Math.abs(dy) - GroundSpoofDetector.VERTICAL_EPS);
                return;
            }
            double observedSpeed = ClientMath.horizontalDistance(dx, dz);
            switch (fastDetector.evaluate(observedSpeed, data.getExternalVelocityData().isActive())) {
                case DECLINE -> {
                    decline(DeclineReason.FAST, dx, dy, dz, true, current, half, height);
                    return;
                }
                case FLAG -> {
                    flagDetection(BoundBreach.FAST, dx, dy, dz,
                            observedSpeed - FastDetector.HORIZONTAL_CAP, 0.0);
                    return;
                }
                case NONE -> {
                }
            }

            if (!reader.columnLoaded(floor(current.getX()) >> 4, floor(current.getZ()) >> 4)) {
                decline(DeclineReason.UNLOADED, dx, dy, dz, true, current, half, height);
                return;
            }

            scanTick(previous, current, half, height, dx, dy, dz);
            GroundFacts ground = groundResolver.resolve(dy, contact, sample.fluid(),
                    data.getAttributeData().stepHeight(), carried.floorVy(), data.isSneaking());
            groundThisTick = ground;
            bubble.observe(sample.bubbleAscent());
            mediums.water().advanceEntryWindow(sample.fluid(), ground.wasFluid());
            stuckFactor.advanceWindow(sample.stuckAlongPath());

            PlayerInput input = inputResolver.build(movement, contact, ground, sample.fluid(), dx, dy, dz);
            inputThisTick = input;
            MediumModel medium = selectMedium(false);

            switch (trust.classify(movement.isLastFlyingPositionChanged(), movement.isLastFlyingWasDuplicate(),
                    player.supportsEndTick(), data.getTeleportData().lastPacketWasTeleport(),
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

    public void onTickEnd() {
        boolean sawFlying = trust.onTickEnd();
        mitigation.clearTickFlags();
        if (sawFlying) {
            // The check listens on tick-end packets too, so one violation must not alert twice.
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
        double half = data.getAttributeData().width() / 2.0;
        double height = poseHeight();
        exemptions.onBlockApplied(reader, shapeQuery(current.getY()),
                current.getX() - half, current.getY(), current.getZ() - half,
                current.getX() + half, current.getY() + height, current.getZ() + half,
                x, y, z, reader.stateMap().toClientId(serverStateId));
    }

    public void onInventoryToggled() {
        clearHistory();
    }

    public void clearHistory() {
        teleportFilter.reset();
        hover.reset();
        bubble.reset();
        stuckFactor.reset();
        mediums.reset();
        carry.clear();
        groundResolver.clearWindows();
        inputResolver.clear();
        fastDetector.reset();
        groundSpoofDetector.reset();
        phase.clear();
        exemptions.clear();
        bounceAltValid = false;
        bounceAltUsedLast = false;
        bounceWindow = 0;
        lastFeetClearance = Double.MAX_VALUE;
        lastPoseBase = MotionDefaults.STANDING_HEIGHT;
        verdict = PhysicsVerdict.INITIAL;
    }

    public void reset() {
        initialized = false;
        carried = MotionArea.rest();
        trust.reset();
        groundResolver.reset();
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
        player.getVehicleEngine().reset();
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

    public boolean improperSprint() {
        return inputResolver.improperSprint();
    }

    public MotionArea carriedArea() {
        return carried;
    }

    private void onSilentTick() {
        if (!initialized) return;
        if (data.isDead()) return;
        if (!world.readiness().ready()) return;
        ConfigView view = TGPlatform.getInstance().getConfigRepository().configView();
        if (!view.physicsEngineEnabled()) return;
        if (data.getTeleportData().hasPendingTeleport()) return;
        MovementData movement = data.getMovementData();
        if (!movement.isCameraIsSelf()) return;
        Location current = movement.getCurrent();
        if (DeclineCheck.check(data, world.border(), current.getX(), current.getZ()) != null) return;
        PhysicsPreset preset = view.physicsPreset();

        reader.resetCounters();
        double half = data.getAttributeData().width() / 2.0;
        double height = poseHeight();
        scannedThisTick = false;
        groundThisTick = null;
        inputThisTick = null;
        mediumThisTick = null;
        scanTick(current, current, half, height, 0.0, 0.0, 0.0);
        if (contact.supportGap() == ContactReport.NO_SUPPORT
                && !reader.columnLoaded(floor(current.getX()) >> 4, floor(current.getZ()) >> 4)) {
            return;
        }
        GroundFacts ground = groundResolver.resolve(0.0, contact, sample.fluid(),
                data.getAttributeData().stepHeight(), carried.floorVy(), data.isSneaking());
        groundThisTick = ground;
        PlayerInput input = inputResolver.build(movement, contact, ground, sample.fluid(), 0.0, 0.0, 0.0);
        inputThisTick = input;
        MediumModel medium = selectMedium(true);

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
                player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_26_1),
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
                           MediumModel medium, PlayerInput input,
                           GroundFacts ground, PhysicsPreset preset) {
        hover.onJudged();
        preStepCarriedX = carried.centerX();
        preStepCarriedZ = carried.centerZ();
        preStepCarriedFloor = carried.floorVy();
        preStepCarriedCeil = carried.ceilVy();
        double carriedFloorAtStart = carried.floorVy();
        boolean landMedium = sample.landMedium();

        AreaExpander.grow(carried, medium, sample, input, ground, contact,
                stuckFactor, bubble, knockback, pistons, riptide, carry, preset, bounds);
        if (medium.kind() == MediumKind.LAND && data.getGlideData().justExited()) {
            glideProbe.reset(carried);
            mediums.glide().prepare(false, false, data.getFireworkData());
            mediums.glide().horizontalOptions(input, ground, glideProbe);
            double reachX = glideProbe.centerX() - bounds.centerX();
            double reachZ = glideProbe.centerZ() - bounds.centerZ();
            bounds.expandRadius(ClientMath.horizontalDistance(reachX, reachZ) + glideProbe.radius());
        }
        if (medium.kind() == MediumKind.GLIDE && data.getGlideData().riptideActive()) {
            double strength = data.getGlideData().riptideStrength();
            double impulseY = input.lookY() * strength;
            glideProbe.reset(carried);
            glideProbe.centerX(carried.centerX() + input.lookX() * strength);
            glideProbe.centerZ(carried.centerZ() + input.lookZ() * strength);
            glideProbe.floor(carried.floorVy() + impulseY);
            glideProbe.ceiling(carried.ceilVy() + impulseY);
            mediums.glide().horizontalOptions(input, ground, glideProbe);
            mediums.glide().verticalOptions(input, ground, contact, glideProbe);
            bounds.altCenter(glideProbe.centerX(), glideProbe.centerZ());
            bounds.expandRadius(glideProbe.radius());
            bounds.raiseCeiling(glideProbe.ceiling());
            if (impulseY < 0.0) bounds.lowerFloor(glideProbe.floor());
        }
        Location current = data.getMovementData().getCurrent();
        double half = data.getAttributeData().width() / 2.0;
        double height = lastPoseHeight;
        EntityPushTracker.apply(bounds, world.entities(),
                Math.min(current.getX() - dx, current.getX()) - half, Math.min(current.getY() - dy, current.getY()),
                Math.min(current.getZ() - dz, current.getZ()) - half,
                Math.max(current.getX() - dx, current.getX()) + half,
                Math.max(current.getY() - dy, current.getY()) + height,
                Math.max(current.getZ() - dz, current.getZ()) + half,
                half, height);

        boolean offerBounceAlt = landMedium && bounceAltValid && !bounceAltUsedLast
                && !bounds.hasAltCenter();
        if (offerBounceAlt) {
            bounds.altCenter(bounceAltCenterX, bounceAltCenterZ);
        }

        double arrestCap = landMedium && dy <= -preset.verticalFlagEpsilon()
                ? Math.max(0.0, contact.nearestSupportGap() - preset.verticalNoisePad())
                : -1.0;
        JudgedExcess excess = AreaJudge.judge(bounds, dx, dy, dz, arrestCap);
        bounceAltUsedLast = offerBounceAlt && excess.altCenterUsed();

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

        BoundBreach breach = classify(horizontalExcess, excess.ascent(), excess.descent(), phaseExcess, preset);

        boolean sneakEdge = input.sneaking() && ground.groundedStart() && landMedium
                && dy <= 0.0
                && observedSpeed < ClientMath.horizontalDistance(bounds.centerX(), bounds.centerZ())
                + bounds.radius() - preset.horizontalFlagEpsilon()
                && contact.supportGap() > preset.horizontalFlagEpsilon();
        boolean carryPredicted = stepped || teleportFilter.inPreserveGrace() || sneakEdge;
        teleportFilter.tickPreserveGrace();

        advanceArea(dx, dy, dz, medium, input, ground, preset, excess, carryPredicted, carriedFloorAtStart);

        boolean phaseBreach = breach == BoundBreach.PHASE_CROSS || breach == BoundBreach.PHASE_EMBED;
        carry.store(phaseBreach ? 0.0 : horizontalExcess,
                breach == BoundBreach.DESCENT_FLOOR ? 0.0 : excess.ascent(),
                false, preset.residualCarryCap());

        verdict = buildVerdict(TickOutcome.JUDGED, null, breach,
                dx, dy, dz, horizontalExcess, excess.ascent(), excess.descent(), phaseExcess,
                excess.altCenterUsed() && breach == null ? 1.0 : 0.0, input, ground);
    }

    private void coastTick(boolean withheld, double dx, double dy, double dz,
                           MediumModel medium, PlayerInput input, GroundFacts ground, PhysicsPreset preset) {
        coastArea(medium, input, ground, preset);
        teleportFilter.tickPreserveGrace();

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

    private void advanceArea(double dx, double dy, double dz, MediumModel medium, PlayerInput input,
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
        double advancedFloorVy = advancedVy;
        if (anchor > 0.0 && contact.ceilingClearance() <= CEILING_FLUSH_EPS) {
            advancedFloorVy = medium.advanceVertical(0.0, input);
        } else if (bounds.legalVy() < anchor && contact.nearestSupportGap() > SUPPORT_CONTACT_EPS) {
            advancedFloorVy = Math.min(advancedFloorVy, medium.advanceVertical(bounds.legalVy(), input));
        }
        if (medium.kind() == MediumKind.CLIMB && bounds.legalVy() < 0.0) {
            advancedFloorVy = Math.min(advancedFloorVy, mediums.land().advanceVertical(bounds.legalVy(), input));
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

        if (medium == mediums.land() && data.getGlideData().exitActive()) {
            double groundDecay = frictionMax * speedFactor;
            double airDecay = LandModel.AIR_FRICTION * speedFactor;
            if (airDecay > groundDecay) {
                double span = ClientMath.horizontalDistance(bounds.legalX(), bounds.legalZ())
                        * (airDecay - groundDecay);
                carried = new MotionArea(carried.centerX(), carried.centerZ(),
                        carried.slack() + span, carried.floorVy(), carried.ceilVy());
            }
        }

        if (medium == mediums.glide() && mediums.glide().dualActive()) {
            double glideX = bounds.legalX() * speedFactor;
            double glideZ = bounds.legalZ() * speedFactor;
            double freeFallShrink = ClientMath.horizontalDistance(glideX, glideZ) * (1.0 - frictionMax);
            carried = new MotionArea(glideX, glideZ, carried.slack() + freeFallShrink,
                    mediums.land().advanceVertical(bounds.legalVy(), input), bounds.legalVy());
        }

        if (medium == mediums.glide() && contact.nearestSupportGap() <= GLIDE_PRESERVE_GAP) {
            double exitFloor = mediums.land().advanceVertical(bounds.floor(), input);
            if (exitFloor < carried.floorVy()) {
                carried = new MotionArea(carried.centerX(), carried.centerZ(), carried.slack(),
                        exitFloor, carried.ceilVy());
            }
        }

        if (ground.bounced() && carriedFloorAtStart < 0.0 && contact.supportBounce() > 0.0) {
            double reflected = -carriedFloorAtStart * contact.supportBounce();
            double advanced = medium.advanceVertical(reflected, input);
            carried = new MotionArea(carried.centerX(), carried.centerZ(), carried.slack(),
                    advanced, advanced);
        }

        if (contact.supportBounce() > 0.0) {
            bounceWindow = BOUNCE_WINDOW;
        } else if (bounceWindow > 0) {
            bounceWindow--;
        }
        if (bounceWindow > 0) {
            bounceAltCenterX = bounds.legalX() * LandModel.AIR_FRICTION;
            bounceAltCenterZ = bounds.legalZ() * LandModel.AIR_FRICTION;
            bounceAltValid = true;
        } else {
            bounceAltValid = false;
        }
    }

    private void coastArea(MediumModel medium, PlayerInput input, GroundFacts ground, PhysicsPreset preset) {
        if (sample.stuck() && !sample.fluid()) {
            carried = MotionArea.rest();
            carry.clear();
            return;
        }
        AreaExpander.grow(carried, medium, sample, input, ground, contact,
                stuckFactor, bubble, knockback, pistons, riptide, carry, preset, bounds);
        double accel = medium.accelBound(input, ground);
        double frictionMax = medium.frictionMax(input, ground);
        if (medium.kind() == MediumKind.GLIDE) {
            carried = new MotionArea(bounds.centerX(), bounds.centerZ(),
                    (carried.slack() + accel), bounds.floor(), bounds.ceiling());
            carry.clear();
            bounceAltValid = false;
            bounceAltUsedLast = false;
            bounceWindow = 0;
            return;
        }
        double floorSource = ground.groundedEnd() ? 0.0 : carried.floorVy();
        double ceilSource = ground.groundedEnd() ? 0.0 : bounds.ceiling();
        carried = AreaAdvancer.coast(carried, accel, frictionMax,
                medium.advanceVertical(floorSource, input),
                medium.advanceVertical(ceilSource, input));
        carry.clear();
        bounceAltValid = false;
        bounceAltUsedLast = false;
        bounceWindow = 0;
    }

    private void coastFrozenMomentum() {
        double gravity = data.getAttributeData().gravity();
        double floor = carried.floorVy();
        double advancedFloor = gravity > 0.0 ? (floor - gravity) * MotionDefaults.VERTICAL_DRAG : floor;
        carried = new MotionArea(carried.centerX(), carried.centerZ(),
                carried.slack() + PRESERVED_WARP_ACCEL,
                advancedFloor, Math.max(carried.ceilVy(), jumpCeiling()));
    }

    private void decline(DeclineReason reason, double dx, double dy, double dz, boolean reseed,
                         Location current, double half, double height) {
        if (reseed) carried = MotionArea.seeded(dx, dz, dy);
        trust.clearDoubleMoveStreak();
        hover.onDeclined();
        inputResolver.onDecline();
        if (reason != DeclineReason.FAST) {
            seedEmbedExemptions(current, half, height);
        }
        if (reason == DeclineReason.RESYNC || reason == DeclineReason.TELEPORT
                || reason == DeclineReason.UNLOADED || reason == DeclineReason.LOADING) {
            phase.seedGrace();
            phase.invalidateEmbed();
        }
        if (reason == DeclineReason.RESYNC || reason == DeclineReason.TELEPORT) {
            groundResolver.displaced();
        }
        carry.clear();
        bounceAltValid = false;
        bounceAltUsedLast = false;
        bounceWindow = 0;
        verdict = buildVerdict(TickOutcome.DECLINED, reason, null,
                dx, dy, dz, 0.0, 0.0, 0.0, 0.0, 0.0, null, null);
    }

    private void flagDetection(BoundBreach breach, double dx, double dy, double dz,
                            double horizontalExcess, double verticalExcess) {
        hover.onDeclined();
        inputResolver.improperSprint(false);
        carried = MotionArea.seeded(dx, dz, dy);
        carry.clear();
        bounceAltValid = false;
        bounceAltUsedLast = false;
        bounceWindow = 0;
        verdict = buildVerdict(TickOutcome.JUDGED, null, breach,
                dx, dy, dz, Math.max(0.0, horizontalExcess), Math.max(0.0, verticalExcess), 0.0, 0.0,
                0.0, null, null);
    }

    private void observeTail(ConfigView view, PhysicsPreset preset, double dy) {
        fall.observe(verdict.outcome(), verdict.declineReason(), verdict.breach(),
                dy, data.getMovementData().isOnGround(),
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
        verdict = verdict.withOutcome(mitigation.outcome(), fall.finding(), inputResolver.improperSprint());

        if (scannedThisTick) {
            Location poseAt = data.getMovementData().getCurrent();
            double poseHalf = data.getAttributeData().width() / 2.0;
            lastFeetClearance = poseHeadroom(poseAt.getX() - poseHalf, poseAt.getY(),
                    poseAt.getZ() - poseHalf, poseAt.getX() + poseHalf, poseAt.getZ() + poseHalf);
        }

        data.getExternalVelocityData().tick();
        data.getPistonData().tick();
        data.getEffectData().tick();
        data.getGlideData().tick();
        data.getFireworkData().tick();

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
                                        PlayerInput input, GroundFacts ground) {
        return new PhysicsVerdict(outcome, reason, breach,
                dx, dy, dz,
                horizontalExcess, ascentExcess, descentExcess, phaseExcess,
                bounds.centerX(), bounds.centerZ(), bounds.radius(),
                bounds.ceiling(), bounds.floor() - bounds.descentSlack(),
                scannedThisTick && mediumThisTick != null ? mediumThisTick.kind() : MediumKind.LAND,
                ground != null ? ground.start() : GroundState.AMBIGUOUS,
                data.isOpenInventory(),
                impulseUsed > 0.0,
                inputResolver.improperSprint(),
                MitigationOutcome.NONE,
                FallFinding.NONE);
    }

    private boolean airborneNow(GroundFacts ground, PhysicsPreset preset) {
        return sample.landMedium() && !ground.groundedEnd()
                && contact.nearestSupportGap() > preset.hoverMinGap()
                && !contact.startOverlapping();
    }

    private void seedEmbedExemptions(Location current, double half, double height) {
        exemptions.seedBodyOverlaps(reader, shapeQuery(current.getY()),
                current.getX() - half, current.getY(), current.getZ() - half,
                current.getX() + half, current.getY() + height, current.getZ() + half);
    }

    private MediumModel selectMedium(boolean silent) {
        GlideState glideState = data.isGliding()
                ? GlideState.FLAG
                : data.getGlideData().claimActive() ? GlideState.CLAIM : GlideState.NONE;
        MediumModel medium = mediums.select(sample, glideState);
        boolean glideNow = medium == mediums.glide();
        if (glideNow) {
            mediums.glide().prepare(glideState == GlideState.CLAIM, silent, data.getFireworkData());
        } else if (glideMediumLastTick) {
            data.getGlideData().armExit();
        }
        glideMediumLastTick = glideNow;
        mediumThisTick = medium;
        return medium;
    }

    private ShapeQuery shapeQuery(double feetY) {
        return new ShapeQuery(feetY, data.isSneaking(), standsOnPowderSnow(),
                fall.engineFall() > DEEP_FALL_POWDER_SNOW);
    }

    private boolean standsOnPowderSnow() {
        ItemStack boots = player.getInventory().getItem(InventoryConstants.SLOT_BOOTS);
        return boots != null && boots.getType() == ItemTypes.LEATHER_BOOTS;
    }

    private double effectiveSpeedFactor() {
        double raw = contact.supportSpeedFactor();
        if (raw >= 1.0) return 1.0;
        if (!player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_20_5)) return 1.0;
        double efficiency = data.getAttributeData().movementEfficiency();
        return raw + efficiency * (1.0 - raw);
    }

    private double jumpCeiling() {
        return data.getAttributeData().jumpStrength()
                + (data.getEffectData().hasJumpBoost()
                ? 0.1 * (data.getEffectData().jumpBoostAmplifier() + 1) : 0.0);
    }

    private double poseHeight() {
        double scale = data.getAttributeData().scale();
        double base;
        if (data.isSwimming() || data.isGliding() || data.isSpinAttacking()) {
            base = MotionDefaults.COMPACT_HEIGHT;
        } else {
            double want = data.isSneaking() ? MotionDefaults.SNEAKING_HEIGHT : MotionDefaults.STANDING_HEIGHT;
            if (poseFits(want * scale)) base = want;
            else if (poseFits(MotionDefaults.SNEAKING_HEIGHT * scale)) base = MotionDefaults.SNEAKING_HEIGHT;
            else if (poseFits(MotionDefaults.COMPACT_HEIGHT * scale)) base = MotionDefaults.COMPACT_HEIGHT;
            else base = lastPoseBase;
        }
        lastPoseBase = base;
        lastPoseHeight = base * scale;
        return lastPoseHeight;
    }

    private boolean poseFits(double height) {
        return lastFeetClearance >= height - POSE_FIT_EPS;
    }

    private double poseHeadroom(double minX, double feetY, double minZ, double maxX, double maxZ) {
        double headroom = Double.MAX_VALUE;
        int count = colliders.count();
        for (int i = 0; i < count; i++) {
            if (!ColliderBuffer.clipEligible(colliders.tagOf(i))) continue;
            if (!poseOverlaps(minX, maxX, colliders.minX(i), colliders.maxX(i))) continue;
            if (!poseOverlaps(minZ, maxZ, colliders.minZ(i), colliders.maxZ(i))) continue;
            if (colliders.maxY(i) <= feetY + POSE_FIT_EPS) continue;
            double bottom = colliders.minY(i);
            if (bottom <= feetY + POSE_FIT_EPS) return -1.0;
            double room = bottom - feetY;
            if (room < headroom) headroom = room;
        }
        return headroom;
    }

    private static boolean poseOverlaps(double movingMin, double movingMax, double boxMin, double boxMax) {
        return movingMin + POSE_FIT_EPS < boxMax && movingMax - POSE_FIT_EPS > boxMin;
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
