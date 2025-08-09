package io.github.alkoleft.mcp.infrastructure.platform.search

import io.github.alkoleft.mcp.core.modules.PlatformType
import io.github.alkoleft.mcp.infrastructure.utility.PlatformDetector
import java.nio.file.Paths

/**
 * Factory for creating platform-specific search strategies
 */
class SearchStrategyFactory {
    /**
     * Creates platform-specific search strategy
     */
    fun createSearchStrategy(): SearchStrategy =
        when (PlatformDetector.current) {
            PlatformType.WINDOWS -> WindowsSearchStrategy()
            PlatformType.LINUX -> LinuxSearchStrategy()
            PlatformType.MACOS -> MacSearchStrategy()
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
class WindowsSearchStrategy : SearchStrategy {
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
class LinuxSearchStrategy : SearchStrategy {
    override val locations =
        listOf(
            VersionLocation("/opt/1cv8/x86_64"),
            VersionLocation("/usr/local/1cv8"),
            VersionLocation("/opt/1cv8/arm64"),
            VersionLocation("/opt/1cv8/e2kv4"),
            VersionLocation("/opt/1cv8/i386"),
        )
}

class MacSearchStrategy : SearchStrategy {
    override val locations =
        listOf(
            VersionLocation("/opt/1cv8"),
        )
}
