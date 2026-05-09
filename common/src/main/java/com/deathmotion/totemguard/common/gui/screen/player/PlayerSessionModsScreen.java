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

package com.deathmotion.totemguard.common.gui.screen.player;

import com.deathmotion.totemguard.api.mod.DetectedMod;
import com.deathmotion.totemguard.api.mod.ModDetectionMethod;
import com.deathmotion.totemguard.api.mod.ModSeverity;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.config.key.MessagesKeys;
import com.deathmotion.totemguard.common.gui.*;
import com.deathmotion.totemguard.common.message.MessageService;
import com.deathmotion.totemguard.common.util.NumberFormatter;
import com.deathmotion.totemguard.common.util.Palette;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

import java.util.*;
import java.util.logging.Level;

public final class PlayerSessionModsScreen extends GuiScreen {

    public static final String PERMISSION = "TotemGuard.Gui.Mods";

    private static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private final UUID targetId;
    private final String targetName;
    private volatile List<DetectedMod> loaded;

    public PlayerSessionModsScreen(UUID targetId, String targetName) {
        this.targetId = targetId;
        this.targetName = targetName;
    }

    private static String severityLabel(MessageService messages, ModSeverity severity) {
        return messages.getString(switch (severity) {
            case LOG -> MessagesKeys.GUI_MOD_SEVERITY_LOG;
            case KICK -> MessagesKeys.GUI_MOD_SEVERITY_KICK;
            case BAN -> MessagesKeys.GUI_MOD_SEVERITY_BAN;
            case KICK_THEN_BAN -> MessagesKeys.GUI_MOD_SEVERITY_KICK_THEN_BAN;
        });
    }

    private static TextColor severityColor(ModSeverity severity) {
        return switch (severity) {
            case LOG -> Palette.LABEL;
            case KICK, KICK_THEN_BAN -> Palette.WARN;
            case BAN -> Palette.DANGER;
        };
    }

    private static ItemType severityItem(ModSeverity severity) {
        return switch (severity) {
            case LOG -> ItemTypes.PAPER;
            case KICK -> ItemTypes.ORANGE_DYE;
            case KICK_THEN_BAN -> ItemTypes.ORANGE_DYE;
            case BAN -> ItemTypes.REDSTONE;
        };
    }

    private static String methodLabel(MessageService messages, ModDetectionMethod method) {
        return messages.getString(switch (method) {
            case PLUGIN_CHANNEL_REGISTRATION -> MessagesKeys.GUI_MOD_METHOD_PLUGIN_CHANNEL;
            case PLUGIN_MESSAGE -> MessagesKeys.GUI_MOD_METHOD_PLUGIN_MESSAGE;
            case TRANSLATION -> MessagesKeys.GUI_MOD_METHOD_TRANSLATION;
        });
    }

    private static int severityWeight(ModSeverity severity) {
        return switch (severity) {
            case LOG -> 0;
            case KICK -> 1;
            case KICK_THEN_BAN -> 2;
            case BAN -> 3;
        };
    }

    @Override
    public String requiredPermission() {
        return PERMISSION;
    }

    @Override
    public void onOpen(GuiSession session) {
        TGPlatform platform = TGPlatform.getInstance();
        platform.getScheduler().runAsyncTask(() -> {
            try {
                Set<DetectedMod> mods = platform.getModDetectionService().getDetectedMods(targetId);
                List<DetectedMod> sorted = new ArrayList<>(mods);
                sorted.sort(Comparator.comparingInt((DetectedMod m) -> severityWeight(m.severity()))
                        .reversed()
                        .thenComparing(DetectedMod::id));
                this.loaded = sorted;
            } catch (Exception ex) {
                platform.getLogger().log(Level.WARNING,
                        "Failed to load detected mods for " + targetId + ": " + ex.getMessage());
                this.loaded = List.of();
            } finally {
                platform.getGuiManager().refresh(session.viewerId());
            }
        });
    }

    @Override
    public GuiRenderResult render(GuiSession session) {
        MessageService messages = TGPlatform.getInstance().getMessageService();
        GuiRenderResult.Builder builder = GuiRenderResult.builder(6,
                GuiTitle.of(messages.getString(MessagesKeys.GUI_MOD_SESSION_TITLE, Map.of("tg_player", targetName))));
        builder.fillEmpty(GuiItems.filler());

        builder.set(0, GuiItems.simple(
                ItemTypes.ARROW,
                messages.getComponent(MessagesKeys.GUI_BTN_BACK_TITLE),
                List.of(messages.getComponent(MessagesKeys.GUI_BTN_BACK_LORE))
        ), ctx -> ctx.back());

        List<DetectedMod> mods = this.loaded;

        if (mods == null) {
            builder.set(22, GuiItems.simple(
                    ItemTypes.CLOCK,
                    messages.getComponent(MessagesKeys.GUI_LOADING_GENERIC),
                    List.of()
            ));
            return builder.build();
        }

        if (mods.isEmpty()) {
            builder.set(22, GuiItems.simple(
                    ItemTypes.LIME_CONCRETE,
                    messages.getComponent(MessagesKeys.GUI_MOD_SESSION_EMPTY_TITLE),
                    List.of(messages.getComponent(MessagesKeys.GUI_MOD_SESSION_EMPTY_LORE))
            ));
            return builder.build();
        }

        for (int i = 0; i < mods.size() && i < CONTENT_SLOTS.length; i++) {
            builder.set(CONTENT_SLOTS[i], buildModTile(mods.get(i), messages));
        }

        renderFooter(builder, mods, messages);
        return builder.build();
    }

    private ItemStack buildModTile(DetectedMod mod, MessageService messages) {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(messages.getString(MessagesKeys.GUI_MOD_SESSION_SEVERITY_LABEL) + ": ", Palette.LABEL)
                .append(Component.text(severityLabel(messages, mod.severity()), severityColor(mod.severity()))));
        lore.add(GuiText.line(messages.getString(MessagesKeys.GUI_MOD_SESSION_METHOD_LABEL),
                methodLabel(messages, mod.detectionMethod())));

        return GuiItems.simple(
                severityItem(mod.severity()),
                Component.text(mod.id(), severityColor(mod.severity())),
                lore
        );
    }

    private void renderFooter(GuiRenderResult.Builder builder, List<DetectedMod> mods, MessageService messages) {
        List<Component> lore = new ArrayList<>();
        lore.add(GuiText.line(messages.getString(MessagesKeys.GUI_MOD_SESSION_FOOTER_LABEL),
                NumberFormatter.grouped(mods.size())));

        builder.set(49, GuiItems.simple(
                ItemTypes.PAPER,
                messages.getComponent(MessagesKeys.GUI_MOD_SESSION_FOOTER_TITLE),
                lore
        ));
    }
}
