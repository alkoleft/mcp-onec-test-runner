package io.github.alkoleft.mcp.infrastructure.platform.search

import io.github.alkoleft.mcp.core.modules.UtilityType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class SearchStrategyTest {

    @Test
    fun `should generate correct paths for Windows strategy`() {
        // Arrange
        val strategy = WindowsSearchStrategy()

        // Act
        val tier1Paths = strategy.tier1Locations.flatMap { it.generatePaths(UtilityType.COMPILER_1CV8C, null) }
        val tier2Paths = strategy.tier2Locations.flatMap { it.generatePaths(UtilityType.COMPILER_1CV8C, "8.3.24") }

        // Assert
        assertNotNull(tier1Paths, "Tier 1 paths should not be null")
        assertNotNull(tier2Paths, "Tier 2 paths should not be null")
        assertTrue(tier1Paths.isNotEmpty(), "Tier 1 paths should not be empty")
        assertTrue(tier2Paths.isNotEmpty(), "Tier 2 paths should not be empty")
        
        // Check that paths contain expected Windows paths
        assertTrue(
            tier1Paths.any { it.toString().contains("C:\\Program Files\\1cv8") },
            "Tier 1 should contain Program Files path"
        )
        assertTrue(
            tier1Paths.any { it.toString().contains("C:\\Program Files (x86)\\1cv8") },
            "Tier 1 should contain Program Files (x86) path"
        )
        assertTrue(
            tier1Paths.any { it.toString().contains("1cv8c") },
            "Tier 1 should contain 1cv8c executable"
        )
    }

    @Test
    fun `should generate correct paths for Linux strategy`() {
        // Arrange
        val strategy = LinuxSearchStrategy()

        // Act
        val tier1Paths = strategy.tier1Locations.flatMap { it.generatePaths(UtilityType.COMPILER_1CV8C, null) }
        val tier2Paths = strategy.tier2Locations.flatMap { it.generatePaths(UtilityType.COMPILER_1CV8C, "8.3.24") }

        // Assert
        assertNotNull(tier1Paths, "Tier 1 paths should not be null")
        assertNotNull(tier2Paths, "Tier 2 paths should not be null")
        assertTrue(tier1Paths.isNotEmpty(), "Tier 1 paths should not be empty")
        assertTrue(tier2Paths.isNotEmpty(), "Tier 2 paths should not be empty")
        
        // Check that paths contain expected Linux paths
        assertTrue(
            tier1Paths.any { it.toString().contains("/opt/1cv8") },
            "Tier 1 should contain /opt/1cv8 path"
        )
        assertTrue(
            tier1Paths.any { it.toString().contains("/usr/local/1cv8") },
            "Tier 1 should contain /usr/local/1cv8 path"
        )
        assertTrue(
            tier1Paths.any { it.toString().contains("1cv8c") && !it.toString().contains(".exe") },
            "Tier 1 should contain 1cv8c executable without .exe extension"
        )
    }

    @Test
    fun `should generate version-specific paths correctly`() {
        // Arrange
        val versionLocation = VersionLocation("/opt/1cv8")
        val version = "8.3.24"

        // Act
        val paths = versionLocation.generatePaths(UtilityType.COMPILER_1CV8C, version)

        // Assert
        assertNotNull(paths, "Paths should not be null")
        assertTrue(paths.isNotEmpty(), "Paths should not be empty")
        assertTrue(
            paths.any { it.toString().contains(version) },
            "Paths should contain the specified version"
        )
        assertTrue(
            paths.any { it.toString().contains("8.3.24") },
            "Paths should contain exact version string"
        )
        assertTrue(
            paths.any { it.toString().contains("8.3") },
            "Paths should contain major.minor version"
        )
    }

    @Test
    fun `should generate paths for different utility types correctly`() {
        // Arrange
        val standardLocation = StandardLocation("/opt/1cv8")

        // Act
        val compilerPaths = standardLocation.generatePaths(UtilityType.COMPILER_1CV8C, null)
        val ibcmdPaths = standardLocation.generatePaths(UtilityType.INFOBASE_MANAGER_IBCMD, null)

        // Assert
        assertNotNull(compilerPaths, "Compiler paths should not be null")
        assertNotNull(ibcmdPaths, "IBCMD paths should not be null")
        assertTrue(compilerPaths.isNotEmpty(), "Compiler paths should not be empty")
        assertTrue(ibcmdPaths.isNotEmpty(), "IBCMD paths should not be empty")
        assertTrue(
            compilerPaths.any { it.toString().contains("1cv8c") },
            "Compiler paths should contain 1cv8c executable"
        )
        assertTrue(
            ibcmdPaths.any { it.toString().contains("ibcmd") },
            "IBCMD paths should contain ibcmd executable"
        )
    }

    @Test
    fun `should handle PATH environment location correctly`() {
        // Arrange
        val pathLocation = PathEnvironmentLocation()

        // Act
        val paths = pathLocation.generatePaths(UtilityType.COMPILER_1CV8C, null)

        // Assert
        assertNotNull(paths, "PATH paths should not be null")
        // PATH environment should be available and contain paths, or empty if PATH is null
        if (System.getenv("PATH") != null) {
            assertTrue(paths.isNotEmpty(), "PATH paths should not be empty when PATH environment exists")
        } else {
            assertTrue(paths.isEmpty(), "PATH paths should be empty when PATH environment is null")
        }
    }

    @Test
    fun `should generate correct number of paths for standard location`() {
        // Arrange
        val standardLocation = StandardLocation("/opt/1cv8")

        // Act
        val paths = standardLocation.generatePaths(UtilityType.COMPILER_1CV8C, null)

        // Assert
        assertEquals(2, paths.size, "Standard location should generate exactly 2 paths")
        assertTrue(
            paths.any { it.toString().contains("/opt/1cv8/bin/") },
            "Should contain bin directory path"
        )
        assertTrue(
            paths.any { it.toString().contains("/opt/1cv8/1cv8c") },
            "Should contain direct executable path"
        )
    }

    @Test
    fun `should generate correct number of paths for version location with version`() {
        // Arrange
        val versionLocation = VersionLocation("/opt/1cv8")
        val version = "8.3.24"

        // Act
        val paths = versionLocation.generatePaths(UtilityType.COMPILER_1CV8C, version)

        // Assert
        assertTrue(paths.size >= 3, "Version location should generate at least 3 paths")
        assertTrue(
            paths.any { it.toString().contains("/opt/1cv8/8.3.24/bin/") },
            "Should contain specific version path"
        )
        assertTrue(
            paths.any { it.toString().contains("/opt/1cv8/8.3.24/bin/1cv8c") },
            "Should contain specific version executable path"
        )
    }

    @Test
    fun `should generate correct number of paths for version location without version`() {
        // Arrange
        val versionLocation = VersionLocation("/opt/1cv8")

        // Act
        val paths = versionLocation.generatePaths(UtilityType.COMPILER_1CV8C, null)

        // Assert
        assertEquals(2, paths.size, "Version location without version should generate exactly 2 paths")
        assertTrue(
            paths.any { it.toString().contains("/opt/1cv8/8.3.24/bin/") },
            "Should contain default version path"
        )
        assertTrue(
            paths.any { it.toString().contains("/opt/1cv8/8.3/bin/") },
            "Should contain major.minor version path"
        )
    }
} 