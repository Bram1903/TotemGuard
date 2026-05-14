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

package com.deathmotion.totemguard.common.gui.screen.top;

import com.deathmotion.totemguard.api.check.Check;
import com.deathmotion.totemguard.api.check.CheckType;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.config.key.MessagesKeys;
import com.deathmotion.totemguard.common.features.session.SessionSnapshot;
import com.deathmotion.totemguard.common.features.session.SessionViolationStore;
import com.deathmotion.totemguard.common.gui.*;
import com.deathmotion.totemguard.common.gui.screen.player.PlayerProfileScreen;
import com.deathmotion.totemguard.common.message.MessageService;
import com.deathmotion.totemguard.common.network.NetworkPresenceRepository;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.util.Palette;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.logging.Level;

public final class TopViolatorsScreen extends GuiScreen {

    public static final String PERMISSION = "TotemGuard.Gui.Top";
    static final int LIMIT = 200;
    private static final int PAGE_SIZE = 28;
    private static final int VIOLATION_LINE_LIMIT = 3;
    private static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private final int page;
    private final @Nullable String checkFilter;
    private volatile @Nullable List<SessionSnapshot> snapshots;
    private volatile boolean localOnly;
    private volatile boolean loaded;
    private volatile @Nullable Map<UUID, UserProfile> profiles;

    public TopViolatorsScreen() {
        this(0, null);
    }

    public TopViolatorsScreen(int page) {
        this(page, null);
    }

    public TopViolatorsScreen(int page, @Nullable String checkFilter) {
        this.page = Math.max(0, page);
        this.checkFilter = (checkFilter != null && checkFilter.isBlank()) ? null : checkFilter;
    }

    static List<SessionSnapshot> collectLocalSnapshots(TGPlatform platform, int limit) {
        NetworkPresenceRepository presence = platform.getNetworkPresenceRepository();
        String serverName = presence != null
                ? presence.getLocalServerName()
                : platform.getConfigRepository().configView().server();
        UUID instanceId = presence != null ? presence.identity().instanceId() : new UUID(0L, 0L);
        Instant now = Instant.now();

        List<SessionSnapshot> out = new ArrayList<>();
        for (TGPlayer player : platform.getPlayerRepository().getPlayers()) {
            String name = player.getName();
            if (name == null) continue;

            Map<String, Integer> violations = new LinkedHashMap<>();
            int total = 0;
            for (Check check : player.getCheckManager().allChecks.values()) {
                if (check.getType() == CheckType.MOD) continue;
                int vl = check.getViolations();
                if (vl <= 0) continue;
                violations.put(check.getName(), vl);
                total += vl;
            }
            if (total <= 0) continue;

            out.add(new SessionSnapshot(
                    player.getUuid(), name, serverName, instanceId,
                    player.getSessionStart(), now, total, violations));
        }
        out.sort((a, b) -> Integer.compare(b.totalViolations(), a.totalViolations()));
        return out.size() > limit ? new ArrayList<>(out.subList(0, limit)) : out;
    }

    private static Map<UUID, UserProfile> resolveProfiles(TGPlatform platform, List<SessionSnapshot> data) {
        Map<UUID, UserProfile> out = new HashMap<>();
        NetworkPresenceRepository presence = platform.getNetworkPresenceRepository();
        for (SessionSnapshot snap : data) {
            TGPlayer local = platform.getPlayerRepository().getPlayer(snap.playerUuid());
            if (local != null) {
                out.put(snap.playerUuid(), local.getUser().getProfile());
                continue;
            }
            if (presence == null) continue;
            try {
                UserProfile profile = presence.loadProfile(snap.playerUuid());
                if (profile != null) out.put(snap.playerUuid(), profile);
            } catch (Exception ignored) {
            }
        }
        return out;
    }

    @Override
    public String requiredPermission() {
        return PERMISSION;
    }

    @Override
    public void onOpen(GuiSession session) {
        TGPlatform platform = TGPlatform.getInstance();
        SessionViolationStore store = platform.getSessionViolationStore();
        boolean redisAvailable = store != null && platform.getRedisRepository().isConnected();
        this.localOnly = !redisAvailable;

        platform.getScheduler().runAsyncTask(() -> {
            try {
                List<SessionSnapshot> data = redisAvailable
                        ? store.topN(LIMIT)
                        : collectLocalSnapshots(platform, LIMIT);
                Map<UUID, UserProfile> resolved = resolveProfiles(platform, data);
                this.snapshots = data;
                this.profiles = resolved;
            } catch (Exception ex) {
                platform.getLogger().log(Level.WARNING,
                        "Failed to load top violators: " + ex.getMessage());
                this.snapshots = List.of();
            } finally {
                this.loaded = true;
                platform.getGuiManager().refresh(session.viewerId());
            }
        });
    }

    @Override
    public GuiRenderResult render(GuiSession session) {
        TGPlatform platform = TGPlatform.getInstance();
        MessageService messages = platform.getMessageService();

        GuiRenderResult.Builder builder = GuiRenderResult.builder(6, GuiTitle.of(buildTitle(messages)));
        builder.fillEmpty(GuiItems.filler());

        if (session.hasParent()) {
            builder.set(0, GuiItems.simple(
                    ItemTypes.ARROW,
                    messages.getComponent(MessagesKeys.GUI_BTN_BACK_TITLE),
                    List.of(messages.getComponent(MessagesKeys.GUI_BTN_BACK_LORE))
            ), ctx -> ctx.back());
        } else {
            builder.set(0, GuiItems.simple(
                    ItemTypes.BARRIER,
                    messages.getComponent(MessagesKeys.GUI_BTN_CLOSE_TITLE),
                    List.of(messages.getComponent(MessagesKeys.GUI_BTN_CLOSE_LORE))
            ), ctx -> ctx.close());
        }

        if (localOnly) {
            builder.set(4, GuiItems.simple(
                    ItemTypes.AMETHYST_SHARD,
                    messages.getComponent(MessagesKeys.GUI_TOP_LOCAL_ONLY_TITLE),
                    List.of(
                            messages.getComponent(MessagesKeys.GUI_TOP_LOCAL_ONLY_LORE_1),
                            messages.getComponent(MessagesKeys.GUI_TOP_LOCAL_ONLY_LORE_2)
                    )
            ));
        }

        if (!loaded || snapshots == null) {
            builder.set(22, GuiItems.simple(
                    ItemTypes.CLOCK,
                    messages.getComponent(MessagesKeys.GUI_LOADING_GENERIC),
                    List.of(messages.getComponent(MessagesKeys.GUI_TOP_LOADING_LORE))
            ));
            renderFilterButtons(builder, messages);
            return builder.build();
        }

        List<SessionSnapshot> visible = applyFilter(this.snapshots);

        if (visible.isEmpty()) {
            Component emptyTitle = checkFilter == null
                    ? messages.getComponent(MessagesKeys.GUI_TOP_EMPTY_TITLE)
                    : messages.getComponent(MessagesKeys.GUI_TOP_EMPTY_FILTER_TITLE,
                    Map.of("tg_check", checkFilter));
            Component emptyLore = checkFilter == null
                    ? messages.getComponent(MessagesKeys.GUI_TOP_EMPTY_LORE)
                    : messages.getComponent(MessagesKeys.GUI_TOP_EMPTY_FILTER_LORE,
                    Map.of("tg_check", checkFilter));
            builder.set(22, GuiItems.simple(
                    ItemTypes.LIME_CONCRETE,
                    emptyTitle,
                    List.of(emptyLore)
            ));
            renderFilterButtons(builder, messages);
            return builder.build();
        }

        int totalPages = Math.max(1, (visible.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int safePage = Math.min(page, totalPages - 1);
        int from = safePage * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, visible.size());

        for (int i = from; i < to; i++) {
            SessionSnapshot snap = visible.get(i);
            int rank = i + 1;
            UserProfile profile = profiles != null ? profiles.get(snap.playerUuid()) : null;
            if (profile == null) profile = new UserProfile(snap.playerUuid(), snap.playerName());

            builder.set(CONTENT_SLOTS[i - from],
                    GuiItems.playerHead(profile, buildHeadName(rank, snap), buildHeadLore(snap, messages)),
                    ctx -> handleClick(ctx, snap));
        }

        renderFooter(builder, safePage, totalPages, visible.size(), messages);
        renderFilterButtons(builder, messages);

        return builder.build();
    }

    private List<SessionSnapshot> applyFilter(List<SessionSnapshot> all) {
        if (checkFilter == null) return all;
        List<SessionSnapshot> filtered = new ArrayList<>(all.size());
        for (SessionSnapshot snap : all) {
            if (snap.violations().getOrDefault(checkFilter, 0) > 0) filtered.add(snap);
        }
        filtered.sort((a, b) -> Integer.compare(
                b.violations().getOrDefault(checkFilter, 0),
                a.violations().getOrDefault(checkFilter, 0)));
        return filtered;
    }

    private String buildTitle(MessageService messages) {
        if (checkFilter == null) return messages.getString(MessagesKeys.GUI_TOP_TITLE);
        String shown = checkFilter.length() > 16 ? checkFilter.substring(0, 15) + "…" : checkFilter;
        return messages.getString(MessagesKeys.GUI_TOP_TITLE_FILTERED, Map.of("tg_check", shown));
    }

    private void handleClick(GuiClickContext ctx, SessionSnapshot snap) {
        WrapperPlayClientClickWindow.WindowClickType type = ctx.packet().getWindowClickType();
        if (type != WrapperPlayClientClickWindow.WindowClickType.PICKUP) return;

        int button = ctx.packet().getButton();
        if (button == 1) {
            triggerTeleport(ctx, snap);
            return;
        }
        ctx.open(new PlayerProfileScreen(snap.playerUuid(), snap.playerName()));
    }

    private void triggerTeleport(GuiClickContext ctx, SessionSnapshot snap) {
        TGPlatform platform = TGPlatform.getInstance();
        MessageService messages = platform.getMessageService();
        if (snap.playerUuid().equals(ctx.session().viewerId())) {
            ctx.message(messages.getComponent(MessagesKeys.GUI_TOP_ERR_SELF_TELEPORT));
            return;
        }
        boolean dispatched = platform.getTeleportService().teleport(ctx.session().viewerId(), snap.playerName());
        if (!dispatched) {
            ctx.message(messages.getComponent(MessagesKeys.GUI_TOP_ERR_TELEPORT_UNAVAILABLE));
            return;
        }
        ctx.close();
    }

    private void renderFooter(GuiRenderResult.Builder builder, int currentPage, int totalPages, int total, MessageService messages) {
        if (currentPage > 0) {
            builder.set(48, GuiItems.simple(
                    ItemTypes.ARROW,
                    messages.getComponent(MessagesKeys.GUI_BTN_PREVIOUS_PAGE_TITLE),
                    List.of(messages.getComponent(MessagesKeys.GUI_TOP_PAGE_NUMBER,
                            Map.of("tg_page", currentPage)))
            ), ctx -> ctx.replace(new TopViolatorsScreen(currentPage - 1, checkFilter)));
        }

        List<Component> footerLore = new ArrayList<>();
        String tracked = messages.getString(checkFilter == null
                ? MessagesKeys.GUI_TOP_FOOTER_TRACKED_LABEL
                : MessagesKeys.GUI_TOP_FOOTER_MATCHING_LABEL);
        footerLore.add(GuiText.line(tracked, String.valueOf(total)));
        if (checkFilter != null) {
            footerLore.add(GuiText.line(messages.getString(MessagesKeys.GUI_TOP_FOOTER_FILTER_LABEL), checkFilter));
        }
        footerLore.add(Component.empty());
        footerLore.add(messages.getComponent(MessagesKeys.GUI_TOP_FOOTER_HINT_LEFT_CLICK));
        footerLore.add(messages.getComponent(MessagesKeys.GUI_TOP_FOOTER_HINT_RIGHT_CLICK));

        builder.set(49, GuiItems.simple(
                ItemTypes.PAPER,
                messages.getComponent(MessagesKeys.GUI_TOP_PAGE_SUMMARY, Map.of(
                        "tg_current", currentPage + 1,
                        "tg_total", totalPages)),
                footerLore
        ));

        if (currentPage < totalPages - 1) {
            builder.set(50, GuiItems.simple(
                    ItemTypes.ARROW,
                    messages.getComponent(MessagesKeys.GUI_BTN_NEXT_PAGE_TITLE),
                    List.of(messages.getComponent(MessagesKeys.GUI_TOP_PAGE_NUMBER,
                            Map.of("tg_page", currentPage + 2)))
            ), ctx -> ctx.replace(new TopViolatorsScreen(currentPage + 1, checkFilter)));
        }
    }

    private void renderFilterButtons(GuiRenderResult.Builder builder, MessageService messages) {
        if (checkFilter == null) {
            builder.set(53, GuiItems.simple(
                    ItemTypes.HOPPER,
                    messages.getComponent(MessagesKeys.GUI_TOP_FILTER_PICK_TITLE),
                    List.of(
                            messages.getComponent(MessagesKeys.GUI_TOP_FILTER_PICK_LORE_1),
                            Component.empty(),
                            messages.getComponent(MessagesKeys.GUI_TOP_FILTER_PICK_LORE_2)
                    )
            ), ctx -> ctx.open(new TopCheckPickerScreen(0), GuiSounds.FILTER));
            return;
        }

        builder.set(52, GuiItems.simple(
                ItemTypes.HOPPER,
                messages.getComponent(MessagesKeys.GUI_TOP_FILTER_CHANGE_TITLE),
                List.of(messages.getComponent(MessagesKeys.GUI_TOP_FILTER_CHANGE_LORE))
        ), ctx -> ctx.replace(new TopCheckPickerScreen(0)));

        builder.set(53, GuiItems.simple(
                ItemTypes.BARRIER,
                messages.getComponent(MessagesKeys.GUI_TOP_FILTER_CLEAR_TITLE),
                List.of(messages.getComponent(MessagesKeys.GUI_TOP_FILTER_CLEAR_LORE))
        ), ctx -> ctx.replace(new TopViolatorsScreen(0, null)));
    }

    private Component buildHeadName(int rank, SessionSnapshot snap) {
        int displayed = checkFilter == null
                ? snap.totalViolations()
                : snap.violations().getOrDefault(checkFilter, 0);
        return Component.text("#" + rank + " ", Palette.CAPTION)
                .append(Component.text(snap.playerName(), Palette.SUCCESS))
                .append(Component.text("  ·  ", Palette.SEPARATOR))
                .append(Component.text(displayed + " VL", Palette.VALUE));
    }

    private List<Component> buildHeadLore(SessionSnapshot snap, MessageService messages) {
        List<Component> lore = new ArrayList<>();
        lore.add(GuiText.line(messages.getString(MessagesKeys.GUI_TOP_HEAD_SERVER_LABEL), snap.serverName()));
        lore.add(GuiText.line(messages.getString(MessagesKeys.GUI_TOP_HEAD_SESSION_LABEL),
                formatSessionLength(snap.sessionStart())));

        if (checkFilter != null) {
            lore.add(Component.empty());
            lore.add(GuiText.line(
                    messages.getString(MessagesKeys.GUI_TOP_HEAD_FILTER_VL_LABEL, Map.of("tg_check", checkFilter)),
                    String.valueOf(snap.violations().getOrDefault(checkFilter, 0))));
            lore.add(GuiText.line(messages.getString(MessagesKeys.GUI_TOP_HEAD_TOTAL_VL_LABEL),
                    String.valueOf(snap.totalViolations())));
        } else {
            List<Map.Entry<String, Integer>> sorted = snap.violations().entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .toList();
            if (!sorted.isEmpty()) {
                lore.add(Component.empty());
                lore.add(messages.getComponent(MessagesKeys.GUI_TOP_HEAD_TOP_CHECKS_LABEL));
                sorted.stream().limit(VIOLATION_LINE_LIMIT).forEach(entry ->
                        lore.add(messages.getComponent(MessagesKeys.GUI_TOP_HEAD_TOP_CHECKS_ENTRY, Map.of(
                                "tg_check", entry.getKey(),
                                "tg_check_vl", entry.getValue()))));
                int hidden = sorted.size() - VIOLATION_LINE_LIMIT;
                if (hidden > 0) {
                    lore.add(messages.getComponent(MessagesKeys.GUI_TOP_HEAD_TOP_CHECKS_OVERFLOW,
                            Map.of("tg_hidden", hidden)));
                }
            }
        }

        lore.add(Component.empty());
        lore.add(messages.getComponent(MessagesKeys.GUI_TOP_HEAD_LEFT_CLICK_ACTION));
        lore.add(messages.getComponent(MessagesKeys.GUI_TOP_HEAD_RIGHT_CLICK_ACTION));
        return lore;
    }

    private String formatSessionLength(Instant start) {
        Duration d = Duration.between(start, Instant.now());
        if (d.isNegative() || d.isZero()) return "just started";
        long hours = d.toHours();
        long minutes = d.toMinutesPart();
        long seconds = d.toSecondsPart();
        if (hours > 0) return hours + "h " + minutes + "m";
        if (minutes > 0) return minutes + "m " + seconds + "s";
        return seconds + "s";
    }
}
