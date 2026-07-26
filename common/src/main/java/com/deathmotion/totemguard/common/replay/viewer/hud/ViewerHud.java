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

package com.deathmotion.totemguard.common.replay.viewer.hud;

import com.deathmotion.totemguard.common.gui.GuiItems;
import com.deathmotion.totemguard.common.player.inventory.InventoryConstants;
import com.deathmotion.totemguard.common.replay.ReplayText;
import com.deathmotion.totemguard.common.replay.viewer.net.ViewerSink;
import com.deathmotion.totemguard.common.util.ActionBars;
import com.deathmotion.totemguard.common.util.Palette;
import com.github.retrooper.packetevents.protocol.chat.ChatTypes;
import com.github.retrooper.packetevents.protocol.chat.message.ChatMessage_v1_16;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class ViewerHud {

    private static final long ACTION_BAR_INTERVAL_MILLIS = 250L;
    private static final UUID SERVER = new UUID(0L, 0L);

    private final ViewerSink sink;
    private final User user;

    private long lastActionBar;
    private int lastLevel = -1;

    public ViewerHud(ViewerSink sink, User user) {
        this.sink = sink;
        this.user = user;
    }

    private static String speedLabel(double speed) {
        String text = String.format(Locale.ROOT, "%.2f", speed);
        while (text.endsWith("0")) text = text.substring(0, text.length() - 1);
        if (text.endsWith(".")) text = text.substring(0, text.length() - 1);
        return "x" + text;
    }

    public void hotbar(ViewerStatus status) {
        List<ItemStack> items = new ArrayList<>(InventoryConstants.INVENTORY_SIZE);
        for (int slot = 0; slot < InventoryConstants.INVENTORY_SIZE; slot++) items.add(ItemStack.EMPTY);

        for (ViewerControl control : ViewerControl.values()) {
            items.set(InventoryConstants.HOTBAR_START + control.getSlot(),
                    GuiItems.simple(control.getItem(), name(control, status), lore(control, status)));
        }

        sink.send(new WrapperPlayServerWindowItems(InventoryConstants.PLAYER_WINDOW_ID, 0, items, null));
    }

    public void selectSlot(int slot) {
        sink.send(new WrapperPlayServerHeldItemChange(slot));
    }

    public void tick(ViewerStatus status) {
        tick(status, false);
    }

    public void tick(ViewerStatus status, boolean force) {
        long now = System.currentTimeMillis();
        if (!force && now - lastActionBar < ACTION_BAR_INTERVAL_MILLIS) return;
        lastActionBar = now;

        sink.send(ActionBars.packet(user, actionBar(status)));

        int level = (int) (status.elapsedNanos() / 1_000_000_000L);
        float progress = status.known()
                ? Math.min(1f, (float) ((double) status.elapsedNanos() / status.durationNanos()))
                : 0f;
        if (level != lastLevel || progress > 0f) {
            lastLevel = level;
            sink.send(new WrapperPlayServerSetExperience(progress, level, 0));
        }
    }

    public void toast(Component message) {
        sink.send(ActionBars.packet(user, message));
        lastActionBar = System.currentTimeMillis();
    }

    public void chat(Component message) {
        sink.send(chatPacket(message));
    }

    private PacketWrapper<?> chatPacket(Component message) {
        if (user.getPacketVersion().isNewerThanOrEquals(ClientVersion.V_1_19)) {
            return new WrapperPlayServerSystemChatMessage(false, message);
        }
        return new WrapperPlayServerChatMessage(new ChatMessage_v1_16(message, ChatTypes.CHAT, SERVER));
    }

    private Component actionBar(ViewerStatus status) {
        Component head = status.seeking()
                ? Component.text("… SEEKING", Palette.WARN, TextDecoration.BOLD)
                : status.finished()
                ? Component.text("■ ENDED", Palette.CONNECTIVE, TextDecoration.BOLD)
                : status.paused()
                ? Component.text("❚❚ PAUSED", Palette.WARN, TextDecoration.BOLD)
                : Component.text("▶ WATCHING", Palette.SUCCESS, TextDecoration.BOLD);

        String clock = status.known()
                ? ReplayText.elapsed(status.elapsedNanos()) + " / " + ReplayText.elapsed(status.durationNanos())
                : ReplayText.elapsed(status.elapsedNanos());

        Component line = head
                .append(sep()).append(Component.text(status.display(), Palette.VALUE))
                .append(sep()).append(Component.text(clock, Palette.LABEL))
                .append(sep()).append(Component.text(speedLabel(status.speed()), Palette.CONNECTIVE))
                .append(sep()).append(Component.text(status.camera().getLabel(), Palette.CONNECTIVE));

        if (!status.phased()) return line;
        return line.append(sep())
                .append(Component.text("PHASED, right click to return", Palette.VIOLET));
    }

    private Component sep() {
        return Component.text("  ·  ", Palette.SEPARATOR);
    }

    private Component name(ViewerControl control, ViewerStatus status) {
        return switch (control) {
            case REWIND -> Component.text("◀◀  Back " + ViewerStatus.SEEK_SECONDS + "s", Palette.VALUE);
            case PLAY_PAUSE -> status.paused()
                    ? Component.text("▶  Resume", Palette.SUCCESS)
                    : Component.text("❚❚  Pause", Palette.WARN);
            case FORWARD -> Component.text("▶▶  Forward " + ViewerStatus.SEEK_SECONDS + "s", Palette.VALUE);
            case SPEED -> Component.text("Speed  " + speedLabel(status.speed()), Palette.VALUE);
            case CAMERA -> Component.text("Camera  " + status.camera().getLabel(), Palette.VALUE);
            case PHASE -> status.phased()
                    ? Component.text("Phase  on", Palette.VIOLET)
                    : Component.text("Phase  off", Palette.LABEL);
            case RESTART -> Component.text("↺  Back to the start", Palette.VALUE);
            case LEAVE -> Component.text("Leave the replay", Palette.DANGER);
        };
    }

    private List<Component> lore(ViewerControl control, ViewerStatus status) {
        Component right = Component.text("Right click", Palette.CAPTION);
        return switch (control) {
            case REWIND, FORWARD -> List.of(right.append(Component.text(
                    " to jump " + ViewerStatus.SEEK_SECONDS + " seconds", Palette.CAPTION)));
            case PLAY_PAUSE -> List.of(right.append(
                    Component.text(" to " + (status.paused() ? "resume" : "pause"), Palette.CAPTION)));
            case SPEED -> List.of(
                    right.append(Component.text(" to speed up", Palette.CAPTION)),
                    Component.text("Left click to slow down", Palette.CAPTION));
            case CAMERA -> List.of(
                    right.append(Component.text(" to cycle free, follow and eyes", Palette.CAPTION)),
                    Component.text(status.camera().getDescription(), Palette.CAPTION));
            case PHASE -> List.of(
                    right.append(Component.text(" to fly through blocks", Palette.CAPTION)),
                    Component.text("Right click again to come back", Palette.CAPTION));
            case RESTART -> List.of(right.append(
                    Component.text(" to replay from the beginning", Palette.CAPTION)));
            case LEAVE -> List.of(right.append(
                    Component.text(" to return to where you were", Palette.CAPTION)));
        };
    }
}
