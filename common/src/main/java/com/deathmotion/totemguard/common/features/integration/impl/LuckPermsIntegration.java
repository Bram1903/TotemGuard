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

package com.deathmotion.totemguard.common.features.integration.impl;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.features.integration.Integration;
import com.deathmotion.totemguard.common.player.PlayerRepositoryImpl;
import com.deathmotion.totemguard.common.player.TGPlayer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.event.EventBus;
import net.luckperms.api.event.EventSubscription;
import net.luckperms.api.event.group.GroupDataRecalculateEvent;
import net.luckperms.api.event.node.NodeAddEvent;
import net.luckperms.api.event.node.NodeRemoveEvent;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import net.luckperms.api.model.PermissionHolder;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class LuckPermsIntegration implements Integration {

    private static final String PLUGIN_NAME = "LuckPerms";

    private final PlayerRepositoryImpl playerRepository = TGPlatform.getInstance().getPlayerRepository();
    private final List<EventSubscription<?>> subscriptions = new ArrayList<>();

    private boolean enabled;
    private LuckPerms luckPerms;

    private static boolean inheritsGroup(User user, String groupName) {
        for (Group inherited : user.getInheritedGroups(user.getQueryOptions())) {
            if (normalizeGroupName(inherited).equals(groupName)) return true;
        }

        return false;
    }

    private static String normalizeGroupName(Group group) {
        return group.getName().toLowerCase(Locale.ROOT);
    }

    @Override
    public String getName() {
        return PLUGIN_NAME;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        try {
            luckPerms = LuckPermsProvider.get();
            registerListeners();

            enabled = true;
            TGPlatform.getInstance().getLogger().info(PLUGIN_NAME + " detected, hooking into the API to refresh cached bypass permissions.");
        } catch (Exception | LinkageError exception) {
            enabled = false;
            closeSubscriptions();
            luckPerms = null;
            TGPlatform.getInstance().getLogger().severe("Failed to enable " + PLUGIN_NAME + " integration: " + exception);
        }
    }

    @Override
    public void disable() {
        closeSubscriptions();
        luckPerms = null;
        enabled = false;
    }

    private void registerListeners() {
        EventBus eventBus = luckPerms.getEventBus();
        subscriptions.add(eventBus.subscribe(UserDataRecalculateEvent.class, this::onUserDataRecalculate));
        subscriptions.add(eventBus.subscribe(GroupDataRecalculateEvent.class, this::onGroupDataRecalculate));
        subscriptions.add(eventBus.subscribe(NodeAddEvent.class, this::onNodeAdd));
        subscriptions.add(eventBus.subscribe(NodeRemoveEvent.class, this::onNodeRemove));
    }

    private void closeSubscriptions() {
        for (EventSubscription<?> subscription : subscriptions) {
            subscription.close();
        }
        subscriptions.clear();
    }

    private void onUserDataRecalculate(UserDataRecalculateEvent event) {
        refreshUser(event.getUser());
    }

    private void onGroupDataRecalculate(GroupDataRecalculateEvent event) {
        refreshPlayersInheritingGroup(event.getGroup());
    }

    private void onNodeAdd(NodeAddEvent event) {
        onNodeMutate(event.getTarget());
    }

    private void onNodeRemove(NodeRemoveEvent event) {
        onNodeMutate(event.getTarget());
    }

    private void onNodeMutate(PermissionHolder target) {
        if (target instanceof User user) {
            refreshUser(user);
            return;
        }

        if (target instanceof Group group) {
            refreshPlayersInheritingGroup(group);
        }
    }

    private void refreshUser(@Nullable User user) {
        if (user == null) return;
        refreshPlayer(user.getUniqueId());
    }

    private void refreshPlayer(@Nullable UUID uuid) {
        if (uuid == null) return;
        TGPlayer player = playerRepository.getPlayer(uuid);
        if (player == null) return;
        player.updatePermissions();
    }

    private void refreshPlayersInheritingGroup(Group group) {
        LuckPerms api = luckPerms;
        if (api == null) return;

        String groupName = normalizeGroupName(group);
        for (TGPlayer player : playerRepository.getPlayers()) {
            UUID uuid = player.getUuid();
            if (uuid == null) continue;

            User user = api.getUserManager().getUser(uuid);
            if (user == null || !inheritsGroup(user, groupName)) continue;

            player.updatePermissions();
        }
    }
}
