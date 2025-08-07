package io.github.alkoleft.mcp.infrastructure.process

import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.core.modules.TestExecutionRequest
import io.github.alkoleft.mcp.core.modules.UtilityLocation
import io.github.alkoleft.mcp.core.modules.YaXUnitExecutionResult
import io.github.alkoleft.mcp.core.modules.YaXUnitRunner
import io.github.alkoleft.mcp.infrastructure.platform.dsl.PlatformUtilityDsl
import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.PlatformUtilityResult
import io.github.alkoleft.mcp.infrastructure.strategy.ErrorHandlerFactory
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.Instant

private val logger = KotlinLogging.logger { }

/**
 * Реализация YaXUnitRunner для запуска тестов через 1С:Предприятие
 * Интегрирован со стратегиями построения команд и обработки ошибок
 */
class YaXUnitRunner(
    private val properties: ApplicationProperties,
    private val platformUtilityDsl: PlatformUtilityDsl,
    private val configWriter: JsonYaXUnitConfigWriter
) : YaXUnitRunner {

    private val errorHandlerFactory = ErrorHandlerFactory()

    override suspend fun executeTests(
        utilityLocation: UtilityLocation,
        request: TestExecutionRequest
    ): YaXUnitExecutionResult = withContext(Dispatchers.IO) {
        val startTime = Instant.now()
        logger.info { "Starting YaXUnit test execution for ${request.javaClass.simpleName}" }

        // Создаем временную конфигурацию
        logger.debug { "Creating temporary configuration" }
        val configPath = configWriter.createTempConfig(request)
        logger.debug { "Configuration created at: $configPath" }

        try {
            // Запускаем тесты через EnterpriseDsl
            logger.debug { "Executing tests via EnterpriseDsl" }
            val result = executeTests(
                request = request,
                configPath = configPath
            )

            logger.info { "Process completed with exit code: ${result.exitCode}" }

            val duration = Duration.between(startTime, Instant.now())
            logger.info { "Test execution completed in ${duration.toSeconds()}s" }

            // Определяем путь к отчету
            val reportPath = determineReportPath(request, configPath)
            if (reportPath != null && Files.exists(reportPath)) {
                logger.info { "Test report found at: $reportPath" }
            } else {
                logger.warn { "Test report not found at expected location" }
            }

            YaXUnitExecutionResult(
                success = result.success,
                reportPath = if (result.success) reportPath else null,
                exitCode = result.exitCode,
                standardOutput = result.output,
                errorOutput = result.error ?: "",
                duration = duration
            )

        } catch (e: Exception) {
            val duration = Duration.between(startTime, Instant.now())
            logger.error(e) { "YaXUnit test execution failed after ${duration.toSeconds()}s" }
            YaXUnitExecutionResult(
                success = false,
                reportPath = null,
                exitCode = -1,
                standardOutput = "",
                errorOutput = e.message ?: "Unknown error",
                duration = duration
            )
        }
    }

    /**
     * Строит аргументы команды для запуска 1С:Предприятие
     */
    private suspend fun executeTests(
        request: TestExecutionRequest,
        configPath: Path
    ): PlatformUtilityResult = platformUtilityDsl.enterprise(request.platformVersion) {
        connect(request.ibConnection)
        properties.connection.user?.let { user(it) }
        properties.connection.password?.let { password(it) }
        runArguments("RunUnitTests=${configPath.toAbsolutePath()}")
    }.run()

    /**
     * Определяет путь к отчету о тестировании
     */
    private fun determineReportPath(request: TestExecutionRequest, configPath: Path): Path? {
        // Пытаемся найти отчет в нескольких возможных местах
        val possiblePaths = listOf(
            request.testsPath.resolve("reports").resolve("report.xml"),
            request.testsPath.resolve("reports").resolve("junit.xml"),
            request.testsPath.resolve("report.xml"),
            request.testsPath.resolve("junit.xml"),
            configPath.parent.resolve("report.xml"),
            configPath.parent.resolve("junit.xml")
        )

        logger.debug { "Searching for test report in possible paths: ${possiblePaths.joinToString(", ")}" }

        for (path in possiblePaths) {
            if (Files.exists(path)) {
                logger.info { "Found test report at: $path" }
                return path
            }
        }

        logger.warn { "Test report not found in any expected location" }
        return null
    }
}
