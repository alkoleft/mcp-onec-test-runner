package io.github.alkoleft.mcp.infrastructure.platform.version

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.nio.file.Path

private val logger = KotlinLogging.logger { }

/**
 * Version extraction service for 1C utilities
 */
class VersionExtractor {
    
    /**
     * Extracts version from utility executable
     */
    suspend fun extractVersion(executablePath: Path): String? =
        withContext(Dispatchers.IO) {
            try {
                withTimeout(3000) { // 3 second timeout
                    val process = ProcessBuilder()
                        .command(executablePath.toString(), "/?")
                        .redirectOutput(ProcessBuilder.Redirect.PIPE)
                        .redirectError(ProcessBuilder.Redirect.PIPE)
                        .start()

                    process.waitFor(3, java.util.concurrent.TimeUnit.SECONDS)

                    val output = process.inputStream.bufferedReader().readText()

                    // Extract version using regex pattern
                    val versionPattern = Regex("""(\d+\.\d+\.\d+\.\d+)""")
                    val version = versionPattern.find(output)?.value

                    logger.debug { "Extracted version from $executablePath: $version" }
                    version
                }
            } catch (e: Exception) {
                logger.debug { "Version extraction failed for $executablePath: ${e.message}" }
                null
            }
        }

    /**
     * Checks if detected version is compatible with required version
     */
    fun isVersionCompatible(
        detectedVersion: String?,
        requiredVersion: String,
    ): Boolean {
        if (detectedVersion == null) return true // Accept if version cannot be determined
        if (detectedVersion == requiredVersion) return true

        // Check major.minor compatibility
        val detectedParts = detectedVersion.split(".")
        val requiredParts = requiredVersion.split(".")

        // Both versions should have at least 4 parts for full compatibility
        if (detectedParts.size >= 4 && requiredParts.size >= 4) {
            val isCompatible = detectedParts[0] == requiredParts[0] && detectedParts[1] == requiredParts[1]
            logger.debug { "Version compatibility check: $detectedVersion vs $requiredVersion = $isCompatible" }
            return isCompatible
        }

        // For incomplete versions, require exact match
        return false
    }
} 