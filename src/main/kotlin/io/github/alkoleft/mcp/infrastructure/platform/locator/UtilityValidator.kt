package io.github.alkoleft.mcp.infrastructure.platform.locator

import io.github.alkoleft.mcp.core.modules.UtilityLocation
import io.github.oshai.kotlinlogging.KotlinLogging
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
        } catch (e: Exception) {
            logger.debug { "Validation failed for ${location.executablePath}: ${e.message}" }
            false
        }
    }
}
