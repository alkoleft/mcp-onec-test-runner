package io.github.alkoleft.mcp.infrastructure.yaxunit

import io.github.alkoleft.mcp.core.modules.RunAllTestsRequest
import io.github.alkoleft.mcp.core.modules.RunListTestsRequest
import io.github.alkoleft.mcp.core.modules.RunModuleTestsRequest
import io.github.alkoleft.mcp.core.modules.TestExecutionRequest
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Files
import java.nio.file.Path

private val logger = KotlinLogging.logger { }

/**
 * Реализация построителя конфигурации YaXUnit
 * Использует Builder Pattern для создания конфигурации
 */
class YaXUnitConfigBuilderImpl : YaXUnitConfigBuilder {

    private var modules: List<String> = emptyList()
    private var tests: List<String> = emptyList()
    private var reportFormat: String = "jUnit"
    private var reportPath: Path? = null
    private var logging: LoggingConfig = LoggingConfig()

    override fun withModuleFilter(modules: List<String>): YaXUnitConfigBuilder {
        this.modules = modules
        logger.debug { "Added module filter: $modules" }
        return this
    }

    override fun withTestFilter(tests: List<String>): YaXUnitConfigBuilder {
        this.tests = tests
        logger.debug { "Added test filter: $tests" }
        return this
    }

    override fun withReportFormat(format: String): YaXUnitConfigBuilder {
        this.reportFormat = format
        logger.debug { "Set report format: $format" }
        return this
    }

    override fun withReportPath(path: Path): YaXUnitConfigBuilder {
        this.reportPath = path
        logger.debug { "Set report path: $path" }
        return this
    }

    override fun withLogging(logging: LoggingConfig): YaXUnitConfigBuilder {
        this.logging = logging
        logger.debug { "Set logging configuration" }
        return this
    }

    override fun build(): YaXUnitConfig {
        logger.debug { "Building YaXUnit configuration" }

        val filter = if (modules.isNotEmpty() || tests.isNotEmpty()) {
            TestFilter(modules = modules, tests = tests)
        } else null

        val finalReportPath = reportPath ?: determineDefaultReportPath()

        val config = YaXUnitConfig(
            filter = filter,
            reportFormat = reportFormat,
            reportPath = finalReportPath.toString(),
            closeAfterTests = true,
            showReport = false,
            logging = logging
        )

        logger.info { "YaXUnit configuration built successfully" }
        return config
    }

    override fun validate(): ValidationResult {
        logger.debug { "Validating YaXUnit configuration" }

        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        // Проверяем обязательные поля
        if (reportFormat.isBlank()) {
            errors.add("Report format is required")
        }

        if (reportPath == null) {
            warnings.add("Report path not specified, will use default")
        }

        // Проверяем формат отчета
        if (reportFormat != "jUnit" && reportFormat != "json" && reportFormat != "xml") {
            errors.add("Unsupported report format: $reportFormat")
        }

        // Проверяем фильтры
        if (modules.isNotEmpty() && tests.isNotEmpty()) {
            warnings.add("Both module and test filters specified, test filter will take precedence")
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
     * Определяет путь к отчету по умолчанию
     */
    private fun determineDefaultReportPath(): Path {
        return Path.of("reports").resolve("junit.xml")
    }

    /**
     * Создает конфигурацию на основе запроса на выполнение тестов
     */
    fun createFromRequest(request: TestExecutionRequest): YaXUnitConfig {
        logger.debug { "Creating configuration from request: ${request.javaClass.simpleName}" }

        // Сбрасываем состояние
        modules = emptyList()
        tests = emptyList()
        reportFormat = "jUnit"
        reportPath = null
        logging = LoggingConfig()

        // Настраиваем фильтры в зависимости от типа запроса
        when (request) {
            is RunAllTestsRequest -> {}
            is RunModuleTestsRequest -> withModuleFilter(listOf(request.moduleName))
            is RunListTestsRequest -> withTestFilter(request.testNames)
        }

        // Устанавливаем путь к отчету
        withReportPath(Files.createTempFile("yaxunit-report-", ".xml"))

        // Настраиваем логирование
        withLogging(
            LoggingConfig(
                file = Files.createTempFile("yaxunit-launch-", ".log").toString(),
                console = false,
                level = "info"
            )
        )

        return build()
    }
}
