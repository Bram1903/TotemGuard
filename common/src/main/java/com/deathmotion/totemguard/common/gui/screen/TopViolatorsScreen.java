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

package com.deathmotion.totemguard.common.gui.screen;

import com.deathmotion.totemguard.api.check.Check;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.config.key.MessagesKeys;
import com.deathmotion.totemguard.common.features.session.SessionSnapshot;
import com.deathmotion.totemguard.common.features.session.SessionViolationStore;
import com.deathmotion.totemguard.common.gui.*;
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

        GuiRenderResult.Builder builder = GuiRenderResult.builder(6, GuiTitle.of(buildTitle()));
        builder.fillEmpty(GuiItems.filler());

        builder.set(0, GuiItems.simple(
                ItemTypes.ARROW,
                messages.getComponent(MessagesKeys.GUI_BTN_BACK_TITLE),
                List.of(messages.getComponent(MessagesKeys.GUI_BTN_BACK_LORE))
        ), session.hasParent() ? ctx -> ctx.back() : ctx -> ctx.close());

        if (localOnly) {
            builder.set(4, GuiItems.simple(
                    ItemTypes.AMETHYST_SHARD,
                    Component.text("Local server only", Palette.WARN),
                    List.of(
                            Component.text("Redis isn't connected. Showing violators", Palette.CONNECTIVE),
                            Component.text("from this server only.", Palette.CONNECTIVE)
                    )
            ));
        }

        if (!loaded || snapshots == null) {
            builder.set(22, GuiItems.simple(
                    ItemTypes.CLOCK,
                    messages.getComponent(MessagesKeys.GUI_LOADING_GENERIC),
                    List.of(Component.text("Aggregating violations from Redis…", Palette.CONNECTIVE))
            ));
            renderFilterButtons(builder);
            return builder.build();
        }

        List<SessionSnapshot> visible = applyFilter(this.snapshots);

        if (visible.isEmpty()) {
            builder.set(22, GuiItems.simple(
                    ItemTypes.LIME_CONCRETE,
                    Component.text(checkFilter == null ? "No violators" : "No violators for " + checkFilter,
                            Palette.SUCCESS),
                    List.of(Component.text(checkFilter == null
                            ? "No online player has flagged this session."
                            : "Nobody has flagged " + checkFilter + " this session.", Palette.CONNECTIVE))
            ));
            renderFilterButtons(builder);
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
                    GuiItems.playerHead(profile, buildHeadName(rank, snap), buildHeadLore(snap)),
                    ctx -> handleClick(ctx, snap));
        }

        renderFooter(builder, safePage, totalPages, visible.size());
        renderFilterButtons(builder);

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

    private String buildTitle() {
        if (checkFilter == null) return "TotemGuard · Top Violators";
        String shown = checkFilter.length() > 16 ? checkFilter.substring(0, 15) + "…" : checkFilter;
        return "TotemGuard · Top [" + shown + "]";
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
        if (snap.playerUuid().equals(ctx.session().viewerId())) {
            ctx.message(Component.text("You can't teleport to yourself.", Palette.DANGER));
            return;
        }
        boolean dispatched = platform.getTeleportService().teleport(ctx.session().viewerId(), snap.playerName());
        if (!dispatched) {
            ctx.message(Component.text("Teleport unavailable: you're no longer online here.", Palette.DANGER));
            return;
        }
        ctx.close();
    }

    private void renderFooter(GuiRenderResult.Builder builder, int currentPage, int totalPages, int total) {
        MessageService messages = TGPlatform.getInstance().getMessageService();

        if (currentPage > 0) {
            builder.set(48, GuiItems.simple(
                    ItemTypes.ARROW,
                    messages.getComponent(MessagesKeys.GUI_BTN_PREVIOUS_PAGE_TITLE),
                    List.of(Component.text("Page " + currentPage, Palette.CONNECTIVE))
            ), ctx -> ctx.replace(new TopViolatorsScreen(currentPage - 1, checkFilter)));
        }

        List<Component> footerLore = new ArrayList<>();
        footerLore.add(GuiText.line(checkFilter == null ? "Tracked violators" : "Matching violators",
                String.valueOf(total)));
        if (checkFilter != null) footerLore.add(GuiText.line("Check filter", checkFilter));
        footerLore.add(Component.empty());
        footerLore.add(Component.text("Left-click a head to open profile", Palette.CONNECTIVE));
        footerLore.add(Component.text("Right-click to teleport", Palette.CONNECTIVE));

        builder.set(49, GuiItems.simple(
                ItemTypes.PAPER,
                Component.text("Page " + (currentPage + 1) + " of " + totalPages, Palette.BRAND),
                footerLore
        ));

        if (currentPage < totalPages - 1) {
            builder.set(50, GuiItems.simple(
                    ItemTypes.ARROW,
                    messages.getComponent(MessagesKeys.GUI_BTN_NEXT_PAGE_TITLE),
                    List.of(Component.text("Page " + (currentPage + 2), Palette.CONNECTIVE))
            ), ctx -> ctx.replace(new TopViolatorsScreen(currentPage + 1, checkFilter)));
        }
    }

    private void renderFilterButtons(GuiRenderResult.Builder builder) {
        if (checkFilter == null) {
            builder.set(53, GuiItems.simple(
                    ItemTypes.HOPPER,
                    Component.text("Filter by check", Palette.BRAND),
                    List.of(
                            Component.text("Restrict the ranking to one check.", Palette.CONNECTIVE),
                            Component.empty(),
                            Component.text("Click ▶ pick a check", Palette.CAPTION)
                    )
            ), ctx -> ctx.open(new TopCheckPickerScreen(0)));
            return;
        }

        builder.set(52, GuiItems.simple(
                ItemTypes.HOPPER,
                Component.text("Change check", Palette.BRAND),
                List.of(Component.text("Pick a different check to filter by.", Palette.CONNECTIVE))
        ), ctx -> ctx.replace(new TopCheckPickerScreen(0)));

        builder.set(53, GuiItems.simple(
                ItemTypes.BARRIER,
                Component.text("Clear filter", Palette.DANGER),
                List.of(Component.text("Show every active violator again.", Palette.CONNECTIVE))
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

    private List<Component> buildHeadLore(SessionSnapshot snap) {
        List<Component> lore = new ArrayList<>();
        lore.add(GuiText.line("Server", snap.serverName()));
        lore.add(GuiText.line("Session", formatSessionLength(snap.sessionStart())));

        if (checkFilter != null) {
            lore.add(Component.empty());
            lore.add(GuiText.line(checkFilter + " VL",
                    String.valueOf(snap.violations().getOrDefault(checkFilter, 0))));
            lore.add(GuiText.line("Total VL across all checks", String.valueOf(snap.totalViolations())));
        } else {
            List<Map.Entry<String, Integer>> sorted = snap.violations().entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .toList();
            if (!sorted.isEmpty()) {
                lore.add(Component.empty());
                lore.add(Component.text("Top checks", Palette.LABEL));
                sorted.stream().limit(VIOLATION_LINE_LIMIT).forEach(entry ->
                        lore.add(Component.text("  " + entry.getKey() + " · VL " + entry.getValue(), Palette.CONNECTIVE)));
                int hidden = sorted.size() - VIOLATION_LINE_LIMIT;
                if (hidden > 0) {
                    lore.add(Component.text("  + " + hidden + " more check(s)", Palette.CAPTION));
                }
            }
        }

        lore.add(Component.empty());
        lore.add(Component.text("Left-click ▶ open profile", Palette.CAPTION));
        lore.add(Component.text("Right-click ▶ teleport", Palette.CAPTION));
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
