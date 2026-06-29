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

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property

object PaperRunDefaults {
    val JVM_ARGS: List<String> = listOf(
        "-Dcom.mojang.eula.agree=true",
        "-DPaper.IgnoreJavaVersion=true",
    )

    val CORE_PLUGINS: List<String> = listOf(
        "https://cdn.modrinth.com/data/HYKaKraK/versions/h0ncTpUP/packetevents-spigot-2.13.0.jar?mr_download_reason=standalone",
        "https://github.com/ViaVersion/ViaVersion/releases/download/5.10.0/ViaVersion-5.10.0.jar",
        "https://github.com/ViaVersion/ViaBackwards/releases/download/5.10.0/ViaBackwards-5.10.0.jar",
        "https://github.com/PlaceholderAPI/PlaceholderAPI/releases/download/2.12.2/PlaceholderAPI-2.12.2.jar",
        "https://download.luckperms.net/1645/bukkit/loader/LuckPerms-Bukkit-5.5.57.jar"
    )

    val PAPER_TEST_PLUGINS: List<String> = listOf(
        "https://cdn.modrinth.com/data/hXiIvTyT/versions/nY6VN1XH/EssentialsX-2.22.0.jar?mr_download_reason=standalone",
        "https://cdn.modrinth.com/data/z4HZZnLr/versions/tG0Vfeqx/FastAsyncWorldEdit-Paper-2.15.2.jar?mr_download_reason=standalone"
    )

    const val DEFAULT_STAGED_PLUGIN_DIR: String = "plugins/TotemGuard-Loader/local"
}

data class PaperRunSpec(
    val minecraftVersion: String,
    val javaVersion: Int,
)

abstract class PaperRunsExtension {

    internal var defaultRun: PaperRunSpec? = null
    internal val extraRuns: MutableList<PaperRunSpec> = mutableListOf()
    internal var defaultFoliaRun: PaperRunSpec? = null
    internal val extraFoliaRuns: MutableList<PaperRunSpec> = mutableListOf()

    internal val sharedJvmArgs: MutableList<String> = mutableListOf(*PaperRunDefaults.JVM_ARGS.toTypedArray())
    internal val sharedPluginUrls: MutableList<String> = mutableListOf(*PaperRunDefaults.CORE_PLUGINS.toTypedArray())
    internal val paperOnlyPluginUrls: MutableList<String> =
        mutableListOf(*PaperRunDefaults.PAPER_TEST_PLUGINS.toTypedArray())

    abstract val stagedSourceJar: RegularFileProperty
    abstract val stagedPluginDir: Property<String>

    fun default(minecraftVersion: String, java: Int) {
        defaultRun = PaperRunSpec(minecraftVersion, java)
    }

    fun version(minecraftVersion: String, java: Int) {
        extraRuns.add(PaperRunSpec(minecraftVersion, java))
    }

    fun defaultFolia(minecraftVersion: String, java: Int) {
        defaultFoliaRun = PaperRunSpec(minecraftVersion, java)
    }

    fun folia(minecraftVersion: String, java: Int) {
        extraFoliaRuns.add(PaperRunSpec(minecraftVersion, java))
    }

    fun jvmArgs(vararg args: String) {
        sharedJvmArgs.clear()
        sharedJvmArgs.addAll(args)
    }

    fun pluginUrls(vararg urls: String) {
        sharedPluginUrls.clear()
        sharedPluginUrls.addAll(urls)
    }

    fun paperOnlyPluginUrls(vararg urls: String) {
        paperOnlyPluginUrls.clear()
        paperOnlyPluginUrls.addAll(urls)
    }
}
