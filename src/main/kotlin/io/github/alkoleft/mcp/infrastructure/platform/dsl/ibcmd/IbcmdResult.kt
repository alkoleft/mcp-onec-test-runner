package io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd

import kotlin.time.Duration

/**
 * Результат выполнения команды ibcmd
 */
data class IbcmdResult(
    val success: Boolean,
    val output: String,
    val error: String,
    val exitCode: Int,
    val duration: Duration,
) {
    /**
     * Проверяет, была ли команда выполнена успешно
     */
    fun isSuccessful(): Boolean = success && exitCode == 0

    /**
     * Получает полное сообщение об ошибке
     */
    fun getErrorMessage(): String =
        if (error.isNotEmpty()) {
            "Ошибка выполнения команды ibcmd (код: $exitCode): $error"
        } else {
            "Команда ibcmd завершена с кодом: $exitCode"
        }

    /**
     * Получает краткое описание результата
     */
    fun getSummary(): String =
        if (isSuccessful()) {
            "Команда выполнена успешно за ${duration.inWholeMilliseconds}ms"
        } else {
            "Команда завершена с ошибкой (код: $exitCode) за ${duration.inWholeMilliseconds}ms"
        }
}
