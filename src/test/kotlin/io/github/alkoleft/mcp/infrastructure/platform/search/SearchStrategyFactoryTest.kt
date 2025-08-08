package io.github.alkoleft.mcp.infrastructure.platform.search

import io.github.alkoleft.mcp.core.modules.PlatformType
import io.github.alkoleft.mcp.infrastructure.utility.PlatformDetector
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SearchStrategyFactoryTest {

    @Test
    fun `should create correct strategy for current platform`() {
        // Arrange
        val factory = SearchStrategyFactory()

        // Act
        val strategy = factory.createSearchStrategy()

        // Assert
        assertNotNull(strategy, "Strategy should not be null")
        when (PlatformDetector.current) {
            PlatformType.WINDOWS -> {
                assertTrue(strategy is WindowsSearchStrategy, "Should create WindowsSearchStrategy for Windows platform")
            }
            PlatformType.LINUX, PlatformType.MACOS -> {
                assertTrue(strategy is LinuxSearchStrategy, "Should create LinuxSearchStrategy for Linux/MacOS platform")
            }
        }
    }

    @Test
    fun `should create Windows strategy for Windows platform`() {
        // Arrange
        val factory = SearchStrategyFactory()

        // Act & Assert
        if (PlatformDetector.current == PlatformType.WINDOWS) {
            val strategy = factory.createSearchStrategy()
            assertTrue(strategy is WindowsSearchStrategy, "Should create WindowsSearchStrategy for Windows platform")
        } else {
            // Skip test if not on Windows platform
            assertTrue(true, "Test skipped - not on Windows platform")
        }
    }

    @Test
    fun `should create Linux strategy for Linux platform`() {
        // Arrange
        val factory = SearchStrategyFactory()

        // Act & Assert
        if (PlatformDetector.current == PlatformType.LINUX) {
            val strategy = factory.createSearchStrategy()
            assertTrue(strategy is LinuxSearchStrategy, "Should create LinuxSearchStrategy for Linux platform")
        } else {
            // Skip test if not on Linux platform
            assertTrue(true, "Test skipped - not on Linux platform")
        }
    }

    @Test
    fun `should create Linux strategy for MacOS platform`() {
        // Arrange
        val factory = SearchStrategyFactory()

        // Act & Assert
        if (PlatformDetector.current == PlatformType.MACOS) {
            val strategy = factory.createSearchStrategy()
            assertTrue(strategy is LinuxSearchStrategy, "Should create LinuxSearchStrategy for MacOS platform")
        } else {
            // Skip test if not on MacOS platform
            assertTrue(true, "Test skipped - not on MacOS platform")
        }
    }

    @Test
    fun `should create strategy with correct tier structure`() {
        // Arrange
        val factory = SearchStrategyFactory()

        // Act
        val strategy = factory.createSearchStrategy()

        // Assert
        assertNotNull(strategy.locations, "Tier 1 locations should not be null")

        assertTrue(strategy.locations.isNotEmpty(), "Tier 1 locations should not be empty")
    }

    @Test
    fun `should create consistent strategies for same platform`() {
        // Arrange
        val factory1 = SearchStrategyFactory()
        val factory2 = SearchStrategyFactory()

        // Act
        val strategy1 = factory1.createSearchStrategy()
        val strategy2 = factory2.createSearchStrategy()

        // Assert
        assertEquals(
            strategy1.javaClass,
            strategy2.javaClass,
            "Strategies for same platform should be of same type"
        )
    }

    @Test
    fun `should handle all supported platform types`() {
        // Arrange
        val factory = SearchStrategyFactory()

        // Act
        val strategy = factory.createSearchStrategy()

        // Assert
        assertNotNull(strategy, "Strategy should be created for any supported platform")
        assertTrue(
            strategy is WindowsSearchStrategy || strategy is LinuxSearchStrategy,
            "Strategy should be either WindowsSearchStrategy or LinuxSearchStrategy"
        )
    }
} 