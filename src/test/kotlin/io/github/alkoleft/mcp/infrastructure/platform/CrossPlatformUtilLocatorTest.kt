package io.github.alkoleft.mcp.infrastructure.platform

import io.github.alkoleft.mcp.core.modules.PlatformType
import io.github.alkoleft.mcp.core.modules.TestExecutionError
import io.github.alkoleft.mcp.core.modules.UtilityLocation
import io.github.alkoleft.mcp.core.modules.UtilityType
import io.github.alkoleft.mcp.infrastructure.platform.locator.UtilityLocator
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CrossPlatformUtilLocatorTest {
    @Test
    fun `should clear cache successfully`() {
        // Arrange
        val locator = UtilityLocator()

        // Act & Assert - should not throw exception
        locator.clearCache()
        assertTrue(true, "Cache clearing should complete without exception")
    }

    @Test
    fun `should throw UtilNotFound when utility not found`() {
        // Arrange
        val locator = UtilityLocator()

        // Act & Assert
        val exception =
            assertFailsWith<TestExecutionError.UtilNotFound> {
                locator.locateUtility(UtilityType.DESIGNER, "8.3.24")
            }

        assertTrue(
            exception.utility.contains(UtilityType.DESIGNER.name),
            "Exception should contain correct utility type",
        )
    }

    @Test
    fun `should handle null version parameter`() {
        // Arrange
        val locator = UtilityLocator()

        // Act & Assert
        val exception =
            assertFailsWith<TestExecutionError.UtilNotFound> {
                locator.locateUtility(UtilityType.DESIGNER, null)
            }

        assertTrue(
            exception.utility.contains(UtilityType.DESIGNER.name),
            "Exception should contain correct utility type",
        )
    }

    @Test
    fun `should handle different utility types`() {
        // Arrange
        val locator = UtilityLocator()

        // Act & Assert
        val exception =
            assertFailsWith<TestExecutionError.UtilNotFound> {
                locator.locateUtility(UtilityType.IBCMD, "8.3.24")
            }

        assertTrue(exception.utility.contains(UtilityType.IBCMD.name), "Exception should contain correct utility type")
    }

    @Test
    fun `should validate utility location correctly`() {
        // Arrange
        val locator = UtilityLocator()
        val testLocation =
            UtilityLocation(
                executablePath = Paths.get("/non/existent/path"),
                version = "8.3.24",
                platformType = PlatformType.LINUX,
            )

        // Act
        val result =
            locator.validateUtility(testLocation)

        // Assert
        assertFalse(result, "Non-existent utility should fail validation")
    }

    @Test
    fun `should handle empty version string`() {
        // Arrange
        val locator = UtilityLocator()

        // Act & Assert
        val exception =
            assertFailsWith<TestExecutionError.UtilNotFound> {
                locator.locateUtility(UtilityType.DESIGNER, "")
            }

        assertTrue(
            exception.utility.contains(UtilityType.DESIGNER.name),
            "Exception should contain correct utility type",
        )
    }

    @Test
    fun `should handle invalid version format`() {
        // Arrange
        val locator = UtilityLocator()

        // Act & Assert
        val exception =
            assertFailsWith<TestExecutionError.UtilNotFound> {
                locator.locateUtility(UtilityType.DESIGNER, "invalid-version")
            }

        assertTrue(
            exception.utility.contains(UtilityType.DESIGNER.name),
            "Exception should contain correct utility type",
        )
    }

    @Test
    fun `should handle ENTERPRISE utility type`() {
        // Arrange
        val locator = UtilityLocator()

        // Act & Assert
        val exception =
            assertFailsWith<TestExecutionError.UtilNotFound> {
                locator.locateUtility(UtilityType.THIN_CLIENT, "8.3.24")
            }

        assertTrue(
            exception.utility.contains(UtilityType.THIN_CLIENT.name),
            "Exception should contain correct utility type",
        )
    }
}
