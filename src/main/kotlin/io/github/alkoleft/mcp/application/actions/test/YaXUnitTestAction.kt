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
    override fun run(request: TestExecutionRequest): TestExecutionResult {
        val startTime = Clock.System.now()
        logger.info { "Запуск выполнения тестов YaXUnit с фильтром: $request" }

        try {
            // Локализуем утилиту 1С:Предприятие
            logger.debug { "Поиск утилиты 1С:Предприятие для версии: ${request.platformVersion}" }
            val utilityLocation = utilLocator.locateUtility(UtilityType.THIN_CLIENT, request.platformVersion)
            logger.info { "Утилита 1С:Предприятие найдена по пути: ${utilityLocation.executablePath}" }

            // Создаем runner и выполняем тесты
            val runner = YaXUnitRunner(platformDsl)
            val executionResult = runner.executeTests(utilityLocation, request)

            // Парсим отчет если он был создан
            val report = parseTestReport(executionResult)

            val duration = Clock.System.now().minus(startTime)

            // Формируем результат
            val testsRun = report?.summary?.totalTests ?: 0
            val testsPassed = report?.summary?.passed ?: 0
            val testsFailed = report?.summary?.failed ?: 0

            logger.info {
                "Выполнение тестов YaXUnit завершено: $testsRun тестов, $testsPassed пройдено, $testsFailed провалено за ${duration.inWholeSeconds}с"
            }

            return TestExecutionResult(
                success = executionResult.success,
                reportPath = executionResult.reportPath ?: throw IllegalStateException("YaXUnit не вернул путь к отчёту"),
                report = report ?: throw IllegalStateException("Не удалось прочитать отчёт YaXUnit"),
                duration = duration,
            )
        } catch (e: Exception) {
            val duration = Clock.System.now().minus(startTime)
            logger.error(e) { "Выполнение тестов YaXUnit завершилось с ошибкой после ${duration.inWholeSeconds}с" }
            throw TestExecuteException("Выполнение тестов YaXUnit завершилось с ошибкой: ${e.message}", e)
        }
    }

    /**
     * Парсит отчет о тестировании
     */
    private fun parseTestReport(executionResult: YaXUnitExecutionResult): GenericTestReport? =
        if (executionResult.reportPath != null && Files.exists(executionResult.reportPath)) {
            try {
                logger.debug { "Парсинг отчета о тестах из: ${executionResult.reportPath}" }

                val inputStream = Files.newInputStream(executionResult.reportPath)
                val format = reportParser.detectFormat(inputStream)
                inputStream.close()

                logger.debug { "Обнаружен формат отчета: $format" }

                val reportInputStream = Files.newInputStream(executionResult.reportPath)
                val report = reportParser.parseReport(reportInputStream, format)

                logger.info { "Отчет о тестах успешно проанализирован: ${report.summary.totalTests} тестов" }
                report
            } catch (e: Exception) {
                logger.warn(e) { "Не удалось проанализировать отчет о тестах из ${executionResult.reportPath}" }
                null
            }
        } else {
            logger.warn { "Отчет о тестах не найден по ожидаемому пути" }
            null
        }
}
