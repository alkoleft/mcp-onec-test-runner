package io.github.alkoleft.mcp.infrastructure.platform.search

import io.github.alkoleft.mcp.core.modules.PlatformType
import io.github.alkoleft.mcp.infrastructure.platform.detection.PlatformDetector
import kotlin.test.Test
import kotlin.test.assertTrue

class SearchStrategyFactoryTest {

    @Test
    fun `should create correct strategy for current platform`() {
        // Arrange
        val detector = PlatformDetector()
        val factory = SearchStrategyFactory(detector)

        // Act
        val strategy = factory.createSearchStrategy()

        // Assert
        when (detector.current) {
            PlatformType.WINDOWS -> assertTrue(strategy is WindowsSearchStrategy)
            PlatformType.LINUX, PlatformType.MACOS -> assertTrue(strategy is LinuxSearchStrategy)
        }
    }

    @Test
    fun `should create Windows strategy for Windows platform`() {
        // Arrange
        val detector = PlatformDetector()
        val factory = SearchStrategyFactory(detector)

        // Act & Assert
        if (detector.current == PlatformType.WINDOWS) {
            val strategy = factory.createSearchStrategy()
            assertTrue(strategy is WindowsSearchStrategy)
        }
    }

    @Test
    fun `should create Linux strategy for Linux platform`() {
        // Arrange
        val detector = PlatformDetector()
        val factory = SearchStrategyFactory(detector)

        // Act & Assert
        if (detector.current == PlatformType.LINUX) {
            val strategy = factory.createSearchStrategy()
            assertTrue(strategy is LinuxSearchStrategy)
        }
    }

    @Test
    fun `should create Linux strategy for MacOS platform`() {
        // Arrange
        val detector = PlatformDetector()
        val factory = SearchStrategyFactory(detector)

        // Act & Assert
        if (detector.current == PlatformType.MACOS) {
            val strategy = factory.createSearchStrategy()
            assertTrue(strategy is LinuxSearchStrategy)
        }
    }
} 