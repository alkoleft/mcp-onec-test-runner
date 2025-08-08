package io.github.alkoleft.mcp.infrastructure.yaxunit

/**
 * Конфигурация YaXUnit
 */
data class YaXUnitConfig(
    val filter: TestFilter? = null,
    val reportFormat: String = "jUnit",
    val reportPath: String? = null,
    val closeAfterTests: Boolean = true,
    val showReport: Boolean = false,
    val logging: LoggingConfig = LoggingConfig(),
    val additionalParameters: Map<String, Any> = emptyMap()
)

/**
 * Конфигурация логирования
 */
data class LoggingConfig(
    val file: String? = null,
    val console: Boolean = false,
    val level: String = "info",
    val includeTimestamp: Boolean = true,
    val includeTestDetails: Boolean = true
)

/**
 * Фильтр тестов
 */
data class TestFilter(
    val modules: List<String> = emptyList(),
    val tests: List<String> = emptyList()
)
