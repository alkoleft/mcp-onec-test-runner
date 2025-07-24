package io.github.alkoleft.mcp.infrastructure.platform.search

import io.github.alkoleft.mcp.core.modules.UtilityType
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Windows-specific search strategy
 */
class WindowsSearchStrategy : SearchStrategy {
    override val tier1Locations = listOf(
        StandardLocation("C:\\Program Files\\1cv8"),
        StandardLocation("C:\\Program Files (x86)\\1cv8"),
    )

    override val tier2Locations = listOf(
        VersionLocation("C:\\Program Files\\1cv8"),
        VersionLocation("C:\\Program Files (x86)\\1cv8"),
    )

    override val tier3Locations = listOf(
        PathEnvironmentLocation(),
    )
}

/**
 * Standard file system location
 */
class StandardLocation(
    private val basePath: String,
) : SearchLocation {
    override fun generatePaths(
        utility: UtilityType,
        version: String?,
    ): List<Path> {
        val executableName = getExecutableName(utility)
        return listOf(
            Paths.get(basePath, "bin", executableName),
            Paths.get(basePath, executableName),
        )
    }
    
    private fun getExecutableName(utility: UtilityType): String {
        val isWindows = System.getProperty("os.name").lowercase().contains("win")
        val extension = if (isWindows) ".exe" else ""
        
        return when (utility) {
            UtilityType.COMPILER_1CV8C -> "1cv8c$extension"
            UtilityType.INFOBASE_MANAGER_IBCMD -> "ibcmd$extension"
        }
    }
}

/**
 * Version-specific location
 */
class VersionLocation(
    private val basePath: String,
) : SearchLocation {
    override fun generatePaths(
        utility: UtilityType,
        version: String?,
    ): List<Path> {
        val executableName = getExecutableName(utility)
        val paths = mutableListOf<Path>()

        if (version != null) {
            paths.add(Paths.get(basePath, version, "bin", executableName))
        }

        // Also try common version patterns
        paths.addAll(
            listOf(
                Paths.get(basePath, "8.3.24", "bin", executableName),
                Paths.get(basePath, "8.3", "bin", executableName),
            ),
        )

        return paths
    }
    
    private fun getExecutableName(utility: UtilityType): String {
        val isWindows = System.getProperty("os.name").lowercase().contains("win")
        val extension = if (isWindows) ".exe" else ""
        
        return when (utility) {
            UtilityType.COMPILER_1CV8C -> "1cv8c$extension"
            UtilityType.INFOBASE_MANAGER_IBCMD -> "ibcmd$extension"
        }
    }
}

/**
 * PATH environment variable location
 */
class PathEnvironmentLocation : SearchLocation {
    override fun generatePaths(
        utility: UtilityType,
        version: String?,
    ): List<Path> {
        val executableName = getExecutableName(utility)
        return System
            .getenv("PATH")
            ?.split(System.getProperty("path.separator"))
            ?.map { Paths.get(it, executableName) } ?: emptyList()
    }
    
    private fun getExecutableName(utility: UtilityType): String {
        val isWindows = System.getProperty("os.name").lowercase().contains("win")
        val extension = if (isWindows) ".exe" else ""
        
        return when (utility) {
            UtilityType.COMPILER_1CV8C -> "1cv8c$extension"
            UtilityType.INFOBASE_MANAGER_IBCMD -> "ibcmd$extension"
        }
    }
} 