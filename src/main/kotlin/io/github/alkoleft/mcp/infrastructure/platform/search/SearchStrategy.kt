package io.github.alkoleft.mcp.infrastructure.platform.search

import io.github.alkoleft.mcp.core.modules.PlatformType
import io.github.alkoleft.mcp.core.modules.TestExecutionError
import io.github.alkoleft.mcp.core.modules.UtilityLocation
import io.github.alkoleft.mcp.core.modules.UtilityType
import io.github.alkoleft.mcp.infrastructure.utility.PlatformDetector
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.isExecutable

private val logger = KotlinLogging.logger { }

/**
 * Factory for creating platform-specific search strategies
 */
class SearchStrategyFactory {
    /**
     * Creates platform-specific search strategy
     */
    fun createSearchStrategy(utility: UtilityType? = null): SearchStrategy =
        if (utility == null || utility.isPlatform()) {
            when (PlatformDetector.current) {
                PlatformType.WINDOWS -> PlatformWindowsSearchStrategy
                PlatformType.LINUX -> PlatformLinuxSearchStrategy
                PlatformType.MACOS -> PlatformMacSearchStrategy
            }
        } else {
            when (PlatformDetector.current) {
                PlatformType.WINDOWS -> EdtWindowsSearchStrategy
                PlatformType.LINUX -> EdtLinuxSearchStrategy
                PlatformType.MACOS -> EdtMacSearchStrategy
            }
        }
}

/**
 * Search strategy interface for different platforms
 */
interface SearchStrategy {
    val locations: List<SearchLocation>
}

/**
 * Windows-specific search strategy
 */
object PlatformWindowsSearchStrategy : SearchStrategy {
    override val locations = systemLocations() + userLocation()

    fun systemLocations() =
        listOf("PROGRAMFILES", "PROGRAMFILES(x86)")
            .map { System.getenv(it) }
            .filter { it != null && it.isNotBlank() }
            .map { Paths.get(it, "1cv8").toString() }
            .map { VersionLocation(it) }

    fun userLocation() =
        System.getenv("LOCALAPPDATA")?.let {
            listOf(
                VersionLocation(Paths.get(it, "Programs", "1cv8").toString()),
                VersionLocation(Paths.get(it, "Programs", "1cv8_x86").toString()),
                VersionLocation(Paths.get(it, "Programs", "1cv8_x64").toString()),
            )
        } ?: emptyList()
}

/**
 * Linux-specific search strategy
 */
object PlatformLinuxSearchStrategy : SearchStrategy {
    override val locations =
        listOf(
            VersionLocation("/opt/1cv8/x86_64"),
            VersionLocation("/usr/local/1cv8"),
            VersionLocation("/opt/1cv8/arm64"),
            VersionLocation("/opt/1cv8/e2kv4"),
            VersionLocation("/opt/1cv8/i386"),
        )
}

object PlatformMacSearchStrategy : SearchStrategy {
    override val locations =
        listOf(
            VersionLocation("/opt/1cv8"),
        )
}

object EdtLinuxSearchStrategy : SearchStrategy {
    override val locations =
        listOf(
            VersionLocation("/opt/1C/1CE/components"),
            VersionLocation("~/.local/share/1C/1cedtstart/installations/"),
        )
}

object EdtWindowsSearchStrategy : SearchStrategy {
    override val locations = systemLocations() + userLocation()

    fun systemLocations() =
        System.getenv("PROGRAMFILES")?.let {
            listOf(
                VersionLocation(Paths.get(it, "1C", "1CE", "components").toString()),
            )
        } ?: emptyList()

    fun userLocation() =
        System.getenv("LOCALAPPDATA")?.let {
            listOf(
                VersionLocation(Paths.get(it, "1C", "1cedtstart", "installation").toString()),
            )
        } ?: emptyList()
}

object EdtMacSearchStrategy : SearchStrategy {
    override val locations =
        listOf(
            VersionLocation("/opt/1C/1CE/components"),
        )
}

fun SearchStrategy.search(
    utility: UtilityType,
    version: String?,
): UtilityLocation {
    // Search Tier 1 locations (most common)
    logger.debug { "Searching locations" }
    for (location in locations) {
        searchInLocation(location, utility, version)?.let { return it }
    }

    throw TestExecutionError.UtilNotFound("$utility not found in any known location")
}

/**
 * Searches for utility in a specific location
 */
private fun searchInLocation(
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
