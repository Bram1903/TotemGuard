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

package com.deathmotion.totemguard.common.player.processor.outbound;

import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.latency.PacketLatencyHandler;
import com.deathmotion.totemguard.common.player.processor.ProcessorOutbound;
import com.deathmotion.totemguard.common.world.team.TeamState;
import com.deathmotion.totemguard.common.world.team.TrackedTeam;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams.CollisionRule;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams.TeamMode;

import java.util.ArrayList;
import java.util.List;

public class OutboundTeamProcessor extends ProcessorOutbound {

    private final TeamState teams;
    private final PacketLatencyHandler latencyHandler;

    public OutboundTeamProcessor(TGPlayer player) {
        super(player);
        this.teams = player.getWorldMirror().teams();
        this.latencyHandler = player.getLatencyHandler();
    }

    @Override
    public void handleOutbound(PacketSendEvent event) {
        if (event.isCancelled()) return;
        final PacketTypeCommon type = event.getPacketType();

        if (type == PacketType.Play.Server.TEAMS) {
            handleTeams(event);
        } else if (type == PacketType.Play.Server.JOIN_GAME
                || type == PacketType.Play.Server.CONFIGURATION_START) {
            latencyHandler.compensateLazy(event, teams::clear);
        } else if (type == PacketType.Play.Server.PLAYER_INFO_UPDATE) {
            handlePlayerInfoUpdate(event);
        } else if (type == PacketType.Play.Server.PLAYER_INFO) {
            handleLegacyPlayerInfo(event);
        }
    }

    private void handleTeams(PacketSendEvent event) {
        WrapperPlayServerTeams packet = new WrapperPlayServerTeams(event);
        final String name = packet.getTeamName();
        final TeamMode mode = packet.getTeamMode();
        final CollisionRule rule = packet.getTeamInfo()
                .map(WrapperPlayServerTeams.ScoreBoardTeamInfo::getCollisionRule)
                .orElse(null);
        final List<String> entries = new ArrayList<>(packet.getPlayers());

        latencyHandler.compensateLazy(event, () -> {
            TrackedTeam team;
            if (mode == TeamMode.CREATE) {
                team = teams.create(name);
            } else {
                team = teams.team(name);
                if (team == null) return;
            }

            if (mode == TeamMode.CREATE || mode == TeamMode.UPDATE) {
                teams.collisionRule(team, rule);
            }
            if (mode == TeamMode.CREATE || mode == TeamMode.ADD_ENTITIES) {
                teams.addEntries(team, entries);
            } else if (mode == TeamMode.REMOVE_ENTITIES) {
                teams.removeEntries(team, entries);
            }
            if (mode == TeamMode.REMOVE) {
                teams.remove(team);
            }
        });
    }

    private void handlePlayerInfoUpdate(PacketSendEvent event) {
        WrapperPlayServerPlayerInfoUpdate packet = new WrapperPlayServerPlayerInfoUpdate(event);
        if (!packet.getActions().contains(WrapperPlayServerPlayerInfoUpdate.Action.ADD_PLAYER)) return;
        List<UserProfile> added = null;
        for (WrapperPlayServerPlayerInfoUpdate.PlayerInfo info : packet.getEntries()) {
            UserProfile profile = info.getGameProfile();
            if (!resolvable(profile)) continue;
            if (added == null) added = new ArrayList<>();
            added.add(profile);
        }
        registerProfiles(event, added);
    }

    private void handleLegacyPlayerInfo(PacketSendEvent event) {
        WrapperPlayServerPlayerInfo packet = new WrapperPlayServerPlayerInfo(event);
        if (packet.getAction() != WrapperPlayServerPlayerInfo.Action.ADD_PLAYER) return;
        List<UserProfile> profiles = null;
        for (WrapperPlayServerPlayerInfo.PlayerData data : packet.getPlayerDataList()) {
            UserProfile profile = data.getUserProfile();
            if (!resolvable(profile)) continue;
            if (profiles == null) profiles = new ArrayList<>();
            profiles.add(profile);
        }
        registerProfiles(event, profiles);
    }

    private static boolean resolvable(UserProfile profile) {
        return profile != null && profile.getUUID() != null && profile.getName() != null;
    }

    private void registerProfiles(PacketSendEvent event, List<UserProfile> profiles) {
        if (profiles == null || profiles.isEmpty()) return;
        latencyHandler.compensateLazy(event, () -> {
            for (UserProfile profile : profiles) {
                teams.profile(profile.getUUID(), profile.getName());
            }
        });
    }

}
