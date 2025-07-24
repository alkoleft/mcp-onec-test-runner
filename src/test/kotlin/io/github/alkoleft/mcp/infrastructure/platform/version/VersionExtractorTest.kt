package io.github.alkoleft.mcp.infrastructure.platform.version

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VersionExtractorTest {

    @Test
    fun `should check exact version compatibility correctly`() {
        // Arrange
        val extractor = VersionExtractor()
        val version1 = "8.3.24.1234"
        val version2 = "8.3.24.1234"

        // Act
        val isCompatible = extractor.isVersionCompatible(version1, version2)

        // Assert
        assertTrue(isCompatible, "Exact version match should be compatible")
    }

    @Test
    fun `should check major minor version compatibility correctly`() {
        // Arrange
        val extractor = VersionExtractor()
        val version1 = "8.3.24.1234"
        val version2 = "8.3.25.5678"

        // Act
        val isCompatible = extractor.isVersionCompatible(version1, version2)

        // Assert
        assertTrue(isCompatible, "Major.minor compatibility should be true for 8.3.x.x versions")
    }

    @Test
    fun `should check major minor version compatibility in reverse order`() {
        // Arrange
        val extractor = VersionExtractor()
        val version1 = "8.3.25.5678"
        val version2 = "8.3.24.1234"

        // Act
        val isCompatible = extractor.isVersionCompatible(version1, version2)

        // Assert
        assertTrue(isCompatible, "Major.minor compatibility should work in both directions")
    }

    @Test
    fun `should reject incompatible major versions`() {
        // Arrange
        val extractor = VersionExtractor()
        val version1 = "8.3.24.1234"
        val version2 = "8.4.0.0"

        // Act
        val isCompatible = extractor.isVersionCompatible(version1, version2)

        // Assert
        assertFalse(isCompatible, "Different major versions should not be compatible")
    }

    @Test
    fun `should reject incompatible major versions in reverse order`() {
        // Arrange
        val extractor = VersionExtractor()
        val version1 = "8.4.0.0"
        val version2 = "8.3.24.1234"

        // Act
        val isCompatible = extractor.isVersionCompatible(version1, version2)

        // Assert
        assertFalse(isCompatible, "Different major versions should not be compatible in reverse order")
    }

    @Test
    fun `should accept null detected version`() {
        // Arrange
        val extractor = VersionExtractor()
        val requiredVersion = "8.3.24.1234"

        // Act
        val isCompatible = extractor.isVersionCompatible(null, requiredVersion)

        // Assert
        assertTrue(isCompatible, "Null detected version should be accepted")
    }

    @Test
    fun `should reject incomplete version strings`() {
        // Arrange
        val extractor = VersionExtractor()
        val incompleteVersion = "8.3"
        val fullVersion = "8.3.24.1234"

        // Act
        val isCompatible1 = extractor.isVersionCompatible(incompleteVersion, fullVersion)
        val isCompatible2 = extractor.isVersionCompatible(fullVersion, incompleteVersion)
        val isCompatible3 = extractor.isVersionCompatible("8", fullVersion)

        // Assert
        assertFalse(isCompatible1, "Incomplete version should not be compatible with full version")
        assertFalse(isCompatible2, "Full version should not be compatible with incomplete version")
        assertFalse(isCompatible3, "Single part version should not be compatible with full version")
    }

    @Test
    fun `should reject empty version strings`() {
        // Arrange
        val extractor = VersionExtractor()
        val emptyVersion = ""
        val fullVersion = "8.3.24.1234"

        // Act
        val isCompatible1 = extractor.isVersionCompatible(emptyVersion, fullVersion)
        val isCompatible2 = extractor.isVersionCompatible(fullVersion, emptyVersion)

        // Assert
        assertFalse(isCompatible1, "Empty version should not be compatible with full version")
        assertFalse(isCompatible2, "Full version should not be compatible with empty version")
    }

    @Test
    fun `should handle edge case versions correctly`() {
        // Arrange
        val extractor = VersionExtractor()

        // Act & Assert
        // Same major.minor but different build numbers
        assertTrue(
            extractor.isVersionCompatible("8.3.24.1234", "8.3.24.5678"),
            "Same major.minor.patch should be compatible even with different build numbers"
        )

        // Different minor versions
        assertFalse(
            extractor.isVersionCompatible("8.3.24.1234", "8.4.0.0"),
            "Different minor versions should not be compatible"
        )

        // Different major versions
        assertFalse(
            extractor.isVersionCompatible("8.3.24.1234", "9.0.0.0"),
            "Different major versions should not be compatible"
        )
    }
} 