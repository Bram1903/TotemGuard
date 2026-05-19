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

package totemguard.build

import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration

abstract class DownloadModrinthModsTask : DefaultTask() {

    @get:Input
    abstract val minecraftVersion: Property<String>

    @get:Input
    abstract val loader: Property<String>

    @get:Input
    abstract val mods: ListProperty<ModrinthModSpec>

    @get:OutputDirectory
    abstract val cacheDir: DirectoryProperty

    @TaskAction
    fun fetch() {
        val mc = minecraftVersion.get()
        val ldr = loader.get()
        val cache = cacheDir.get().asFile.toPath()
        Files.createDirectories(cache)

        val http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build()
        val slurper = JsonSlurper()

        val resolved = mutableListOf<String>()
        for (spec in mods.get()) {
            val file = resolveAndDownload(http, slurper, spec, mc, ldr, cache) ?: continue
            resolved.add(file.fileName.toString())
        }
        logger.info("Resolved Modrinth mods for {} on {}: {}", ldr, mc, resolved)
    }

    private fun resolveAndDownload(
        http: HttpClient,
        slurper: JsonSlurper,
        spec: ModrinthModSpec,
        mc: String,
        ldr: String,
        cache: Path,
    ): Path? {
        val loaders = encodeJsonArray(ldr)
        val gameVersions = encodeJsonArray(mc)
        val versionsUrl = "https://api.modrinth.com/v2/project/${spec.slug}/version" +
                "?loaders=$loaders&game_versions=$gameVersions"

        val versionsBody = httpGetString(http, versionsUrl)
            ?: run {
                logger.warn("Modrinth: no response listing versions of {} (loader={}, mc={})", spec.slug, ldr, mc)
                return null
            }

        @Suppress("UNCHECKED_CAST")
        val versions = slurper.parseText(versionsBody) as? List<Map<String, Any>>
        if (versions == null || versions.isEmpty()) {
            logger.warn("Modrinth: no {} versions of {} available for Minecraft {}", ldr, spec.slug, mc)
            return null
        }

        val chosen = if (spec.versionOverride != null) {
            versions.firstOrNull { it["version_number"] == spec.versionOverride }
                ?: run {
                    logger.warn(
                        "Modrinth: requested {}@{} not found, falling back to latest",
                        spec.slug, spec.versionOverride,
                    )
                    versions.first()
                }
        } else {
            versions.first()
        }

        @Suppress("UNCHECKED_CAST")
        val files = chosen["files"] as? List<Map<String, Any>> ?: return null
        if (files.isEmpty()) return null
        val primary = files.firstOrNull { (it["primary"] as? Boolean) == true } ?: files.first()
        val filename = primary["filename"] as String
        val downloadUrl = primary["url"] as String

        val dest = cache.resolve(filename)
        if (Files.exists(dest)) return dest

        logger.lifecycle("Modrinth: downloading {} from {}", filename, downloadUrl)
        val request = HttpRequest.newBuilder(URI.create(downloadUrl))
            .header("User-Agent", "TotemGuard-build (gradle)")
            .GET()
            .build()
        val response = http.send(request, HttpResponse.BodyHandlers.ofFile(dest))
        if (response.statusCode() !in 200..299) {
            Files.deleteIfExists(dest)
            throw RuntimeException("Modrinth download failed (${response.statusCode()}): $downloadUrl")
        }
        return dest
    }

    private fun httpGetString(http: HttpClient, url: String): String? {
        val request = HttpRequest.newBuilder(URI.create(url))
            .header("User-Agent", "TotemGuard-build (gradle)")
            .GET()
            .build()
        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            logger.warn("Modrinth: GET {} returned status {}", url, response.statusCode())
            return null
        }
        return response.body()
    }

    private fun encodeJsonArray(value: String): String =
        URLEncoder.encode("[\"$value\"]", Charsets.UTF_8)
}
