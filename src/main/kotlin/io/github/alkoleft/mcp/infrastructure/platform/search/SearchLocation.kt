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

import io.github.alkoleft.mcp.application.core.UtilityType
import io.github.alkoleft.mcp.infrastructure.utility.PlatformDetector
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists

enum class SearchCandidateSource {
    PATH,
    USER_INSTALLATION,
    SYSTEM_INSTALLATION,
    UNKNOWN,
}

data class SearchCandidate(
    val path: Path,
    val version: String? = null,
    val source: SearchCandidateSource = SearchCandidateSource.UNKNOWN,
)

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
    ): List<SearchCandidate> = generatePaths(utility, version).map { SearchCandidate(it) }
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
class PathEnvironmentLocation(
    private val pathEnvironment: String? = System.getenv("PATH"),
) : BaseSearchLocation() {
    override fun generatePaths(
        utility: UtilityType,
        version: String?,
    ): List<Path> {
        val executableName = getExecutableName(utility)
        return pathEnvironment
            ?.split(File.pathSeparator)
            ?.map { Paths.get(it, executableName) } ?: emptyList()
    }

    override fun generateCandidates(
        utility: UtilityType,
        version: String?,
    ): List<SearchCandidate> =
        generatePaths(utility, version).map { path ->
            SearchCandidate(
                path = path,
                version = extractVersionFromPath(path, utility),
                source = SearchCandidateSource.PATH,
            )
        }

    private fun extractVersionFromPath(
        path: Path,
        utility: UtilityType,
    ): String? {
        if (utility != UtilityType.EDT_CLI) return null

        val normalized = path.toAbsolutePath().normalize().toString().replace('\\', '/')
        val patterns =
            listOf(
                Regex(""".*/1C_EDT\s+(\d+(?:\.\d+)+)/1cedt/1cedtcli(?:\.exe)?$""", RegexOption.IGNORE_CASE),
                Regex(""".*/1c-edt-(\d+(?:\.\d+)+(?:\+\d+)?)(?:-[^/]+)?/1cedtcli(?:\.exe)?$""", RegexOption.IGNORE_CASE),
                Regex(""".*/1c-edt-(\d+(?:\.\d+)+(?:\+\d+)?)(?:-[^/]+)?/1cedt/1cedtcli(?:\.exe)?$""", RegexOption.IGNORE_CASE),
            )

        return patterns.firstNotNullOfOrNull { it.find(normalized)?.groupValues?.getOrNull(1) }
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
    private val source: SearchCandidateSource = SearchCandidateSource.UNKNOWN,
) : BaseSearchLocation() {
    override fun generatePaths(
        utility: UtilityType,
        version: String?,
    ): List<Path> {
        // For compatibility with older code paths that only expect paths,
        // return executable paths for existing directories regardless of version input.
        return generateCandidates(utility, version).map { it.path }
    }

    override fun generateCandidates(
        utility: UtilityType,
        version: String?,
    ): List<SearchCandidate> {
        val expandedBase = expandHome(basePath)
        val baseDir = Paths.get(expandedBase)
        if (!baseDir.exists()) return emptyList()
        val executableName = getExecutableName(utility)
        val result = mutableListOf<SearchCandidate>()
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
                        result.add(
                            SearchCandidate(
                                path = candidate,
                                version = ver,
                                source = source,
                            ),
                        )
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
