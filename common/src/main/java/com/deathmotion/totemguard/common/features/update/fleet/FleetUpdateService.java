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

package com.deathmotion.totemguard.common.features.update.fleet;

import com.deathmotion.totemguard.api.event.impl.TGPluginShutdownEvent;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.network.ServerIdentity;
import com.deathmotion.totemguard.common.platform.sender.Sender;
import com.deathmotion.totemguard.common.redis.RedisConnection;
import com.deathmotion.totemguard.common.redis.RedisRepositoryImpl;
import com.deathmotion.totemguard.common.redis.broker.packets.PacketRegistry;
import com.deathmotion.totemguard.common.redis.broker.packets.Packets;
import com.deathmotion.totemguard.common.redis.broker.packets.impl.SyncFleetJarReadyPacket;
import com.deathmotion.totemguard.common.redis.broker.packets.impl.SyncFleetUpdateAckPacket;
import com.deathmotion.totemguard.common.redis.broker.packets.impl.SyncFleetUpdateRequestPacket;
import com.deathmotion.totemguard.host.LoaderController;
import com.deathmotion.totemguard.host.TGPluginHost;
import com.deathmotion.totemguard.host.UpdateTarget;
import io.lettuce.core.SetArgs;
import io.lettuce.core.api.async.RedisAsyncCommands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class FleetUpdateService {

    private static final String KEY_PREFIX = "totemguard:fleet";
    private static final Duration LOCK_TTL = Duration.ofMinutes(5);
    private static final Duration BLOB_TTL = Duration.ofHours(1);
    private static final long MAX_BLOB_BYTES = 64L * 1024 * 1024;
    private static final int MAX_JITTER_SECONDS = 30;

    private final TGPlatform platform;
    private final Logger logger;
    private final PacketRegistry registry;
    private final Executor worker;

    private final ConcurrentMap<UUID, PendingRequest> pending = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, AckCollector> outboundCollectors = new ConcurrentHashMap<>();

    public FleetUpdateService(TGPlatform platform, PacketRegistry registry) {
        this.platform = platform;
        this.logger = platform.getLogger();
        this.registry = registry;
        this.worker = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "TotemGuard-FleetUpdate");
            t.setDaemon(true);
            return t;
        });
    }

    private static String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(bytes);
            return HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public void register() {
        registry.registerProcessor(Packets.SYNC_FLEET_UPDATE_REQUEST.<SyncFleetUpdateRequestPacket.Payload>packet(),
                this::onRequest);
        registry.registerProcessor(Packets.SYNC_FLEET_JAR_READY.<SyncFleetJarReadyPacket.Payload>packet(),
                this::onJarReady);
        registry.registerProcessor(Packets.SYNC_FLEET_UPDATE_ACK.<SyncFleetUpdateAckPacket.Payload>packet(),
                this::onAck);
    }

    public void shutdown() {
        outboundCollectors.clear();
        pending.clear();
        if (worker instanceof java.util.concurrent.ExecutorService es) {
            es.shutdownNow();
        }
    }

    /**
     * Operator-issued entry point. Broadcasts the update request to the fleet and
     * tracks acks for the supplied sender.
     */
    public void issueUpdate(@NotNull Sender originator, boolean force, boolean dryRun, boolean restartFleet) {
        RedisRepositoryImpl redis = platform.getRedisRepository();
        if (redis == null || !redis.isConnected() || !redis.isClusterMode()) {
            // Standalone: no fleet to coordinate with, just run the update against the
            // local loader. Same flow as a fleet member, minus the redis blob dance.
            runStandalone(originator, force, dryRun, restartFleet);
            return;
        }

        ServerIdentity identity = identity();
        if (identity == null) {
            originator.sendMessage(Component.text("Fleet update unavailable: this server has no fleet identity.", NamedTextColor.RED));
            return;
        }

        UUID requestId = UUID.randomUUID();
        AckCollector collector = new AckCollector(originator);
        outboundCollectors.put(requestId, collector);

        SyncFleetUpdateRequestPacket.Payload payload = new SyncFleetUpdateRequestPacket.Payload(
                requestId, identity.instanceId(), platform.getNetworkPresenceRepository().getLocalServerName(),
                force, dryRun, restartFleet);

        originator.sendMessage(Component.text(
                (dryRun ? "Dry-run " : "") + "fleet update broadcast (id " + shortId(requestId) + ")."
                        + (restartFleet ? " Fleet restart after staging." : ""),
                NamedTextColor.GOLD));

        // Schedule collector expiry. After 2 minutes we report what we got.
        platform.getScheduler().runAsyncTaskDelayed(() -> finalizeCollector(requestId),
                120, TimeUnit.SECONDS);

        redis.publish(Packets.SYNC_FLEET_UPDATE_REQUEST.<SyncFleetUpdateRequestPacket.Payload>packet(), payload)
                .exceptionally(ex -> {
                    logger.log(Level.WARNING, "Failed to publish fleet update request", ex);
                    return false;
                });
    }

    private void runStandalone(Sender originator, boolean force, boolean dryRun, boolean restartLocal) {
        TGPluginHost host = platform.getPluginHost();
        Optional<LoaderController> controllerOpt = host == null ? Optional.empty() : host.loaderController();
        if (controllerOpt.isEmpty()) {
            originator.sendMessage(Component.text(
                    "Update unavailable: this server is not managed by the loader.", NamedTextColor.RED));
            return;
        }
        LoaderController controller = controllerOpt.get();

        worker.execute(() -> {
            try {
                UpdateTarget target = controller.resolveTarget();
                if ("LOCAL".equalsIgnoreCase(target.source())) {
                    originator.sendMessage(Component.text(
                            "Local source does not participate in updates. Edit loader-config.yml first.",
                            NamedTextColor.YELLOW));
                    return;
                }

                originator.sendMessage(Component.text(
                        "Resolved " + target.resolutionKey() + ".", NamedTextColor.GOLD));

                if (dryRun) {
                    originator.sendMessage(Component.text(
                            "Dry-run: would download " + target.fileName() + " from " + target.source() + ".",
                            NamedTextColor.YELLOW));
                    return;
                }

                if (!force && target.version().equalsIgnoreCase(controller.info().loadedVersion())) {
                    originator.sendMessage(Component.text(
                            "Already on " + target.version() + ". Use --force to re-stage.",
                            NamedTextColor.GRAY));
                    return;
                }

                originator.sendMessage(Component.text("Downloading " + target.fileName() + "...", NamedTextColor.GOLD));
                byte[] bytes = controller.download(target);
                String sha = sha256(bytes);
                controller.stageJar(bytes, target.withSha256(sha));

                originator.sendMessage(Component.text(
                        "Staged " + target.version() + ". Restart to apply.", NamedTextColor.GREEN));

                if (restartLocal) {
                    originator.sendMessage(Component.text("Restarting...", NamedTextColor.AQUA));
                    controller.restart(TGPluginShutdownEvent.Reason.UPDATE_TRIGGERED).whenComplete((ok, err) -> {
                        if (err != null) {
                            logger.log(Level.WARNING, "Standalone update restart failed", err);
                            originator.sendMessage(Component.text(
                                    "Restart failed: " + err.getMessage(), NamedTextColor.RED));
                        } else {
                            originator.sendMessage(Component.text(
                                    "TotemGuard restarted on " + target.version() + ".", NamedTextColor.GREEN));
                        }
                    });
                }
            } catch (Throwable t) {
                logger.log(Level.WARNING, "Standalone update failed", t);
                originator.sendMessage(Component.text(
                        "Update failed: " + t.getMessage(), NamedTextColor.RED));
            }
        });
    }

    private void onRequest(SyncFleetUpdateRequestPacket.Payload payload) {
        worker.execute(() -> handleRequest(payload));
    }

    private void onJarReady(SyncFleetJarReadyPacket.Payload payload) {
        worker.execute(() -> handleJarReady(payload));
    }

    private void onAck(SyncFleetUpdateAckPacket.Payload payload) {
        AckCollector collector = outboundCollectors.get(payload.requestId());
        if (collector == null) return;
        collector.record(payload);
    }

    private void handleRequest(SyncFleetUpdateRequestPacket.Payload payload) {
        TGPluginHost host = platform.getPluginHost();
        Optional<LoaderController> controllerOpt = host == null ? Optional.empty() : host.loaderController();
        if (controllerOpt.isEmpty()) {
            sendAck(payload.requestId(), payload.originatorInstanceId(),
                    SyncFleetUpdateAckPacket.Payload.STATUS_SKIPPED,
                    "unknown", "UNKNOWN", "Not managed by loader");
            return;
        }
        LoaderController controller = controllerOpt.get();

        UpdateTarget target;
        try {
            target = controller.resolveTarget();
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Fleet update: failed to resolve target", ex);
            sendAck(payload.requestId(), payload.originatorInstanceId(),
                    SyncFleetUpdateAckPacket.Payload.STATUS_FAILED,
                    "unknown", "UNKNOWN", "resolve: " + ex.getMessage());
            return;
        }

        if ("LOCAL".equalsIgnoreCase(target.source())) {
            sendAck(payload.requestId(), payload.originatorInstanceId(),
                    SyncFleetUpdateAckPacket.Payload.STATUS_SKIPPED,
                    target.version(), target.source(), "Local source does not participate");
            return;
        }

        if (payload.dryRun()) {
            sendAck(payload.requestId(), payload.originatorInstanceId(),
                    SyncFleetUpdateAckPacket.Payload.STATUS_DRY_RUN,
                    target.version(), target.source(),
                    "Would download " + target.resolutionKey());
            return;
        }

        if (!payload.force() && target.version().equalsIgnoreCase(controller.info().loadedVersion())) {
            sendAck(payload.requestId(), payload.originatorInstanceId(),
                    SyncFleetUpdateAckPacket.Payload.STATUS_SKIPPED,
                    target.version(), target.source(), "Already on " + target.version());
            return;
        }

        pending.put(payload.requestId(), new PendingRequest(payload, target, Instant.now()));

        try {
            if (tryAcquireLeader(target.resolutionKey())) {
                runAsLeader(payload, controller, target);
            }
            // If we didn't get the lock, just wait for JarReady to arrive.
        } catch (Throwable t) {
            logger.log(Level.WARNING, "Fleet update leader run failed", t);
            sendAck(payload.requestId(), payload.originatorInstanceId(),
                    SyncFleetUpdateAckPacket.Payload.STATUS_FAILED,
                    target.version(), target.source(), "leader: " + t.getMessage());
            pending.remove(payload.requestId());
        }
    }

    private void runAsLeader(SyncFleetUpdateRequestPacket.Payload request,
                             LoaderController controller, UpdateTarget target) throws Exception {
        RedisConnection connection = connection();
        if (connection == null) return;
        RedisAsyncCommands<byte[], byte[]> async = connection.commands().async();

        byte[] blobKey = blobKey(target.resolutionKey());
        byte[] existing = async.get(blobKey).get(15, TimeUnit.SECONDS);

        byte[] bytes;
        String resolvedSha;
        if (existing != null && !request.force()) {
            bytes = existing;
            resolvedSha = sha256(bytes);
        } else {
            bytes = controller.download(target);
            if (bytes.length > MAX_BLOB_BYTES) {
                throw new IllegalStateException("Downloaded jar exceeds " + MAX_BLOB_BYTES + " byte limit (was "
                        + bytes.length + "). Refusing to broadcast.");
            }
            resolvedSha = sha256(bytes);
            async.set(blobKey, bytes, SetArgs.Builder.ex(BLOB_TTL.toSeconds()))
                    .get(20, TimeUnit.SECONDS);
        }

        UpdateTarget filled = target.withSha256(resolvedSha);
        controller.stageJar(bytes, filled);

        SyncFleetJarReadyPacket.Payload ready = new SyncFleetJarReadyPacket.Payload(
                request.requestId(), target.resolutionKey(), target.source(),
                target.version(), resolvedSha, target.fileName(), bytes.length);
        platform.getRedisRepository()
                .publish(Packets.SYNC_FLEET_JAR_READY.<SyncFleetJarReadyPacket.Payload>packet(), ready);

        sendAck(request.requestId(), request.originatorInstanceId(),
                SyncFleetUpdateAckPacket.Payload.STATUS_STAGED,
                target.version(), target.source(), "leader");

        if (request.restartFleet()) {
            scheduleRestart(request.requestId(), controller, target);
        }

        pending.remove(request.requestId());
    }

    private void handleJarReady(SyncFleetJarReadyPacket.Payload payload) {
        PendingRequest pendingRequest = pending.get(payload.requestId());
        if (pendingRequest == null) return;
        if (!pendingRequest.target.resolutionKey().equals(payload.resolutionKey())) return;

        TGPluginHost host = platform.getPluginHost();
        Optional<LoaderController> controllerOpt = host == null ? Optional.empty() : host.loaderController();
        if (controllerOpt.isEmpty()) return;
        LoaderController controller = controllerOpt.get();

        try {
            RedisConnection connection = connection();
            if (connection == null) return;
            byte[] bytes = connection.commands().async().get(blobKey(payload.resolutionKey()))
                    .get(20, TimeUnit.SECONDS);
            if (bytes == null) {
                sendAck(payload.requestId(), pendingRequest.payload.originatorInstanceId(),
                        SyncFleetUpdateAckPacket.Payload.STATUS_FAILED,
                        payload.version(), payload.source(), "blob expired");
                pending.remove(payload.requestId());
                return;
            }
            String actual = sha256(bytes);
            if (!actual.equalsIgnoreCase(payload.sha256())) {
                sendAck(payload.requestId(), pendingRequest.payload.originatorInstanceId(),
                        SyncFleetUpdateAckPacket.Payload.STATUS_FAILED,
                        payload.version(), payload.source(), "blob sha mismatch");
                pending.remove(payload.requestId());
                return;
            }

            UpdateTarget target = new UpdateTarget(payload.source(), payload.version(),
                    payload.sha256(), payload.sizeBytes(), payload.fileName());
            controller.stageJar(bytes, target);

            sendAck(payload.requestId(), pendingRequest.payload.originatorInstanceId(),
                    SyncFleetUpdateAckPacket.Payload.STATUS_STAGED,
                    payload.version(), payload.source(), "follower");

            if (pendingRequest.payload.restartFleet()) {
                scheduleRestart(payload.requestId(), controller, target);
            } else {
                pending.remove(payload.requestId());
            }
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Fleet update follower failed", ex);
            sendAck(payload.requestId(), pendingRequest.payload.originatorInstanceId(),
                    SyncFleetUpdateAckPacket.Payload.STATUS_FAILED,
                    payload.version(), payload.source(), "follower: " + ex.getMessage());
            pending.remove(payload.requestId());
        }
    }

    private void scheduleRestart(UUID requestId, LoaderController controller, UpdateTarget target) {
        int delaySeconds = ThreadLocalRandom.current().nextInt(MAX_JITTER_SECONDS + 1);
        logger.info("Fleet update: scheduling local restart in " + delaySeconds + "s for " + target.version());

        PendingRequest req = pending.get(requestId);
        UUID originator = req != null ? req.payload.originatorInstanceId() : null;

        platform.getScheduler().runAsyncTaskDelayed(() -> {
            try {
                if (originator != null) {
                    sendAck(requestId, originator,
                            SyncFleetUpdateAckPacket.Payload.STATUS_RESTARTING,
                            target.version(), target.source(), "delay=" + delaySeconds + "s");
                }
                controller.restart(TGPluginShutdownEvent.Reason.UPDATE_TRIGGERED).whenComplete((ok, err) -> {
                    if (err != null) {
                        logger.log(Level.WARNING, "Fleet update restart failed", err);
                    }
                    pending.remove(requestId);
                });
            } catch (Throwable t) {
                logger.log(Level.WARNING, "Fleet update restart trigger failed", t);
                pending.remove(requestId);
            }
        }, delaySeconds, TimeUnit.SECONDS);
    }

    private boolean tryAcquireLeader(String resolutionKey) {
        RedisConnection connection = connection();
        if (connection == null) return false;
        try {
            ServerIdentity identity = identity();
            byte[] value = identity != null ? identity.instanceId().toString().getBytes(StandardCharsets.UTF_8)
                    : "anonymous".getBytes(StandardCharsets.UTF_8);
            String response = connection.commands().async().set(
                    lockKey(resolutionKey), value,
                    SetArgs.Builder.nx().ex(LOCK_TTL.toSeconds())
            ).get(5, TimeUnit.SECONDS);
            return "OK".equals(response);
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Failed to acquire fleet lock for " + resolutionKey, ex);
            return false;
        }
    }

    private void sendAck(UUID requestId, UUID originator, String status, String version, String source, String detail) {
        ServerIdentity identity = identity();
        if (identity == null) return;
        String serverName = platform.getNetworkPresenceRepository() != null
                ? platform.getNetworkPresenceRepository().getLocalServerName()
                : "unknown";
        SyncFleetUpdateAckPacket.Payload payload = new SyncFleetUpdateAckPacket.Payload(
                requestId, identity.instanceId(), serverName,
                status, version, source, detail);
        platform.getRedisRepository().publishToInstance(originator,
                        Packets.SYNC_FLEET_UPDATE_ACK.<SyncFleetUpdateAckPacket.Payload>packet(), payload)
                .exceptionally(ex -> {
                    logger.log(Level.WARNING, "Failed to publish fleet ack", ex);
                    return false;
                });
    }

    private void finalizeCollector(UUID requestId) {
        AckCollector collector = outboundCollectors.remove(requestId);
        if (collector == null) return;
        collector.report(requestId);
    }

    private RedisConnection connection() {
        RedisRepositoryImpl redis = platform.getRedisRepository();
        return redis != null ? redis.connection() : null;
    }

    private ServerIdentity identity() {
        return platform.getNetworkPresenceRepository() != null
                ? platform.getNetworkPresenceRepository().identity() : null;
    }

    private byte[] lockKey(String resolutionKey) {
        return (KEY_PREFIX + ":lock:" + resolutionKey).getBytes(StandardCharsets.UTF_8);
    }

    private byte[] blobKey(String resolutionKey) {
        return (KEY_PREFIX + ":bytes:" + resolutionKey).getBytes(StandardCharsets.UTF_8);
    }

    private String shortId(UUID id) {
        return id.toString().substring(0, 8);
    }

    private SyncFleetUpdateAckPacket.Payload pickLatestAck(SyncFleetUpdateAckPacket.Payload a, SyncFleetUpdateAckPacket.Payload b) {
        return statusWeight(b.status()) >= statusWeight(a.status()) ? b : a;
    }

    private int statusWeight(String status) {
        return switch (status.toUpperCase(Locale.ROOT)) {
            case SyncFleetUpdateAckPacket.Payload.STATUS_FAILED -> 5;
            case SyncFleetUpdateAckPacket.Payload.STATUS_RESTARTING -> 4;
            case SyncFleetUpdateAckPacket.Payload.STATUS_STAGED -> 3;
            case SyncFleetUpdateAckPacket.Payload.STATUS_DRY_RUN -> 2;
            case SyncFleetUpdateAckPacket.Payload.STATUS_SKIPPED -> 1;
            default -> 0;
        };
    }

    private NamedTextColor statusColor(String status) {
        return switch (status.toUpperCase(Locale.ROOT)) {
            case SyncFleetUpdateAckPacket.Payload.STATUS_FAILED -> NamedTextColor.RED;
            case SyncFleetUpdateAckPacket.Payload.STATUS_RESTARTING -> NamedTextColor.AQUA;
            case SyncFleetUpdateAckPacket.Payload.STATUS_STAGED -> NamedTextColor.GREEN;
            case SyncFleetUpdateAckPacket.Payload.STATUS_DRY_RUN -> NamedTextColor.YELLOW;
            case SyncFleetUpdateAckPacket.Payload.STATUS_SKIPPED -> NamedTextColor.GRAY;
            default -> NamedTextColor.WHITE;
        };
    }

    private record PendingRequest(SyncFleetUpdateRequestPacket.Payload payload, UpdateTarget target,
                                  Instant startedAt) {
    }

    private final class AckCollector {

        private final Sender originator;
        private final ConcurrentMap<UUID, SyncFleetUpdateAckPacket.Payload> latest = new ConcurrentHashMap<>();

        AckCollector(Sender originator) {
            this.originator = originator;
        }

        void record(SyncFleetUpdateAckPacket.Payload payload) {
            latest.merge(payload.instanceId(), payload, FleetUpdateService.this::pickLatestAck);
            originator.sendMessage(Component.text(
                    "[" + shortId(payload.requestId()) + "] " + payload.serverName()
                            + " -> " + payload.status() + " (" + payload.version() + ") "
                            + payload.detail(),
                    statusColor(payload.status())));
        }

        void report(UUID requestId) {
            int total = latest.size();
            int staged = 0;
            int restart = 0;
            int dry = 0;
            int skipped = 0;
            int failed = 0;
            for (SyncFleetUpdateAckPacket.Payload p : latest.values()) {
                switch (p.status()) {
                    case SyncFleetUpdateAckPacket.Payload.STATUS_STAGED -> staged++;
                    case SyncFleetUpdateAckPacket.Payload.STATUS_RESTARTING -> restart++;
                    case SyncFleetUpdateAckPacket.Payload.STATUS_DRY_RUN -> dry++;
                    case SyncFleetUpdateAckPacket.Payload.STATUS_SKIPPED -> skipped++;
                    case SyncFleetUpdateAckPacket.Payload.STATUS_FAILED -> failed++;
                    default -> {
                    }
                }
            }
            originator.sendMessage(Component.text(
                    "Fleet update " + shortId(requestId) + " report: " + total + " instances responded ("
                            + staged + " staged, " + restart + " restarting, " + dry + " dry-run, "
                            + skipped + " skipped, " + failed + " failed)",
                    NamedTextColor.GOLD));
        }
    }
}
