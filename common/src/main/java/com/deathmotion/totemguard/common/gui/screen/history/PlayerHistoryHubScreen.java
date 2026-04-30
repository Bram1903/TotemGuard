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
import com.deathmotion.totemguard.common.database.model.PlayerRecord;
import com.deathmotion.totemguard.common.gui.*;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.util.Palette;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public final class PlayerHistoryHubScreen extends GuiScreen {

    private final UUID targetId;
    private final String fallbackName;

    private volatile @Nullable PlayerRecord dbRecord;
    private volatile boolean dbAttempted;

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
                        "Failed to load history times for " + targetId + ": " + ex.getMessage());
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

        GuiRenderResult.Builder builder = GuiRenderResult.builder(3,
                GuiTitle.of("History: " + targetName));
        builder.fillEmpty(GuiItems.filler());

        if (session.hasParent()) {
            builder.set(0, GuiItems.simple(
                    ItemTypes.ARROW,
                    Component.text("Back", Palette.BRAND),
                    List.of(Component.text("Return to the profile", Palette.CONNECTIVE))
            ), ctx -> ctx.back());
        } else {
            builder.set(0, GuiItems.simple(
                    ItemTypes.BARRIER,
                    Component.text("Close", Palette.DANGER),
                    List.of(Component.text("Close this screen", Palette.CONNECTIVE))
            ), ctx -> ctx.close());
        }

        List<Component> headLore = buildHeadLore();
        if (target != null) {
            builder.set(4, GuiItems.playerHead(
                    target.getUser().getProfile(),
                    Component.text(target.getName(), Palette.SUCCESS),
                    headLore
            ));
        } else {
            builder.set(4, GuiItems.simple(
                    ItemTypes.PLAYER_HEAD,
                    Component.text(targetName, Palette.SUCCESS),
                    headLore
            ));
        }

        boolean dbReady = TGPlatform.getInstance().getDatabaseRepository().isConnected();
        boolean canViewAlerts = session.hasPermission("TotemGuardV3.Gui.History.Alerts");
        boolean canViewPunishments = session.hasPermission("TotemGuardV3.Gui.History.Punishments");

        if (dbReady) {
            if (canViewAlerts) {
                builder.set(11, GuiItems.simple(
                        ItemTypes.PAPER,
                        Component.text("Alerts", Palette.BRAND),
                        List.of(
                                Component.text("Every violation TotemGuard has flagged", Palette.CONNECTIVE),
                                Component.text("for this player, newest first.", Palette.CONNECTIVE),
                                Component.empty(),
                                Component.text("Click to browse ▶", Palette.CAPTION)
                        )
                ), ctx -> ctx.open(new PlayerAlertsScreen(targetId, targetName, 0)));
            }

            if (canViewPunishments) {
                builder.set(15, GuiItems.simple(
                        ItemTypes.IRON_AXE,
                        Component.text("Punishments", Palette.DANGER),
                        List.of(
                                Component.text("Every kick or ban", Palette.CONNECTIVE),
                                Component.text("TotemGuard dispatched, newest first.", Palette.CONNECTIVE),
                                Component.empty(),
                                Component.text("Click to browse ▶", Palette.CAPTION)
                        )
                ), ctx -> ctx.open(new PlayerPunishmentsScreen(targetId, targetName, 0)));
            }
        } else {
            builder.set(22, GuiItems.simple(
                    ItemTypes.RED_CONCRETE,
                    Component.text("Database offline", Palette.DANGER),
                    List.of(
                            Component.text("History is unavailable - the database", Palette.CONNECTIVE),
                            Component.text("is disabled or currently unreachable.", Palette.CONNECTIVE)
                    )
            ));
        }

        return builder.build();
    }

    private List<Component> buildHeadLore() {
        List<Component> lore = new ArrayList<>();
        lore.add(GuiText.line("UUID", targetId.toString()));

        PlayerRecord rec = this.dbRecord;
        if (rec != null) {
            lore.add(Component.empty());
            lore.add(GuiText.line("First joined",
                    HistoryText.relative(rec.firstSeen()) + "  (" + HistoryText.absolute(rec.firstSeen()) + ")"));
            lore.add(GuiText.line("Last joined",
                    HistoryText.relative(rec.lastSeen()) + "  (" + HistoryText.absolute(rec.lastSeen()) + ")"));
        } else if (!dbAttempted) {
            lore.add(Component.empty());
            lore.add(Component.text("Loading join times…", Palette.CONNECTIVE));
        }

        return lore;
    }
}
