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

import com.deathmotion.totemguard.api.check.Check;
import com.deathmotion.totemguard.api.mod.DetectedMod;
import com.deathmotion.totemguard.api.mod.ModSeverity;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.config.key.MessagesKeys;
import com.deathmotion.totemguard.common.database.model.PlayerRecord;
import com.deathmotion.totemguard.common.features.session.SessionSnapshot;
import com.deathmotion.totemguard.common.features.session.SessionViolationStore;
import com.deathmotion.totemguard.common.gui.*;
import com.deathmotion.totemguard.common.gui.screen.history.HistoryText;
import com.deathmotion.totemguard.common.gui.screen.history.PlayerHistoryHubScreen;
import com.deathmotion.totemguard.common.message.MessageService;
import com.deathmotion.totemguard.common.network.NetworkPresenceRepository;
import com.deathmotion.totemguard.common.network.RemotePlayerEntry;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.util.Palette;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.logging.Level;

public final class PlayerProfileScreen extends GuiScreen {

    public static final String PERMISSION = "TotemGuard.Gui.Profile";

    private static final int SLOT_BACK = 0;
    private static final int SLOT_HEAD = 4;
    private static final int SLOT_MODS = 20;
    private static final int SLOT_MONITOR = 22;
    private static final int SLOT_HISTORY = 24;
    private static final int VIOLATION_LIST_LIMIT = 3;
    private static final int MOD_LIST_LIMIT = 3;
    private final UUID targetId;
    private final String fallbackName;
    private volatile @Nullable PlayerRecord dbRecord;
    private volatile boolean dbAttempted;
    private volatile @Nullable RemotePlayerEntry remoteEntry;
    private volatile @Nullable UserProfile remoteProfile;
    private volatile boolean presenceAttempted;
    private volatile @Nullable SessionSnapshot sessionSnapshot;
    private volatile @Nullable Set<DetectedMod> detectedMods;

    public PlayerProfileScreen(TGPlayer player) {
        this(player.getUuid(), player.getName());
    }

    public PlayerProfileScreen(UUID targetId, String fallbackName) {
        this.targetId = targetId;
        this.fallbackName = fallbackName;
    }

    private static PlayerHistoryHubScreen historyHub(UUID targetUuid, String targetName) {
        TGPlayer local = TGPlatform.getInstance().getPlayerRepository().getPlayer(targetUuid);
        return local != null ? new PlayerHistoryHubScreen(local) : new PlayerHistoryHubScreen(targetUuid, targetName);
    }

    private static String formatSessionLength(Instant start) {
        Duration d = Duration.between(start, Instant.now());
        if (d.isNegative() || d.isZero()) return "just started";
        long hours = d.toHours();
        long minutes = d.toMinutesPart();
        long seconds = d.toSecondsPart();
        if (hours > 0) return hours + "h " + minutes + "m";
        if (minutes > 0) return minutes + "m " + seconds + "s";
        return seconds + "s";
    }

    @Override
    public String requiredPermission() {
        return PERMISSION;
    }

    @Override
    public void onOpen(GuiSession session) {
        TGPlatform platform = TGPlatform.getInstance();

        boolean dbReady = platform.getDatabaseRepository().isConnected();
        boolean needsRemoteLookup = platform.getPlayerRepository().getPlayer(targetId) == null
                && platform.getNetworkPresenceRepository() != null;
        boolean needsSessionLookup = platform.getSessionViolationStore() != null
                && platform.getRedisRepository().isConnected();

        if (!dbReady) this.dbAttempted = true;
        if (!needsRemoteLookup) this.presenceAttempted = true;

        platform.getScheduler().runAsyncTask(() -> {
            try {
                if (dbReady) {
                    try {
                        this.dbRecord = platform.getDatabaseRepository().findPlayerByUuid(targetId);
                    } catch (Exception ex) {
                        platform.getLogger().log(Level.WARNING,
                                "Failed to load profile times for " + targetId + ": " + ex.getMessage());
                    } finally {
                        this.dbAttempted = true;
                    }
                }
                if (needsRemoteLookup) {
                    try {
                        NetworkPresenceRepository presence = platform.getNetworkPresenceRepository();
                        this.remoteEntry = presence.findByUuid(targetId);
                        this.remoteProfile = presence.loadProfile(targetId);
                    } catch (Exception ex) {
                        platform.getLogger().log(Level.WARNING,
                                "Failed to load remote presence for " + targetId + ": " + ex.getMessage());
                    } finally {
                        this.presenceAttempted = true;
                    }
                }
                if (needsSessionLookup) {
                    try {
                        SessionViolationStore store = platform.getSessionViolationStore();
                        if (store != null) this.sessionSnapshot = store.getSession(targetId);
                    } catch (Exception ex) {
                        platform.getLogger().log(Level.WARNING,
                                "Failed to load session snapshot for " + targetId + ": " + ex.getMessage());
                    }
                }
                try {
                    this.detectedMods = platform.getModDetectionService().getDetectedMods(targetId);
                } catch (Exception ex) {
                    platform.getLogger().log(Level.WARNING,
                            "Failed to load detected mods for " + targetId + ": " + ex.getMessage());
                    this.detectedMods = Set.of();
                }
            } finally {
                platform.getGuiManager().refresh(session.viewerId());
            }
        });
    }

    @Override
    public GuiRenderResult render(GuiSession session) {
        TGPlatform platform = TGPlatform.getInstance();
        MessageService messages = platform.getMessageService();
        TGPlayer target = platform.getPlayerRepository().getPlayer(targetId);

        RemotePlayerEntry remote = target == null ? this.remoteEntry : null;

        String targetName = target != null ? target.getName()
                : (remote != null ? remote.playerName() : fallbackName);

        GuiRenderResult.Builder builder = GuiRenderResult.builder(4,
                GuiTitle.of(messages.getString(MessagesKeys.GUI_PROFILE_TITLE, Map.of("tg_player", targetName))));
        builder.fillEmpty(GuiItems.filler());

        renderBackOrClose(builder, session, messages);

        if (target == null && remote == null && presenceAttempted) {
            builder.set(SLOT_HEAD, GuiItems.simple(
                    ItemTypes.RED_CONCRETE,
                    messages.getComponent(MessagesKeys.GUI_PROFILE_UNTRACKED_TITLE, Map.of("tg_player", targetName)),
                    List.of(
                            GuiText.line(messages.getString(MessagesKeys.GUI_PROFILE_HEAD_UUID_LABEL), targetId.toString()),
                            messages.getComponent(MessagesKeys.GUI_PROFILE_UNTRACKED_LORE)
                    )
            ));
            return builder.build();
        }

        if (target != null) {
            builder.set(SLOT_HEAD, GuiItems.playerHead(
                    target.getUser().getProfile(),
                    Component.text(target.getName(), Palette.SUCCESS),
                    buildHeadLore(target, messages)
            ));
        } else if (remote != null) {
            UserProfile profile = this.remoteProfile;
            if (profile == null) profile = new UserProfile(remote.playerUuid(), remote.playerName());
            builder.set(SLOT_HEAD, GuiItems.playerHead(
                    profile,
                    Component.text(remote.playerName(), Palette.SUCCESS),
                    buildRemoteHeadLore(remote, messages)
            ));
        } else {
            builder.set(SLOT_HEAD, GuiItems.simple(
                    ItemTypes.PLAYER_HEAD,
                    Component.text(targetName, Palette.SUCCESS),
                    List.of(messages.getComponent(MessagesKeys.GUI_PROFILE_FIRST_JOINED_LOADING))
            ));
        }

        renderModsButton(builder, session, targetId, targetName, messages);
        renderMonitorButton(builder, session, targetId, targetName, messages);
        renderHistoryButton(builder, session, targetId, targetName, messages);

        return builder.build();
    }

    private void renderBackOrClose(GuiRenderResult.Builder builder, GuiSession session, MessageService messages) {
        if (session.hasParent()) {
            builder.set(SLOT_BACK, GuiItems.simple(
                    ItemTypes.ARROW,
                    messages.getComponent(MessagesKeys.GUI_BTN_BACK_TITLE),
                    List.of(messages.getComponent(MessagesKeys.GUI_BTN_BACK_LORE))
            ), ctx -> ctx.back());
        } else {
            builder.set(SLOT_BACK, GuiItems.simple(
                    ItemTypes.BARRIER,
                    messages.getComponent(MessagesKeys.GUI_BTN_CLOSE_TITLE),
                    List.of(messages.getComponent(MessagesKeys.GUI_BTN_CLOSE_LORE))
            ), ctx -> ctx.close());
        }
    }

    private void renderMonitorButton(GuiRenderResult.Builder builder, GuiSession session, UUID targetUuid, String targetName, MessageService messages) {
        if (!session.hasPermission(PlayerMonitorScreen.PERMISSION)) return;

        boolean self = session.viewerId().equals(targetUuid);
        builder.set(SLOT_MONITOR, GuiItems.simple(
                self ? ItemTypes.BARRIER : ItemTypes.CHEST,
                self
                        ? messages.getComponent(MessagesKeys.GUI_PROFILE_MONITOR_SELF_TITLE)
                        : messages.getComponent(MessagesKeys.GUI_PROFILE_MONITOR_OPEN_TITLE),
                self
                        ? List.of(messages.getComponent(MessagesKeys.GUI_PROFILE_MONITOR_SELF_LORE))
                        : List.of(
                        messages.getComponent(MessagesKeys.GUI_PROFILE_MONITOR_OPEN_LORE_1),
                        messages.getComponent(MessagesKeys.GUI_PROFILE_MONITOR_OPEN_LORE_2))
        ), ctx -> {
            if (ctx.session().viewerId().equals(targetUuid)) {
                ctx.message(messages.getComponent(MessagesKeys.GUI_ERR_CANNOT_MONITOR_SELF));
                return;
            }

            TGPlatform plat = TGPlatform.getInstance();
            TGPlayer freshLocal = plat.getPlayerRepository().getPlayer(targetUuid);
            UUID serverInstanceId;
            String serverName;
            if (freshLocal != null) {
                serverInstanceId = plat.getNetworkPresenceRepository().identity().instanceId();
                serverName = plat.getNetworkPresenceRepository().getLocalServerName();
            } else if (this.remoteEntry != null) {
                serverInstanceId = this.remoteEntry.serverInstanceId();
                serverName = this.remoteEntry.serverName();
            } else {
                ctx.message(messages.getComponent(MessagesKeys.GUI_ERR_MONITOR_BLOCKED));
                return;
            }
            boolean crossServer = !plat.getNetworkPresenceRepository().identity().instanceId().equals(serverInstanceId);

            if (plat.getEventBus().getMonitorOpen().fire(
                    ctx.session().viewerId(), targetUuid, targetName, freshLocal,
                    serverInstanceId, serverName, crossServer, false)) {
                ctx.message(messages.getComponent(MessagesKeys.GUI_ERR_MONITOR_BLOCKED));
                return;
            }

            ctx.open(new PlayerMonitorScreen(targetUuid, targetName));
        });
    }

    private void renderHistoryButton(GuiRenderResult.Builder builder, GuiSession session, UUID targetUuid, String targetName, MessageService messages) {
        if (!session.hasPermission(PlayerHistoryHubScreen.PERMISSION)) return;

        builder.set(SLOT_HISTORY, GuiItems.simple(
                ItemTypes.BOOK,
                messages.getComponent(MessagesKeys.GUI_PROFILE_HISTORY_TITLE),
                List.of(
                        messages.getComponent(MessagesKeys.GUI_PROFILE_HISTORY_LORE_1),
                        messages.getComponent(MessagesKeys.GUI_PROFILE_HISTORY_LORE_2)
                )
        ), ctx -> ctx.open(historyHub(targetUuid, targetName)));
    }

    private void renderModsButton(GuiRenderResult.Builder builder, GuiSession session, UUID targetUuid, String targetName, MessageService messages) {
        Set<DetectedMod> mods = this.detectedMods;
        if (mods == null || mods.isEmpty()) {
            builder.set(SLOT_MODS, GuiItems.simple(
                    ItemTypes.LIME_DYE,
                    messages.getComponent(MessagesKeys.GUI_PROFILE_MODS_CLEAN_TITLE),
                    List.of(messages.getComponent(MessagesKeys.GUI_PROFILE_MODS_CLEAN_LORE))
            ), ctx -> ctx.open(new PlayerSessionModsScreen(targetUuid, targetName)));
            return;
        }

        ModSeverity peak = peakSeverity(mods);
        ItemType icon = switch (peak) {
            case LOG -> ItemTypes.GLOWSTONE_DUST;
            case KICK, KICK_THEN_BAN -> ItemTypes.ORANGE_DYE;
            case BAN -> ItemTypes.REDSTONE;
        };

        List<DetectedMod> sorted = new ArrayList<>(mods);
        sorted.sort(Comparator.comparingInt((DetectedMod m) -> severityWeight(m.severity()))
                .reversed()
                .thenComparing(DetectedMod::id));

        List<Component> lore = new ArrayList<>();
        lore.add(messages.getComponent(MessagesKeys.GUI_PROFILE_MODS_DETECTED_SUMMARY,
                Map.of("tg_count", mods.size())));

        sorted.stream().limit(MOD_LIST_LIMIT).forEach(mod -> lore.add(
                messages.getComponent(MessagesKeys.GUI_PROFILE_MODS_DETECTED_ENTRY, Map.of(
                        "tg_mod_id", mod.id(),
                        "tg_severity", severityLabel(messages, mod.severity())))));

        int hidden = mods.size() - MOD_LIST_LIMIT;
        if (hidden > 0) {
            lore.add(messages.getComponent(MessagesKeys.GUI_PROFILE_MODS_DETECTED_OVERFLOW,
                    Map.of("tg_hidden", hidden)));
        }

        lore.add(Component.empty());
        lore.add(messages.getComponent(MessagesKeys.GUI_PROFILE_MODS_CLICK_HINT));

        builder.set(SLOT_MODS, GuiItems.simple(
                icon,
                messages.getComponent(MessagesKeys.GUI_PROFILE_MODS_DETECTED_TITLE),
                lore
        ), ctx -> ctx.open(new PlayerSessionModsScreen(targetUuid, targetName)));
    }

    private ModSeverity peakSeverity(Set<DetectedMod> mods) {
        ModSeverity peak = ModSeverity.LOG;
        for (DetectedMod mod : mods) {
            if (severityWeight(mod.severity()) > severityWeight(peak)) {
                peak = mod.severity();
            }
        }
        return peak;
    }

    private int severityWeight(ModSeverity severity) {
        return switch (severity) {
            case LOG -> 0;
            case KICK -> 1;
            case KICK_THEN_BAN -> 2;
            case BAN -> 3;
        };
    }

    private String severityLabel(MessageService messages, ModSeverity severity) {
        return messages.getString(switch (severity) {
            case LOG -> MessagesKeys.GUI_MOD_SEVERITY_LOG;
            case KICK -> MessagesKeys.GUI_MOD_SEVERITY_KICK;
            case KICK_THEN_BAN -> MessagesKeys.GUI_MOD_SEVERITY_KICK_THEN_BAN;
            case BAN -> MessagesKeys.GUI_MOD_SEVERITY_BAN;
        });
    }

    private List<Component> buildRemoteHeadLore(RemotePlayerEntry remote, MessageService messages) {
        List<Component> lore = new ArrayList<>();
        lore.add(GuiText.line(messages.getString(MessagesKeys.GUI_PROFILE_HEAD_UUID_LABEL),
                remote.playerUuid().toString()));
        lore.add(GuiText.line("Server", remote.serverName()));
        appendRemoteSessionSummary(lore, messages);
        appendSessionDuration(lore);
        appendFirstJoined(lore, messages);
        return lore;
    }

    private void appendRemoteSessionSummary(List<Component> lore, MessageService messages) {
        SessionSnapshot snap = this.sessionSnapshot;
        lore.add(Component.empty());
        if (snap == null || snap.totalViolations() <= 0 || snap.violations().isEmpty()) {
            lore.add(messages.getComponent(MessagesKeys.GUI_PROFILE_NO_VIOLATIONS));
            return;
        }

        List<Map.Entry<String, Integer>> sorted = snap.violations().entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .toList();

        lore.add(GuiText.line(
                messages.getString(MessagesKeys.GUI_PROFILE_HEAD_VIOLATIONS_LABEL),
                messages.getString(MessagesKeys.GUI_PROFILE_HEAD_VIOLATIONS_SUMMARY, Map.of(
                        "tg_total_vl", snap.totalViolations(),
                        "tg_active_checks", sorted.size()))));

        sorted.stream().limit(VIOLATION_LIST_LIMIT).forEach(entry -> lore.add(
                messages.getComponent(MessagesKeys.GUI_PROFILE_HEAD_VIOLATIONS_ENTRY, Map.of(
                        "tg_check_name", entry.getKey(),
                        "tg_check_vl", entry.getValue()))));

        int hidden = sorted.size() - VIOLATION_LIST_LIMIT;
        if (hidden > 0) {
            lore.add(messages.getComponent(MessagesKeys.GUI_PROFILE_HEAD_VIOLATIONS_OVERFLOW,
                    Map.of("tg_hidden", hidden)));
        }
    }

    private List<Component> buildHeadLore(TGPlayer target, MessageService messages) {
        List<Component> lore = new ArrayList<>();
        lore.add(GuiText.line(messages.getString(MessagesKeys.GUI_PROFILE_HEAD_CLIENT_VERSION_LABEL),
                target.getClientVersion().getReleaseName()));
        lore.add(GuiText.line(messages.getString(MessagesKeys.GUI_PROFILE_HEAD_CLIENT_BRAND_LABEL),
                target.getClientBrand()));

        lore.add(Component.empty());
        lore.add(GuiText.line(messages.getString(MessagesKeys.GUI_PROFILE_HEAD_KEEPALIVE_PING_LABEL),
                target.getPingData().getKeepAlivePing() + " ms"));
        lore.add(GuiText.line(messages.getString(MessagesKeys.GUI_PROFILE_HEAD_TRANSACTION_PING_LABEL),
                target.getPingData().getTransactionPing() + " ms"));

        appendViolationSummary(lore, target, messages);
        appendSessionDuration(lore);
        appendFirstJoined(lore, messages);

        return lore;
    }

    private void appendViolationSummary(List<Component> lore, TGPlayer target, MessageService messages) {
        List<Check> active = target.getCheckManager().allChecks.values().stream()
                .filter(check -> check.getViolations() > 0)
                .sorted(Comparator.comparingInt(Check::getViolations).reversed())
                .toList();

        lore.add(Component.empty());
        if (active.isEmpty()) {
            lore.add(messages.getComponent(MessagesKeys.GUI_PROFILE_NO_VIOLATIONS));
            return;
        }

        int totalVl = active.stream().mapToInt(Check::getViolations).sum();
        lore.add(GuiText.line(
                messages.getString(MessagesKeys.GUI_PROFILE_HEAD_VIOLATIONS_LABEL),
                messages.getString(MessagesKeys.GUI_PROFILE_HEAD_VIOLATIONS_SUMMARY, Map.of(
                        "tg_total_vl", totalVl,
                        "tg_active_checks", active.size()))));

        active.stream().limit(VIOLATION_LIST_LIMIT).forEach(check -> lore.add(
                messages.getComponent(MessagesKeys.GUI_PROFILE_HEAD_VIOLATIONS_ENTRY, Map.of(
                        "tg_check_name", check.getName(),
                        "tg_check_vl", check.getViolations()))));

        int hidden = active.size() - VIOLATION_LIST_LIMIT;
        if (hidden > 0) {
            lore.add(messages.getComponent(MessagesKeys.GUI_PROFILE_HEAD_VIOLATIONS_OVERFLOW,
                    Map.of("tg_hidden", hidden)));
        }
    }

    private void appendSessionDuration(List<Component> lore) {
        SessionSnapshot snap = this.sessionSnapshot;
        if (snap == null) return;
        lore.add(Component.empty());
        lore.add(GuiText.line("Session", formatSessionLength(snap.sessionStart())));
    }

    private void appendFirstJoined(List<Component> lore, MessageService messages) {
        PlayerRecord rec = this.dbRecord;
        if (rec != null) {
            lore.add(Component.empty());
            lore.add(GuiText.line(messages.getString(MessagesKeys.GUI_PROFILE_HEAD_FIRST_JOINED_LABEL),
                    HistoryText.relative(rec.firstSeen()) + " (" + HistoryText.absolute(rec.firstSeen()) + ")"));
            return;
        }

        if (TGPlatform.getInstance().getDatabaseRepository().isConnected() && !dbAttempted) {
            lore.add(Component.empty());
            lore.add(messages.getComponent(MessagesKeys.GUI_PROFILE_FIRST_JOINED_LOADING));
        }
    }
}
