package io.github.alkoleft.mcp.core.modules.strategy

import java.nio.file.Path

/**
 * Построитель конфигурации YaXUnit с использованием Builder Pattern
 * Обеспечивает гибкую настройку всех параметров запуска тестов
 */
interface YaXUnitConfigBuilder {
    /**
     * Добавляет фильтр по модулям
     */
    fun withModuleFilter(modules: List<String>): YaXUnitConfigBuilder

    /**
     * Добавляет фильтр по тестам
     */
    fun withTestFilter(tests: List<String>): YaXUnitConfigBuilder

    /**
     * Устанавливает формат отчета
     */
    fun withReportFormat(format: String): YaXUnitConfigBuilder

    /**
     * Устанавливает путь к отчету
     */
    fun withReportPath(path: Path): YaXUnitConfigBuilder

    /**
     * Устанавливает настройки логирования
     */
    fun withLogging(logging: LoggingConfig): YaXUnitConfigBuilder

    /**
     * Устанавливает дополнительные параметры
     */
    fun withParameter(key: String, value: Any): YaXUnitConfigBuilder

    /**
     * Устанавливает параметры подключения
     */
    fun withConnection(connection: ConnectionConfig): YaXUnitConfigBuilder

    /**
     * Строит конфигурацию
     */
    fun build(): YaXUnitConfig

    /**
     * Валидирует конфигурацию
     */
    fun validate(): ValidationResult
}

/**
 * Конфигурация логирования
 */
data class LoggingConfig(
    val file: Path? = null,
    val console: Boolean = false,
    val level: String = "info",
    val includeTimestamp: Boolean = true,
    val includeTestDetails: Boolean = true
)

/**
 * Конфигурация подключения
 */
data class ConnectionConfig(
    val connectionString: String,
    val username: String? = null,
    val password: String? = null,
    val timeout: Int? = null
)

/**
 * Конфигурация YaXUnit
 */
data class YaXUnitConfig(
    val filter: TestFilter? = null,
    val reportFormat: String = "jUnit",
    val reportPath: Path? = null,
    val closeAfterTests: Boolean = true,
    val showReport: Boolean = false,
    val logging: LoggingConfig = LoggingConfig(),
    val connection: ConnectionConfig? = null,
    val additionalParameters: Map<String, Any> = emptyMap()
)

/**
 * Фильтр тестов
 */
data class TestFilter(
    val modules: List<String> = emptyList(),
    val tests: List<String> = emptyList()
)

/**
 * Результат валидации
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
)
