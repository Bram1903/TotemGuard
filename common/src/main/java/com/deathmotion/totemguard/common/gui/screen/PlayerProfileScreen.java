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
import com.deathmotion.totemguard.api3.event.impl.TGMonitorOpenEvent;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.database.model.PlayerRecord;
import com.deathmotion.totemguard.common.event.api.impl.TGMonitorOpenEventImpl;
import com.deathmotion.totemguard.common.gui.*;
import com.deathmotion.totemguard.common.gui.screen.history.HistoryText;
import com.deathmotion.totemguard.common.gui.screen.history.PlayerHistoryHubScreen;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public final class PlayerProfileScreen extends GuiScreen {

    private static final int SLOT_MONITOR = 11;
    private static final int SLOT_HEAD = 13;
    private static final int SLOT_HISTORY = 15;
    private static final int SLOT_BACK = 31;
    private static final int VIOLATION_LIST_LIMIT = 3;
    private final UUID targetId;
    private final String fallbackName;
    private volatile @Nullable PlayerRecord dbRecord;
    private volatile boolean dbAttempted;

    public PlayerProfileScreen(TGPlayer player) {
        this(player.getUuid(), player.getName());
    }

    public PlayerProfileScreen(UUID targetId, String fallbackName) {
        this.targetId = targetId;
        this.fallbackName = fallbackName;
    }

    @Override
    public String requiredPermission() {
        return "TotemGuardV3.Gui.Profile";
    }

    @Override
    public void onOpen(GuiSession session) {
        TGPlatform platform = TGPlatform.getInstance();
        if (!platform.getDatabaseRepository().isConnected()) {
            this.dbAttempted = true;
            return;
        }

        platform.getScheduler().runAsyncTask(() -> {
            try {
                this.dbRecord = platform.getDatabaseRepository().findPlayerByUuid(targetId);
            } catch (Exception ex) {
                platform.getLogger().log(Level.WARNING,
                        "Failed to load profile times for " + targetId + ": " + ex.getMessage());
            } finally {
                this.dbAttempted = true;
                platform.getGuiManager().refresh(session.viewerId());
            }
        });
    }

    @Override
    public GuiRenderResult render(GuiSession session) {
        TGPlayer target = TGPlatform.getInstance().getPlayerRepository().getPlayer(targetId);
        String targetName = target != null ? target.getName() : fallbackName;

        GuiRenderResult.Builder builder = GuiRenderResult.builder(4, GuiTitle.of("Profile: " + targetName));
        builder.fillEmpty(GuiItems.filler());

        renderBackOrClose(builder, session);

        if (target == null) {
            builder.set(SLOT_HEAD, GuiItems.simple(
                    ItemTypes.RED_CONCRETE,
                    Component.text(targetName + " is no longer tracked", NamedTextColor.RED),
                    List.of(
                            GuiText.line("UUID", targetId.toString()),
                            Component.text("The player left the repository.", NamedTextColor.GRAY)
                    )
            ));
            return builder.build();
        }

        builder.set(SLOT_HEAD, GuiItems.playerHead(
                target.getUser().getProfile(),
                Component.text(target.getName(), NamedTextColor.GREEN),
                buildHeadLore(target)
        ));

        renderMonitorButton(builder, session, target);
        renderHistoryButton(builder, session, target);

        return builder.build();
    }

    private void renderBackOrClose(GuiRenderResult.Builder builder, GuiSession session) {
        if (session.hasParent()) {
            builder.set(SLOT_BACK, GuiItems.simple(
                    ItemTypes.ARROW,
                    Component.text("Back", NamedTextColor.GOLD),
                    List.of(Component.text("Return to the previous screen", NamedTextColor.GRAY))
            ), ctx -> ctx.back());
        } else {
            builder.set(SLOT_BACK, GuiItems.simple(
                    ItemTypes.BARRIER,
                    Component.text("Close", NamedTextColor.RED),
                    List.of(Component.text("Close this screen", NamedTextColor.GRAY))
            ), ctx -> ctx.close());
        }
    }

    private void renderMonitorButton(GuiRenderResult.Builder builder, GuiSession session, TGPlayer target) {
        if (!session.hasPermission("TotemGuardV3.Gui.Monitor")) return;

        boolean self = session.viewerId().equals(target.getUuid());
        builder.set(SLOT_MONITOR, GuiItems.simple(
                self ? ItemTypes.BARRIER : ItemTypes.CHEST,
                Component.text(self ? "Self Monitor Disabled" : "Open Monitor",
                        self ? NamedTextColor.RED : NamedTextColor.GOLD),
                self
                        ? List.of(Component.text("Monitoring your own inventory is disabled", NamedTextColor.GRAY))
                        : List.of(
                        Component.text("View the live packet inventory", NamedTextColor.GRAY),
                        Component.text("and watch updates in-place", NamedTextColor.GRAY))
        ), ctx -> {
            if (ctx.session().viewerId().equals(target.getUuid())) {
                ctx.message(Component.text("You cannot monitor your own inventory.", NamedTextColor.RED));
                return;
            }

            TGMonitorOpenEvent event = TGPlatform.getInstance().getEventRepository().post(
                    new TGMonitorOpenEventImpl(ctx.session().viewerId(), target.getUuid())
            );
            if (event.isCancelled()) {
                ctx.message(Component.text("Opening the monitor was blocked.", NamedTextColor.RED));
                return;
            }

            ctx.open(new PlayerMonitorScreen(target));
        });
    }

    private void renderHistoryButton(GuiRenderResult.Builder builder, GuiSession session, TGPlayer target) {
        if (!session.hasPermission("TotemGuardV3.Gui.History")) return;

        builder.set(SLOT_HISTORY, GuiItems.simple(
                ItemTypes.BOOK,
                Component.text("History", NamedTextColor.GOLD),
                List.of(
                        Component.text("Browse this player's alert and", NamedTextColor.GRAY),
                        Component.text("punishment history from the database.", NamedTextColor.GRAY)
                )
        ), ctx -> ctx.open(new PlayerHistoryHubScreen(target)));
    }

    private List<Component> buildHeadLore(TGPlayer target) {
        List<Component> lore = new ArrayList<>();
        lore.add(GuiText.line("Client version", target.getClientVersion().getReleaseName()));
        lore.add(GuiText.line("Client brand", target.getClientBrand()));
        if (TGPlatform.getInstance().getAntiVPNRepository().isEnabled()) {
            lore.add(GuiText.status("VPN flagged", target.isVpn()));
        }

        lore.add(Component.empty());
        lore.add(GuiText.line("KeepAlive ping", target.getPingData().getKeepAlivePing() + " ms"));
        lore.add(GuiText.line("Transaction ping", target.getPingData().getTransactionPing() + " ms"));

        appendViolationSummary(lore, target);
        appendFirstJoined(lore);

        return lore;
    }

    private void appendViolationSummary(List<Component> lore, TGPlayer target) {
        List<Check> active = target.getCheckManager().allChecks.values().stream()
                .filter(check -> check.getViolations() > 0)
                .sorted(Comparator.comparingInt(Check::getViolations).reversed())
                .toList();

        lore.add(Component.empty());
        if (active.isEmpty()) {
            lore.add(Component.text("No active violations", NamedTextColor.GREEN));
            return;
        }

        int totalVl = active.stream().mapToInt(Check::getViolations).sum();
        lore.add(GuiText.line("Total violations", totalVl + " across " + active.size() + " check(s)"));

        active.stream().limit(VIOLATION_LIST_LIMIT).forEach(check -> lore.add(Component.text(
                "  " + check.getName() + " - VL " + check.getViolations(),
                NamedTextColor.GRAY)));

        int hidden = active.size() - VIOLATION_LIST_LIMIT;
        if (hidden > 0) {
            lore.add(Component.text("  + " + hidden + " more (see History)", NamedTextColor.DARK_GRAY));
        }
    }

    private void appendFirstJoined(List<Component> lore) {
        PlayerRecord rec = this.dbRecord;
        if (rec != null) {
            lore.add(Component.empty());
            lore.add(GuiText.line("First joined",
                    HistoryText.relative(rec.firstSeen()) + "  (" + HistoryText.absolute(rec.firstSeen()) + ")"));
            return;
        }

        if (TGPlatform.getInstance().getDatabaseRepository().isConnected() && !dbAttempted) {
            lore.add(Component.empty());
            lore.add(Component.text("First joined: loading…", NamedTextColor.GRAY));
        }
    }
}
