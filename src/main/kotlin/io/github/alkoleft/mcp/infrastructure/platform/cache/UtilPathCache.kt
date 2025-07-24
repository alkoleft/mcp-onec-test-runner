package io.github.alkoleft.mcp.infrastructure.platform.cache

import io.github.alkoleft.mcp.core.modules.UtilityLocation
import io.github.alkoleft.mcp.core.modules.UtilityType
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger { }

/**
 * Multi-level caching system with TTL and validation for utility locations
 */
class UtilPathCache {
    private val memoryCache = ConcurrentHashMap<CacheKey, CachedEntry>()
    private val ttl = Duration.ofHours(24)

    data class CacheKey(
        val utility: UtilityType,
        val version: String?,
    )

    data class CachedEntry(
        val location: UtilityLocation,
        val timestamp: Instant,
        val validationHash: String,
    ) {
        fun isValid(): Boolean = Instant.now() < timestamp.plus(Duration.ofHours(24))
    }

    fun getCachedLocation(
        utility: UtilityType,
        version: String?,
    ): UtilityLocation? {
        val key = CacheKey(utility, version)
        val entry = memoryCache[key]

        return if (entry?.isValid() == true) {
            logger.debug("Cache hit for utility: $utility, version: $version")
            entry.location
        } else {
            logger.debug("Cache miss for utility: $utility, version: $version")
            null
        }
    }

    fun store(
        utility: UtilityType,
        version: String?,
        location: UtilityLocation,
    ) {
        val key = CacheKey(utility, version)
        val entry = CachedEntry(
            location = location,
            timestamp = Instant.now(),
            validationHash = location.executablePath.toString().hashCode().toString(),
        )
        memoryCache[key] = entry
        logger.debug("Stored in cache: $utility, version: $version at ${location.executablePath}")
    }

    fun invalidate(
        utility: UtilityType,
        version: String?,
    ) {
        val key = CacheKey(utility, version)
        memoryCache.remove(key)
        logger.debug("Invalidated cache for: $utility, version: $version")
    }

    fun clear() {
        memoryCache.clear()
        logger.info("Utility location cache cleared")
    }

    fun getCacheSize(): Int = memoryCache.size
} 