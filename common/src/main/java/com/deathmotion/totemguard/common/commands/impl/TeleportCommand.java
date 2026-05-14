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

package com.deathmotion.totemguard.common.commands.impl;

import com.deathmotion.totemguard.api.event.impl.TGTeleportEvent;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.commands.AbstractCommand;
import com.deathmotion.totemguard.common.commands.suggestion.TGPlayerSuggestionProvider;
import com.deathmotion.totemguard.common.config.key.MessagesKeys;
import com.deathmotion.totemguard.common.event.api.impl.TGTeleportEventImpl;
import com.deathmotion.totemguard.common.features.teleport.TeleportService;
import com.deathmotion.totemguard.common.network.NetworkPresenceRepository;
import com.deathmotion.totemguard.common.network.ProxyTopologyService;
import com.deathmotion.totemguard.common.network.RemotePlayerEntry;
import com.deathmotion.totemguard.common.platform.player.PlatformPlayer;
import com.deathmotion.totemguard.common.platform.sender.Sender;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.data.MovementData;
import com.deathmotion.totemguard.common.redis.broker.packets.impl.SyncTeleportRequestPacket;
import com.github.retrooper.packetevents.protocol.world.Location;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.flag.CommandFlag;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.permission.PredicatePermission;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Map;
import java.util.UUID;

public final class TeleportCommand extends AbstractCommand {

    private static final long REQUEST_TTL_MILLIS = 30_000L;
    private static final String SILENT_FLAG = "silent";

    private final TGPlatform platform;

    public TeleportCommand() {
        this.platform = TGPlatform.getInstance();
    }

    private static void applyLocalTeleport(PlatformPlayer senderPlatform, TGPlayer target) {
        MovementData movement = target.getData().getMovementData();
        Location loc = movement.getCurrent();
        PlatformPlayer targetPlayer = target.getPlatformPlayer();
        String world = targetPlayer != null ? targetPlayer.getWorldName() : null;
        senderPlatform.teleport(world == null ? "" : world,
                loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
    }

    @Override
    public void register(@NonNull CommandManager<Sender> manager) {
        String teleportPerm = perm("teleport");
        PredicatePermission<Sender> permission = PredicatePermission.of(sender ->
                TeleportService.isSilentDispatch() || sender.hasPermission(teleportPerm));

        manager.command(
                base(manager)
                        .literal("teleport")
                        .required(
                                "tg_player",
                                StringParser.stringParser(),
                                TGPlayerSuggestionProvider.suggestionProviderExcludingSelf()
                        )
                        .flag(CommandFlag.builder(SILENT_FLAG))
                        .permission(permission)
                        .handler(this::handle)
        );
    }

    private void handle(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        if (!requirePlayer(sender)) return;
        boolean silent = TeleportService.isSilentDispatch();

        PlatformPlayer senderPlatform = platform.getPlatformPlayerFactory().create(sender.getUniqueId());
        if (senderPlatform == null) {
            if (!silent) {
                sender.sendMessage(platform.getMessageService().getComponent(MessagesKeys.GENERAL_PLAYER_DATA_MISSING));
            }
            return;
        }

        String rawTarget = context.get("tg_player");
        NetworkPresenceRepository presence = platform.getNetworkPresenceRepository();
        RemotePlayerEntry target = presence == null ? null : presence.findByName(rawTarget);
        if (target == null) {
            if (!silent) {
                sender.sendMessage(platform.getMessageService().getComponent(
                        MessagesKeys.TELEPORT_NOT_FOUND,
                        Map.of("tg_input", rawTarget)
                ));
            }
            return;
        }

        if (target.playerUuid().equals(sender.getUniqueId())) {
            if (!silent) {
                sender.sendMessage(platform.getMessageService().getComponent(MessagesKeys.TELEPORT_SELF));
            }
            return;
        }

        boolean local = presence.isLocal(target.serverInstanceId());
        TGPlayer localTarget = local ? platform.getPlayerRepository().getPlayer(target.playerUuid()) : null;

        if (local && localTarget == null && target.bypassed()) {
            if (!silent) {
                sender.sendMessage(platform.getMessageService().getComponent(
                        MessagesKeys.TELEPORT_TARGET_BYPASSED,
                        Map.of("tg_player", target.playerName(), "tg_server", target.serverName())
                ));
            }
            return;
        }

        if (local && localTarget == null) {
            if (!silent) {
                sender.sendMessage(platform.getMessageService().getComponent(
                        MessagesKeys.TELEPORT_NOT_FOUND,
                        Map.of("tg_input", rawTarget)
                ));
            }
            return;
        }

        if (!local && !platform.getRedisRepository().isConnected()) {
            if (!silent) {
                sender.sendMessage(platform.getMessageService().getComponent(MessagesKeys.TELEPORT_NO_REDIS));
            }
            return;
        }

        if (!local) {
            if (!platform.getProxyTopologyService().bridgeAvailable()) {
                if (!silent) {
                    sender.sendMessage(platform.getMessageService().getComponent(MessagesKeys.TELEPORT_NO_BRIDGE));
                }
                return;
            }
            if (platform.checkRoute(target.serverInstanceId()) == ProxyTopologyService.RouteStatus.NOT_ROUTABLE) {
                if (!silent) {
                    sender.sendMessage(platform.getMessageService().getComponent(
                            MessagesKeys.TELEPORT_DIFFERENT_PROXY,
                            Map.of("tg_player", target.playerName(), "tg_server", target.serverName())
                    ));
                }
                return;
            }
        }

        if (!silent) {
            TGTeleportEvent event = platform.getEventRepository().post(new TGTeleportEventImpl(
                    sender.getUniqueId(),
                    target.playerUuid(),
                    target.playerName(),
                    localTarget,
                    target.serverInstanceId(),
                    target.serverName(),
                    !local
            ));
            if (event.isCancelled()) return;
        }

        if (local) {
            applyLocalTeleport(senderPlatform, localTarget);
            if (!silent) {
                sender.sendMessage(platform.getMessageService().getComponent(
                        MessagesKeys.TELEPORT_SAME_SERVER,
                        Map.of("tg_player", target.playerName())
                ));
            }
            return;
        }

        long expiresAt = System.currentTimeMillis() + REQUEST_TTL_MILLIS;
        SyncTeleportRequestPacket.Payload payload = new SyncTeleportRequestPacket.Payload(
                UUID.randomUUID(),
                sender.getUniqueId(),
                target.playerUuid(),
                target.serverName(),
                target.serverInstanceId(),
                expiresAt
        );
        presence.publishTeleportRequest(payload);
        platform.getProxyTopologyService().connectToInstance(sender.getUniqueId(), target.serverInstanceId());
        if (!silent) {
            sender.sendMessage(platform.getMessageService().getComponent(
                    MessagesKeys.TELEPORT_CROSS_SERVER,
                    Map.of("tg_player", target.playerName(), "tg_server", target.serverName())
            ));
        }
    }
}
