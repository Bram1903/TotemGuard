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

import com.deathmotion.totemguard.api3.check.Check;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.gui.*;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class PlayerProfileScreen extends GuiScreen {

    private final UUID targetId;
    private final String fallbackName;

    public PlayerProfileScreen(TGPlayer player) {
        this(player.getUuid(), player.getName());
    }

    public PlayerProfileScreen(UUID targetId, String fallbackName) {
        this.targetId = targetId;
        this.fallbackName = fallbackName;
    }

    @Override
    public GuiRenderResult render(GuiSession session) {
        TGPlayer target = TGPlatform.getInstance().getPlayerRepository().getPlayer(targetId);
        String targetName = target != null ? target.getName() : fallbackName;

        GuiRenderResult.Builder builder = GuiRenderResult.builder(3, Component.text("Profile: " + targetName, NamedTextColor.GOLD));
        builder.fillEmpty(GuiItems.filler());

        if (session.hasParent()) {
            builder.set(0, GuiItems.simple(
                    ItemTypes.ARROW,
                    Component.text("Back", NamedTextColor.GOLD),
                    List.of(Component.text("Return to the previous screen", NamedTextColor.GRAY))
            ), ctx -> ctx.back());
        } else {
            builder.set(0, GuiItems.simple(
                    ItemTypes.BARRIER,
                    Component.text("Close", NamedTextColor.RED),
                    List.of(Component.text("Close this screen", NamedTextColor.GRAY))
            ), ctx -> ctx.close());
        }

        if (target == null) {
            builder.set(13, GuiItems.simple(
                    ItemTypes.RED_CONCRETE,
                    Component.text(targetName + " is no longer tracked", NamedTextColor.RED),
                    List.of(
                            GuiText.line("UUID", targetId.toString()),
                            Component.text("The player left the repository.", NamedTextColor.GRAY)
                    )
            ));
            return builder.build();
        }

        builder.set(10, GuiItems.playerHead(
                target.getUser().getProfile(),
                Component.text(target.getName(), NamedTextColor.GREEN),
                List.of(
                        GuiText.line("UUID", target.getUuid().toString()),
                        GuiText.status("Alerts enabled", target.hasAlertsEnabled()),
                        GuiText.status("VPN flagged", target.isVpn())
                )
        ));

        builder.set(12, GuiItems.simple(
                ItemTypes.BOOK,
                Component.text("General", NamedTextColor.AQUA),
                List.of(
                        GuiText.line("Client version", target.getClientVersion().getReleaseName()),
                        GuiText.line("Client brand", target.getClientBrand() == null ? "Unknown" : target.getClientBrand()),
                        GuiText.status("Inventory open", target.getData().isOpenInventory())
                )
        ));

        builder.set(14, GuiItems.simple(
                ItemTypes.ENCHANTED_BOOK,
                Component.text("Violations", NamedTextColor.YELLOW),
                buildViolationLore(target)
        ));

        builder.set(16, GuiItems.simple(
                ItemTypes.COMPARATOR,
                Component.text("Latency", NamedTextColor.LIGHT_PURPLE),
                List.of(
                        GuiText.line("KeepAlive ping", String.valueOf(target.getPingData().getKeepAlivePing())),
                        GuiText.line("Transaction ping", String.valueOf(target.getPingData().getTransactionPing())),
                        GuiText.line("Pending transactions", String.valueOf(target.getPingData().getPendingTransactionCount()))
                )
        ));

        builder.set(22, GuiItems.simple(
                session.viewerId().equals(target.getUuid()) ? ItemTypes.BARRIER : ItemTypes.CHEST,
                Component.text(
                        session.viewerId().equals(target.getUuid()) ? "Self Monitor Disabled" : "Open Monitor",
                        session.viewerId().equals(target.getUuid()) ? NamedTextColor.RED : NamedTextColor.GOLD
                ),
                session.viewerId().equals(target.getUuid())
                        ? List.of(Component.text("Monitoring your own inventory is disabled", NamedTextColor.GRAY))
                        : List.of(
                        Component.text("View the live packet inventory", NamedTextColor.GRAY),
                        Component.text("and watch updates in-place", NamedTextColor.GRAY)
                )
        ), ctx -> {
            if (ctx.session().viewerId().equals(target.getUuid())) {
                ctx.message(Component.text("You cannot monitor your own inventory.", NamedTextColor.RED));
                return;
            }

            ctx.open(new PlayerMonitorScreen(target));
        });

        return builder.build();
    }

    private List<Component> buildViolationLore(TGPlayer player) {
        List<Check> checks = player.getCheckManager().allChecks.values().stream()
                .sorted(Comparator.comparingInt(Check::getViolations).reversed())
                .toList();

        int totalViolations = checks.stream()
                .mapToInt(Check::getViolations)
                .sum();

        long activeChecks = checks.stream()
                .filter(check -> check.getViolations() > 0)
                .count();

        List<Component> lines = new java.util.ArrayList<>();
        lines.add(GuiText.line("Total violations", String.valueOf(totalViolations)));
        lines.add(GuiText.line("Checks with VL", String.valueOf(activeChecks)));

        checks.stream()
                .filter(check -> check.getViolations() > 0)
                .limit(4)
                .forEach(check -> lines.add(Component.text(
                        check.getName() + " - VL " + check.getViolations(),
                        NamedTextColor.GRAY
                )));

        if (lines.size() == 2) {
            lines.add(Component.text("No active violations", NamedTextColor.GREEN));
        }

        return lines;
    }
}
