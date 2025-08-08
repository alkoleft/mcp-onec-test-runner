package io.github.alkoleft.mcp.infrastructure.strategy.handlers

import io.github.alkoleft.mcp.core.modules.strategy.ErrorContext
import io.github.alkoleft.mcp.core.modules.strategy.ErrorHandler
import io.github.alkoleft.mcp.core.modules.strategy.ErrorResolution
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Duration

private val logger = KotlinLogging.logger { }

/**
 * Обработчик ошибок подключения к информационной базе
 * Обрабатывает ошибки подключения и предлагает стратегии восстановления
 */
class ConnectionErrorHandler : ErrorHandler {

    private var nextHandler: ErrorHandler? = null

    override fun canHandle(error: Throwable): Boolean {
        val message = error.message?.lowercase() ?: ""
        return message.contains("connection") ||
                message.contains("подключение") ||
                message.contains("connect") ||
                message.contains("network") ||
                message.contains("timeout") ||
                message.contains("refused")
    }

    override fun handle(error: Throwable, context: ErrorContext): ErrorResolution {
        logger.warn { "Handling connection error: ${error.message}" }

        return when {
            context.attempt < context.maxAttempts -> {
                logger.info { "Retrying connection (attempt ${context.attempt}/${context.maxAttempts})" }
                ErrorResolution.Retry(
                    maxAttempts = context.maxAttempts,
                    delay = Duration.ofSeconds(5),
                    reason = "Connection failed, retrying"
                )
            }

            error.message?.contains("timeout", ignoreCase = true) == true -> {
                logger.error { "Connection timeout after ${context.maxAttempts} attempts" }
                ErrorResolution.Fail(
                    reason = "Connection timeout",
                    details = "Failed to connect to database after ${context.maxAttempts} attempts"
                )
            }

            error.message?.contains("refused", ignoreCase = true) == true -> {
                logger.error { "Connection refused" }
                ErrorResolution.Fail(
                    reason = "Connection refused",
                    details = "Database server is not available or connection parameters are incorrect"
                )
            }

            else -> {
                logger.error { "Connection error: ${error.message}" }
                ErrorResolution.Fail(
                    reason = "Connection failed",
                    details = error.message ?: "Unknown connection error"
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
