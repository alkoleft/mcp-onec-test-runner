package io.github.alkoleft.mcp.infrastructure.platform

import io.github.alkoleft.mcp.core.modules.PlatformType
import io.github.alkoleft.mcp.core.modules.TestExecutionError
import io.github.alkoleft.mcp.core.modules.UtilityType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class CrossPlatformUtilLocatorTest {

    @Test
    fun `should clear cache successfully`() {
        // Arrange
        val locator = CrossPlatformUtilLocator()

        // Act & Assert - should not throw exception
        locator.clearCache()
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
        assertTrue(platform in listOf(PlatformType.WINDOWS, PlatformType.LINUX, PlatformType.MACOS))
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

        assertTrue(pathCache != null)
        assertTrue(platformDetector != null)
        assertTrue(searchStrategyFactory != null)
        assertTrue(utilityValidator != null)
        assertTrue(versionExtractor != null)
    }

    @Test
    fun `should throw UtilNotFound when utility not found`() {
        // Arrange
        val locator = CrossPlatformUtilLocator()

        // Act & Assert
        assertFailsWith<TestExecutionError.UtilNotFound> {
            runBlocking {
                locator.locateUtility(UtilityType.COMPILER_1CV8C, "8.3.24")
            }
        }
    }

    @Test
    fun `should handle null version parameter`() {
        // Arrange
        val locator = CrossPlatformUtilLocator()

        // Act & Assert
        assertFailsWith<TestExecutionError.UtilNotFound> {
            runBlocking {
                locator.locateUtility(UtilityType.COMPILER_1CV8C, null)
            }
        }
    }

    @Test
    fun `should handle different utility types`() {
        // Arrange
        val locator = CrossPlatformUtilLocator()

        // Act & Assert
        assertFailsWith<TestExecutionError.UtilNotFound> {
            runBlocking {
                locator.locateUtility(UtilityType.INFOBASE_MANAGER_IBCMD, "8.3.24")
            }
        }
    }
} 