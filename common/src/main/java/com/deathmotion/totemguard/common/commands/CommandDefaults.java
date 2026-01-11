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

package com.deathmotion.totemguard.common.commands;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.platform.sender.Sender;
import lombok.experimental.UtilityClass;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.description.Description;
import org.spongepowered.configurate.CommentedConfigurationNode;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class CommandDefaults {

    public static final String PERMISSION_PREFIX = "totemguard.";
    private static final CommentedConfigurationNode CONFIG = TGPlatform.getInstance().getConfigRepository().config();
    public static final String ROOT = readRoot();
    public static final String[] ALIASES = readAliases();

    public static Command.Builder<Sender> root(final CommandManager<Sender> manager) {
        return manager.commandBuilder(ROOT, Description.of("The root command for TotemGuard."), ALIASES);
    }

    private static String readRoot() {
        final String root = CONFIG.node("commands", "base").getString("totemguard");
        return (root.isBlank()) ? "totemguard" : root;
    }

    private static String[] readAliases() {
        final CommentedConfigurationNode aliasesNode = CONFIG.node("commands", "aliases");

        if (aliasesNode.isList()) {
            final List<String> out = new ArrayList<>();
            for (final CommentedConfigurationNode child : aliasesNode.childrenList()) {
                final String s = child.getString();
                if (s != null && !s.isBlank()) out.add(s);
            }
            if (!out.isEmpty()) {
                return out.toArray(String[]::new);
            }
        }

        return new String[]{"tg"};
    }
}