package io.github.alkoleft.mcp.infrastructure.platform.dsl.common

/**
 * Базовый контекст DSL для работы с командами конфигуратора
 *
 * @param platformContext контекст для работы с утилитами платформы
 */
abstract class DslContext(
    protected val platformContext: PlatformUtilities,
) {
    /** Имя пользователя для подключения */
    protected var user: String? = null

    /** Пароль для подключения */
    protected var password: String? = null

    /**
     * Строит базовые аргументы для команд конфигуратора
     *
     * @return список базовых аргументов
     */
    abstract fun buildBaseArgs(): List<String>
}
