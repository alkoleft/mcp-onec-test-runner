/*
 * This file is part of METR.
 *
 * Copyright (C) 2025 Aleksey Koryakin <alkoleft@gmail.com> and contributors.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * METR is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * METR is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with METR.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.alkoleft.mcp.infrastructure.platform.search

import java.nio.file.Path

interface VersionResolver {
    fun selectBest(
        candidates: List<Pair<Path, String?>>,
        requirement: String?,
    ): Path?
}

class DefaultVersionResolver : VersionResolver {
    override fun selectBest(
        candidates: List<Pair<Path, String?>>,
        requirement: String?,
    ): Path? {
        if (candidates.isEmpty()) return null
        val parsed =
            candidates.map { pair ->
                val path = pair.first
                val ver = pair.second?.let { v -> Version.parse(v) }
                path to ver
            }
        if (requirement == null || requirement.equals("latest", ignoreCase = true)) {
            val withVersion =
                parsed
                    .filter { it.second != null }
                    .map { it.first to it.second!! }
            if (withVersion.isNotEmpty()) {
                return withVersion.maxByOrNull { it.second }!!.first
            }
            return candidates.first().first
        }
        val reqVersion = Version.parse(requirement)
        if (reqVersion == null) {
            // Invalid requirement format -> signal no match
            return null
        }
        val isMask = reqVersion.parts.size < 4
        val withVersion =
            parsed
                .filter { it.second != null }
                .map { it.first to it.second!! }
        val filtered =
            if (isMask) {
                withVersion.filter { it.second.startsWith(reqVersion) }
            } else {
                withVersion.filter { it.second == reqVersion }
            }
        if (filtered.isNotEmpty()) {
            return filtered.maxByOrNull { it.second }!!.first
        }
        // No candidates satisfy the version requirement -> signal no match
        return null
    }
}
