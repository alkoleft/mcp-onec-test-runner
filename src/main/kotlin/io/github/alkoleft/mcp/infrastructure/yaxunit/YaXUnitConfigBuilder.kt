package io.github.alkoleft.mcp.infrastructure.yaxunit

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
     * Строит конфигурацию
     */
    fun build(): YaXUnitConfig

    /**
     * Валидирует конфигурацию
     */
    fun validate(): ValidationResult
}

/**
 * Результат валидации
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
)
