package io.github.alkoleft.mcp.infrastructure.strategy.handlers

import io.github.alkoleft.mcp.core.modules.strategy.ErrorContext
import io.github.alkoleft.mcp.core.modules.strategy.ErrorHandler
import io.github.alkoleft.mcp.core.modules.strategy.ErrorResolution
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Duration

private val logger = KotlinLogging.logger { }

/**
 * Обработчик ошибок конфигурации
 * Обрабатывает ошибки в конфигурации YaXUnit и предлагает решения
 */
class ConfigurationErrorHandler : ErrorHandler {

    private var nextHandler: ErrorHandler? = null

    override fun canHandle(error: Throwable): Boolean {
        val message = error.message?.lowercase() ?: ""
        return message.contains("config") ||
                message.contains("configuration") ||
                message.contains("конфигурация") ||
                message.contains("invalid") ||
                message.contains("missing") ||
                message.contains("required") ||
                message.contains("format")
    }

    override fun handle(error: Throwable, context: ErrorContext): ErrorResolution {
        logger.warn { "Handling configuration error: ${error.message}" }

        return when {
            error.message?.contains("missing", ignoreCase = true) == true -> {
                logger.error { "Missing required configuration parameter" }
                ErrorResolution.Fail(
                    reason = "Missing configuration parameter",
                    details = "Required configuration parameter is missing: ${error.message}"
                )
            }

            error.message?.contains("invalid", ignoreCase = true) == true -> {
                logger.error { "Invalid configuration format" }
                ErrorResolution.Fail(
                    reason = "Invalid configuration format",
                    details = "Configuration format is invalid: ${error.message}"
                )
            }

            error.message?.contains("format", ignoreCase = true) == true -> {
                logger.error { "Unsupported configuration format" }
                ErrorResolution.Fail(
                    reason = "Unsupported configuration format",
                    details = "Configuration format is not supported: ${error.message}"
                )
            }

            context.attempt < context.maxAttempts -> {
                logger.info { "Retrying configuration (attempt ${context.attempt}/${context.maxAttempts})" }
                ErrorResolution.Retry(
                    maxAttempts = context.maxAttempts,
                    delay = Duration.ofSeconds(2),
                    reason = "Configuration error, retrying"
                )
            }

            else -> {
                logger.error { "Configuration error: ${error.message}" }
                ErrorResolution.Fail(
                    reason = "Configuration error",
                    details = error.message ?: "Unknown configuration error"
                )
            }
        }
    }

    override fun setNext(handler: ErrorHandler): ErrorHandler {
        nextHandler = handler
        return this
    }

    override fun getNext(): ErrorHandler? = nextHandler
}
