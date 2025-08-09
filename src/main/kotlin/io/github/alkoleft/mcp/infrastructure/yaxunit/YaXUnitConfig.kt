package io.github.alkoleft.mcp.infrastructure.yaxunit

import io.github.alkoleft.mcp.core.modules.RunAllTestsRequest
import io.github.alkoleft.mcp.core.modules.RunListTestsRequest
import io.github.alkoleft.mcp.core.modules.RunModuleTestsRequest
import io.github.alkoleft.mcp.core.modules.TestExecutionRequest
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger { }


/**
 * Конфигурация YaXUnit
 */
data class YaXUnitConfig(
    val filter: TestFilter?,
    val reportFormat: String,
    val reportPath: String,
    val closeAfterTests: Boolean,
    val showReport: Boolean,
    val logging: LoggingConfig,
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
    val modules: List<String>? = null,
    val tests: List<String>? = null
)

fun TestExecutionRequest.toConfig(): YaXUnitConfig {
    return YaXUnitConfig(
        filter = filter(this),
        reportPath = reportPath(),
        logging = LoggingConfig(
            file = logPath(),
            console = false,
            level = "info"
        ),
        closeAfterTests = true,
        reportFormat = "jUnit",
        showReport = false
    )
}

fun filter(request: TestExecutionRequest) =
    when (request) {
        is RunAllTestsRequest -> null
        is RunModuleTestsRequest -> TestFilter(modules = listOf(request.moduleName))
        is RunListTestsRequest -> TestFilter(tests = request.testNames)
    }

fun YaXUnitConfig.validate(): ValidationResult {
    logger.debug { "Validating YaXUnit configuration" }

    val errors = mutableListOf<String>()
    val warnings = mutableListOf<String>()

    // Проверяем обязательные поля
    if (reportFormat.isBlank()) {
        errors.add("Report format is required")
    }

    if (reportPath.isEmpty()) {
        errors.add("Report path not specified, will use default")
    }

    // Проверяем формат отчета
    if (reportFormat != "jUnit" && reportFormat != "json" && reportFormat != "xml") {
        errors.add("Unsupported report format: $reportFormat")
    }

    // Проверяем фильтры
    filter?.modules?.also {
        if (it.isEmpty()) {
            warnings.add("Empty modules filter specified")
        }
    }
    filter?.tests?.also {
        if (it.isEmpty()) {
            warnings.add("Empty tests filter specified")
        }
    }

    // Проверяем логирование
    if (logging.file == null && !logging.console) {
        warnings.add("No logging output specified")
    }

    val isValid = errors.isEmpty()

    logger.debug { "Configuration validation completed: isValid=$isValid, errors=${errors.size}, warnings=${warnings.size}" }

    return ValidationResult(
        isValid = isValid,
        errors = errors,
        warnings = warnings
    )

}

/**
 * Результат валидации
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
)