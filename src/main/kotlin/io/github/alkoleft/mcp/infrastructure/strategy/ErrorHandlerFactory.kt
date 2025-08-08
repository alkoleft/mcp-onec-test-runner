package io.github.alkoleft.mcp.infrastructure.strategy

import io.github.alkoleft.mcp.core.modules.strategy.ErrorHandler
import io.github.alkoleft.mcp.infrastructure.strategy.handlers.ConfigurationErrorHandler
import io.github.alkoleft.mcp.infrastructure.strategy.handlers.ConnectionErrorHandler
import io.github.alkoleft.mcp.infrastructure.strategy.handlers.DefaultErrorHandler
import io.github.alkoleft.mcp.infrastructure.strategy.handlers.ProcessErrorHandler
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger { }

/**
 * Фабрика для создания обработчиков ошибок
 * Создает цепочку обработчиков с Chain of Responsibility
 */
class ErrorHandlerFactory {

    /**
     * Создает цепочку обработчиков ошибок
     */
    fun createErrorHandlerChain(): ErrorHandler {
        logger.debug { "Creating error handler chain" }

        // Создаем специализированные обработчики
        val connectionHandler = ConnectionErrorHandler()
        val configurationHandler = ConfigurationErrorHandler()
        val processHandler = ProcessErrorHandler()
        val defaultHandler = DefaultErrorHandler()

        // Строим цепочку: Connection -> Configuration -> Process -> Default
        connectionHandler.setNext(configurationHandler)
        configurationHandler.setNext(processHandler)
        processHandler.setNext(defaultHandler)

        logger.debug { "Error handler chain created successfully" }
        return connectionHandler
    }

    /**
     * Создает обработчик для конкретного типа ошибки
     */
    fun createHandlerForError(error: Throwable): ErrorHandler {
        return when {
            error.message?.contains("connection", ignoreCase = true) == true -> ConnectionErrorHandler()
            error.message?.contains("config", ignoreCase = true) == true -> ConfigurationErrorHandler()
            error.message?.contains("process", ignoreCase = true) == true -> ProcessErrorHandler()
            else -> DefaultErrorHandler()
        }
    }

    /**
     * Создает минимальную цепочку обработчиков
     */
    fun createMinimalChain(): ErrorHandler {
        logger.debug { "Creating minimal error handler chain" }

        val processHandler = ProcessErrorHandler()
        val defaultHandler = DefaultErrorHandler()

        processHandler.setNext(defaultHandler)

        return processHandler
    }
}
