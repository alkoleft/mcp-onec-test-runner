package io.github.alkoleft.mcp.infrastructure.platform.search

import io.github.alkoleft.mcp.core.modules.UtilityType
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

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
        val extension = if (isWindows) ".exe" else ""

        return "${utility.fileName}$extension"
    }

    protected val isWindows = System.getProperty("os.name").lowercase().contains("win")
}

/**
 * Standard file system location
 */
class StandardLocation(
    private val basePath: String,
) : BaseSearchLocation() {
    override fun generatePaths(
        utility: UtilityType,
        version: String?,
    ): List<Path> {
        val executableName = getExecutableName(utility)

        return if (isWindows) listOf(Paths.get(basePath, "bin", executableName))
        else listOf(Paths.get(basePath, executableName))
    }
}

/**
 * Version-specific location
 */
class VersionLocation(
    private val basePath: String,
) : BaseSearchLocation() {
    override fun generatePaths(
        utility: UtilityType,
        version: String?,
    ): List<Path> {
        if (version == null) return emptyList()

        val executableName = getExecutableName(utility)

        return if (isWindows) listOf(Paths.get(basePath, version, "bin", executableName))
        else listOf(Paths.get(basePath, version, executableName))
    }
}

/**
 * PATH environment variable location
 */
class PathEnvironmentLocation : BaseSearchLocation() {
    override fun generatePaths(
        utility: UtilityType,
        version: String?,
    ): List<Path> {
        val executableName = getExecutableName(utility)
        return System
            .getenv("PATH")
            ?.split(File.pathSeparator)
            ?.map { Paths.get(it, executableName) } ?: emptyList()
    }
}