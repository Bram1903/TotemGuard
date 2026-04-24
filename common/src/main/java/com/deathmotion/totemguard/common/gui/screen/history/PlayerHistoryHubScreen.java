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

package com.deathmotion.totemguard.common.gui.screen.history;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.gui.*;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;
import java.util.UUID;

public final class PlayerHistoryHubScreen extends GuiScreen {

    private final UUID targetId;
    private final String fallbackName;

    public PlayerHistoryHubScreen(TGPlayer player) {
        this(player.getUuid(), player.getName());
    }

    public PlayerHistoryHubScreen(UUID targetId, String fallbackName) {
        this.targetId = targetId;
        this.fallbackName = fallbackName;
    }

    @Override
    public String requiredPermission() {
        return "TotemGuardV3.Gui.History";
    }

    @Override
    public GuiRenderResult render(GuiSession session) {
        TGPlayer target = TGPlatform.getInstance().getPlayerRepository().getPlayer(targetId);
        String targetName = target != null ? target.getName() : fallbackName;

        GuiRenderResult.Builder builder = GuiRenderResult.builder(3,
                Component.text("History: " + targetName, NamedTextColor.GOLD));
        builder.fillEmpty(GuiItems.filler());

        if (session.hasParent()) {
            builder.set(0, GuiItems.simple(
                    ItemTypes.ARROW,
                    Component.text("Back", NamedTextColor.GOLD),
                    List.of(Component.text("Return to the profile", NamedTextColor.GRAY))
            ), ctx -> ctx.back());
        } else {
            builder.set(0, GuiItems.simple(
                    ItemTypes.BARRIER,
                    Component.text("Close", NamedTextColor.RED),
                    List.of(Component.text("Close this screen", NamedTextColor.GRAY))
            ), ctx -> ctx.close());
        }

        if (target != null) {
            builder.set(4, GuiItems.playerHead(
                    target.getUser().getProfile(),
                    Component.text(target.getName(), NamedTextColor.GREEN),
                    List.of(GuiText.line("UUID", target.getUuid().toString()))
            ));
        } else {
            builder.set(4, GuiItems.simple(
                    ItemTypes.PLAYER_HEAD,
                    Component.text(targetName, NamedTextColor.GREEN),
                    List.of(GuiText.line("UUID", targetId.toString()))
            ));
        }

        boolean dbReady = TGPlatform.getInstance().getDatabaseRepository().isConnected();
        boolean canViewAlerts = session.hasPermission("TotemGuardV3.Gui.History.Alerts");
        boolean canViewPunishments = session.hasPermission("TotemGuardV3.Gui.History.Punishments");

        if (dbReady) {
            if (canViewAlerts) {
                builder.set(11, GuiItems.simple(
                        ItemTypes.PAPER,
                        Component.text("Alerts", NamedTextColor.YELLOW),
                        List.of(
                                Component.text("Every violation TotemGuard has flagged", NamedTextColor.GRAY),
                                Component.text("for this player, newest first.", NamedTextColor.GRAY),
                                Component.empty(),
                                Component.text("Click to browse ▶", NamedTextColor.DARK_GRAY)
                        )
                ), ctx -> ctx.open(new PlayerAlertsScreen(targetId, targetName, 0)));
            }

            if (canViewPunishments) {
                builder.set(15, GuiItems.simple(
                        ItemTypes.IRON_AXE,
                        Component.text("Punishments", NamedTextColor.RED),
                        List.of(
                                Component.text("Every kick or ban", NamedTextColor.GRAY),
                                Component.text("TotemGuard dispatched, newest first.", NamedTextColor.GRAY),
                                Component.empty(),
                                Component.text("Click to browse ▶", NamedTextColor.DARK_GRAY)
                        )
                ), ctx -> ctx.open(new PlayerPunishmentsScreen(targetId, targetName, 0)));
            }
        } else {
            builder.set(22, GuiItems.simple(
                    ItemTypes.RED_CONCRETE,
                    Component.text("Database offline", NamedTextColor.RED),
                    List.of(
                            Component.text("History is unavailable — the database", NamedTextColor.GRAY),
                            Component.text("is disabled or currently unreachable.", NamedTextColor.GRAY)
                    )
            ));
        }

        return builder.build();
    }

}
