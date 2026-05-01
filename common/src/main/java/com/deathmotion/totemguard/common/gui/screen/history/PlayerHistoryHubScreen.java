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
import com.deathmotion.totemguard.common.config.key.MessagesKeys;
import com.deathmotion.totemguard.common.database.model.PlayerRecord;
import com.deathmotion.totemguard.common.gui.*;
import com.deathmotion.totemguard.common.message.MessageService;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.util.Palette;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public final class PlayerHistoryHubScreen extends GuiScreen {

    public static final String PERMISSION = "TotemGuard.Gui.History";
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
        return PERMISSION;
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
        TGPlatform platform = TGPlatform.getInstance();
        MessageService messages = platform.getMessageService();
        TGPlayer target = platform.getPlayerRepository().getPlayer(targetId);
        String targetName = target != null ? target.getName() : fallbackName;

        GuiRenderResult.Builder builder = GuiRenderResult.builder(3,
                GuiTitle.of(messages.getString(MessagesKeys.GUI_HISTORY_HUB_TITLE, Map.of("tg_player", targetName))));
        builder.fillEmpty(GuiItems.filler());

        if (session.hasParent()) {
            builder.set(0, GuiItems.simple(
                    ItemTypes.ARROW,
                    messages.getComponent(MessagesKeys.GUI_BTN_BACK_TITLE),
                    List.of(messages.getComponent(MessagesKeys.GUI_BTN_BACK_TO_PROFILE_LORE))
            ), ctx -> ctx.back());
        } else {
            builder.set(0, GuiItems.simple(
                    ItemTypes.BARRIER,
                    messages.getComponent(MessagesKeys.GUI_BTN_CLOSE_TITLE),
                    List.of(messages.getComponent(MessagesKeys.GUI_BTN_CLOSE_LORE))
            ), ctx -> ctx.close());
        }

        List<Component> headLore = buildHeadLore(messages);
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

        boolean dbReady = platform.getDatabaseRepository().isConnected();
        boolean canViewAlerts = session.hasPermission(PlayerAlertsScreen.PERMISSION);
        boolean canViewPunishments = session.hasPermission(PlayerPunishmentsScreen.PERMISSION);

        if (dbReady) {
            if (canViewAlerts) {
                builder.set(11, GuiItems.simple(
                        ItemTypes.PAPER,
                        messages.getComponent(MessagesKeys.GUI_HISTORY_HUB_ALERTS_TITLE),
                        List.of(
                                messages.getComponent(MessagesKeys.GUI_HISTORY_HUB_ALERTS_LORE_1),
                                messages.getComponent(MessagesKeys.GUI_HISTORY_HUB_ALERTS_LORE_2),
                                Component.empty(),
                                messages.getComponent(MessagesKeys.GUI_STATUS_CLICK_TO_BROWSE)
                        )
                ), ctx -> ctx.open(new PlayerAlertsScreen(targetId, targetName, 0)));
            }

            if (canViewPunishments) {
                builder.set(15, GuiItems.simple(
                        ItemTypes.IRON_AXE,
                        messages.getComponent(MessagesKeys.GUI_HISTORY_HUB_PUNISHMENTS_TITLE),
                        List.of(
                                messages.getComponent(MessagesKeys.GUI_HISTORY_HUB_PUNISHMENTS_LORE_1),
                                messages.getComponent(MessagesKeys.GUI_HISTORY_HUB_PUNISHMENTS_LORE_2),
                                Component.empty(),
                                messages.getComponent(MessagesKeys.GUI_STATUS_CLICK_TO_BROWSE)
                        )
                ), ctx -> ctx.open(new PlayerPunishmentsScreen(targetId, targetName, 0)));
            }
        } else {
            builder.set(22, GuiItems.simple(
                    ItemTypes.RED_CONCRETE,
                    messages.getComponent(MessagesKeys.GUI_ERR_DATABASE_OFFLINE),
                    List.of(
                            messages.getComponent(MessagesKeys.GUI_HISTORY_HUB_DB_LORE_1),
                            messages.getComponent(MessagesKeys.GUI_ERR_DB_UNREACHABLE)
                    )
            ));
        }

        return builder.build();
    }

    private List<Component> buildHeadLore(MessageService messages) {
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
            lore.add(messages.getComponent(MessagesKeys.GUI_LOADING_JOIN_TIMES));
        }

        return lore;
    }
}
