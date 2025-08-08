package io.github.alkoleft.mcp.infrastructure.platform

import io.github.alkoleft.mcp.core.modules.PlatformType
import io.github.alkoleft.mcp.core.modules.TestExecutionError
import io.github.alkoleft.mcp.core.modules.UtilityLocation
import io.github.alkoleft.mcp.core.modules.UtilityType
import kotlinx.coroutines.runBlocking
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CrossPlatformUtilLocatorTest {

    @Test
    fun `should clear cache successfully`() {
        // Arrange
        val locator = CrossPlatformUtilLocator()

        // Act & Assert - should not throw exception
        locator.clearCache()
        assertTrue(true, "Cache clearing should complete without exception")
    }

    @Test
    fun `should detect current platform correctly`() {
        // Arrange
        val locator = CrossPlatformUtilLocator()

        // Act
        val platformDetector = locator.javaClass.getDeclaredField("platformDetector")
            .apply { isAccessible = true }
            .get(locator) as io.github.alkoleft.mcp.infrastructure.platform.detection.PlatformDetector
        val platform = platformDetector.current

        // Assert
        assertNotNull(platform, "Platform should not be null")
        assertTrue(
            platform in listOf(PlatformType.WINDOWS, PlatformType.LINUX, PlatformType.MACOS),
            "Platform should be one of the supported types"
        )
    }

    @Test
    fun `should have all required components initialized`() {
        // Arrange
        val locator = CrossPlatformUtilLocator()

        // Act & Assert - check that all components are initialized
        val pathCache = locator.javaClass.getDeclaredField("pathCache").apply { isAccessible = true }.get(locator)
        val platformDetector = locator.javaClass.getDeclaredField("platformDetector").apply { isAccessible = true }.get(locator)
        val searchStrategyFactory = locator.javaClass.getDeclaredField("searchStrategyFactory").apply { isAccessible = true }.get(locator)
        val utilityValidator = locator.javaClass.getDeclaredField("utilityValidator").apply { isAccessible = true }.get(locator)
        val versionExtractor = locator.javaClass.getDeclaredField("versionExtractor").apply { isAccessible = true }.get(locator)

        assertNotNull(pathCache, "Path cache should be initialized")
        assertNotNull(platformDetector, "Platform detector should be initialized")
        assertNotNull(searchStrategyFactory, "Search strategy factory should be initialized")
        assertNotNull(utilityValidator, "Utility validator should be initialized")
        assertNotNull(versionExtractor, "Version extractor should be initialized")
    }

    @Test
    fun `should throw UtilNotFound when utility not found`() {
        // Arrange
        val locator = CrossPlatformUtilLocator()

        // Act & Assert
        val exception = assertFailsWith<TestExecutionError.UtilNotFound> {
            runBlocking {
                locator.locateUtility(UtilityType.DESIGNER, "8.3.24")
            }
        }

        assertTrue(exception.utility.contains(UtilityType.DESIGNER.name), "Exception should contain correct utility type")
    }

    @Test
    fun `should handle null version parameter`() {
        // Arrange
        val locator = CrossPlatformUtilLocator()

        // Act & Assert
        val exception = assertFailsWith<TestExecutionError.UtilNotFound> {
            runBlocking {
                locator.locateUtility(UtilityType.DESIGNER, null)
            }
        }

        assertTrue(exception.utility.contains(UtilityType.DESIGNER.name), "Exception should contain correct utility type")
    }

    @Test
    fun `should handle different utility types`() {
        // Arrange
        val locator = CrossPlatformUtilLocator()

        // Act & Assert
        val exception = assertFailsWith<TestExecutionError.UtilNotFound> {
            runBlocking {
                locator.locateUtility(UtilityType.IBCMD, "8.3.24")
            }
        }

        assertTrue(exception.utility.contains(UtilityType.IBCMD.name), "Exception should contain correct utility type")
    }

    @Test
    fun `should validate utility location correctly`() {
        // Arrange
        val locator = CrossPlatformUtilLocator()
        val testLocation = UtilityLocation(
            executablePath = Paths.get("/non/existent/path"),
            version = "8.3.24",
            platformType = PlatformType.LINUX
        )

        // Act
        val result = runBlocking {
            locator.validateUtility(testLocation)
        }

        // Assert
        assertFalse(result, "Non-existent utility should fail validation")
    }

    @Test
    fun `should handle empty version string`() {
        // Arrange
        val locator = CrossPlatformUtilLocator()

        // Act & Assert
        val exception = assertFailsWith<TestExecutionError.UtilNotFound> {
            runBlocking {
                locator.locateUtility(UtilityType.DESIGNER, "")
            }
        }

        assertTrue(exception.utility.contains(UtilityType.DESIGNER.name), "Exception should contain correct utility type")
    }

    @Test
    fun `should handle invalid version format`() {
        // Arrange
        val locator = CrossPlatformUtilLocator()

        // Act & Assert
        val exception = assertFailsWith<TestExecutionError.UtilNotFound> {
            runBlocking {
                locator.locateUtility(UtilityType.DESIGNER, "invalid-version")
            }
        }

        assertTrue(exception.utility.contains(UtilityType.DESIGNER.name), "Exception should contain correct utility type")
    }

    @Test
    fun `should handle ENTERPRISE utility type`() {
        // Arrange
        val locator = CrossPlatformUtilLocator()

        // Act & Assert
        val exception = assertFailsWith<TestExecutionError.UtilNotFound> {
            runBlocking {
                locator.locateUtility(UtilityType.THIN_CLIENT, "8.3.24")
            }
        }

        assertTrue(
            exception.utility.contains(UtilityType.THIN_CLIENT.name),
            "Exception should contain correct utility type"
        )
    }
} 