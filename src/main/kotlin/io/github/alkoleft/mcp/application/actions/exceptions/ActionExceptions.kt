package io.github.alkoleft.mcp.application.actions.exceptions

/**
 * Структурированная иерархия ошибок с контекстом
 */
sealed class ActionError(
    message: String,
    cause: Throwable? = null,
    val context: Map<String, Any> = emptyMap(),
) : Exception(message, cause)

/**
 * Ошибка сборки с контекстом
 */
class BuildError(
    message: String,
    cause: Throwable? = null,
    context: Map<String, Any> = emptyMap(),
) : ActionError(message, cause, context)

/**
 * Ошибка конфигурации с контекстом
 */
class ConfigurationError(
    message: String,
    cause: Throwable? = null,
    context: Map<String, Any> = emptyMap(),
) : ActionError(message, cause, context)

/**
 * Ошибка валидации с контекстом
 */
class ValidationError(
    message: String,
    cause: Throwable? = null,
    context: Map<String, Any> = emptyMap(),
) : ActionError(message, cause, context)

/**
 * Ошибка анализа изменений с контекстом
 */
class AnalysisError(
    message: String,
    cause: Throwable? = null,
    context: Map<String, Any> = emptyMap(),
) : ActionError(message, cause, context)

/**
 * Ошибка выполнения тестов с контекстом
 */
class TestExecutionError(
    message: String,
    cause: Throwable? = null,
    context: Map<String, Any> = emptyMap(),
) : ActionError(message, cause, context)

// Обратная совместимость с существующими исключениями
sealed class ActionException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

// Алиасы для обратной совместимости
typealias BuildException = BuildError
typealias AnalyzeException = AnalysisError
typealias TestExecuteException = TestExecutionError
