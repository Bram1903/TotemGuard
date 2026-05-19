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

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import java.io.Serializable

object FabricRunDefaults {
    val MOD_SLUGS: List<String> = listOf(
        "packetevents",
        "luckperms",
    )
}

data class ModrinthModSpec(
    val slug: String,
    val versionOverride: String? = null,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

abstract class FabricRunsExtension {

    abstract val minecraftVersion: Property<String>
    abstract val loader: Property<String>
    abstract val mods: ListProperty<ModrinthModSpec>
    abstract val runDirectory: Property<String>

    fun mod(slug: String) {
        mods.add(ModrinthModSpec(slug))
    }

    fun mod(slug: String, version: String) {
        mods.add(ModrinthModSpec(slug, version))
    }
}
