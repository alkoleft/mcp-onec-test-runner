package io.github.alkoleft.mcp.infrastructure.platform.detection

import io.github.alkoleft.mcp.core.modules.PlatformType
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertContains
import kotlin.test.assertEquals

class PlatformDetectorTest {

    @Test
    fun `should detect current platform correctly`() {
        // Arrange
        val detector = PlatformDetector()

        // Act
        val platform = detector.current

        // Assert
        assertNotNull(platform, "Platform should not be null")
        assertContains(
            listOf(PlatformType.WINDOWS, PlatformType.LINUX, PlatformType.MACOS),
            platform,
            "Platform should be one of the supported types"
        )
    }

    @Test
    fun `should provide correct platform-specific helper methods`() {
        // Arrange
        val detector = PlatformDetector()

        // Act & Assert
        when (detector.current) {
            PlatformType.WINDOWS -> {
                assertEquals(true, detector.isWindows(), "isWindows() should return true for Windows platform")
                assertEquals(false, detector.isLinux(), "isLinux() should return false for Windows platform")
                assertEquals(false, detector.isMacOS(), "isMacOS() should return false for Windows platform")
            }
            PlatformType.LINUX -> {
                assertEquals(false, detector.isWindows(), "isWindows() should return false for Linux platform")
                assertEquals(true, detector.isLinux(), "isLinux() should return true for Linux platform")
                assertEquals(false, detector.isMacOS(), "isMacOS() should return false for Linux platform")
            }
            PlatformType.MACOS -> {
                assertEquals(false, detector.isWindows(), "isWindows() should return false for MacOS platform")
                assertEquals(false, detector.isLinux(), "isLinux() should return false for MacOS platform")
                assertEquals(true, detector.isMacOS(), "isMacOS() should return true for MacOS platform")
            }
        }
    }

    @Test
    fun `should have consistent platform detection`() {
        // Arrange
        val detector1 = PlatformDetector()
        val detector2 = PlatformDetector()

        // Act
        val platform1 = detector1.current
        val platform2 = detector2.current

        // Assert
        assertEquals(platform1, platform2, "Multiple detector instances should return the same platform")
    }

    @Test
    fun `should return correct platform type enum`() {
        // Arrange
        val detector = PlatformDetector()

        // Act
        val platform = detector.current

        // Assert
        assertNotNull(platform, "Platform should not be null")
        when (platform) {
            PlatformType.WINDOWS -> {
                assertEquals("WINDOWS", platform.name, "Platform name should be WINDOWS")
            }
            PlatformType.LINUX -> {
                assertEquals("LINUX", platform.name, "Platform name should be LINUX")
            }
            PlatformType.MACOS -> {
                assertEquals("MACOS", platform.name, "Platform name should be MACOS")
            }
        }
    }
} 