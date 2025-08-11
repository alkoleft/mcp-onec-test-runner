package io.github.alkoleft.mcp.application.actions.test

import io.github.alkoleft.mcp.application.actions.RunTestAction
import io.github.alkoleft.mcp.application.actions.exceptions.TestExecuteException
import io.github.alkoleft.mcp.core.modules.GenericTestReport
import io.github.alkoleft.mcp.core.modules.TestExecutionRequest
import io.github.alkoleft.mcp.core.modules.TestExecutionResult
import io.github.alkoleft.mcp.core.modules.UtilityType
import io.github.alkoleft.mcp.core.modules.YaXUnitExecutionResult
import io.github.alkoleft.mcp.infrastructure.platform.dsl.PlatformDsl
import io.github.alkoleft.mcp.infrastructure.platform.locator.UtilityLocator
import io.github.alkoleft.mcp.infrastructure.yaxunit.ReportParser
import io.github.alkoleft.mcp.infrastructure.yaxunit.YaXUnitRunner
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private val logger = KotlinLogging.logger { }

/**
 * Реализация RunTestAction для тестирования через YaXUnit
 * Поддерживает запуск всех тестов, тестов модуля и конкретных тестов
 * Интегрирован со стратегиями обработки ошибок
 */
class YaXUnitTestAction(
    private val platformDsl: PlatformDsl,
    private val utilLocator: UtilityLocator,
    private val reportParser: ReportParser,
) : RunTestAction {
    @OptIn(ExperimentalTime::class)
    override suspend fun run(request: TestExecutionRequest): TestExecutionResult {
        val startTime = Clock.System.now()
        logger.info { "Starting YaXUnit test execution with filter: $request" }

        return withContext(Dispatchers.IO) {
            try {
                // Локализуем утилиту 1С:Предприятие
                logger.debug { "Locating ENTERPRISE utility for version: ${request.platformVersion}" }
                val utilityLocation = utilLocator.locateUtility(UtilityType.THIN_CLIENT, request.platformVersion)
                logger.info { "Found ENTERPRISE utility at: ${utilityLocation.executablePath}" }

                // Создаем runner и выполняем тесты
                val runner = YaXUnitRunner(platformDsl)
                logger.info { "Executing tests via ProcessYaXUnitRunner" }
                val executionResult = runner.executeTests(utilityLocation, request)

                // Парсим отчет если он был создан
                val report = parseTestReport(executionResult)

                val duration = Clock.System.now().minus(startTime)

                // Формируем результат
                val testsRun = report?.summary?.totalTests ?: 0
                val testsPassed = report?.summary?.passed ?: 0
                val testsFailed = report?.summary?.failed ?: 0

                logger.info {
                    "YaXUnit test execution completed: $testsRun tests, $testsPassed passed, $testsFailed failed in ${duration.inWholeSeconds}s"
                }

                TestExecutionResult(
                    success = executionResult.success,
                    reportPath = executionResult.reportPath!!,
                    report = report!!,
                    duration = duration,
                )
            } catch (e: Exception) {
                val duration = Clock.System.now().minus(startTime)
                logger.error(e) { "YaXUnit test execution failed after ${duration.inWholeSeconds}s" }
                throw TestExecuteException("YaXUnit test execution failed: ${e.message}", e)
            }
        }
    }

    /**
     * Парсит отчет о тестировании
     */
    private suspend fun parseTestReport(executionResult: YaXUnitExecutionResult): GenericTestReport? =
        if (executionResult.reportPath != null && Files.exists(executionResult.reportPath)) {
            try {
                logger.debug { "Parsing test report from: ${executionResult.reportPath}" }

                val inputStream = Files.newInputStream(executionResult.reportPath)
                val format = reportParser.detectFormat(inputStream)
                inputStream.close()

                logger.debug { "Detected report format: $format" }

                val reportInputStream = Files.newInputStream(executionResult.reportPath)
                val report = reportParser.parseReport(reportInputStream, format)

                logger.info { "Successfully parsed test report: ${report.summary.totalTests} tests" }
                report
            } catch (e: Exception) {
                logger.warn(e) { "Failed to parse test report from ${executionResult.reportPath}" }
                null
            }
        } else {
            logger.warn { "No test report found at expected location" }
            null
        }
}
