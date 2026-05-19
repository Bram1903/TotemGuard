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

import java.util.*

private val SNAPSHOT_HASH_SUFFIX = Regex("\\+[0-9a-f]+-SNAPSHOT$")

fun String.withoutSnapshotHash(): String = replace(SNAPSHOT_HASH_SUFFIX, "-SNAPSHOT")

fun String.capitalizedName(): String =
    replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }

fun String.withSnapshotMetadata(snapshot: Boolean, gitHash: String?): String {
    if (!snapshot) return this
    return buildString {
        append(this@withSnapshotMetadata)
        if (!gitHash.isNullOrBlank()) {
            append('+')
            append(gitHash)
        }
        append("-SNAPSHOT")
    }
}
