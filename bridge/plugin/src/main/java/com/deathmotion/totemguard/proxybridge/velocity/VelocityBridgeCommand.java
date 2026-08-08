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

package com.deathmotion.totemguard.proxybridge.velocity;

import com.deathmotion.totemguard.proxybridge.common.BridgeBootstrap;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.logging.Logger;

final class VelocityBridgeCommand implements SimpleCommand {

    static final String PERMISSION = "totemguard.proxybridge.reload";

    private final BridgeBootstrap bootstrap;
    private final Logger logger;

    VelocityBridgeCommand(@NonNull BridgeBootstrap bootstrap, @NonNull Logger logger) {
        this.bootstrap = bootstrap;
        this.logger = logger;
    }

    private static NamedTextColor colorFor(BridgeBootstrap.BootstrapOutcome.Kind kind) {
        return switch (kind) {
            case STARTED, RELOADED -> NamedTextColor.GREEN;
            case DISABLED -> NamedTextColor.YELLOW;
            case FAILED -> NamedTextColor.RED;
        };
    }

    @Override
    public void execute(@NonNull Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length == 0 || !"reload".equalsIgnoreCase(args[0])) {
            source.sendMessage(Component.text("Usage: /tgbridge reload", NamedTextColor.YELLOW));
            return;
        }

        try {
            BridgeBootstrap.BootstrapOutcome outcome = bootstrap.reload();
            source.sendMessage(Component.text(outcome.message(), colorFor(outcome.kind())));
        } catch (Exception ex) {
            source.sendMessage(Component.text("Reload failed: " + ex.getMessage(), NamedTextColor.RED));
            logger.warning("Reload failed: " + ex.getMessage());
        }
    }

    @Override
    public boolean hasPermission(@NonNull Invocation invocation) {
        return invocation.source().hasPermission(PERMISSION);
    }

    @Override
    public List<String> suggest(@NonNull Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length <= 1) return List.of("reload");
        return List.of();
    }
}
