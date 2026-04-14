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
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SearchStrategyTest {
    @EnabledOnOs(OS.WINDOWS)
    @Test
    fun `should generate correct paths for Windows strategy`() {
        val strategy = PlatformWindowsSearchStrategy
        val tier1Paths = strategy.locations.flatMap { it.generatePaths(UtilityType.DESIGNER, null) }
        assertNotNull(tier1Paths, "Tier 1 paths should not be null")
        assertTrue(tier1Paths.isNotEmpty(), "Tier 1 paths should not be empty")
    }

    @EnabledOnOs(OS.LINUX)
    @Test
    fun `should generate correct paths for Linux strategy`() {
        val strategy = PlatformLinuxSearchStrategy
        val tier1Paths = strategy.locations.flatMap { it.generatePaths(UtilityType.DESIGNER, null) }
        val tier2Candidates = strategy.locations.flatMap { it.generateCandidates(UtilityType.DESIGNER, "8.3.24") }
        assertNotNull(tier1Paths, "Tier 1 paths should not be null")
        assertNotNull(tier2Candidates, "Tier 2 candidates should not be null")
        assertTrue(tier1Paths.isNotEmpty(), "Tier 1 paths should not be empty due to PATH fallback")
        assertTrue(tier2Candidates.isNotEmpty(), "Tier 2 candidates should not be empty for enumerating locations")
    }

    @Test
    fun `should generate version-specific candidates correctly`() {
        val location =
            DirectoryEnumeratingLocation(
                basePath = "/opt/1cv8",
                relativeExecutableSubPath = "bin",
                dirNameToVersion = { dir -> dir },
            )
        val version = "8.3.24"
        val candidates = location.generateCandidates(UtilityType.DESIGNER, version)
        assertNotNull(candidates, "Candidates should not be null")
    }

    @Test
    fun `should generate paths for different utility types correctly`() {
        val standardLocation = StandardLocation("/opt/1cv8")
        val compilerPaths = standardLocation.generatePaths(UtilityType.DESIGNER, null)
        val ibcmdPaths = standardLocation.generatePaths(UtilityType.IBCMD, null)
        assertNotNull(compilerPaths, "Compiler paths should not be null")
        assertNotNull(ibcmdPaths, "IBCMD paths should not be null")
        assertTrue(compilerPaths.isNotEmpty(), "Compiler paths should not be empty")
        assertTrue(ibcmdPaths.isNotEmpty(), "IBCMD paths should not be empty")
    }

    @Test
    fun `should handle PATH environment location correctly`() {
        val pathLocation = PathEnvironmentLocation()
        val paths = pathLocation.generatePaths(UtilityType.DESIGNER, null)
        val candidates = pathLocation.generateCandidates(UtilityType.DESIGNER, null)
        assertNotNull(paths, "PATH paths should not be null")
        assertNotNull(candidates, "PATH candidates should not be null")
        if (System.getenv("PATH") != null) {
            assertTrue(paths.isNotEmpty(), "PATH paths should not be empty when PATH environment exists")
            assertTrue(candidates.isNotEmpty(), "PATH candidates should not be empty when PATH environment exists")
        } else {
            assertTrue(paths.isEmpty(), "PATH paths should be empty when PATH environment is null")
            assertTrue(candidates.isEmpty(), "PATH candidates should be empty when PATH environment is null")
        }
    }

    @Test
    fun `should generate correct number of paths for version location with version`() {
        val versionLocation = VersionLocation("/opt/1cv8")
        val version = "8.3.24"
        val paths = versionLocation.generatePaths(UtilityType.DESIGNER, version)
        assertEquals(1, paths.size, "Version location should generate one path")
    }

    @Test
    fun `should extract EDT version from PATH candidate for Windows user installation layout`(
        @TempDir tempDir: Path,
    ) {
        val edtDir = tempDir.resolve("1C_EDT 2025.2").resolve("1cedt")
        edtDir.createDirectories()
        edtDir.resolve("1cedtcli.exe").writeText("")
        val pathLocation = PathEnvironmentLocation(edtDir.toString())
        val candidates =
            pathLocation.generateCandidates(
                UtilityType.EDT_CLI,
                null,
            )

        val candidate =
            candidates.first {
                it.path.toString().replace('\\', '/').contains("1C_EDT 2025.2/1cedt/1cedtcli", ignoreCase = true)
            }

        assertEquals("2025.2", candidate.version)
        assertEquals(SearchCandidateSource.PATH, candidate.source)
    }

    @Test
    fun `search should return resolved EDT version from selected candidate`() {
        val javaBinary =
            Path.of(
                System.getProperty("java.home"),
                "bin",
                if (System.getProperty("os.name").lowercase().contains("windows")) "java.exe" else "java",
            )
        val strategy =
            object : SearchStrategy {
                override val locations: List<SearchLocation> =
                    listOf(
                        object : SearchLocation {
                            override fun generatePaths(
                                utility: UtilityType,
                                version: String?,
                            ): List<Path> = emptyList()

                            override fun generateCandidates(
                                utility: UtilityType,
                                version: String?,
                            ): List<SearchCandidate> =
                                listOf(
                                    SearchCandidate(
                                        path = javaBinary,
                                        version = "2025.2.1",
                                        source = SearchCandidateSource.PATH,
                                    ),
                                )
                        },
                    )
            }

        val location = strategy.search(UtilityType.EDT_CLI, "latest")

        assertEquals("2025.2.1", location.version)
    }
}
