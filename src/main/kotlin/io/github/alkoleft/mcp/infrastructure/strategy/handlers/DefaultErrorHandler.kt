package io.github.alkoleft.mcp.infrastructure.strategy.handlers

import io.github.alkoleft.mcp.core.modules.strategy.ErrorContext
import io.github.alkoleft.mcp.core.modules.strategy.ErrorResolution
import io.github.alkoleft.mcp.core.modules.strategy.IErrorHandler
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger { }

/**
 * Обработчик ошибок по умолчанию
 * Обрабатывает все остальные ошибки, которые не были обработаны специализированными обработчиками
 */
class DefaultErrorHandler : IErrorHandler {

    private var nextHandler: IErrorHandler? = null

    override fun canHandle(error: Throwable): Boolean {
        // Обрабатывает все ошибки, которые дошли до этого обработчика
        return true
    }

    override fun handle(error: Throwable, context: ErrorContext): ErrorResolution {
        logger.error { "Handling error with default handler: ${error.message}" }

        return ErrorResolution.Fail(
            reason = "Unknown error",
            details = "Unhandled error occurred: ${error.message ?: "Unknown error"}"
        )
    }

    override fun setNext(handler: IErrorHandler): IErrorHandler {
        nextHandler = handler
        return this
    }

    override fun getNext(): IErrorHandler? = nextHandler
}
