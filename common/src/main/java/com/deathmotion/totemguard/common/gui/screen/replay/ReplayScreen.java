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

package com.deathmotion.totemguard.common.gui.screen.replay;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.commands.CommandDefaults;
import com.deathmotion.totemguard.common.config.key.MessagesKeys;
import com.deathmotion.totemguard.common.gui.*;
import com.deathmotion.totemguard.common.message.MessageService;
import com.deathmotion.totemguard.common.replay.ReplayService;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public abstract class ReplayScreen extends GuiScreen {

    public static final String PERMISSION = CommandDefaults.PERMISSION_PREFIX + "replay";

    protected static @Nullable ReplayService service() {
        return TGPlatform.getInstance().getReplayService();
    }

    protected static MessageService messages() {
        return TGPlatform.getInstance().getMessageService();
    }

    @Override
    public String requiredPermission() {
        return PERMISSION;
    }

    protected void backButton(GuiRenderResult.Builder builder, int slot, Supplier<GuiScreen> parent) {
        builder.set(slot, GuiItems.simple(
                ItemTypes.ARROW,
                messages().getComponent(MessagesKeys.GUI_BTN_BACK_TITLE),
                List.of(messages().getComponent(MessagesKeys.GUI_BTN_BACK_LORE))
        ), context -> leave(context, parent.get()));
    }

    protected void closeButton(GuiRenderResult.Builder builder, int slot) {
        builder.set(slot, GuiItems.simple(
                ItemTypes.BARRIER,
                messages().getComponent(MessagesKeys.GUI_BTN_CLOSE_TITLE),
                List.of(messages().getComponent(MessagesKeys.GUI_BTN_CLOSE_LORE))
        ), GuiClickContext::close);
    }

    protected Component promptFor(String field) {
        return messages().getComponent(MessagesKeys.GUI_CHAT_INPUT_PROMPT,
                Map.of("tg_field", field, "tg_cancel", GuiChatInput.abortWord()));
    }

    protected void leave(GuiClickContext context, @Nullable GuiScreen parent) {
        if (context.session().hasParent() || parent == null) {
            context.back();
            return;
        }
        context.replace(parent);
    }
}
