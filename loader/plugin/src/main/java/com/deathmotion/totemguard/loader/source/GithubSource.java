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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Locale;

public final class GithubSource implements VersionResolver {

    static final String REPOSITORY = "Bram1903/TotemGuard";

    private static JsonObject pickByChannelOrTag(JsonArray releases, String requested, String assetSuffix) {
        String upper = requested.toUpperCase(Locale.ROOT);
        boolean isChannel = upper.equals("LATEST") || upper.equals("EXPERIMENTAL") || upper.equals("GIT");
        for (JsonElement element : releases) {
            JsonObject release = element.getAsJsonObject();
            if (release.has("draft") && release.get("draft").getAsBoolean()) continue;
            boolean prerelease = release.has("prerelease") && release.get("prerelease").getAsBoolean();
            String tag = release.has("tag_name") ? release.get("tag_name").getAsString() : "";

            boolean match = switch (upper) {
                case "LATEST" -> !prerelease;
                case "EXPERIMENTAL" -> prerelease;
                case "GIT" -> true;
                default -> tag.equals(requested) || tag.equals("v" + requested);
            };
            if (!match) continue;

            // Skip releases that don't carry an asset for this platform (e.g. ancient
            // 2.x releases that predate the Paper/Fabric split). For explicit tags we
            // let the asset check fail downstream so the user sees the exact reason.
            if (isChannel && !hasPlatformAsset(release, assetSuffix)) continue;
            return release;
        }
        return null;
    }

    private static boolean hasPlatformAsset(JsonObject release, String assetSuffix) {
        if (!release.has("assets")) return false;
        for (JsonElement assetEl : release.getAsJsonArray("assets")) {
            JsonObject asset = assetEl.getAsJsonObject();
            String name = asset.get("name").getAsString();
            if (name.contains(assetSuffix) && name.endsWith(".jar")) return true;
        }
        return false;
    }

    private static Artifact artifactFromRelease(JsonObject release, String assetSuffix) throws IOException {
        String tag = release.get("tag_name").getAsString();
        String version = tag.startsWith("v") ? tag.substring(1) : tag;

        JsonArray assets = release.has("assets") ? release.getAsJsonArray("assets") : new JsonArray();
        for (JsonElement assetEl : assets) {
            JsonObject asset = assetEl.getAsJsonObject();
            String name = asset.get("name").getAsString();
            if (!name.contains(assetSuffix) || !name.endsWith(".jar")) continue;

            String digest = asset.has("digest") && !asset.get("digest").isJsonNull()
                    ? asset.get("digest").getAsString() : null;
            if (digest == null || !digest.startsWith("sha256:")) {
                throw new IOException("GitHub asset " + name
                        + " is missing a SHA-256 digest. Refusing to load without checksum verification.");
            }
            String hashHex = digest.substring("sha256:".length());

            String url = asset.get("browser_download_url").getAsString();
            return new Artifact(version, URI.create(url), Artifact.HashAlgorithm.SHA_256,
                    hashHex, name, "GitHub " + REPOSITORY + " @ " + tag);
        }

        throw new IOException("No asset matching '*" + assetSuffix + "*.jar' on GitHub release " + tag);
    }

    @Override
    public String sourceName() {
        return "GitHub";
    }

    @Override
    public Artifact resolve(ResolverContext context) throws Exception {
        JsonArray releases = GithubReleases.fetchAll(context.paths(), context.fleetCacheRef());
        String requested = context.config().version();
        String assetSuffix = context.platform().assetSuffix();

        JsonObject viaChannel = pickByChannelOrTag(releases, requested, assetSuffix);
        if (viaChannel != null) {
            return artifactFromRelease(viaChannel, assetSuffix);
        }

        String upper = requested.toUpperCase(Locale.ROOT);
        boolean isChannel = upper.equals("LATEST") || upper.equals("EXPERIMENTAL") || upper.equals("GIT");
        if (isChannel) {
            throw new IOException("GitHub has no compatible " + upper + " build"
                    + " (no release ships a *" + assetSuffix + "*.jar asset).");
        }

        List<SearchMatch> matches = GithubSearch.searchMatches(releases, requested, context.platform());
        if (matches.isEmpty()) {
            throw new IOException("No GitHub release matched '" + requested + "'.");
        }
        if (matches.size() > 1) {
            throw new IOException("'" + requested + "' matched " + matches.size()
                    + " releases. Run /tgloader search " + requested + " and pin one.");
        }
        return matches.get(0).artifact();
    }
}
