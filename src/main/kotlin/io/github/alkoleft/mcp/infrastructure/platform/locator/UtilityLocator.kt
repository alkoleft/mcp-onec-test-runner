package io.github.alkoleft.mcp.infrastructure.platform.locator

import io.github.alkoleft.mcp.core.modules.TestExecutionError
import io.github.alkoleft.mcp.core.modules.UtilityLocation
import io.github.alkoleft.mcp.core.modules.UtilityType
import io.github.alkoleft.mcp.infrastructure.platform.search.SearchLocation
import io.github.alkoleft.mcp.infrastructure.platform.search.SearchStrategy
import io.github.alkoleft.mcp.infrastructure.platform.search.SearchStrategyFactory
import io.github.alkoleft.mcp.infrastructure.utility.PlatformDetector
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component
import kotlin.io.path.exists
import kotlin.io.path.isExecutable

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

    suspend fun locateUtility(
        utility: UtilityType,
        version: String?,
    ): UtilityLocation =
        coroutineScope {
            logger.debug { "Starting utility location for: $utility, version: $version" }

            // Phase 1: Check cache first
            pathCache.getCachedLocation(utility, version)?.let { cached ->
                if (utilityValidator.validateUtility(cached)) {
                    logger.debug { "Found cached utility location: ${cached.executablePath}" }
                    return@coroutineScope cached
                } else {
                    pathCache.invalidate(utility, version)
                }
            }

            // Phase 2: Simple hierarchical search
            val searchStrategy = searchStrategyFactory.createSearchStrategy()
            val location = executeSimpleSearch(searchStrategy, utility, version)

            // Phase 3: Cache successful result
            pathCache.store(utility, version, location)

            location
        }

    suspend fun validateUtility(location: UtilityLocation): Boolean = utilityValidator.validateUtility(location)

    fun clearCache() {
        pathCache.clear()
    }

    /**
     * Executes simple hierarchical search through known file system paths
     */
    private suspend fun executeSimpleSearch(
        strategy: SearchStrategy,
        utility: UtilityType,
        version: String?,
    ): UtilityLocation =
        withContext(Dispatchers.IO) {
            // Search Tier 1 locations (most common)
            logger.debug { "Searching locations" }
            for (location in strategy.locations) {
                searchInLocation(location, utility, version)?.let { return@withContext it }
            }

            throw TestExecutionError.UtilNotFound("$utility not found in any known location")
        }

    /**
     * Searches for utility in a specific location
     */
    private suspend fun searchInLocation(
        location: SearchLocation,
        utility: UtilityType,
        version: String?,
    ): UtilityLocation? {
        try {
            val paths = location.generatePaths(utility, version)

            for (path in paths) {
                if (path.exists() && path.isExecutable()) {
                    val utilityLocation =
                        UtilityLocation(
                            executablePath = path,
                            version = version,
                            platformType = PlatformDetector.current,
                        )

                    logger.debug { "Found utility at: $path, version: $version" }
                    return utilityLocation
                }
            }

            return null
        } catch (e: Exception) {
            logger.debug { "Error searching in location ${location.javaClass.simpleName}: ${e.message}" }
            return null
        }
    }
}
