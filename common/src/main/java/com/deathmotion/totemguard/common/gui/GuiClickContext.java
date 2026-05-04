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

package com.deathmotion.totemguard.common.gui;

import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import net.kyori.adventure.text.Component;

public final class GuiClickContext {

    private final GuiManager manager;
    private final GuiSession session;
    private final WrapperPlayClientClickWindow packet;
    private boolean rendered;

    GuiClickContext(GuiManager manager, GuiSession session, WrapperPlayClientClickWindow packet) {
        this.manager = manager;
        this.session = session;
        this.packet = packet;
    }

    public GuiSession session() {
        return session;
    }

    public WrapperPlayClientClickWindow packet() {
        return packet;
    }

    public int clickedSlot() {
        return packet.getSlot();
    }

    public void open(GuiScreen screen) {
        if (manager.pushScreen(session.viewerId(), screen)) {
            this.rendered = true;
        }
    }

    public void replace(GuiScreen screen) {
        if (manager.replaceScreen(session.viewerId(), screen)) {
            this.rendered = true;
        }
    }

    public void back() {
        manager.back(session.viewerId());
        this.rendered = true;
    }

    public void close() {
        manager.close(session.viewerId(), true);
        this.rendered = true;
    }

    public void refresh() {
        manager.refresh(session.viewerId());
        this.rendered = true;
    }

    public void message(Component message) {
        session.user().sendMessage(message);
    }

    boolean rendered() {
        return rendered;
    }
}
