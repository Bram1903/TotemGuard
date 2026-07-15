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

package com.deathmotion.totemguard.common.permission;

import com.deathmotion.totemguard.common.check.CheckManagerImpl;

import java.util.ArrayList;
import java.util.List;

public final class PermissionNodes {

    private static final List<String> OPERATOR_DEFAULT_NODES = List.of(
            "TotemGuard.Alerts",
            "TotemGuard.Focus",
            "TotemGuard.Tester",
            "TotemGuard.UpdateNotify",
            "TotemGuard.Gui.History",
            "TotemGuard.Gui.History.Alerts",
            "TotemGuard.Gui.History.Punishments",
            "TotemGuard.Gui.Info",
            "TotemGuard.Gui.Mods",
            "TotemGuard.Gui.Monitor",
            "TotemGuard.Gui.Profile",
            "TotemGuard.Gui.Statistics",
            "TotemGuard.Gui.Top"
    );

    private static final List<String> NEVER_GRANTED_BYPASS_ROOT_NODES = List.of(
            "TotemGuard.Bypass",
            "TotemGuard.Bypass.*"
    );

    private PermissionNodes() {
    }

    public static List<PermissionNode> all() {
        List<PermissionNode> nodes = new ArrayList<>();

        for (String node : OPERATOR_DEFAULT_NODES) {
            nodes.add(new PermissionNode(node, PermissionDefault.OP));
        }
        for (String node : NEVER_GRANTED_BYPASS_ROOT_NODES) {
            nodes.add(new PermissionNode(node, PermissionDefault.FALSE));
        }
        for (String node : CheckManagerImpl.bypassPermissionNodes()) {
            nodes.add(new PermissionNode(node, PermissionDefault.FALSE));
        }

        return List.copyOf(nodes);
    }
}
