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

import io.github.alkoleft.mcp.core.modules.UtilityType
import io.github.alkoleft.mcp.infrastructure.utility.PlatformDetector
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists

/**
 * Search location interface for generating paths
 */
interface SearchLocation {
    fun generatePaths(
        utility: UtilityType,
        version: String?,
    ): List<Path>

    /**
     * Version-aware candidates with optional extracted version metadata.
     * Default implementation wraps plain paths without version metadata.
     */
    fun generateCandidates(
        utility: UtilityType,
        version: String?,
    ): List<Pair<Path, String?>> = generatePaths(utility, version).map { it to null }
}

/**
 * Base class for standard file system locations
 */
abstract class BaseSearchLocation : SearchLocation {
    protected fun getExecutableName(utility: UtilityType): String {
        val extension = if (PlatformDetector.isWindows) ".exe" else ""
        return "${utility.fileName}$extension"
    }
}

/**
 * Standard file system location
 */
class StandardLocation(
    private val basePath: String,
) : BaseSearchLocation() {
    override fun generatePaths(
        utility: UtilityType,
        version: String?,
    ): List<Path> {
        val executableName = getExecutableName(utility)
        return if (PlatformDetector.isWindows) {
            listOf(Paths.get(basePath, "bin", executableName))
        } else {
            listOf(Paths.get(basePath, executableName))
        }
    }
}

/**
 * Version-specific location
 */
class VersionLocation(
    private val basePath: String,
) : BaseSearchLocation() {
    override fun generatePaths(
        utility: UtilityType,
        version: String?,
    ): List<Path> {
        if (version == null) return emptyList()
        val executableName = getExecutableName(utility)
        return if (PlatformDetector.isWindows) {
            listOf(Paths.get(basePath, version, "bin", executableName))
        } else {
            listOf(Paths.get(basePath, version, executableName))
        }
    }
}

/**
 * PATH environment variable location
 */
class PathEnvironmentLocation : BaseSearchLocation() {
    override fun generatePaths(
        utility: UtilityType,
        version: String?,
    ): List<Path> {
        val executableName = getExecutableName(utility)
        return System
            .getenv("PATH")
            ?.split(File.pathSeparator)
            ?.map { Paths.get(it, executableName) } ?: emptyList()
    }
}

/**
 * Enumerates subdirectories of a base folder and builds candidate executable paths
 * with optional version extraction from directory names.
 */
class DirectoryEnumeratingLocation(
    private val basePath: String,
    private val relativeExecutableSubPath: String? = null,
    private val dirNameToVersion: (String) -> String? = { it },
) : BaseSearchLocation() {
    override fun generatePaths(
        utility: UtilityType,
        version: String?,
    ): List<Path> {
        // For compatibility with older code paths that only expect paths,
        // return executable paths for existing directories regardless of version input.
        return generateCandidates(utility, version).map { it.first }
    }

    override fun generateCandidates(
        utility: UtilityType,
        version: String?,
    ): List<Pair<Path, String?>> {
        val expandedBase = expandHome(basePath)
        val baseDir = Paths.get(expandedBase)
        if (!baseDir.exists()) return emptyList()
        val executableName = getExecutableName(utility)
        val result = mutableListOf<Pair<Path, String?>>()
        try {
            Files.newDirectoryStream(baseDir).use { stream ->
                stream.forEach { entry ->
                    if (Files.isDirectory(entry)) {
                        val candidate =
                            if (relativeExecutableSubPath != null && relativeExecutableSubPath.isNotBlank()) {
                                entry.resolve(relativeExecutableSubPath).resolve(executableName)
                            } else {
                                entry.resolve(executableName)
                            }
                        val ver = dirNameToVersion(entry.fileName.toString())
                        result.add(candidate to ver)
                    }
                }
            }
        } catch (_: Exception) {
            // ignore enumeration errors
        }
        return result
    }

    private fun expandHome(path: String): String {
        if (path.startsWith("~")) {
            val home = System.getProperty("user.home") ?: return path
            return path.replaceFirst("~", home)
        }
        return path
    }
}
