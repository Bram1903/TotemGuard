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

package com.deathmotion.totemguard.loader.command;

import com.deathmotion.totemguard.api.fleet.FleetCache;
import com.deathmotion.totemguard.loader.command.LoaderMessage.Color;
import com.deathmotion.totemguard.loader.command.LoaderMessage.Line;
import com.deathmotion.totemguard.loader.command.LoaderMessage.Sink;
import com.deathmotion.totemguard.loader.core.LoaderCore;
import com.deathmotion.totemguard.loader.core.LoaderManifest;
import com.deathmotion.totemguard.loader.core.PluginVersionGate;
import com.deathmotion.totemguard.loader.fleet.RolloutCoordinator;
import com.deathmotion.totemguard.loader.runtime.PluginRuntime;
import com.deathmotion.totemguard.loader.source.GithubSearch;
import com.deathmotion.totemguard.loader.source.SearchMatch;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.logging.Level;

/**
 * Shared {@code /tgloader} handler logic. Pure of platform types: the only thing
 * it knows about is a {@link LoaderApp} (for backend hooks) and a {@link Sink}
 * (for sending lines). Each platform supplies its own thin command-frontend that
 * parses args and routes to the methods here.
 */
public final class LoaderCommandService {

    public static final List<String> TOP_LEVEL = List.of(
            "status", "peers", "versions", "search", "import",
            "plugin", "load", "stage", "apply", "rollout");
    public static final List<String> PLUGIN_SUBS = List.of("start", "stop", "restart");
    public static final List<String> ROLLOUT_SUBS = List.of("stage", "apply", "deploy", "cancel", "status");
    public static final List<String> CHANNELS = List.of("LATEST", "EXPERIMENTAL", "GIT");

    public static final Duration DEPLOY_SETTLE = Duration.ofSeconds(5);

    private final LoaderApp app;

    public LoaderCommandService(LoaderApp app) {
        this.app = app;
    }

    private static String shortSha(String sha) {
        if (sha == null || sha.length() < 10) return sha == null ? "" : sha;
        return sha.substring(0, 10) + "...";
    }

    public static List<String> filter(Iterable<String> candidates, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String c : candidates) {
            if (c.toLowerCase(Locale.ROOT).startsWith(lower)) out.add(c);
        }
        return out;
    }

    // ---------- top-level: status / peers / versions / search / import ----------

    public void dispatch(Sink sink, String[] args) {
        String sub = args.length == 0 ? "status" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "status" -> handleStatus(sink);
            case "peers" -> handlePeers(sink);
            case "versions" -> handleVersions(sink);
            case "search" -> handleSearch(sink, args);
            case "import" -> handleImport(sink);
            case "plugin" -> handlePluginGroup(sink, args);
            case "load" -> handleLoad(sink, args);
            case "stage" -> handleStage(sink, args);
            case "apply" -> handleApply(sink);
            case "rollout" -> handleRolloutGroup(sink, args);
            default -> sendUsage(sink);
        }
    }

    private void sendUsage(Sink sink) {
        sink.send(Line.of(Color.DANGER, "Usage: ",
                Color.VALUE, "/tgloader <status|peers|versions|search|import|plugin|load|stage|apply|rollout>"));
    }

    private void handleStatus(Sink sink) {
        PluginRuntime runtime = app.runtime();
        String pluginVersion = runtime == null ? null : runtime.loadedVersion();
        sink.send(Line.of(Color.LABEL, "Loader ",
                Color.VALUE, LoaderManifest.loaderVersion()));
        if (pluginVersion == null) {
            sink.send(Line.of(Color.LABEL, "TotemGuard ",
                    Color.DANGER, "NOT LOADED"));
            sink.send(Line.of(Color.CAPTION, "Use ",
                    Color.VALUE, "/tgloader load <version>",
                    Color.CAPTION, " to load a build."));
        } else {
            sink.send(Line.of(Color.LABEL, "TotemGuard ",
                    Color.VALUE, pluginVersion));
        }
        app.core().readStaged().ifPresent(staged -> sink.send(Line.of(
                Color.LABEL, "Staged ",
                Color.VALUE, staged.version(),
                Color.CAPTION, "  (" + shortSha(staged.sha256()) + ")")));
        RolloutCoordinator coord = app.core().rolloutCoordinator();
        if (coord != null) {
            coord.active().ifPresent(roll -> sink.send(Line.of(
                    Color.LABEL, "Rollout ",
                    Color.VALUE, roll.opId().toString().substring(0, 8),
                    Color.CAPTION, "  -> " + roll.targetVersion() + "  [" + roll.phase() + "]")));
        }
    }

    private void handlePeers(Sink sink) {
        FleetCache cache = app.core().fleetCacheRef().available().orElse(null);
        if (cache == null) {
            sink.send(Line.of(Color.DANGER,
                    "Fleet cache not attached. Peers are only visible after TotemGuard connects to Redis."));
            return;
        }
        List<String> keys = cache.scanKeys("totemguard:loader:catalog:", 256);
        if (keys.isEmpty()) {
            sink.send(Line.of(Color.CONNECTIVE,
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
        sink.send(Line.of(Color.LABEL, "Fleet membership ",
                Color.CAPTION, "(" + keys.size() + " entries)"));
        for (Map.Entry<String, Set<String>> e : byVersion.entrySet()) {
            Set<String> hosts = e.getValue();
            sink.send(Line.of(Color.CAPTION, " - ",
                    Color.VALUE, e.getKey(),
                    Color.CAPTION, "  " + hosts.size() + " host" + (hosts.size() == 1 ? "" : "s")));
        }
    }

    private void handleVersions(Sink sink) {
        List<String> cached = app.core().listCachedVersions();
        if (cached.isEmpty()) {
            sink.send(Line.of(Color.CONNECTIVE,
                    "No cached versions on disk. Use ",
                    Color.VALUE, "/tgloader load <version>",
                    Color.CONNECTIVE, " to fetch one."));
            return;
        }
        PluginRuntime runtime = app.runtime();
        String running = runtime == null ? null : runtime.loadedVersion();
        sink.send(Line.of(Color.LABEL, "Cached versions ",
                Color.CAPTION, "(" + cached.size() + ")"));
        for (String version : cached) {
            if (version.equals(running)) {
                sink.send(Line.of(Color.CAPTION, " - ",
                        Color.VALUE, version,
                        Color.SUCCESS, " (running)"));
            } else {
                sink.send(Line.of(Color.CAPTION, " - ",
                        Color.VALUE, version));
            }
        }
        sink.send(Line.of(Color.CAPTION, "Channels: ",
                Color.CONNECTIVE, "LATEST, EXPERIMENTAL, GIT"));
    }

    private void handleSearch(Sink sink, String[] args) {
        if (args.length < 2) {
            sink.send(Line.of(Color.DANGER, "Usage: ",
                    Color.VALUE, "/tgloader search <commit-or-version>"));
            return;
        }
        String query = args[1].trim();
        sink.send(Line.of(Color.LABEL, "Searching GitHub for ",
                Color.VALUE, query, Color.CONNECTIVE, "..."));
        app.submitBackground(() -> {
            try {
                List<SearchMatch> matches = GithubSearch.search(query, app.core().platform(),
                        app.core().paths(), app.core().fleetCacheRef());
                renderSearchResults(sink, query, matches);
            } catch (Throwable t) {
                app.logger().log(Level.SEVERE, "TotemGuard search " + query + " failed.", t);
                sink.send(Line.of(Color.DANGER, "Search failed: ",
                        Color.DANGER_SOFT, String.valueOf(t.getMessage())));
            }
        });
    }

    private void renderSearchResults(Sink sink, String query, List<SearchMatch> matches) {
        if (matches.isEmpty()) {
            sink.send(Line.of(Color.DANGER, "No releases matched ",
                    Color.DANGER_SOFT, query, Color.DANGER, "."));
            return;
        }
        sink.send(Line.of(Color.LABEL, "Matches ",
                Color.CAPTION, "(" + matches.size() + ")"));
        int shown = 0;
        for (SearchMatch match : matches) {
            if (shown++ >= 10) {
                sink.send(Line.of(Color.CAPTION,
                        "... and " + (matches.size() - 10) + " more. Narrow the query."));
                break;
            }
            sink.send(Line.of(Color.CAPTION, " - ",
                    Color.VALUE, match.version(),
                    Color.CAPTION, "  ",
                    Color.CONNECTIVE, match.matchReason()));
        }
        sink.send(Line.of(Color.CAPTION, "Install with ",
                Color.VALUE, "/tgloader load <version>"));
    }

    // ---------- /tgloader plugin <start|stop|restart> ----------

    private void handleImport(Sink sink) {
        app.submitBackground(() -> {
            try {
                int imported = app.core().importLocal();
                if (imported == 0) {
                    sink.send(Line.of(Color.CONNECTIVE, "Nothing to import. Drop jars in ",
                            Color.VALUE, "loader/local/",
                            Color.CONNECTIVE, " and try again."));
                } else {
                    sink.send(Line.of(Color.SUCCESS, "Imported ",
                            Color.VALUE, String.valueOf(imported),
                            Color.SUCCESS, " jar" + (imported == 1 ? "" : "s") + " into the version catalog."));
                }
            } catch (Throwable t) {
                app.logger().log(Level.SEVERE, "Local import failed.", t);
                sink.send(Line.of(Color.DANGER, "Import failed: ",
                        Color.DANGER_SOFT, String.valueOf(t.getMessage())));
            }
        });
    }

    private void handlePluginGroup(Sink sink, String[] args) {
        String sub = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "";
        switch (sub) {
            case "start" -> handlePluginStart(sink);
            case "stop" -> handlePluginStop(sink);
            case "restart" -> handlePluginRestart(sink);
            default -> sink.send(Line.of(Color.DANGER, "Usage: ",
                    Color.VALUE, "/tgloader plugin <start|stop|restart>"));
        }
    }

    private void handlePluginStart(Sink sink) {
        PluginRuntime runtime = app.runtime();
        if (runtime != null && runtime.isLoaded()) {
            sink.send(Line.of(Color.DANGER, "TotemGuard already running ",
                    Color.CONNECTIVE, "(", Color.VALUE, runtime.loadedVersion(),
                    Color.CONNECTIVE, "). Use ",
                    Color.VALUE, "/tgloader plugin restart"));
            return;
        }
        sink.send(Line.of(Color.LABEL, "Starting TotemGuard",
                Color.CONNECTIVE, "..."));
        app.submitBackground(() -> {
            try {
                app.attemptStart(null);
                PluginRuntime r = app.runtime();
                String version = r == null ? null : r.loadedVersion();
                sink.send(Line.of(Color.SUCCESS, "TotemGuard started. Running ",
                        Color.VALUE, version == null ? "unknown" : version));
            } catch (Throwable t) {
                app.logger().log(Level.SEVERE, "TotemGuard plugin start failed.", t);
                sink.send(Line.of(Color.DANGER, "Start failed: ",
                        Color.DANGER_SOFT, String.valueOf(t.getMessage())));
            }
        });
    }

    private void handlePluginStop(Sink sink) {
        PluginRuntime runtime = app.runtime();
        if (runtime == null || !runtime.isLoaded()) {
            sink.send(Line.of(Color.DANGER, "TotemGuard is not currently loaded."));
            return;
        }
        sink.send(Line.of(Color.LABEL, "Stopping TotemGuard",
                Color.CONNECTIVE, "..."));
        app.submitBackground(() -> {
            try {
                runtime.stopForCommand();
                sink.send(Line.of(Color.SUCCESS, "TotemGuard stopped. Run ",
                        Color.VALUE, "/tgloader plugin start",
                        Color.SUCCESS, " to bring it back."));
            } catch (Throwable t) {
                app.logger().log(Level.SEVERE, "TotemGuard plugin stop failed.", t);
                sink.send(Line.of(Color.DANGER, "Stop failed: ",
                        Color.DANGER_SOFT, String.valueOf(t.getMessage())));
            }
        });
    }

    // ---------- /tgloader load|stage|apply (local) ----------

    private void handlePluginRestart(Sink sink) {
        PluginRuntime runtime = app.runtime();
        if (runtime == null) {
            sink.send(Line.of(Color.DANGER,
                    "TotemGuard is not loaded. Use ", Color.VALUE, "/tgloader plugin start"));
            return;
        }
        sink.send(Line.of(Color.LABEL, "Restarting TotemGuard",
                Color.CONNECTIVE, "..."));
        app.submitBackground(() -> {
            try {
                runtime.restart();
                String version = runtime.loadedVersion();
                sink.send(Line.of(Color.SUCCESS, "TotemGuard restarted. Running ",
                        Color.VALUE, version == null ? "unknown" : version));
            } catch (Throwable t) {
                app.logger().log(Level.SEVERE, "TotemGuard plugin restart failed.", t);
                sink.send(Line.of(Color.DANGER, "Restart failed: ",
                        Color.DANGER_SOFT, String.valueOf(t.getMessage())));
            }
        });
    }

    private void handleLoad(Sink sink, String[] args) {
        if (args.length < 2 || args[1].trim().isEmpty()) {
            sink.send(Line.of(Color.DANGER, "Usage: ",
                    Color.VALUE, "/tgloader load <version>"));
            return;
        }
        String requested = args[1].trim();
        try {
            PluginVersionGate.rejectIfPinnedTooOld(requested, "/tgloader load");
        } catch (IOException ex) {
            sink.send(Line.of(Color.DANGER, ex.getMessage()));
            return;
        }
        sink.send(Line.of(Color.LABEL, "Loading ",
                Color.VALUE, "TotemGuard " + requested,
                Color.CONNECTIVE, "..."));
        app.submitBackground(() -> {
            try {
                app.attemptStart(requested);
                PluginRuntime runtime = app.runtime();
                String version = runtime == null ? null : runtime.loadedVersion();
                sink.send(Line.of(Color.SUCCESS, "TotemGuard ",
                        Color.VALUE, version == null ? requested : version,
                        Color.SUCCESS, " is now running."));
            } catch (Throwable t) {
                app.logger().log(Level.SEVERE, "TotemGuard load " + requested + " failed.", t);
                sink.send(Line.of(Color.DANGER, "Load failed: ",
                        Color.DANGER_SOFT, String.valueOf(t.getMessage())));
            }
        });
    }

    private void handleStage(Sink sink, String[] args) {
        if (args.length < 2 || args[1].trim().isEmpty()) {
            sink.send(Line.of(Color.DANGER, "Usage: ",
                    Color.VALUE, "/tgloader stage <version>"));
            return;
        }
        String requested = args[1].trim();
        try {
            PluginVersionGate.rejectIfPinnedTooOld(requested, "/tgloader stage");
        } catch (IOException ex) {
            sink.send(Line.of(Color.DANGER, ex.getMessage()));
            return;
        }
        sink.send(Line.of(Color.LABEL, "Staging ",
                Color.VALUE, "TotemGuard " + requested,
                Color.CONNECTIVE, "..."));
        app.submitBackground(() -> {
            try {
                LoaderCore.StageResult result = app.attemptStage(requested);
                sink.send(Line.of(Color.SUCCESS, "Staged TotemGuard ",
                        Color.VALUE, result.version(),
                        Color.SUCCESS, ". Run ",
                        Color.VALUE, "/tgloader apply",
                        Color.SUCCESS, " to restart with it."));
            } catch (Throwable t) {
                app.logger().log(Level.SEVERE, "TotemGuard stage " + requested + " failed.", t);
                sink.send(Line.of(Color.DANGER, "Stage failed: ",
                        Color.DANGER_SOFT, String.valueOf(t.getMessage())));
            }
        });
    }

    // ---------- /tgloader rollout <stage|apply|deploy|cancel|status> (fleet) ----------

    private void handleApply(Sink sink) {
        Optional<LoaderCore.StagedSnapshot> staged = app.core().readStaged();
        if (staged.isEmpty()) {
            sink.send(Line.of(Color.DANGER,
                    "Nothing is staged. Use ", Color.VALUE, "/tgloader stage <version>",
                    Color.DANGER, " first."));
            return;
        }
        sink.send(Line.of(Color.LABEL, "Applying staged ",
                Color.VALUE, "TotemGuard " + staged.get().version(),
                Color.CONNECTIVE, "..."));
        app.submitBackground(() -> {
            try {
                app.attemptApplyStaged();
                String version = app.runtime() == null ? null : app.runtime().loadedVersion();
                sink.send(Line.of(Color.SUCCESS, "TotemGuard ",
                        Color.VALUE, version == null ? staged.get().version() : version,
                        Color.SUCCESS, " is now running."));
            } catch (Throwable t) {
                app.logger().log(Level.SEVERE, "TotemGuard apply failed.", t);
                sink.send(Line.of(Color.DANGER, "Apply failed: ",
                        Color.DANGER_SOFT, String.valueOf(t.getMessage())));
            }
        });
    }

    private void handleRolloutGroup(Sink sink, String[] args) {
        String sub = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "";
        switch (sub) {
            case "stage" -> handleRolloutStage(sink, args);
            case "apply" -> handleRolloutApply(sink);
            case "deploy" -> handleRolloutDeploy(sink, args);
            case "cancel" -> handleRolloutCancel(sink);
            case "status" -> handleRolloutStatus(sink);
            default -> sink.send(Line.of(Color.DANGER, "Usage: ",
                    Color.VALUE, "/tgloader rollout <stage|apply|deploy|cancel|status>"));
        }
    }

    private void handleRolloutStage(Sink sink, String[] args) {
        if (args.length < 3 || args[2].trim().isEmpty()) {
            sink.send(Line.of(Color.DANGER, "Usage: ",
                    Color.VALUE, "/tgloader rollout stage <version>"));
            return;
        }
        String requested = args[2].trim();
        try {
            PluginVersionGate.rejectIfPinnedTooOld(requested, "/tgloader rollout stage");
        } catch (IOException ex) {
            sink.send(Line.of(Color.DANGER, ex.getMessage()));
            return;
        }
        RolloutCoordinator coordinator = requireRolloutCoordinator(sink);
        if (coordinator == null) return;

        sink.send(Line.of(Color.LABEL, "Staging ",
                Color.VALUE, "TotemGuard " + requested,
                Color.CONNECTIVE, " to the fleet..."));
        app.submitBackground(() -> {
            try {
                LoaderCore.StageResult stageResult = app.attemptStage(requested);
                RolloutCoordinator.RolloutBegin begin = coordinator.begin(
                        stageResult.version(), stageResult.sha256(), stageResult.source());
                sink.send(Line.of(Color.SUCCESS, "Rollout ",
                        Color.VALUE, begin.opId().toString().substring(0, 8),
                        Color.SUCCESS, " staged. Run ",
                        Color.VALUE, "/tgloader rollout apply",
                        Color.SUCCESS, " to commit fleet-wide."));
            } catch (RolloutCoordinator.RolloutBusyException ex) {
                sink.send(Line.of(Color.DANGER, ex.getMessage()));
            } catch (Throwable t) {
                app.logger().log(Level.SEVERE, "TotemGuard rollout stage " + requested + " failed.", t);
                sink.send(Line.of(Color.DANGER, "Rollout stage failed: ",
                        Color.DANGER_SOFT, String.valueOf(t.getMessage())));
            }
        });
    }

    private void handleRolloutApply(Sink sink) {
        RolloutCoordinator coordinator = requireRolloutCoordinator(sink);
        if (coordinator == null) return;
        if (coordinator.leaderState().isEmpty()) {
            sink.send(Line.of(Color.DANGER,
                    "No rollout staged from this host. Run ", Color.VALUE,
                    "/tgloader rollout stage <version>", Color.DANGER, " first."));
            return;
        }
        app.submitBackground(() -> {
            try {
                coordinator.applyActive(RolloutCoordinator.DEFAULT_APPLY_OFFSET);
                sink.send(Line.of(Color.SUCCESS, "Apply broadcast. Fleet restart in ",
                        Color.VALUE, RolloutCoordinator.DEFAULT_APPLY_OFFSET.toSeconds() + "s",
                        Color.SUCCESS, "."));
            } catch (Throwable t) {
                app.logger().log(Level.SEVERE, "Rollout apply failed.", t);
                sink.send(Line.of(Color.DANGER, "Apply failed: ",
                        Color.DANGER_SOFT, String.valueOf(t.getMessage())));
            }
        });
    }

    private void handleRolloutDeploy(Sink sink, String[] args) {
        if (args.length < 3 || args[2].trim().isEmpty()) {
            sink.send(Line.of(Color.DANGER, "Usage: ",
                    Color.VALUE, "/tgloader rollout deploy <version>"));
            return;
        }
        String requested = args[2].trim();
        try {
            PluginVersionGate.rejectIfPinnedTooOld(requested, "/tgloader rollout deploy");
        } catch (IOException ex) {
            sink.send(Line.of(Color.DANGER, ex.getMessage()));
            return;
        }
        RolloutCoordinator coordinator = requireRolloutCoordinator(sink);
        if (coordinator == null) return;

        sink.send(Line.of(Color.LABEL, "Deploying ",
                Color.VALUE, "TotemGuard " + requested,
                Color.CONNECTIVE, " (stage + apply)..."));
        app.submitBackground(() -> {
            try {
                LoaderCore.StageResult stageResult = app.attemptStage(requested);
                RolloutCoordinator.RolloutBegin begin = coordinator.begin(
                        stageResult.version(), stageResult.sha256(), stageResult.source());
                Duration applyOffset = DEPLOY_SETTLE.plus(RolloutCoordinator.DEFAULT_APPLY_OFFSET);
                // The settle window is part of the applyOffset so peers have time to pull the
                // jar blob from Redis before the APPLY broadcast fires. Avoids a Thread.sleep
                // that would pin a worker thread for 5+ seconds.
                coordinator.applyActive(applyOffset);
                sink.send(Line.of(Color.SUCCESS, "Deploy ",
                        Color.VALUE, begin.opId().toString().substring(0, 8),
                        Color.SUCCESS, " in flight. Fleet restart in ",
                        Color.VALUE, applyOffset.toSeconds() + "s",
                        Color.SUCCESS, "."));
            } catch (RolloutCoordinator.RolloutBusyException ex) {
                sink.send(Line.of(Color.DANGER, ex.getMessage()));
            } catch (Throwable t) {
                app.logger().log(Level.SEVERE, "TotemGuard rollout deploy " + requested + " failed.", t);
                sink.send(Line.of(Color.DANGER, "Deploy failed: ",
                        Color.DANGER_SOFT, String.valueOf(t.getMessage())));
            }
        });
    }

    private void handleRolloutCancel(Sink sink) {
        RolloutCoordinator coordinator = requireRolloutCoordinator(sink);
        if (coordinator == null) return;
        if (coordinator.leaderState().isEmpty()) {
            sink.send(Line.of(Color.CONNECTIVE, "No active rollout on this host to cancel."));
            return;
        }
        try {
            coordinator.cancelActive();
            sink.send(Line.of(Color.SUCCESS, "Rollout cancelled."));
        } catch (Throwable t) {
            sink.send(Line.of(Color.DANGER, "Cancel failed: ",
                    Color.DANGER_SOFT, String.valueOf(t.getMessage())));
        }
    }

    private void handleRolloutStatus(Sink sink) {
        RolloutCoordinator coordinator = requireRolloutCoordinator(sink);
        if (coordinator == null) return;
        Optional<RolloutCoordinator.RolloutSnapshot> active = coordinator.active();
        if (active.isEmpty()) {
            sink.send(Line.of(Color.CONNECTIVE, "No active rollout."));
            return;
        }
        RolloutCoordinator.RolloutSnapshot roll = active.get();
        sink.send(Line.of(Color.LABEL, "Rollout ",
                Color.VALUE, roll.opId().toString().substring(0, 8),
                Color.CAPTION, "  [" + roll.phase() + "]"));
        sink.send(Line.of(Color.LABEL, " target ",
                Color.VALUE, roll.targetVersion(),
                Color.CAPTION, "  (" + shortSha(roll.targetSha256()) + ")"));
        sink.send(Line.of(Color.LABEL, " leader ",
                Color.VALUE, roll.leader().toString().substring(0, 8),
                Color.CAPTION, "  started " + roll.startedAt()));
    }

    // ---------- helpers for platform tab-completion ----------

    private RolloutCoordinator requireRolloutCoordinator(Sink sink) {
        RolloutCoordinator coordinator = app.core().rolloutCoordinator();
        if (coordinator == null || app.core().fleetCacheRef().available().isEmpty()) {
            sink.send(Line.of(Color.DANGER,
                    "Fleet sync is not available (Redis not attached). Use ",
                    Color.VALUE, "/tgloader stage|apply", Color.DANGER, " for local-only updates."));
            return null;
        }
        return coordinator;
    }

    public List<String> versionCandidates(String prefix) {
        Set<String> candidates = new LinkedHashSet<>(CHANNELS);
        candidates.addAll(app.core().listCachedVersions());
        return filter(candidates, prefix);
    }
}
