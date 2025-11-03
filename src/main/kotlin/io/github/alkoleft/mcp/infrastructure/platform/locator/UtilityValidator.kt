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
                logger.debug { "Утилита не найдена по пути: ${location.executablePath}" }
                return false
            }

            // Permission check
            if (!location.executablePath.isExecutable()) {
                logger.debug { "Утилита не исполняемая (нет прав на выполнение): ${location.executablePath}" }
                return false
            }
            return true
        } catch (e: Exception) {
            logger.debug { "Проверка не пройдена для ${location.executablePath}: ${e.message}" }
            false
        }
    }
}
