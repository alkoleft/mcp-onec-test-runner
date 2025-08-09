package io.github.alkoleft.mcp.application.actions.exceptions

import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * Централизованный обработчик ошибок с логированием и специфичной логикой
 */
object ErrorHandler {
    private val logger = KotlinLogging.logger { }

    /**
     * Обрабатывает ошибку действия с логированием и специфичной логикой
     */
    fun handleActionError(error: ActionError) {
        logger.error(error) {
            "Action error: ${error.message}, context: ${error.context}"
        }

        when (error) {
            is BuildError -> handleBuildError(error)
            is ConfigurationError -> handleConfigurationError(error)
            is ValidationError -> handleValidationError(error)
            is AnalysisError -> handleAnalysisError(error)
            is TestExecutionError -> handleTestExecutionError(error)
        }
    }

    /**
     * Обрабатывает ошибки сборки
     */
    private fun handleBuildError(error: BuildError) {
        logger.warn { "Build operation failed: ${error.context}" }
        // Здесь можно добавить специфичную логику для ошибок сборки
        // например, очистка временных файлов, уведомления и т.д.
    }

    /**
     * Обрабатывает ошибки конфигурации
     */
    private fun handleConfigurationError(error: ConfigurationError) {
        logger.warn { "Configuration error: ${error.context}" }
        // Здесь можно добавить специфичную логику для ошибок конфигурации
        // например, проверка файлов конфигурации, восстановление и т.д.
    }

    /**
     * Обрабатывает ошибки валидации
     */
    private fun handleValidationError(error: ValidationError) {
        logger.warn { "Validation error: ${error.context}" }
        // Здесь можно добавить специфичную логику для ошибок валидации
        // например, предложения по исправлению, проверка схемы и т.д.
    }

    /**
     * Обрабатывает ошибки анализа
     */
    private fun handleAnalysisError(error: AnalysisError) {
        logger.warn { "Analysis error: ${error.context}" }
        // Здесь можно добавить специфичную логику для ошибок анализа
        // например, повторный анализ, уведомления и т.д.
    }

    /**
     * Обрабатывает ошибки выполнения тестов
     */
    private fun handleTestExecutionError(error: TestExecutionError) {
        logger.warn { "Test execution error: ${error.context}" }
        // Здесь можно добавить специфичную логику для ошибок тестов
        // например, сохранение отчетов, уведомления и т.д.
    }

    /**
     * Обрабатывает общие ошибки
     */
    private fun handleGenericError(error: ActionError) {
        logger.warn { "Generic action error: ${error.context}" }
        // Общая логика обработки ошибок
    }

    /**
     * Создает контекст ошибки с дополнительной информацией
     */
    fun createErrorContext(vararg pairs: Pair<String, Any>): Map<String, Any> = mapOf(*pairs)
}
