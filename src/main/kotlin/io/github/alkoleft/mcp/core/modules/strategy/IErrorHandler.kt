package io.github.alkoleft.mcp.core.modules.strategy

import java.time.Duration

/**
 * Обработчик ошибок с поддержкой Chain of Responsibility
 * Обеспечивает детальную обработку различных типов ошибок
 */
interface IErrorHandler {
    /**
     * Проверяет, может ли обработчик обработать указанную ошибку
     */
    fun canHandle(error: Throwable): Boolean

    /**
     * Обрабатывает ошибку и возвращает решение
     */
    fun handle(error: Throwable, context: ErrorContext): ErrorResolution

    /**
     * Устанавливает следующий обработчик в цепочке
     */
    fun setNext(handler: IErrorHandler): IErrorHandler

    /**
     * Возвращает следующий обработчик в цепочке
     */
    fun getNext(): IErrorHandler?
}

/**
 * Контекст ошибки для передачи дополнительной информации
 */
data class ErrorContext(
    val request: String? = null,
    val utilityLocation: String? = null,
    val configPath: String? = null,
    val attempt: Int = 1,
    val maxAttempts: Int = 3,
    val additionalInfo: Map<String, Any> = emptyMap()
)

/**
 * Решение обработчика ошибок
 */
sealed class ErrorResolution {
    object Success : ErrorResolution()

    data class Retry(
        val maxAttempts: Int,
        val delay: Duration,
        val reason: String? = null
    ) : ErrorResolution()

    data class Fail(
        val reason: String,
        val details: String? = null
    ) : ErrorResolution()

    data class Recover(
        val action: String,
        val fallbackData: Any? = null
    ) : ErrorResolution()
}
