package io.github.alkoleft.mcp.infrastructure.platform.dsl.configurator

/**
 * Режим динамического обновления
 */
enum class DynamicMode(val value: String) {
    PLUS("+"),
    MINUS("-")
}

/**
 * Режим завершения сеансов
 */
enum class SessionTerminateMode(val value: String) {
    DISABLE("disable"),
    FORCE("force")
}

/**
 * Формат загрузки файлов конфигурации
 * Используется для частичной загрузки в команде LoadConfigFromFiles
 */
enum class LoadFormat(val value: String) {
    /**
     * Загрузка выполняется в иерархическом формате (по умолчанию)
     */
    HIERARCHICAL("Hierarchical"),

    /**
     * Загрузка выполняется в линейном формате
     */
    PLAIN("Plain")
} 