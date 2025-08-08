package io.github.alkoleft.mcp.infrastructure.strategy.handlers

import io.github.alkoleft.mcp.core.modules.strategy.ErrorContext
import io.github.alkoleft.mcp.core.modules.strategy.ErrorHandler
import io.github.alkoleft.mcp.core.modules.strategy.ErrorResolution
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Duration

private val logger = KotlinLogging.logger { }

/**
 * Обработчик ошибок выполнения процессов
 * Обрабатывает ошибки запуска и выполнения процессов 1С:Предприятие
 */
class ProcessErrorHandler : ErrorHandler {

    private var nextHandler: ErrorHandler? = null

    override fun canHandle(error: Throwable): Boolean {
        val message = error.message?.lowercase() ?: ""
        return message.contains("process") ||
                message.contains("execution") ||
                message.contains("exit") ||
                message.contains("command") ||
                message.contains("executable") ||
                message.contains("permission") ||
                message.contains("not found") ||
                message.contains("не найден")
    }

    override fun handle(error: Throwable, context: ErrorContext): ErrorResolution {
        logger.warn { "Handling process error: ${error.message}" }

        return when {
            error.message?.contains("not found", ignoreCase = true) == true -> {
                logger.error { "Executable not found" }
                ErrorResolution.Fail(
                    reason = "Executable not found",
                    details = "1С:Предприятие executable not found at specified location"
                )
            }

            error.message?.contains("permission", ignoreCase = true) == true -> {
                logger.error { "Permission denied" }
                ErrorResolution.Fail(
                    reason = "Permission denied",
                    details = "Insufficient permissions to execute 1С:Предприятие"
                )
            }

            error.message?.contains("exit", ignoreCase = true) == true -> {
                logger.error { "Process exited with error" }
                ErrorResolution.Fail(
                    reason = "Process execution failed",
                    details = "1С:Предприятие process exited with error: ${error.message}"
                )
            }

            context.attempt < context.maxAttempts -> {
                logger.info { "Retrying process execution (attempt ${context.attempt}/${context.maxAttempts})" }
                ErrorResolution.Retry(
                    maxAttempts = context.maxAttempts,
                    delay = Duration.ofSeconds(3),
                    reason = "Process execution failed, retrying"
                )
            }

            else -> {
                logger.error { "Process error: ${error.message}" }
                ErrorResolution.Fail(
                    reason = "Process execution error",
                    details = error.message ?: "Unknown process execution error"
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
