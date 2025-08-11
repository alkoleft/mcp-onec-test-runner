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
                PlatformType.MACOS -> PlatformLinuxSearchStrategy
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
    override val locations = systemLocations() + userLocation() + pathFallback()

    fun systemLocations() =
        listOf("PROGRAMFILES", "PROGRAMFILES(x86)")
            .map { System.getenv(it) }
            .filter { it != null && it.isNotBlank() }
            .map { Paths.get(it, "1cv8").toString() }
            .flatMap { base ->
                listOf(
                    DirectoryEnumeratingLocation(
                        basePath = base,
                        relativeExecutableSubPath = "bin",
                    ) { dir -> dir },
                )
            }

    fun userLocation() =
        System.getenv("LOCALAPPDATA")?.let {
            listOf(
                DirectoryEnumeratingLocation(
                    basePath = Paths.get(it, "Programs", "1cv8").toString(),
                    relativeExecutableSubPath = "bin",
                ) { dir -> dir },
                DirectoryEnumeratingLocation(
                    basePath = Paths.get(it, "Programs", "1cv8_x86").toString(),
                    relativeExecutableSubPath = "bin",
                ) { dir -> dir },
                DirectoryEnumeratingLocation(
                    basePath = Paths.get(it, "Programs", "1cv8_x64").toString(),
                    relativeExecutableSubPath = "bin",
                ) { dir -> dir },
            )
        } ?: emptyList()

    private fun pathFallback(): List<SearchLocation> = listOf(PathEnvironmentLocation())
}

/**
 * Linux-specific search strategy
 */
object PlatformLinuxSearchStrategy : SearchStrategy {
    override val locations =
        listOf(
            DirectoryEnumeratingLocation(
                basePath = "/opt/1cv8/x86_64",
            ) { it },
            DirectoryEnumeratingLocation(
                basePath = "/usr/local/1cv8",
            ) { it },
            DirectoryEnumeratingLocation(
                basePath = "/opt/1cv8/arm64",
            ) { it },
            DirectoryEnumeratingLocation(
                basePath = "/opt/1cv8/e2kv4",
            ) { it },
            DirectoryEnumeratingLocation(
                basePath = "/opt/1cv8/i386",
            ) { it },
            PathEnvironmentLocation(),
        )
}

object PlatformMacSearchStrategy : SearchStrategy {
    override val locations =
        listOf(
            DirectoryEnumeratingLocation(
                basePath = "/opt/1cv8",
            ) { it },
            PathEnvironmentLocation(),
        )
}

object EdtLinuxSearchStrategy : SearchStrategy {
    override val locations =
        listOf(
            // Components install dir: 1c-edt-<ver>-<arch>/1cedt/
            DirectoryEnumeratingLocation(
                basePath = "/opt/1C/1CE/components",
            ) { dirName ->
                dirName.substringAfter("1c-edt-")
            },
            // User installations: 1C_EDT <ver>/1cedt/
            DirectoryEnumeratingLocation(
                basePath = "~/.local/share/1C/1cedtstart/installations",
                relativeExecutableSubPath = "1cedt",
            ) { dirName ->
                dirName.substringAfter("1C_EDT ", "")
            },
            PathEnvironmentLocation(),
        )
}

object EdtWindowsSearchStrategy : SearchStrategy {
    override val locations = systemLocations() + userLocation() + pathFallback()

    fun systemLocations() =
        System.getenv("PROGRAMFILES")?.let {
            listOf(
                // Components: 1c-edt-<ver>-<arch>/1cedt/
                DirectoryEnumeratingLocation(
                    basePath = Paths.get(it, "1C", "1CE", "components").toString(),
                    relativeExecutableSubPath = "1cedt",
                ) { dir ->
                    dir.substringAfter("1c-edt-")
                },
            )
        } ?: emptyList()

    fun userLocation() =
        System.getenv("LOCALAPPDATA")?.let {
            listOf(
                // 1C_EDT <ver>/1cedt/
                DirectoryEnumeratingLocation(
                    basePath = Paths.get(it, "1C", "1cedtstart", "installations").toString(),
                    relativeExecutableSubPath = "1cedt",
                ) { dir ->
                    dir.substringAfter("1C_EDT ", "")
                },
            )
        } ?: emptyList()

    private fun pathFallback(): List<SearchLocation> = listOf(PathEnvironmentLocation())
}

object EdtMacSearchStrategy : SearchStrategy {
    override val locations =
        listOf(
            DirectoryEnumeratingLocation(
                basePath = "/opt/1C/1CE/components",
                relativeExecutableSubPath = "1cedt",
            ) { dir ->
                dir.substringAfter("1c-edt-")
            },
            PathEnvironmentLocation(),
        )
}

fun SearchStrategy.search(
    utility: UtilityType,
    version: String?,
): UtilityLocation {
    logger.debug { "Searching locations" }

    // Normalize requirement: EDT_CLI defaults to latest if not provided
    val requirement =
        if (utility == UtilityType.EDT_CLI && (version == null || version.isBlank())) "latest" else version

    // If platform utility and version is null/blank or invalid, we should fail early
    if (utility.isPlatform()) {
        if (requirement.isNullOrBlank()) {
            throw TestExecutionError.UtilNotFound("${utility.name} version is required")
        }
        val parsedReq = Version.parse(requirement)
        if (parsedReq == null) {
            throw TestExecutionError.UtilNotFound("${utility.name} invalid version: $requirement")
        }
        if (parsedReq.parts.size < 4) {
            // For platform utilities, enforce exact version (major.minor.patch.build)
            throw TestExecutionError.UtilNotFound("${utility.name} exact version required: $requirement")
        }
    }

    // Aggregate candidates from all locations
    val allCandidates = mutableListOf<Pair<java.nio.file.Path, String?>>()
    for (location in locations) {
        try {
            val candidates = location.generateCandidates(utility, requirement)
            allCandidates.addAll(candidates)
        } catch (e: Exception) {
            logger.debug { "Error searching in location ${location.javaClass.simpleName}: ${e.message}" }
        }
    }

    // Validate existence/executable and pick best by version
    val existing =
        allCandidates.filter { (path, _) ->
            try {
                path.exists() && path.isExecutable()
            } catch (_: Exception) {
                false
            }
        }

    if (existing.isEmpty()) {
        throw TestExecutionError.UtilNotFound("$utility not found in any known location")
    }

    val resolver = DefaultVersionResolver()
    val bestPath =
        resolver.selectBest(existing, requirement)
            ?: throw TestExecutionError.UtilNotFound("$utility not found for version requirement: $requirement")

    return UtilityLocation(
        executablePath = bestPath,
        version = requirement,
        platformType = PlatformDetector.current,
    )
}
