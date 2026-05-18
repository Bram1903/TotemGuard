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

package com.deathmotion.totemguard.loader.paper;

import com.deathmotion.totemguard.api.fleet.FleetCache;
import com.deathmotion.totemguard.loader.core.LoaderCore;
import com.deathmotion.totemguard.loader.core.LoaderManifest;
import com.deathmotion.totemguard.loader.core.PluginVersionGate;
import com.deathmotion.totemguard.loader.fleet.RolloutCoordinator;
import com.deathmotion.totemguard.loader.runtime.PluginRuntime;
import com.deathmotion.totemguard.loader.source.GithubSearch;
import com.deathmotion.totemguard.loader.source.SearchMatch;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.logging.Level;

public final class PaperTgLoaderCommand implements TabExecutor {

    private static final List<String> TOP_LEVEL = List.of(
            "status", "peers", "versions", "search", "import",
            "plugin", "load", "stage", "apply", "rollout");
    private static final List<String> PLUGIN_SUBS = List.of("start", "stop", "restart");
    private static final List<String> ROLLOUT_SUBS = List.of("stage", "apply", "deploy", "cancel", "status");
    private static final List<String> CHANNELS = List.of("LATEST", "EXPERIMENTAL", "GIT");

    private static final Duration DEPLOY_SETTLE = Duration.ofSeconds(5);

    private final TGLoaderPaper plugin;

    public PaperTgLoaderCommand(TGLoaderPaper plugin) {
        this.plugin = plugin;
    }

    private static Component prefixed() {
        return LoaderPalette.PREFIX;
    }

    private static Component line(Object... colorText) {
        Component out = prefixed();
        for (int i = 0; i + 1 < colorText.length; i += 2) {
            TextColor color = (TextColor) colorText[i];
            String text = String.valueOf(colorText[i + 1]);
            out = out.append(Component.text(text, color));
        }
        return out;
    }

    private static String shortSha(String sha) {
        if (sha == null || sha.length() < 10) return sha == null ? "" : sha;
        return sha.substring(0, 10) + "...";
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NonNull [] args) {
        String sub = args.length == 0 ? "status" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "status" -> handleStatus(sender);
            case "peers" -> handlePeers(sender);
            case "versions" -> handleVersions(sender);
            case "search" -> handleSearch(sender, args);
            case "import" -> handleImport(sender);
            case "plugin" -> handlePluginGroup(sender, args);
            case "load" -> handleLoad(sender, args);
            case "stage" -> handleStage(sender, args);
            case "apply" -> handleApply(sender);
            case "rollout" -> handleRolloutGroup(sender, args);
            default -> sendUsage(sender);
        }
        return true;
    }

    // ---------- top-level: status / peers / versions / search / import ----------

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(line(LoaderPalette.DANGER, "Usage: ",
                LoaderPalette.VALUE, "/tgloader <status|peers|versions|search|import|plugin|load|stage|apply|rollout>"));
    }

    private void handleStatus(CommandSender sender) {
        PluginRuntime runtime = plugin.runtime();
        String pluginVersion = runtime == null ? null : runtime.loadedVersion();
        sender.sendMessage(line(LoaderPalette.LABEL, "Loader ",
                LoaderPalette.VALUE, LoaderManifest.loaderVersion()));
        if (pluginVersion == null) {
            sender.sendMessage(line(LoaderPalette.LABEL, "TotemGuard ",
                    LoaderPalette.DANGER, "NOT LOADED"));
            sender.sendMessage(line(LoaderPalette.CAPTION, "Use ",
                    LoaderPalette.VALUE, "/tgloader load <version>",
                    LoaderPalette.CAPTION, " to load a build."));
        } else {
            sender.sendMessage(line(LoaderPalette.LABEL, "TotemGuard ",
                    LoaderPalette.VALUE, pluginVersion));
        }
        plugin.core().readStaged().ifPresent(staged -> sender.sendMessage(line(
                LoaderPalette.LABEL, "Staged ",
                LoaderPalette.VALUE, staged.version(),
                LoaderPalette.CAPTION, "  (" + shortSha(staged.sha256()) + ")")));
        RolloutCoordinator coord = plugin.core().rolloutCoordinator();
        if (coord != null) {
            coord.active().ifPresent(roll -> sender.sendMessage(line(
                    LoaderPalette.LABEL, "Rollout ",
                    LoaderPalette.VALUE, roll.opId().toString().substring(0, 8),
                    LoaderPalette.CAPTION, "  -> " + roll.targetVersion() + "  [" + roll.phase() + "]")));
        }
    }

    private void handlePeers(CommandSender sender) {
        FleetCache cache = plugin.core().fleetCacheRef().available().orElse(null);
        if (cache == null) {
            sender.sendMessage(line(LoaderPalette.DANGER,
                    "Fleet cache not attached. Peers are only visible after TotemGuard connects to Redis."));
            return;
        }
        List<String> keys = cache.scanKeys("totemguard:loader:catalog:", 256);
        if (keys.isEmpty()) {
            sender.sendMessage(line(LoaderPalette.CONNECTIVE,
                    "No fleet catalog entries (yet). Heartbeats publish every 5m."));
            return;
        }
        Map<String, Set<String>> byVersion = new LinkedHashMap<>();
        for (String key : keys) {
            Map<String, String> hash = cache.getHash(key);
            if (hash.isEmpty()) continue;
            String version = hash.getOrDefault("version", "?");
            String addedBy = hash.getOrDefault("addedBy", "?");
            byVersion.computeIfAbsent(version, k -> new LinkedHashSet<>()).add(addedBy);
        }
        sender.sendMessage(line(LoaderPalette.LABEL, "Fleet membership ",
                LoaderPalette.CAPTION, "(" + keys.size() + " entries)"));
        for (Map.Entry<String, Set<String>> e : byVersion.entrySet()) {
            Set<String> hosts = e.getValue();
            sender.sendMessage(line(LoaderPalette.CAPTION, " - ",
                    LoaderPalette.VALUE, e.getKey(),
                    LoaderPalette.CAPTION, "  " + hosts.size() + " host" + (hosts.size() == 1 ? "" : "s")));
        }
    }

    private void handleVersions(CommandSender sender) {
        List<String> cached = plugin.core().listCachedVersions();
        if (cached.isEmpty()) {
            sender.sendMessage(line(LoaderPalette.CONNECTIVE,
                    "No cached versions on disk. Use ",
                    LoaderPalette.VALUE, "/tgloader load <version>",
                    LoaderPalette.CONNECTIVE, " to fetch one."));
            return;
        }
        PluginRuntime runtime = plugin.runtime();
        String running = runtime == null ? null : runtime.loadedVersion();
        sender.sendMessage(line(LoaderPalette.LABEL, "Cached versions ",
                LoaderPalette.CAPTION, "(" + cached.size() + ")"));
        for (String version : cached) {
            Component entry = Component.text()
                    .append(prefixed())
                    .append(Component.text(" - ", LoaderPalette.CAPTION))
                    .append(Component.text(version, LoaderPalette.VALUE))
                    .append(version.equals(running)
                            ? Component.text(" (running)", LoaderPalette.SUCCESS)
                            : Component.empty())
                    .build();
            sender.sendMessage(entry);
        }
        sender.sendMessage(line(LoaderPalette.CAPTION, "Channels: ",
                LoaderPalette.CONNECTIVE, "LATEST, EXPERIMENTAL, GIT"));
    }

    private void handleSearch(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(line(LoaderPalette.DANGER, "Usage: ",
                    LoaderPalette.VALUE, "/tgloader search <commit-or-version>"));
            return;
        }
        String query = args[1].trim();
        sender.sendMessage(line(LoaderPalette.LABEL, "Searching GitHub for ",
                LoaderPalette.VALUE, query, LoaderPalette.CONNECTIVE, "..."));
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                List<SearchMatch> matches = GithubSearch.search(query, plugin.core().platform(),
                        plugin.core().fleetCacheRef());
                Bukkit.getScheduler().runTask(plugin, () -> renderSearchResults(sender, query, matches));
            } catch (Throwable t) {
                plugin.getLogger().log(Level.SEVERE, "TotemGuard search " + query + " failed.", t);
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(line(
                        LoaderPalette.DANGER, "Search failed: ",
                        LoaderPalette.DANGER_SOFT, String.valueOf(t.getMessage()))));
            }
        });
    }

    private void renderSearchResults(CommandSender sender, String query, List<SearchMatch> matches) {
        if (matches.isEmpty()) {
            sender.sendMessage(line(LoaderPalette.DANGER, "No releases matched ",
                    LoaderPalette.DANGER_SOFT, query, LoaderPalette.DANGER, "."));
            return;
        }
        sender.sendMessage(line(LoaderPalette.LABEL, "Matches ",
                LoaderPalette.CAPTION, "(" + matches.size() + ")"));
        int shown = 0;
        for (SearchMatch match : matches) {
            if (shown++ >= 10) {
                sender.sendMessage(line(LoaderPalette.CAPTION,
                        "... and " + (matches.size() - 10) + " more. Narrow the query."));
                break;
            }
            sender.sendMessage(line(LoaderPalette.CAPTION, " - ",
                    LoaderPalette.VALUE, match.version(),
                    LoaderPalette.CAPTION, "  ",
                    LoaderPalette.CONNECTIVE, match.matchReason()));
        }
        sender.sendMessage(line(LoaderPalette.CAPTION, "Install with ",
                LoaderPalette.VALUE, "/tgloader load <version>"));
    }

    // ---------- /tgloader plugin <start|stop|restart> (inner plugin lifecycle) ----------

    private void handleImport(CommandSender sender) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                int imported = plugin.core().importLocal();
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (imported == 0) {
                        sender.sendMessage(line(LoaderPalette.CONNECTIVE, "Nothing to import. Drop jars in ",
                                LoaderPalette.VALUE, "loader/local/",
                                LoaderPalette.CONNECTIVE, " and try again."));
                    } else {
                        sender.sendMessage(line(LoaderPalette.SUCCESS, "Imported ",
                                LoaderPalette.VALUE, String.valueOf(imported),
                                LoaderPalette.SUCCESS, " jar" + (imported == 1 ? "" : "s") + " into the version catalog."));
                    }
                });
            } catch (Throwable t) {
                plugin.getLogger().log(Level.SEVERE, "Local import failed.", t);
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(line(
                        LoaderPalette.DANGER, "Import failed: ",
                        LoaderPalette.DANGER_SOFT, String.valueOf(t.getMessage()))));
            }
        });
    }

    private void handlePluginGroup(CommandSender sender, String[] args) {
        String sub = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "";
        switch (sub) {
            case "start" -> handlePluginStart(sender);
            case "stop" -> handlePluginStop(sender);
            case "restart" -> handlePluginRestart(sender);
            default -> sender.sendMessage(line(LoaderPalette.DANGER, "Usage: ",
                    LoaderPalette.VALUE, "/tgloader plugin <start|stop|restart>"));
        }
    }

    private void handlePluginStart(CommandSender sender) {
        PluginRuntime runtime = plugin.runtime();
        if (runtime != null && runtime.isLoaded()) {
            sender.sendMessage(line(LoaderPalette.DANGER, "TotemGuard already running ",
                    LoaderPalette.CONNECTIVE, "(", LoaderPalette.VALUE, runtime.loadedVersion(),
                    LoaderPalette.CONNECTIVE, "). Use ",
                    LoaderPalette.VALUE, "/tgloader plugin restart"));
            return;
        }
        sender.sendMessage(line(LoaderPalette.LABEL, "Starting TotemGuard",
                LoaderPalette.CONNECTIVE, "..."));
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                plugin.attemptStart(null);
                PluginRuntime r = plugin.runtime();
                String version = r == null ? null : r.loadedVersion();
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(line(
                        LoaderPalette.SUCCESS, "TotemGuard started. Running ",
                        LoaderPalette.VALUE, version == null ? "unknown" : version)));
            } catch (Throwable t) {
                plugin.getLogger().log(Level.SEVERE, "TotemGuard plugin start failed.", t);
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(line(
                        LoaderPalette.DANGER, "Start failed: ",
                        LoaderPalette.DANGER_SOFT, String.valueOf(t.getMessage()))));
            }
        });
    }

    private void handlePluginStop(CommandSender sender) {
        PluginRuntime runtime = plugin.runtime();
        if (runtime == null || !runtime.isLoaded()) {
            sender.sendMessage(line(LoaderPalette.DANGER, "TotemGuard is not currently loaded."));
            return;
        }
        sender.sendMessage(line(LoaderPalette.LABEL, "Stopping TotemGuard",
                LoaderPalette.CONNECTIVE, "..."));
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                runtime.stopForCommand();
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(line(
                        LoaderPalette.SUCCESS, "TotemGuard stopped. Run ",
                        LoaderPalette.VALUE, "/tgloader plugin start",
                        LoaderPalette.SUCCESS, " to bring it back.")));
            } catch (Throwable t) {
                plugin.getLogger().log(Level.SEVERE, "TotemGuard plugin stop failed.", t);
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(line(
                        LoaderPalette.DANGER, "Stop failed: ",
                        LoaderPalette.DANGER_SOFT, String.valueOf(t.getMessage()))));
            }
        });
    }

    // ---------- /tgloader load|stage|apply (local install on this host only) ----------

    private void handlePluginRestart(CommandSender sender) {
        PluginRuntime runtime = plugin.runtime();
        if (runtime == null) {
            sender.sendMessage(line(LoaderPalette.DANGER,
                    "TotemGuard is not loaded. Use ", LoaderPalette.VALUE, "/tgloader plugin start"));
            return;
        }
        sender.sendMessage(line(LoaderPalette.LABEL, "Restarting TotemGuard",
                LoaderPalette.CONNECTIVE, "..."));
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                runtime.restart();
                String version = runtime.loadedVersion();
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(line(
                        LoaderPalette.SUCCESS, "TotemGuard restarted. Running ",
                        LoaderPalette.VALUE, version == null ? "unknown" : version)));
            } catch (Throwable t) {
                plugin.getLogger().log(Level.SEVERE, "TotemGuard plugin restart failed.", t);
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(line(
                        LoaderPalette.DANGER, "Restart failed: ",
                        LoaderPalette.DANGER_SOFT, String.valueOf(t.getMessage()))));
            }
        });
    }

    private void handleLoad(CommandSender sender, String[] args) {
        if (args.length < 2 || args[1].trim().isEmpty()) {
            sender.sendMessage(line(LoaderPalette.DANGER, "Usage: ",
                    LoaderPalette.VALUE, "/tgloader load <version>"));
            return;
        }
        String requested = args[1].trim();
        try {
            PluginVersionGate.rejectIfPinnedTooOld(requested, "/tgloader load");
        } catch (IOException ex) {
            sender.sendMessage(line(LoaderPalette.DANGER, ex.getMessage()));
            return;
        }
        sender.sendMessage(line(LoaderPalette.LABEL, "Loading ",
                LoaderPalette.VALUE, "TotemGuard " + requested,
                LoaderPalette.CONNECTIVE, "..."));
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                plugin.attemptStart(requested);
                PluginRuntime runtime = plugin.runtime();
                String version = runtime == null ? null : runtime.loadedVersion();
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(line(
                        LoaderPalette.SUCCESS, "TotemGuard ",
                        LoaderPalette.VALUE, version == null ? requested : version,
                        LoaderPalette.SUCCESS, " is now running.")));
            } catch (Throwable t) {
                plugin.getLogger().log(Level.SEVERE, "TotemGuard load " + requested + " failed.", t);
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(line(
                        LoaderPalette.DANGER, "Load failed: ",
                        LoaderPalette.DANGER_SOFT, String.valueOf(t.getMessage()))));
            }
        });
    }

    private void handleStage(CommandSender sender, String[] args) {
        if (args.length < 2 || args[1].trim().isEmpty()) {
            sender.sendMessage(line(LoaderPalette.DANGER, "Usage: ",
                    LoaderPalette.VALUE, "/tgloader stage <version>"));
            return;
        }
        String requested = args[1].trim();
        try {
            PluginVersionGate.rejectIfPinnedTooOld(requested, "/tgloader stage");
        } catch (IOException ex) {
            sender.sendMessage(line(LoaderPalette.DANGER, ex.getMessage()));
            return;
        }
        sender.sendMessage(line(LoaderPalette.LABEL, "Staging ",
                LoaderPalette.VALUE, "TotemGuard " + requested,
                LoaderPalette.CONNECTIVE, "..."));
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                LoaderCore.StageResult result = plugin.attemptStage(requested);
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(line(
                        LoaderPalette.SUCCESS, "Staged TotemGuard ",
                        LoaderPalette.VALUE, result.version(),
                        LoaderPalette.SUCCESS, ". Run ",
                        LoaderPalette.VALUE, "/tgloader apply",
                        LoaderPalette.SUCCESS, " to restart with it.")));
            } catch (Throwable t) {
                plugin.getLogger().log(Level.SEVERE, "TotemGuard stage " + requested + " failed.", t);
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(line(
                        LoaderPalette.DANGER, "Stage failed: ",
                        LoaderPalette.DANGER_SOFT, String.valueOf(t.getMessage()))));
            }
        });
    }

    // ---------- /tgloader rollout <stage|apply|deploy|cancel|status> (fleet) ----------

    private void handleApply(CommandSender sender) {
        Optional<LoaderCore.StagedSnapshot> staged = plugin.core().readStaged();
        if (staged.isEmpty()) {
            sender.sendMessage(line(LoaderPalette.DANGER,
                    "Nothing is staged. Use ", LoaderPalette.VALUE, "/tgloader stage <version>",
                    LoaderPalette.DANGER, " first."));
            return;
        }
        sender.sendMessage(line(LoaderPalette.LABEL, "Applying staged ",
                LoaderPalette.VALUE, "TotemGuard " + staged.get().version(),
                LoaderPalette.CONNECTIVE, "..."));
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                plugin.attemptApplyStaged();
                String version = plugin.runtime() == null ? null : plugin.runtime().loadedVersion();
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(line(
                        LoaderPalette.SUCCESS, "TotemGuard ",
                        LoaderPalette.VALUE, version == null ? staged.get().version() : version,
                        LoaderPalette.SUCCESS, " is now running.")));
            } catch (Throwable t) {
                plugin.getLogger().log(Level.SEVERE, "TotemGuard apply failed.", t);
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(line(
                        LoaderPalette.DANGER, "Apply failed: ",
                        LoaderPalette.DANGER_SOFT, String.valueOf(t.getMessage()))));
            }
        });
    }

    private void handleRolloutGroup(CommandSender sender, String[] args) {
        String sub = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "";
        switch (sub) {
            case "stage" -> handleRolloutStage(sender, args);
            case "apply" -> handleRolloutApply(sender);
            case "deploy" -> handleRolloutDeploy(sender, args);
            case "cancel" -> handleRolloutCancel(sender);
            case "status" -> handleRolloutStatus(sender);
            default -> sender.sendMessage(line(LoaderPalette.DANGER, "Usage: ",
                    LoaderPalette.VALUE, "/tgloader rollout <stage|apply|deploy|cancel|status>"));
        }
    }

    private void handleRolloutStage(CommandSender sender, String[] args) {
        if (args.length < 3 || args[2].trim().isEmpty()) {
            sender.sendMessage(line(LoaderPalette.DANGER, "Usage: ",
                    LoaderPalette.VALUE, "/tgloader rollout stage <version>"));
            return;
        }
        String requested = args[2].trim();
        try {
            PluginVersionGate.rejectIfPinnedTooOld(requested, "/tgloader rollout stage");
        } catch (IOException ex) {
            sender.sendMessage(line(LoaderPalette.DANGER, ex.getMessage()));
            return;
        }
        RolloutCoordinator coordinator = requireRolloutCoordinator(sender);
        if (coordinator == null) return;

        sender.sendMessage(line(LoaderPalette.LABEL, "Staging ",
                LoaderPalette.VALUE, "TotemGuard " + requested,
                LoaderPalette.CONNECTIVE, " to the fleet..."));
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                LoaderCore.StageResult stageResult = plugin.attemptStage(requested);
                RolloutCoordinator.RolloutBegin begin = coordinator.begin(
                        stageResult.version(), stageResult.sha256(), stageResult.source());
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(line(
                        LoaderPalette.SUCCESS, "Rollout ",
                        LoaderPalette.VALUE, begin.opId().toString().substring(0, 8),
                        LoaderPalette.SUCCESS, " staged. Run ",
                        LoaderPalette.VALUE, "/tgloader rollout apply",
                        LoaderPalette.SUCCESS, " to commit fleet-wide.")));
            } catch (RolloutCoordinator.RolloutBusyException ex) {
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(line(
                        LoaderPalette.DANGER, ex.getMessage())));
            } catch (Throwable t) {
                plugin.getLogger().log(Level.SEVERE, "TotemGuard rollout stage " + requested + " failed.", t);
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(line(
                        LoaderPalette.DANGER, "Rollout stage failed: ",
                        LoaderPalette.DANGER_SOFT, String.valueOf(t.getMessage()))));
            }
        });
    }

    private void handleRolloutApply(CommandSender sender) {
        RolloutCoordinator coordinator = requireRolloutCoordinator(sender);
        if (coordinator == null) return;
        if (coordinator.leaderState().isEmpty()) {
            sender.sendMessage(line(LoaderPalette.DANGER,
                    "No rollout staged from this host. Run ", LoaderPalette.VALUE,
                    "/tgloader rollout stage <version>", LoaderPalette.DANGER, " first."));
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                coordinator.applyActive(RolloutCoordinator.DEFAULT_APPLY_OFFSET);
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(line(
                        LoaderPalette.SUCCESS, "Apply broadcast. Fleet restart in ",
                        LoaderPalette.VALUE, RolloutCoordinator.DEFAULT_APPLY_OFFSET.toSeconds() + "s",
                        LoaderPalette.SUCCESS, ".")));
            } catch (Throwable t) {
                plugin.getLogger().log(Level.SEVERE, "Rollout apply failed.", t);
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(line(
                        LoaderPalette.DANGER, "Apply failed: ",
                        LoaderPalette.DANGER_SOFT, String.valueOf(t.getMessage()))));
            }
        });
    }

    private void handleRolloutDeploy(CommandSender sender, String[] args) {
        if (args.length < 3 || args[2].trim().isEmpty()) {
            sender.sendMessage(line(LoaderPalette.DANGER, "Usage: ",
                    LoaderPalette.VALUE, "/tgloader rollout deploy <version>"));
            return;
        }
        String requested = args[2].trim();
        try {
            PluginVersionGate.rejectIfPinnedTooOld(requested, "/tgloader rollout deploy");
        } catch (IOException ex) {
            sender.sendMessage(line(LoaderPalette.DANGER, ex.getMessage()));
            return;
        }
        RolloutCoordinator coordinator = requireRolloutCoordinator(sender);
        if (coordinator == null) return;

        sender.sendMessage(line(LoaderPalette.LABEL, "Deploying ",
                LoaderPalette.VALUE, "TotemGuard " + requested,
                LoaderPalette.CONNECTIVE, " (stage + apply)..."));
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                LoaderCore.StageResult stageResult = plugin.attemptStage(requested);
                RolloutCoordinator.RolloutBegin begin = coordinator.begin(
                        stageResult.version(), stageResult.sha256(), stageResult.source());
                // Settle window lets peers pull the jar bytes from Redis before APPLY.
                Thread.sleep(DEPLOY_SETTLE.toMillis());
                Duration applyOffset = RolloutCoordinator.DEFAULT_APPLY_OFFSET;
                coordinator.applyActive(applyOffset);
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(line(
                        LoaderPalette.SUCCESS, "Deploy ",
                        LoaderPalette.VALUE, begin.opId().toString().substring(0, 8),
                        LoaderPalette.SUCCESS, " in flight. Fleet restart in ",
                        LoaderPalette.VALUE, applyOffset.toSeconds() + "s",
                        LoaderPalette.SUCCESS, ".")));
            } catch (RolloutCoordinator.RolloutBusyException ex) {
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(line(
                        LoaderPalette.DANGER, ex.getMessage())));
            } catch (Throwable t) {
                plugin.getLogger().log(Level.SEVERE, "TotemGuard rollout deploy " + requested + " failed.", t);
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(line(
                        LoaderPalette.DANGER, "Deploy failed: ",
                        LoaderPalette.DANGER_SOFT, String.valueOf(t.getMessage()))));
            }
        });
    }

    private void handleRolloutCancel(CommandSender sender) {
        RolloutCoordinator coordinator = requireRolloutCoordinator(sender);
        if (coordinator == null) return;
        if (coordinator.leaderState().isEmpty()) {
            sender.sendMessage(line(LoaderPalette.CONNECTIVE, "No active rollout on this host to cancel."));
            return;
        }
        try {
            coordinator.cancelActive();
            sender.sendMessage(line(LoaderPalette.SUCCESS, "Rollout cancelled."));
        } catch (Throwable t) {
            sender.sendMessage(line(LoaderPalette.DANGER, "Cancel failed: ",
                    LoaderPalette.DANGER_SOFT, String.valueOf(t.getMessage())));
        }
    }

    private void handleRolloutStatus(CommandSender sender) {
        RolloutCoordinator coordinator = requireRolloutCoordinator(sender);
        if (coordinator == null) return;
        Optional<RolloutCoordinator.RolloutSnapshot> active = coordinator.active();
        if (active.isEmpty()) {
            sender.sendMessage(line(LoaderPalette.CONNECTIVE, "No active rollout."));
            return;
        }
        RolloutCoordinator.RolloutSnapshot roll = active.get();
        sender.sendMessage(line(LoaderPalette.LABEL, "Rollout ",
                LoaderPalette.VALUE, roll.opId().toString().substring(0, 8),
                LoaderPalette.CAPTION, "  [" + roll.phase() + "]"));
        sender.sendMessage(line(LoaderPalette.LABEL, " target ",
                LoaderPalette.VALUE, roll.targetVersion(),
                LoaderPalette.CAPTION, "  (" + shortSha(roll.targetSha256()) + ")"));
        sender.sendMessage(line(LoaderPalette.LABEL, " leader ",
                LoaderPalette.VALUE, roll.leader().toString().substring(0, 8),
                LoaderPalette.CAPTION, "  started " + roll.startedAt()));
    }

    private RolloutCoordinator requireRolloutCoordinator(CommandSender sender) {
        RolloutCoordinator coordinator = plugin.core().rolloutCoordinator();
        if (coordinator == null || plugin.core().fleetCacheRef().available().isEmpty()) {
            sender.sendMessage(line(LoaderPalette.DANGER,
                    "Fleet sync is not available (Redis not attached). Use ",
                    LoaderPalette.VALUE, "/tgloader stage|apply", LoaderPalette.DANGER, " for local-only updates."));
            return null;
        }
        return coordinator;
    }

    // ---------- tab completion ----------

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return filter(TOP_LEVEL, args[0]);
        }
        if (args.length == 2) {
            return switch (args[0].toLowerCase(Locale.ROOT)) {
                case "plugin" -> filter(PLUGIN_SUBS, args[1]);
                case "rollout" -> filter(ROLLOUT_SUBS, args[1]);
                case "load", "stage" -> versionCandidates(args[1]);
                default -> Collections.emptyList();
            };
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("rollout")) {
            String sub = args[1].toLowerCase(Locale.ROOT);
            if (sub.equals("stage") || sub.equals("deploy")) {
                return versionCandidates(args[2]);
            }
        }
        return Collections.emptyList();
    }

    private List<String> versionCandidates(String prefix) {
        Set<String> candidates = new LinkedHashSet<>(CHANNELS);
        candidates.addAll(plugin.core().listCachedVersions());
        return filter(candidates, prefix);
    }

    private List<String> filter(Iterable<String> candidates, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String c : candidates) {
            if (c.toLowerCase(Locale.ROOT).startsWith(lower)) out.add(c);
        }
        return out;
    }
}
