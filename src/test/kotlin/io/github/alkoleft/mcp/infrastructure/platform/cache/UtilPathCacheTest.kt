package io.github.alkoleft.mcp.infrastructure.platform.cache

import io.github.alkoleft.mcp.core.modules.PlatformType
import io.github.alkoleft.mcp.core.modules.UtilityLocation
import io.github.alkoleft.mcp.core.modules.UtilityType
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class UtilPathCacheTest {

    @Test
    fun `should store and retrieve cached location successfully`() {
        // Arrange
        val cache = UtilPathCache()
        val utility = UtilityType.DESIGNER
        val version = "8.3.24"
        val expectedLocation = createTestLocation()

        // Act
        cache.store(utility, version, expectedLocation)
        val actualLocation = cache.getCachedLocation(utility, version)

        // Assert
        assertNotNull(actualLocation, "Cached location should not be null")
        assertEquals(expectedLocation, actualLocation, "Retrieved location should match stored location")
        assertEquals(expectedLocation.executablePath, actualLocation.executablePath, "Executable paths should match")
        assertEquals(expectedLocation.version, actualLocation.version, "Versions should match")
        assertEquals(expectedLocation.platformType, actualLocation.platformType, "Platform types should match")
    }

    @Test
    fun `should return null for non-existent cache entry`() {
        // Arrange
        val cache = UtilPathCache()
        val utility = UtilityType.DESIGNER
        val version = "8.3.24"

        // Act
        val retrieved = cache.getCachedLocation(utility, version)

        // Assert
        assertNull(retrieved, "Non-existent cache entry should return null")
    }

    @Test
    fun `should invalidate specific cache entry`() {
        // Arrange
        val cache = UtilPathCache()
        val utility = UtilityType.DESIGNER
        val version = "8.3.24"
        val location = createTestLocation()

        cache.store(utility, version, location)
        assertEquals(location, cache.getCachedLocation(utility, version), "Location should be cached initially")

        // Act
        cache.invalidate(utility, version)
        val retrieved = cache.getCachedLocation(utility, version)

        // Assert
        assertNull(retrieved, "Invalidated cache entry should return null")
    }

    @Test
    fun `should clear all cache entries`() {
        // Arrange
        val cache = UtilPathCache()
        val location1 = createTestLocation()
        val location2 = createTestLocation()

        cache.store(UtilityType.DESIGNER, "8.3.24", location1)
        cache.store(UtilityType.IBCMD, "8.3.25", location2)
        
        assertEquals(2, cache.getCacheSize(), "Cache should contain 2 entries before clearing")

        // Act
        cache.clear()

        // Assert
        val retrieved1 = cache.getCachedLocation(UtilityType.DESIGNER, "8.3.24")
        val retrieved2 = cache.getCachedLocation(UtilityType.IBCMD, "8.3.25")
        
        assertNull(retrieved1, "First cache entry should be cleared")
        assertNull(retrieved2, "Second cache entry should be cleared")
        assertEquals(0, cache.getCacheSize(), "Cache size should be 0 after clearing")
    }

    @Test
    fun `should handle null version correctly`() {
        // Arrange
        val cache = UtilPathCache()
        val utility = UtilityType.DESIGNER
        val location = createTestLocation()

        // Act
        cache.store(utility, null, location)
        val retrieved = cache.getCachedLocation(utility, null)

        // Assert
        assertNotNull(retrieved, "Cached location with null version should not be null")
        assertEquals(location, retrieved, "Retrieved location should match stored location")
    }

    @Test
    fun `should return correct cache size`() {
        // Arrange
        val cache = UtilPathCache()
        val location = createTestLocation()

        // Act
        cache.store(UtilityType.DESIGNER, "8.3.24", location)
        assertEquals(1, cache.getCacheSize(), "Cache should contain 1 entry after first store")
        
        cache.store(UtilityType.IBCMD, "8.3.25", location)
        val finalSize = cache.getCacheSize()

        // Assert
        assertEquals(2, finalSize, "Cache should contain 2 entries after second store")
    }

    @Test
    fun `should handle different utility types independently`() {
        // Arrange
        val cache = UtilPathCache()
        val location1 = createTestLocation("/test/path/1cv8c")
        val location2 = createTestLocation("/test/path/ibcmd")

        // Act
        cache.store(UtilityType.DESIGNER, "8.3.24", location1)
        cache.store(UtilityType.IBCMD, "8.3.24", location2)

        // Assert
        val retrieved1 = cache.getCachedLocation(UtilityType.DESIGNER, "8.3.24")
        val retrieved2 = cache.getCachedLocation(UtilityType.IBCMD, "8.3.24")
        
        assertNotNull(retrieved1, "First utility should be cached")
        assertNotNull(retrieved2, "Second utility should be cached")
        assertNotEquals(retrieved1, retrieved2, "Different utilities should have different cache entries")
    }

    @Test
    fun `should handle different versions independently`() {
        // Arrange
        val cache = UtilPathCache()
        val location1 = createTestLocation("/test/path/1cv8c", "8.3.24")
        val location2 = createTestLocation("/test/path/1cv8c", "8.3.25")

        // Act
        cache.store(UtilityType.DESIGNER, "8.3.24", location1)
        cache.store(UtilityType.DESIGNER, "8.3.25", location2)

        // Assert
        val retrieved1 = cache.getCachedLocation(UtilityType.DESIGNER, "8.3.24")
        val retrieved2 = cache.getCachedLocation(UtilityType.DESIGNER, "8.3.25")
        
        assertNotNull(retrieved1, "First version should be cached")
        assertNotNull(retrieved2, "Second version should be cached")
        assertNotEquals(retrieved1, retrieved2, "Different versions should have different cache entries")
    }

    private fun createTestLocation(): UtilityLocation {
        return createTestLocation("/test/path/1cv8c", "8.3.24")
    }

    private fun createTestLocation(path: String): UtilityLocation {
        return createTestLocation(path, "8.3.24")
    }

    private fun createTestLocation(path: String, version: String): UtilityLocation {
        return UtilityLocation(
            executablePath = Paths.get(path),
            version = version,
            platformType = PlatformType.LINUX
        )
    }
} 