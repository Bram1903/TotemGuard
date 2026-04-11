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

package com.deathmotion.totemguard.common.gui.screen;

import com.deathmotion.totemguard.api3.TotemGuard;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.commands.CommandDefaults;
import com.deathmotion.totemguard.common.gui.*;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.List;
import java.util.stream.Collectors;

public final class TotemGuardInfoScreen extends GuiScreen {

    private final int page;

    public TotemGuardInfoScreen() {
        this(0);
    }

    public TotemGuardInfoScreen(int page) {
        this.page = page;
    }

    @Override
    public GuiRenderResult render(GuiSession session) {
        return page == 0 ? renderOverview() : renderRuntime(session);
    }

    private GuiRenderResult renderOverview() {
        TGPlatform platform = TGPlatform.getInstance();

        GuiRenderResult.Builder builder = GuiRenderResult.builder(3, Component.text("TotemGuard AntiCheat", TextColor.fromHexString("#574F4D")).decoration(TextDecoration.BOLD, true));
        builder.fillEmpty(GuiItems.filler());

        builder.set(10, GuiItems.simple(
                ItemTypes.TOTEM_OF_UNDYING,
                Component.text("Plugin", NamedTextColor.GREEN),
                List.of(
                        GuiText.line("Version", TotemGuard.get().getVersion().toString()),
                        GuiText.line("Platform", platform.getPlatform().name()),
                        GuiText.line("Platform build", platform.getPlatformVersion())
                )
        ));

        builder.set(12, GuiItems.simple(
                ItemTypes.PLAYER_HEAD,
                Component.text("Tracked Players", NamedTextColor.AQUA),
                List.of(
                        GuiText.line("Repository size", String.valueOf(platform.getPlayerRepository().getPlayers().size())),
                        GuiText.line("Alerts enabled", String.valueOf(platform.getAlertRepository().getEnabledAlerts().size())),
                        GuiText.line("GUI sessions", String.valueOf(platform.getGuiManager().activeSessionCount()))
                )
        ));

        builder.set(14, GuiItems.simple(
                ItemTypes.BOOK,
                Component.text("Commands", NamedTextColor.YELLOW),
                List.of(
                        GuiText.line("Root", "/" + CommandDefaults.ROOT),
                        GuiText.line("Aliases", String.join(", ", CommandDefaults.ALIASES)),
                        GuiText.line("Examples", "/" + CommandDefaults.ROOT + " profile <player>")
                )
        ));

        builder.set(16, GuiItems.simple(
                ItemTypes.ARROW,
                Component.text("Next Page", NamedTextColor.GOLD),
                List.of(
                        Component.text("Runtime, protocol,", NamedTextColor.GRAY),
                        Component.text("and backend state", NamedTextColor.GRAY)
                )
        ), ctx -> ctx.open(new TotemGuardInfoScreen(1)));

        builder.set(22, GuiItems.simple(
                ItemTypes.BARRIER,
                Component.text("Close", NamedTextColor.RED),
                List.of(Component.text("Close this screen", NamedTextColor.GRAY))
        ), ctx -> ctx.close());

        return builder.build();
    }

    private GuiRenderResult renderRuntime(GuiSession session) {
        TGPlatform platform = TGPlatform.getInstance();

        GuiRenderResult.Builder builder = GuiRenderResult.builder(3, Component.text("TotemGuard Runtime", NamedTextColor.GOLD));
        builder.fillEmpty(GuiItems.filler());

        builder.set(10, GuiItems.simple(
                ItemTypes.REDSTONE,
                Component.text("Protocol", NamedTextColor.RED),
                List.of(
                        GuiText.line("PacketEvents", String.valueOf(PacketEvents.getAPI().getVersion())),
                        GuiText.line("Server version", String.valueOf(PacketEvents.getAPI().getServerManager().getVersion())),
                        GuiText.line("Enabled", String.valueOf(platform.isEnabled()))
                )
        ));

        builder.set(12, GuiItems.simple(
                ItemTypes.ENDER_CHEST,
                Component.text("Backend", NamedTextColor.LIGHT_PURPLE),
                List.of(
                        GuiText.status("Redis enabled", platform.getRedisRepository().isEnabled()),
                        GuiText.status("Redis connected", platform.getRedisRepository().isConnected()),
                        GuiText.status("Anti-VPN enabled", platform.getAntiVPNRepository().isEnabled())
                )
        ));

        builder.set(14, GuiItems.simple(
                ItemTypes.COMPASS,
                Component.text("Live State", NamedTextColor.AQUA),
                List.of(
                        GuiText.line("Tracked players", String.valueOf(platform.getPlayerRepository().getPlayers().size())),
                        GuiText.line("Loaded placeholders", String.valueOf(platform.getPlaceholderRepository().registeredKeys().size())),
                        GuiText.line("Checks/player", platform.getPlayerRepository().getPlayers().stream()
                                .map(player -> String.valueOf(player.getCheckManager().allChecks.size()))
                                .findFirst()
                                .orElse("0"))
                )
        ));

        builder.set(16, GuiItems.simple(
                ItemTypes.PAPER,
                Component.text("Known Players", NamedTextColor.YELLOW),
                platform.getPlayerRepository().getPlayers().stream()
                        .limit(5)
                        .map(player -> Component.text(player.getName(), NamedTextColor.GRAY))
                        .collect(Collectors.toList())
        ));

        builder.set(18, GuiItems.simple(
                ItemTypes.ARROW,
                Component.text("Back", NamedTextColor.GOLD),
                List.of(Component.text("Return to the overview", NamedTextColor.GRAY))
        ), ctx -> {
            if (session.hasParent()) {
                ctx.back();
            } else {
                ctx.replace(new TotemGuardInfoScreen());
            }
        });

        builder.set(22, GuiItems.simple(
                ItemTypes.BARRIER,
                Component.text("Close", NamedTextColor.RED),
                List.of(Component.text("Close this screen", NamedTextColor.GRAY))
        ), ctx -> ctx.close());

        return builder.build();
    }
}
