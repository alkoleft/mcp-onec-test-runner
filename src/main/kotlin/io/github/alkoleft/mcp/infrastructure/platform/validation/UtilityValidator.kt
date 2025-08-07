package io.github.alkoleft.mcp.infrastructure.platform.validation

import io.github.alkoleft.mcp.core.modules.UtilityLocation
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.io.path.exists
import kotlin.io.path.isExecutable

private val logger = KotlinLogging.logger { }

/**
 * Utility validation service for checking executable existence and functionality
 */
class UtilityValidator {
    
    /**
     * Validates utility location by checking existence, permissions and basic functionality
     */
    fun validateUtility(location: UtilityLocation): Boolean {
        return try {
            // Basic existence check
            if (!location.executablePath.exists()) {
                logger.debug { "Utility not found at: ${location.executablePath}" }
                return false
            }

            // Permission check
            if (!location.executablePath.isExecutable()) {
                logger.debug { "Utility not executable: ${location.executablePath}" }
                return false
            }
            return true
            // Quick functional validation
//            runQuickValidation(location)
        } catch (e: Exception) {
            logger.debug { "Validation failed for ${location.executablePath}: ${e.message}" }
            false
        }
    }

    /**
     * Runs quick validation to ensure utility is functional
     */
    private suspend fun runQuickValidation(location: UtilityLocation): Boolean =
        withContext(Dispatchers.IO) {
            try {
                withTimeout(5000) { // 5 second timeout
                    val process = ProcessBuilder()
                        .command(location.executablePath.toString(), "/?")
                        .redirectOutput(ProcessBuilder.Redirect.PIPE)
                        .redirectError(ProcessBuilder.Redirect.PIPE)
                        .start()

                    val completed = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)

                    if (completed) {
                        // For 1C utilities, exit code 0 or 1 is typically acceptable for help command
                        val isValid = process.exitValue() in 0..1
                        logger.debug { "Quick validation result for ${location.executablePath}: $isValid" }
                        isValid
                    } else {
                        process.destroyForcibly()
                        logger.debug { "Quick validation timeout for ${location.executablePath}" }
                        false
                    }
                }
            } catch (_: TimeoutCancellationException) {
                logger.debug { "Quick validation timeout for ${location.executablePath}" }
                false
            } catch (e: Exception) {
                logger.debug { "Quick validation failed for ${location.executablePath}: ${e.message}" }
                false
            }
        }
} 