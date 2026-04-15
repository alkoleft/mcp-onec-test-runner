package io.github.alkoleft.mcp.infrastructure.platform.search

import io.github.alkoleft.mcp.application.core.UtilityType
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class VersionResolverTest {
    private val resolver: DefaultVersionResolver = DefaultVersionResolver()

    @Test
    fun `latest should choose newest version across all sources`(): Unit {
        val inputCandidates: List<SearchCandidate> =
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
        val expectedPath: Path = Paths.get("F:/path/1cedtcli.exe")

        val actualSelected: SearchCandidate? = resolver.selectBest(inputCandidates, "latest")

        assertNotNull(actualSelected)
        assertEquals(expectedPath, actualSelected.path)
    }

    @Test
    fun `latest should prefer PATH when same version exists in multiple sources`(): Unit {
        val inputCandidates: List<SearchCandidate> =
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
        val expectedSource: SearchCandidateSource = SearchCandidateSource.PATH

        val actualSelected: SearchCandidate? = resolver.selectBest(inputCandidates, "latest")

        assertNotNull(actualSelected)
        assertEquals(expectedSource, actualSelected.source)
    }

    @Test
    fun `exact version should prefer PATH among matching candidates`(): Unit {
        val inputCandidates: List<SearchCandidate> =
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
        val expectedPath: Path = Paths.get("F:/path/1cedtcli.exe")

        val actualSelected: SearchCandidate? = resolver.selectBest(inputCandidates, "2025.2")

        assertNotNull(actualSelected)
        assertEquals(expectedPath, actualSelected.path)
    }

    @Test
    fun `exact version should return null when only PATH candidate has unknown version`(): Unit {
        val inputCandidates: List<SearchCandidate> =
            listOf(
                SearchCandidate(
                    path = Paths.get("F:/path/1cedtcli.exe"),
                    version = null,
                    source = SearchCandidateSource.PATH,
                ),
            )

        val actualSelected: SearchCandidate? = resolver.selectBest(inputCandidates, "2025.2")

        assertEquals(null, actualSelected)
    }

    @Test
    fun `windows EDT user installation should include 1cedt subdirectory`(
        @TempDir tempDir: Path,
    ): Unit {
        val inputInstallationDir: Path = tempDir.resolve("1C_EDT 2025.2").resolve("1cedt")
        inputInstallationDir.createDirectories()
        inputInstallationDir.resolve("1cedtcli.exe").writeText("")
        val mockLocation: DirectoryEnumeratingLocation =
            DirectoryEnumeratingLocation(
                basePath = tempDir.toString(),
                relativeExecutableSubPath = "1cedt",
                dirNameToVersion = { dir -> dir.substringAfter("1C_EDT ", "") },
                source = SearchCandidateSource.USER_INSTALLATION,
            )

        val inputCandidates: List<SearchCandidate> = mockLocation.generateCandidates(UtilityType.EDT_CLI, "2025.2")

        assertTrue(inputCandidates.isNotEmpty())
        inputCandidates.forEach {
            val actualNormalized: String = it.path.toString().replace('\\', '/')
            assertEquals(true, actualNormalized.contains("/1cedt/1cedtcli", ignoreCase = true))
        }
    }

    @Test
    fun `exact version should match normalized EDT system candidate version`(): Unit {
        val inputCandidates: List<SearchCandidate> =
            listOf(
                SearchCandidate(
                    path = Paths.get("C:/system/1cedtcli.exe"),
                    version = "2025.2",
                    source = SearchCandidateSource.SYSTEM_INSTALLATION,
                ),
            )
        val expectedVersion: String = "2025.2"

        val actualSelected: SearchCandidate? = resolver.selectBest(inputCandidates, "2025.2")

        assertNotNull(actualSelected)
        assertEquals(expectedVersion, actualSelected.version)
    }
}
