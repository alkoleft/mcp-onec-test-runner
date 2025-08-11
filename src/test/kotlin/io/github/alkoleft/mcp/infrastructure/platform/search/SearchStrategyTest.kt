package io.github.alkoleft.mcp.infrastructure.platform.search

import io.github.alkoleft.mcp.core.modules.UtilityType
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
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
        val location = DirectoryEnumeratingLocation("/opt/1cv8", "bin") { it }
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
        assertNotNull(paths, "PATH paths should not be null")
        if (System.getenv("PATH") != null) {
            assertTrue(paths.isNotEmpty(), "PATH paths should not be empty when PATH environment exists")
        } else {
            assertTrue(paths.isEmpty(), "PATH paths should be empty when PATH environment is null")
        }
    }

    @Test
    fun `should generate correct number of paths for version location with version`() {
        val versionLocation = VersionLocation("/opt/1cv8")
        val version = "8.3.24"
        val paths = versionLocation.generatePaths(UtilityType.DESIGNER, version)
        assertEquals(1, paths.size, "Version location should generate one path")
    }
}
