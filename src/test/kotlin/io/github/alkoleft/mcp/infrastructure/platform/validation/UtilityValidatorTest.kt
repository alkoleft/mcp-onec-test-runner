package io.github.alkoleft.mcp.infrastructure.platform.validation

import io.github.alkoleft.mcp.core.modules.PlatformType
import io.github.alkoleft.mcp.core.modules.UtilityLocation
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
import kotlinx.coroutines.runBlocking
import java.nio.file.Paths

class UtilityValidatorTest {

    @Test
    fun `should validate utility with correct path successfully`() {
        // Arrange
        val validator = UtilityValidator()
        val location = createTestLocation("/bin/echo") // Use echo which should work with /?

        // Act
        val result = runBlocking {
            validator.validateUtility(location)
        }

        // Assert
        assertTrue(result, "Valid utility path should be validated successfully")
    }

    @Test
    fun `should fail validation for non-existent path`() {
        // Arrange
        val validator = UtilityValidator()
        val location = createTestLocation("/non/existent/path")

        // Act
        val result = runBlocking {
            validator.validateUtility(location)
        }

        // Assert
        assertFalse(result, "Non-existent path should fail validation")
    }

    @Test
    fun `should fail validation for non-executable file`() {
        // Arrange
        val validator = UtilityValidator()
        val location = createTestLocation("/etc/passwd") // Usually exists but not executable

        // Act
        val result = runBlocking {
            validator.validateUtility(location)
        }

        // Assert
        assertFalse(result, "Non-executable file should fail validation")
    }

    @Test
    fun `should handle null path gracefully`() {
        // Arrange
        val validator = UtilityValidator()
        val location = createTestLocation(null)

        // Act
        val result = runBlocking {
            validator.validateUtility(location)
        }

        // Assert
        assertFalse(result, "Null path should fail validation")
    }

    @Test
    fun `should validate utility with different platform types`() {
        // Arrange
        val validator = UtilityValidator()
        val linuxLocation = createTestLocation("/bin/echo", PlatformType.LINUX)
        val windowsLocation = createTestLocation("/bin/echo", PlatformType.WINDOWS)

        // Act
        val linuxResult = runBlocking {
            validator.validateUtility(linuxLocation)
        }
        val windowsResult = runBlocking {
            validator.validateUtility(windowsLocation)
        }

        // Assert
        assertTrue(linuxResult, "Linux platform utility should be validated")
        assertTrue(windowsResult, "Windows platform utility should be validated")
    }

    @Test
    fun `should handle utility with different versions`() {
        // Arrange
        val validator = UtilityValidator()
        val location1 = createTestLocation("/bin/echo", version = "8.3.24")
        val location2 = createTestLocation("/bin/echo", version = "8.3.25")

        // Act
        val result1 = runBlocking {
            validator.validateUtility(location1)
        }
        val result2 = runBlocking {
            validator.validateUtility(location2)
        }

        // Assert
        assertTrue(result1, "Utility with version 8.3.24 should be validated")
        assertTrue(result2, "Utility with version 8.3.25 should be validated")
    }

    @Test
    fun `should validate utility with null version`() {
        // Arrange
        val validator = UtilityValidator()
        val location = createTestLocation("/bin/echo", version = null)

        // Act
        val result = runBlocking {
            validator.validateUtility(location)
        }

        // Assert
        assertTrue(result, "Utility with null version should be validated")
    }

    @Test
    fun `should handle directory path correctly`() {
        // Arrange
        val validator = UtilityValidator()
        val location = createTestLocation("/tmp") // Directory, not executable file

        // Act
        val result = runBlocking {
            validator.validateUtility(location)
        }

        // Assert
        assertFalse(result, "Directory path should fail validation")
    }

    private fun createTestLocation(path: String?): UtilityLocation {
        return UtilityLocation(
            executablePath = if (path != null) Paths.get(path) else Paths.get(""),
            version = "8.3.24",
            platformType = PlatformType.LINUX
        )
    }

    private fun createTestLocation(path: String, platformType: PlatformType): UtilityLocation {
        return UtilityLocation(
            executablePath = Paths.get(path),
            version = "8.3.24",
            platformType = platformType
        )
    }

    private fun createTestLocation(path: String, version: String?): UtilityLocation {
        return UtilityLocation(
            executablePath = Paths.get(path),
            version = version,
            platformType = PlatformType.LINUX
        )
    }
} 