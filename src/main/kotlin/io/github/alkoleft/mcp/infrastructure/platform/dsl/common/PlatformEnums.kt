package io.github.alkoleft.mcp.infrastructure.platform.dsl.common

/**
 * Режим динамического обновления
 */
enum class DynamicMode(
    val value: String,
) {
    /**
     * Включить динамическое обновление
     */
    PLUS("+"),

    /**
     * Выключить динамическое обновление
     */
    MINUS("-"),
}

/**
 * Режим завершения сеансов
 */
enum class SessionTerminateMode(
    val value: String,
) {
    /**
     * Не завершать активные сеансы
     */
    DISABLE("disable"),

    /**
     * Принудительно завершить активные сеансы
     */
    FORCE("force"),
}

/**
 * Формат загрузки файлов конфигурации
 * Используется для частичной загрузки в команде LoadConfigFromFiles
 */
enum class LoadFormat(
    val value: String,
) {
    /**
     * Загрузка выполняется в иерархическом формате (по умолчанию)
     */
    HIERARCHICAL("Hierarchical"),

    /**
     * Загрузка выполняется в линейном формате
     */
    PLAIN("Plain"),
}
