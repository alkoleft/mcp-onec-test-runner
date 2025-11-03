package io.github.alkoleft.mcp.infrastructure.platform.locator

import io.github.alkoleft.mcp.core.modules.UtilityLocation
import io.github.alkoleft.mcp.core.modules.UtilityType
import io.github.alkoleft.mcp.infrastructure.platform.search.SearchStrategyFactory
import io.github.alkoleft.mcp.infrastructure.platform.search.search
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger { }

/**
 * Cross-platform utility locator implementing simple file system search with caching.
 * Discovers 1C:Enterprise platform utilities across different operating systems.
 */
@Component
class UtilityLocator {
    private val pathCache = UtilityCache()
    private val searchStrategyFactory = SearchStrategyFactory()
    private val utilityValidator = UtilityValidator()

    fun locateUtility(
        utility: UtilityType,
        version: String?,
    ): UtilityLocation {
        logger.debug { "Начало поиска утилиты: $utility, версия: $version" }

        // Phase 1: Check cache first
        pathCache.getCachedLocation(utility, version)?.let { cached ->
            if (utilityValidator.validateUtility(cached)) {
                logger.debug { "Найдена кэшированная локация утилиты: ${cached.executablePath}" }
                return cached
            } else {
                pathCache.invalidate(utility, version)
            }
        }

        // Phase 2: Simple hierarchical search
        val searchStrategy = searchStrategyFactory.createSearchStrategy(utility)
        val location = searchStrategy.search(utility, version)

        // Phase 3: Cache successful result
        pathCache.store(utility, version, location)

        return location
    }

    fun validateUtility(location: UtilityLocation): Boolean = utilityValidator.validateUtility(location)
}
