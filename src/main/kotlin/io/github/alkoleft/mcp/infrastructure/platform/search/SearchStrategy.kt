package io.github.alkoleft.mcp.infrastructure.platform.search

import io.github.alkoleft.mcp.core.modules.UtilityType
import java.nio.file.Path

/**
 * Search strategy interface for different platforms
 */
interface SearchStrategy {
    val tier1Locations: List<SearchLocation>
    val tier2Locations: List<SearchLocation>
    val tier3Locations: List<SearchLocation>
}

/**
 * Search location interface for generating paths
 */
interface SearchLocation {
    fun generatePaths(
        utility: UtilityType,
        version: String?,
    ): List<Path>
}

/**
 * Base class for standard file system locations
 */
abstract class BaseSearchLocation : SearchLocation {
    
    protected fun getExecutableName(utility: UtilityType): String {
        val isWindows = System.getProperty("os.name").lowercase().contains("win")
        val extension = if (isWindows) ".exe" else ""
        
        return when (utility) {
            UtilityType.COMPILER_1CV8C -> "1cv8c$extension"
            UtilityType.INFOBASE_MANAGER_IBCMD -> "ibcmd$extension"
        }
    }
} 