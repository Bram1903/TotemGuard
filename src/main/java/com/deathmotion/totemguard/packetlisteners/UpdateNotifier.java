/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2024 Bram and contributors
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

package com.deathmotion.totemguard.packetlisteners;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.api.versioning.TGVersion;
import com.deathmotion.totemguard.models.Constants;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.UserLoginEvent;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import java.util.concurrent.TimeUnit;

public class UpdateNotifier extends PacketListenerAbstract {
    private final TotemGuard plugin;
    private final Component updateComponent;

    public UpdateNotifier(TotemGuard plugin, TGVersion latestVersion) {
        this.plugin = plugin;

        this.updateComponent = Component.text()
                .append(Component.text("[TotemGuard] ", NamedTextColor.RED)
                        .decoration(TextDecoration.BOLD, true))
                .append(Component.text("Version " + latestVersion.toString() + " is ", NamedTextColor.GREEN))
                .append(Component.text("now available", NamedTextColor.GREEN)
                        .decorate(TextDecoration.UNDERLINED)
                        .hoverEvent(HoverEvent.showText(Component.text("Click to download", NamedTextColor.GREEN)))
                        .clickEvent(ClickEvent.openUrl(Constants.GITHUB_URL)))
                .append(Component.text("!", NamedTextColor.GREEN))
                .build();
    }

    @Override
    public void onUserLogin(UserLoginEvent event) {
        Player player = event.getPlayer();

        FoliaScheduler.getAsyncScheduler().runDelayed(plugin, (o) -> {
            if (player.hasPermission("TotemGuard.Update")) {
                player.sendMessage(updateComponent);
            }
        }, 2, TimeUnit.SECONDS);
    }
}