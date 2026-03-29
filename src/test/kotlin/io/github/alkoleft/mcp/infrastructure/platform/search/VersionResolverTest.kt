package io.github.alkoleft.mcp.infrastructure.platform.search

import io.github.alkoleft.mcp.application.core.UtilityType
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class VersionResolverTest {
    private val resolver = DefaultVersionResolver()

    @Test
    fun `latest should choose newest version across all sources`() {
        val candidates =
            listOf(
                SearchCandidate(
                    path = Paths.get("C:/system/1cedtcli.exe"),
                    version = "2024.2.3+54",
                    source = SearchCandidateSource.SYSTEM_INSTALLATION,
                ),
                SearchCandidate(
                    path = Paths.get("F:/path/1cedtcli.exe"),
                    version = "2025.2",
                    source = SearchCandidateSource.PATH,
                ),
            )

        val selected = resolver.selectBest(candidates, "latest")

        assertNotNull(selected)
        assertEquals(Paths.get("F:/path/1cedtcli.exe"), selected.path)
    }

    @Test
    fun `latest should prefer PATH when same version exists in multiple sources`() {
        val candidates =
            listOf(
                SearchCandidate(
                    path = Paths.get("C:/system/1cedtcli.exe"),
                    version = "2025.2",
                    source = SearchCandidateSource.SYSTEM_INSTALLATION,
                ),
                SearchCandidate(
                    path = Paths.get("F:/path/1cedtcli.exe"),
                    version = "2025.2",
                    source = SearchCandidateSource.PATH,
                ),
            )

        val selected = resolver.selectBest(candidates, "latest")

        assertNotNull(selected)
        assertEquals(SearchCandidateSource.PATH, selected.source)
    }

    @Test
    fun `exact version should prefer PATH among matching candidates`() {
        val candidates =
            listOf(
                SearchCandidate(
                    path = Paths.get("C:/system/1cedtcli.exe"),
                    version = "2025.2",
                    source = SearchCandidateSource.SYSTEM_INSTALLATION,
                ),
                SearchCandidate(
                    path = Paths.get("F:/path/1cedtcli.exe"),
                    version = "2025.2",
                    source = SearchCandidateSource.PATH,
                ),
                SearchCandidate(
                    path = Paths.get("C:/older/1cedtcli.exe"),
                    version = "2024.2.3+54",
                    source = SearchCandidateSource.SYSTEM_INSTALLATION,
                ),
            )

        val selected = resolver.selectBest(candidates, "2025.2")

        assertNotNull(selected)
        assertEquals(Paths.get("F:/path/1cedtcli.exe"), selected.path)
    }

    @Test
    fun `exact version should return null when only PATH candidate has unknown version`() {
        val candidates =
            listOf(
                SearchCandidate(
                    path = Paths.get("F:/path/1cedtcli.exe"),
                    version = null,
                    source = SearchCandidateSource.PATH,
                ),
            )

        val selected = resolver.selectBest(candidates, "2025.2")

        assertEquals(null, selected)
    }

    @Test
    fun `windows EDT user installation should include 1cedt subdirectory`() {
        val location =
            DirectoryEnumeratingLocation(
                basePath = "C:/Users/Oleg/AppData/Local/1C/1cedtstart/installations",
                relativeExecutableSubPath = "1cedt",
                dirNameToVersion = { dir -> dir.substringAfter("1C_EDT ", "") },
                source = SearchCandidateSource.USER_INSTALLATION,
            )

        val candidates = location.generateCandidates(UtilityType.EDT_CLI, "2025.2")

        candidates.forEach {
            val normalized = it.path.toString().replace('\\', '/')
            assertEquals(true, normalized.contains("/1cedt/1cedtcli", ignoreCase = true))
        }
    }
}
