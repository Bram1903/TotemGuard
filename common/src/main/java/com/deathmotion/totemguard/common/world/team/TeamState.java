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

package com.deathmotion.totemguard.common.world.team;

import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams.CollisionRule;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public final class TeamState {

    private final Supplier<String> localName;
    private final Map<String, TrackedTeam> teamsByName = new HashMap<>();
    private final Map<String, TrackedTeam> teamByEntry = new HashMap<>();
    private final Map<UUID, String> profileNames = new HashMap<>();

    private int restrictiveTeams;

    public TeamState(Supplier<String> localName) {
        this.localName = localName;
    }

    private static boolean canPush(CollisionRule pusher, CollisionRule pushed, boolean sameTeam) {
        if (pusher == CollisionRule.NEVER || pushed == CollisionRule.NEVER) return false;
        if (sameTeam) {
            return pusher != CollisionRule.PUSH_OWN_TEAM && pushed != CollisionRule.PUSH_OWN_TEAM;
        }
        return pusher != CollisionRule.PUSH_OTHER_TEAMS && pushed != CollisionRule.PUSH_OTHER_TEAMS;
    }

    public boolean pushableBy(boolean playerEntity, @Nullable UUID uuid, @Nullable String uuidString) {
        if (restrictiveTeams == 0) return true;
        String local = localName.get();
        if (local == null) return true;

        String entry = playerEntity ? profileNames.get(uuid) : uuidString;
        if (entry == null) return true;

        TrackedTeam pusher = teamByEntry.get(entry);
        TrackedTeam pushed = teamByEntry.get(local);
        if (pusher == null && pushed == null) return true;

        CollisionRule pusherRule = pusher == null ? CollisionRule.ALWAYS : pusher.collisionRule();
        CollisionRule pushedRule = pushed == null ? CollisionRule.ALWAYS : pushed.collisionRule();
        return canPush(pusherRule, pushedRule, pusher != null && pusher == pushed);
    }

    public @Nullable TrackedTeam team(String name) {
        return teamsByName.get(name);
    }

    public TrackedTeam create(String name) {
        TrackedTeam existing = teamsByName.get(name);
        if (existing != null) return existing;
        TrackedTeam team = new TrackedTeam(name);
        teamsByName.put(name, team);
        return team;
    }

    public void remove(TrackedTeam team) {
        if (teamsByName.remove(team.name()) == null) return;
        if (team.restrictive()) restrictiveTeams--;
        for (String entry : team.entries()) {
            teamByEntry.remove(entry, team);
        }
        team.entries().clear();
    }

    public void collisionRule(TrackedTeam team, @Nullable CollisionRule rule) {
        if (rule == null) return;
        boolean was = team.restrictive();
        team.collisionRule(rule);
        boolean now = team.restrictive();
        if (was != now) restrictiveTeams += now ? 1 : -1;
    }

    public void addEntries(TrackedTeam team, Collection<String> entries) {
        for (String entry : entries) {
            TrackedTeam previous = teamByEntry.get(entry);
            if (previous == team) continue;
            if (previous != null) previous.entries().remove(entry);
            teamByEntry.put(entry, team);
            team.entries().add(entry);
        }
    }

    public void removeEntries(TrackedTeam team, Collection<String> entries) {
        for (String entry : entries) {
            if (teamByEntry.get(entry) != team) continue;
            teamByEntry.remove(entry);
            team.entries().remove(entry);
        }
    }

    public void profile(UUID uuid, String name) {
        profileNames.put(uuid, name);
    }

    public void clear() {
        teamsByName.clear();
        teamByEntry.clear();
        profileNames.clear();
        restrictiveTeams = 0;
    }
}
