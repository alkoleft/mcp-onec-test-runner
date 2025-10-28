package io.github.alkoleft.mcp.infrastructure.platform.dsl.common

import kotlin.time.Duration

/**
 * Базовый контекст DSL для работы с командами конфигуратора
 *
 * @param platformContext контекст для работы с утилитами платформы
 */
abstract class DslContext(
    protected val platformContext: PlatformUtilityContext,
) {
    /** Имя пользователя для подключения */
    protected var user: String? = null

    /** Пароль для подключения */
    protected var password: String? = null

    /**
     * Устанавливает результат выполнения команды
     *
     * @param success флаг успешного выполнения
     * @param output вывод команды
     * @param error текст ошибки
     * @param exitCode код завершения
     * @param duration длительность выполнения
     */
    fun setResult(
        success: Boolean,
        output: String,
        error: String?,
        exitCode: Int,
        duration: Duration,
    ) {
        platformContext.setResult(success, output, error, exitCode, duration)
    }

    /**
     * Строит результат выполнения операций
     *
     * @return результат выполнения команды
     */
    fun buildResult() = platformContext.buildResult()

    /**
     * Строит базовые аргументы для команд конфигуратора
     *
     * @return список базовых аргументов
     */
    abstract fun buildBaseArgs(): List<String>

}