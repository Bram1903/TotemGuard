package com.deathmotion.totemguard.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import com.deathmotion.totemguard.data.CheckDetails;
import com.deathmotion.totemguard.data.TotemPlayer;

public class AlertCreator {
    public static Component createAlertComponent(TotemPlayer player, CheckDetails checkDetails, Component details, String prefix) {
        Component hoverInfo = Component.text()
                .append(Component.text("TPS: ", NamedTextColor.GRAY))
                .append(Component.text(checkDetails.getTps(), NamedTextColor.GOLD))
                .append(Component.text(" |", NamedTextColor.DARK_GRAY))
                .append(Component.text(" Client Version: ", NamedTextColor.GRAY))
                .append(Component.text(player.getClientVersion().getReleaseName(), NamedTextColor.GOLD))
                .append(Component.text(" |", NamedTextColor.DARK_GRAY))
                .append(Component.text(" Client Brand: ", NamedTextColor.GRAY))
                .append(Component.text(player.getClientBrandName(), NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("Player: ", NamedTextColor.GRAY))
                .append(Component.text(player.getUsername(), NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.text("Gamemode: ", NamedTextColor.GRAY))
                .append(Component.text(checkDetails.getGamemode(), NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.text("Ping: ", NamedTextColor.GRAY))
                .append(Component.text(checkDetails.getPing() + "ms", NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.newline())
                .append(details)
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("Click to ", NamedTextColor.GRAY))
                .append(Component.text("teleport ", NamedTextColor.GOLD))
                .append(Component.text("to " + player.getUsername() + ".", NamedTextColor.GRAY))
                .build();

        Component message = Component.text()
                .append(LegacyComponentSerializer.legacyAmpersand().deserialize(prefix))
                .append(Component.text(player.getUsername(), NamedTextColor.YELLOW))
                .append(Component.text(" failed ", NamedTextColor.GRAY))
                .append(Component.text(checkDetails.getCheckName(), NamedTextColor.GOLD)
                        .hoverEvent(HoverEvent.showText(Component.text(checkDetails.getCheckDescription(), NamedTextColor.GRAY))))
                .clickEvent(ClickEvent.runCommand("/tp " + player.getUsername()))
                .build();

        Component totalViolationsComponent;

        if (checkDetails.isPunishable()) {
            totalViolationsComponent = Component.text()
                    .append(Component.text(" VL[", NamedTextColor.GRAY))
                    .append(Component.text(checkDetails.getViolations() + "/" + checkDetails.getMaxViolations(), NamedTextColor.GOLD))
                    .append(Component.text("]", NamedTextColor.GRAY))
                    .build();
        } else {
            totalViolationsComponent = Component.text()
                    .append(Component.text(" VL[", NamedTextColor.GRAY))
                    .append(Component.text(checkDetails.getViolations(), NamedTextColor.GOLD))
                    .append(Component.text("]", NamedTextColor.GRAY))
                    .build();

        }
        message = message.append(totalViolationsComponent);

        message = message
                .append(Component.text(" [Info]", NamedTextColor.DARK_GRAY)
                        .hoverEvent(HoverEvent.showText(hoverInfo)))
                .decoration(TextDecoration.ITALIC, false);

        if (checkDetails.isExperimental()) {
            message = message.append(Component.text(" *", NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD));
        }

        return message;
    }
}
