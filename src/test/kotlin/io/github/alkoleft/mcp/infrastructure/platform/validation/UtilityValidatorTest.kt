package io.github.alkoleft.mcp.infrastructure.platform.validation

import io.github.alkoleft.mcp.core.modules.PlatformType
import io.github.alkoleft.mcp.core.modules.UtilityLocation
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import java.nio.file.Paths

class UtilityValidatorTest {

    @Test
    fun `should validate utility with correct path`() {
        // Arrange
        val validator = UtilityValidator()
        val location = createTestLocation("/bin/echo") // Use echo which should work with /?

        // Act
        val result = runBlocking {
            validator.validateUtility(location)
        }

        // Assert
        // Should be true if /bin/echo exists and is executable
        assertTrue(result)
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
        assertFalse(result)
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
        assertFalse(result)
    }

    private fun createTestLocation(path: String): UtilityLocation {
        return UtilityLocation(
            executablePath = Paths.get(path),
            version = "8.3.24",
            platformType = PlatformType.LINUX
        )
    }
} 