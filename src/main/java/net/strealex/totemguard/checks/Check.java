package net.strealex.totemguard.checks;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.User;
import net.strealex.totemguard.TotemGuard;
import net.strealex.totemguard.config.Settings;
import net.strealex.totemguard.discord.DiscordWebhook;
import net.strealex.totemguard.manager.AlertManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.awt.*;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Check {

    private final ConcurrentHashMap<UUID, Integer> violations;

    private final String checkName;
    private final String checkDescription;
    private final boolean experimental;

    private final TotemGuard plugin;
    private final AlertManager alertManager;

    public Check(TotemGuard plugin, String checkName, String checkDescription, boolean experimental) {
        this.plugin = plugin;
        this.checkName = checkName;
        this.checkDescription = checkDescription;
        this.experimental = experimental;

        this.violations = new ConcurrentHashMap<>();

        this.alertManager = plugin.getAlertManager();

        // Convert minutes to ticks (20 ticks = 1 second)
        long resetInterval = plugin.getConfigManager().getSettings().getResetViolationsInterval() * 60L * 20L;
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::resetData, resetInterval, resetInterval);
    }

    public Check(TotemGuard plugin, String checkName, String checkDescription) {
        this(plugin, checkName, checkDescription, false);
    }

    public final void flag(Player player, Component details, ICheckSettings settings) {
        UUID uuid = player.getUniqueId();
        violations.compute(uuid, (key, value) -> value == null ? 1 : value + 1);

        User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
        int ping = player.getPing();

        String gamemode = String.valueOf(player.getGameMode());
        String clientBrand = Objects.requireNonNullElse(player.getClientBrandName(), "Unknown");

        Component message = createAlertComponent(user, clientBrand, player, gamemode, ping, details, settings);

        alertManager.sendAlert(message);
        sendWebhookMessage(player, details, false);
        punishPlayer(player, settings);
    }

    public void resetData() {
        violations.clear();

        alertManager.sendAlert(Component.text()
                .append(LegacyComponentSerializer.legacyAmpersand().deserialize(plugin.getConfigManager().getSettings().getPrefix()))
                .append(Component.text("All flag counts have been reset.", NamedTextColor.GREEN))
                .build());
    }

    public int getTps() {
        return (int) Math.round(Bukkit.getTPS()[0]);
    }

    private int getViolations(UUID player) {
        return violations.getOrDefault(player, 0);
    }

    private void sendWebhookMessage(Player player, Component details, boolean isPunishment) {
        final Settings.Webhook settings = plugin.getConfigManager().getSettings().getWebhook();
        final Settings.Webhook.PunishmentWebhook punishmentSettings = plugin.getConfigManager().getSettings().getWebhook().getPunishmentWebhook();
        if (!settings.isEnabled()) return;

        DiscordWebhook webhook = new DiscordWebhook(settings.getUrl());
        webhook.setUsername(settings.getName());
        webhook.setAvatarUrl(settings.getProfileImage());

        DiscordWebhook.EmbedObject embed = new DiscordWebhook.EmbedObject();
        embed.setTitle(isPunishment ? punishmentSettings.getPunishmentTitle() : settings.getTitle());
        embed.setThumbnail("http://cravatar.eu/avatar/" + player.getName() + "/64.png");
        embed.setColor(Color.decode(isPunishment ? punishmentSettings.getPunishmentTitle() : settings.getColor()));
        embed.addField("**Player**", player.getName(), true);
        embed.addField("**Check**", checkName, true);
        embed.addField("**Violations**", String.valueOf(getViolations(player.getUniqueId())), true);
        embed.addField("**Client Brand**", player.getClientBrandName(), true);
        embed.addField("**Ping**", String.valueOf(player.getPing()), true);
        embed.addField("**TPS**", String.valueOf(getTps()), true);

        // Serialize details to plain text
        String serializedDetails = PlainTextComponentSerializer.plainText().serialize(details);
        String formattedDetails = "```" + serializedDetails.replace("\\", "\\\\")
                .replace("`", "\\`")
                .replace("\n", "\\n") +
                "```";

        embed.addField("**Details**", formattedDetails, false);

        if (settings.isTimestamp()) {
            embed.setTimestamp(Instant.now().toString());
        }

        webhook.addEmbed(embed);

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                webhook.execute();
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to send webhook message: " + e.getMessage());
            }
        });
    }

    private void punishPlayer(Player player, ICheckSettings settings) {
        if (!(settings.isPunishable())) return;
        if (getViolations(player.getUniqueId()) >= settings.getMaxViolations()) {
            violations.remove(player.getUniqueId());

            Bukkit.getScheduler().runTask(plugin, () -> {
                Arrays.stream(settings.getPunishmentCommands()).iterator().forEachRemaining(punishCommand -> {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), punishCommand.replace("%player%", player.getName()));
                });
            });

            // Send punishment webhook
            sendWebhookMessage(player, Component.text("Player has been punished for exceeding violation limit."), true);
        }
    }

    private Component createAlertComponent(User user, String clientBrand, Player player, String gamemode, int ping, Component details, ICheckSettings settings) {
        final Settings globalSettings = plugin.getConfigManager().getSettings();

        Component hoverInfo = Component.text()
                .append(Component.text("TPS: ", NamedTextColor.GRAY))
                .append(Component.text(getTps(), NamedTextColor.GOLD))
                .append(Component.text(" |", NamedTextColor.DARK_GRAY))
                .append(Component.text(" Client Version: ", NamedTextColor.GRAY))
                .append(Component.text(user.getClientVersion().getReleaseName(), NamedTextColor.GOLD))
                .append(Component.text(" |", NamedTextColor.DARK_GRAY))
                .append(Component.text(" Client Brand: ", NamedTextColor.GRAY))
                .append(Component.text(clientBrand, NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("Player: ", NamedTextColor.GRAY))
                .append(Component.text(player.getName(), NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.text("Gamemode: ", NamedTextColor.GRAY))
                .append(Component.text(gamemode, NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.text("Ping: ", NamedTextColor.GRAY))
                .append(Component.text(ping + "ms", NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.newline())
                .append(details)
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("Click to ", NamedTextColor.GRAY))
                .append(Component.text("teleport ", NamedTextColor.GOLD))
                .append(Component.text("to " + player.getName() + ".", NamedTextColor.GRAY))
                .build();

        Component message = Component.text()
                .append(LegacyComponentSerializer.legacyAmpersand().deserialize(globalSettings.getPrefix()))
                .append(Component.text(player.getName(), NamedTextColor.YELLOW))
                .append(Component.text(" failed ", NamedTextColor.GRAY))
                .append(Component.text(checkName, NamedTextColor.GOLD)
                        .hoverEvent(HoverEvent.showText(Component.text(checkDescription, NamedTextColor.GRAY))))
                .clickEvent(ClickEvent.runCommand("/tp " + player.getName()))
                .build();

        // Determine the violation format based on whether punishment is enabled
        Component totalViolationsComponent;
        int totalViolations = getViolations(player.getUniqueId());

        if (settings.isPunishable()) {
            totalViolationsComponent = Component.text()
                    .append(Component.text(" VL[", NamedTextColor.GRAY))
                    .append(Component.text(totalViolations + "/" + settings.getMaxViolations(), NamedTextColor.GOLD))
                    .append(Component.text("]", NamedTextColor.GRAY))
                    .build();

        } else {
            totalViolationsComponent = Component.text()
                    .append(Component.text(" VL[", NamedTextColor.GRAY))
                    .append(Component.text(totalViolations, NamedTextColor.GOLD))
                    .append(Component.text("]", NamedTextColor.GRAY))
                    .build();

        }
        message = message.append(totalViolationsComponent);

        message = message
                .append(Component.text(" [Info]", NamedTextColor.DARK_GRAY)
                        .hoverEvent(HoverEvent.showText(hoverInfo)))
                .decoration(TextDecoration.ITALIC, false);

        return message;
    }

}
