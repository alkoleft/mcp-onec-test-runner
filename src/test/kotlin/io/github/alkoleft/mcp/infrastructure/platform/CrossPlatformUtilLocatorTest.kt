package io.github.alkoleft.mcp.infrastructure.platform

import io.github.alkoleft.mcp.core.modules.PlatformType
import io.github.alkoleft.mcp.core.modules.TestExecutionError
import io.github.alkoleft.mcp.core.modules.UtilityType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlinx.coroutines.runBlocking

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
                locator.locateUtility(UtilityType.COMPILER_1CV8C, "8.3.24")
            }
        }

        assertTrue(exception.utility.contains(UtilityType.COMPILER_1CV8C.name), "Exception should contain correct utility type")
    }

    @Test
    fun `should handle null version parameter`() {
        // Arrange
        val locator = CrossPlatformUtilLocator()

        // Act & Assert
        val exception = assertFailsWith<TestExecutionError.UtilNotFound> {
            runBlocking {
                locator.locateUtility(UtilityType.COMPILER_1CV8C, null)
            }
        }

        assertTrue(exception.utility.contains(UtilityType.COMPILER_1CV8C.name), "Exception should contain correct utility type")
    }

    @Test
    fun `should handle different utility types`() {
        // Arrange
        val locator = CrossPlatformUtilLocator()

        // Act & Assert
        val exception = assertFailsWith<TestExecutionError.UtilNotFound> {
            runBlocking {
                locator.locateUtility(UtilityType.INFOBASE_MANAGER_IBCMD, "8.3.24")
            }
        }

        assertTrue(exception.utility.contains(UtilityType.INFOBASE_MANAGER_IBCMD.name), "Exception should contain correct utility type")
    }

    @Test
    fun `should validate utility location correctly`() {
        // Arrange
        val locator = CrossPlatformUtilLocator()
        val testLocation = io.github.alkoleft.mcp.core.modules.UtilityLocation(
            executablePath = java.nio.file.Paths.get("/non/existent/path"),
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
                locator.locateUtility(UtilityType.COMPILER_1CV8C, "")
            }
        }

        assertTrue(exception.utility.contains(UtilityType.COMPILER_1CV8C.name), "Exception should contain correct utility type")
    }

    @Test
    fun `should handle invalid version format`() {
        // Arrange
        val locator = CrossPlatformUtilLocator()

        // Act & Assert
        val exception = assertFailsWith<TestExecutionError.UtilNotFound> {
            runBlocking {
                locator.locateUtility(UtilityType.COMPILER_1CV8C, "invalid-version")
            }
        }

        assertTrue(exception.utility.contains(UtilityType.COMPILER_1CV8C.name), "Exception should contain correct utility type")
    }
} 