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

package com.deathmotion.totemguard.loader.catalog;

import com.google.gson.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Per-jar provenance metadata written next to each entry in {@code versions/}. The
 * sidecar lets retention group catalog entries into buckets ({@code channel:LATEST},
 * {@code local}, {@code pinned}, {@code fleet}) and pick top-N per bucket without
 * relying on filename parsing or wall-clock heuristics.
 *
 * <p>Sources is a multi-set; if the same SHA arrives via multiple acquire paths
 * (e.g. local import then auto-resolved as LATEST), every label is added and the
 * jar is kept as long as any bucket retention rule wants it.</p>
 */
public final class CatalogSidecar {

    public static final int SCHEMA_VERSION = 1;
    public static final String SOURCE_LOCAL = "local";
    public static final String SOURCE_PINNED = "pinned";
    public static final String SOURCE_FLEET = "fleet";
    public static final String SOURCE_UNKNOWN = "unknown";
    public static final String SOURCE_CHANNEL_PREFIX = "channel:";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final int schemaVersion;
    private final String sha256;
    private final String version;
    private final String fileName;
    private final Set<String> sources;
    private final Instant firstSeen;
    private final String addedBy;

    private CatalogSidecar(int schemaVersion, String sha256, String version, String fileName,
                           Set<String> sources, Instant firstSeen, String addedBy) {
        this.schemaVersion = schemaVersion;
        this.sha256 = sha256;
        this.version = version;
        this.fileName = fileName;
        this.sources = sources;
        this.firstSeen = firstSeen;
        this.addedBy = addedBy;
    }

    public static CatalogSidecar create(String sha256, String version, String fileName,
                                        String source, String addedBy) {
        Set<String> sources = new LinkedHashSet<>();
        sources.add(source);
        return new CatalogSidecar(SCHEMA_VERSION, sha256, version, fileName, sources, Instant.now(), addedBy);
    }

    public static Path pathFor(Path jar) {
        return jar.resolveSibling(jar.getFileName() + ".meta.json");
    }

    public static CatalogSidecar read(Path metaPath) throws IOException {
        String body = Files.readString(metaPath, StandardCharsets.UTF_8);
        JsonObject root = JsonParser.parseString(body).getAsJsonObject();

        int sv = root.has("schemaVersion") ? root.get("schemaVersion").getAsInt() : 0;
        if (sv > SCHEMA_VERSION) {
            throw new IOException("Sidecar " + metaPath + " schemaVersion " + sv
                    + " is newer than this loader (max " + SCHEMA_VERSION + ")");
        }

        String sha = root.has("sha256") ? root.get("sha256").getAsString() : "";
        String version = root.has("version") ? root.get("version").getAsString() : "";
        String fileName = root.has("fileName") ? root.get("fileName").getAsString() : "";
        Set<String> sources = new LinkedHashSet<>();
        if (root.has("sources")) {
            root.getAsJsonArray("sources").forEach(e -> sources.add(e.getAsString()));
        }
        if (sources.isEmpty()) sources.add(SOURCE_UNKNOWN);
        Instant firstSeen = root.has("firstSeen")
                ? Instant.parse(root.get("firstSeen").getAsString())
                : Instant.EPOCH;
        String addedBy = root.has("addedBy") && !root.get("addedBy").isJsonNull()
                ? root.get("addedBy").getAsString() : null;

        return new CatalogSidecar(sv, sha, version, fileName, sources, firstSeen, addedBy);
    }

    public void write(Path metaPath) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", schemaVersion);
        root.addProperty("sha256", sha256);
        root.addProperty("version", version);
        root.addProperty("fileName", fileName);
        JsonArray array = new JsonArray();
        sources.forEach(array::add);
        root.add("sources", array);
        root.addProperty("firstSeen", firstSeen.toString());
        if (addedBy != null) root.addProperty("addedBy", addedBy);

        Path tmp = metaPath.resolveSibling(metaPath.getFileName() + ".tmp");
        Files.writeString(tmp, GSON.toJson(root), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        Files.move(tmp, metaPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }

    public CatalogSidecar withAddedSource(String source) {
        if (sources.contains(source)) return this;
        Set<String> merged = new LinkedHashSet<>(sources);
        merged.add(source);
        return new CatalogSidecar(schemaVersion, sha256, version, fileName, merged, firstSeen, addedBy);
    }

    public String sha256() {
        return sha256;
    }

    public String version() {
        return version;
    }

    public String fileName() {
        return fileName;
    }

    public List<String> sources() {
        return new ArrayList<>(sources);
    }

    public Instant firstSeen() {
        return firstSeen;
    }

    public String addedBy() {
        return addedBy;
    }
}
