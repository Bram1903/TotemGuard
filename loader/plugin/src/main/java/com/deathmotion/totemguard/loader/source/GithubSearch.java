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

package com.deathmotion.totemguard.loader.source;

import com.deathmotion.totemguard.loader.core.HostPlatform;
import com.deathmotion.totemguard.loader.core.LoaderPaths;
import com.deathmotion.totemguard.loader.fleet.FleetCacheRef;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class GithubSearch {

    private GithubSearch() {
    }

    public static List<SearchMatch> search(String query, HostPlatform platform, LoaderPaths paths,
                                           @Nullable FleetCacheRef fleetCacheRef) throws Exception {
        JsonArray releases = GithubReleases.fetchAll(paths, fleetCacheRef);
        return matchAgainst(releases, query, platform);
    }

    static List<SearchMatch> searchMatches(JsonArray releases, String query, HostPlatform platform) {
        return matchAgainst(releases, query, platform);
    }

    private static List<SearchMatch> matchAgainst(JsonArray releases, String query, HostPlatform platform) {
        String qLower = query.toLowerCase(Locale.ROOT);
        String assetSuffix = platform.assetSuffix();
        List<SearchMatch> result = new ArrayList<>();

        for (JsonElement element : releases) {
            JsonObject release = element.getAsJsonObject();
            if (release.has("draft") && release.get("draft").getAsBoolean()) continue;

            String tag = release.has("tag_name") ? release.get("tag_name").getAsString() : "";
            String commit = release.has("target_commitish") ? release.get("target_commitish").getAsString() : "";
            JsonArray assets = release.has("assets") ? release.getAsJsonArray("assets") : new JsonArray();

            JsonObject platformAsset = findPlatformAsset(assets, assetSuffix);
            if (platformAsset == null) continue;
            String assetName = platformAsset.get("name").getAsString();

            String reason = matchReason(tag, commit, assetName, qLower);
            if (reason == null) continue;

            Artifact artifact = buildArtifactOrNull(release, platformAsset, tag);
            if (artifact == null) continue;

            String version = tag.startsWith("v") ? tag.substring(1) : tag;
            result.add(new SearchMatch(version, reason, artifact));
        }
        return result;
    }

    private static String matchReason(String tag, String commit, String assetName, String qLower) {
        if (tag.toLowerCase(Locale.ROOT).contains(qLower)) {
            return "tag " + tag;
        }
        if (!commit.isEmpty() && commit.toLowerCase(Locale.ROOT).startsWith(qLower)) {
            return "release commit " + abbreviate(commit);
        }
        if (assetName.toLowerCase(Locale.ROOT).contains(qLower)) {
            return "asset " + assetName;
        }
        return null;
    }

    private static String abbreviate(String commit) {
        return commit.length() > 8 ? commit.substring(0, 8) : commit;
    }

    private static JsonObject findPlatformAsset(JsonArray assets, String assetSuffix) {
        for (JsonElement el : assets) {
            JsonObject asset = el.getAsJsonObject();
            String name = asset.get("name").getAsString();
            if (name.contains(assetSuffix) && name.endsWith(".jar")) return asset;
        }
        return null;
    }

    private static Artifact buildArtifactOrNull(JsonObject release, JsonObject asset, String tag) {
        String digest = asset.has("digest") && !asset.get("digest").isJsonNull()
                ? asset.get("digest").getAsString() : null;
        if (digest == null || !digest.startsWith("sha256:")) return null;
        String hashHex = digest.substring("sha256:".length());

        String name = asset.get("name").getAsString();
        String url = asset.get("browser_download_url").getAsString();
        String version = tag.startsWith("v") ? tag.substring(1) : tag;
        return new Artifact(version, URI.create(url), Artifact.HashAlgorithm.SHA_256,
                hashHex, name, "GitHub " + GithubSource.REPOSITORY + " @ " + tag);
    }

}
