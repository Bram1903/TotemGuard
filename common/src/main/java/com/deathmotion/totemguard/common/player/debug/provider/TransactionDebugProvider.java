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

package com.deathmotion.totemguard.common.player.debug.provider;

import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.data.ping.PingData;
import com.deathmotion.totemguard.common.player.debug.DebugOverlayFrame;
import com.deathmotion.totemguard.common.player.debug.DebugOverlayProvider;
import com.deathmotion.totemguard.common.util.Palette;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

public final class TransactionDebugProvider implements DebugOverlayProvider {

    private static Component label(String text) {
        return Component.text(text, Palette.LABEL);
    }

    private static Component value(String text, TextColor color) {
        return Component.text(text, color);
    }

    private static Component separator() {
        return Component.text(" | ", Palette.SEPARATOR);
    }

    private static String formatPing(int ping) {
        return ping < 0 ? "-" : ping + "ms";
    }

    private static TextColor pingColor(int ping) {
        if (ping < 0) {
            return Palette.CONNECTIVE;
        }

        if (ping >= 250) {
            return Palette.DANGER;
        }

        if (ping >= 125) {
            return Palette.WARN;
        }

        return Palette.SUCCESS;
    }

    private static TextColor countColor(int count) {
        return count > 0 ? Palette.WARN : Palette.SUCCESS;
    }

    private static String replyStatus(PingData pingData) {
        if (!pingData.isObservedTransactionReply()) {
            return "none";
        }

        if (!pingData.isLastTransactionReplyValid()) {
            return "invalid";
        }

        StringBuilder status = new StringBuilder();

        if (pingData.isLastTransactionReplySkipped()) {
            status.append("skip:").append(pingData.getLastSkippedTransactionReplyCount());
        } else {
            status.append("ok");
        }

        if (pingData.isLastTransactionReplySynthetic()) {
            status.append("/syn");
        }

        return status.toString();
    }

    private static TextColor replyColor(PingData pingData) {
        if (!pingData.isObservedTransactionReply()) {
            return Palette.CONNECTIVE;
        }

        if (!pingData.isLastTransactionReplyValid() || pingData.isLastTransactionReplySkipped()) {
            return Palette.DANGER;
        }

        return pingData.isLastTransactionReplySynthetic() ? Palette.BRAND : Palette.SUCCESS;
    }

    private static String teleportStatus(PingData pingData) {
        if (!pingData.isObservedTeleportReply()) {
            return "none";
        }

        if (pingData.isLastTeleportReplySkipped()) {
            return "skip:" + pingData.getLastSkippedTeleportReplyCount();
        }

        return "ok";
    }

    private static TextColor teleportColor(PingData pingData) {
        if (!pingData.isObservedTeleportReply()) {
            return Palette.CONNECTIVE;
        }

        return pingData.isLastTeleportReplySkipped() ? Palette.DANGER : Palette.SUCCESS;
    }

    @Override
    public String getKey() {
        return "transactions";
    }

    @Override
    public String getDisplayName() {
        return "Transactions";
    }

    @Override
    public DebugOverlayFrame buildFrame(TGPlayer player) {
        PingData pingData = player.getPingData();

        Component line = Component.empty()
                .append(label("TX "))
                .append(value(formatPing(pingData.getTransactionPing()), pingColor(pingData.getTransactionPing())))
                .append(separator())
                .append(label("KA "))
                .append(value(formatPing(pingData.getKeepAlivePing()), pingColor(pingData.getKeepAlivePing())))
                .append(separator())
                .append(label("Own Q/A "))
                .append(value(String.valueOf(pingData.getPendingSyntheticTransactionCount()), countColor(pingData.getPendingSyntheticTransactionCount())))
                .append(Component.text("/", Palette.CAPTION))
                .append(value(String.valueOf(pingData.getAcceptedSyntheticTransactionCount()), Palette.BRAND))
                .append(separator())
                .append(label("All Q/A "))
                .append(value(String.valueOf(pingData.getPendingTransactionCount()), countColor(pingData.getPendingTransactionCount())))
                .append(Component.text("/", Palette.CAPTION))
                .append(value(String.valueOf(pingData.getAcceptedTransactionCount()), Palette.BRAND))
                .append(separator())
                .append(label("Reply "))
                .append(value(replyStatus(pingData), replyColor(pingData)))
                .append(separator())
                .append(label("TP "))
                .append(value(teleportStatus(pingData), teleportColor(pingData)));

        return DebugOverlayFrame.of(line);
    }
}
