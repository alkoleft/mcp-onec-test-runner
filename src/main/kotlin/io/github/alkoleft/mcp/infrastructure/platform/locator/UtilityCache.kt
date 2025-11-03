package io.github.alkoleft.mcp.infrastructure.platform.locator

import io.github.alkoleft.mcp.core.modules.UtilityLocation
import io.github.alkoleft.mcp.core.modules.UtilityType
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger { }

/**
 * Multi-level caching system with TTL and validation for utility locations
 */
class UtilityCache {
    private val memoryCache = ConcurrentHashMap<CacheKey, UtilityLocation>()

    data class CacheKey(
        val utility: UtilityType,
        val version: String?,
    )

    fun getCachedLocation(
        utility: UtilityType,
        version: String?,
    ): UtilityLocation? {
        val key = CacheKey(utility, version)
        val entry = memoryCache[key]

        return if (entry != null) {
            logger.debug { "Попадание в кэш для утилиты: $utility, версия: $version" }
            entry
        } else {
            logger.debug { "Промах кэша для утилиты: $utility, версия: $version" }
            null
        }
    }

    fun store(
        utility: UtilityType,
        version: String?,
        location: UtilityLocation,
    ) {
        val key = CacheKey(utility, version)
        memoryCache[key] = location
        logger.debug { "Сохранено в кэш: $utility, версия: $version по пути ${location.executablePath}" }
    }

    fun invalidate(
        utility: UtilityType,
        version: String?,
    ) {
        val key = CacheKey(utility, version)
        memoryCache.remove(key)
        logger.debug { "Кэш инвалидирован для: $utility, версия: $version" }
    }
}
