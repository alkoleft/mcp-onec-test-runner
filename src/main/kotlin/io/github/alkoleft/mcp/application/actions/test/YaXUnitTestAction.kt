package io.github.alkoleft.mcp.application.actions.test

import io.github.alkoleft.mcp.application.actions.RunTestAction
import io.github.alkoleft.mcp.application.actions.TestExecutionResult
import io.github.alkoleft.mcp.application.actions.exceptions.TestExecuteException
import io.github.alkoleft.mcp.core.modules.GenericTestReport
import io.github.alkoleft.mcp.core.modules.TestExecutionRequest
import io.github.alkoleft.mcp.core.modules.TestStatus
import io.github.alkoleft.mcp.core.modules.UtilityType
import io.github.alkoleft.mcp.core.modules.YaXUnitExecutionResult
import io.github.alkoleft.mcp.core.modules.strategy.ErrorContext
import io.github.alkoleft.mcp.core.modules.strategy.ErrorResolution
import io.github.alkoleft.mcp.infrastructure.platform.CrossPlatformUtilLocator
import io.github.alkoleft.mcp.infrastructure.platform.dsl.PlatformUtilityDsl
import io.github.alkoleft.mcp.infrastructure.process.EnhancedReportParser
import io.github.alkoleft.mcp.infrastructure.process.JsonYaXUnitConfigWriter
import io.github.alkoleft.mcp.infrastructure.process.YaXUnitRunner
import io.github.alkoleft.mcp.infrastructure.strategy.ErrorHandlerFactory
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.time.Duration
import java.time.Instant

private val logger = KotlinLogging.logger { }

/**
 * Реализация RunTestAction для тестирования через YaXUnit
 * Поддерживает запуск всех тестов, тестов модуля и конкретных тестов
 * Интегрирован со стратегиями обработки ошибок
 */
class YaXUnitTestAction(
    private val platformUtilityDsl: PlatformUtilityDsl,
    private val utilLocator: CrossPlatformUtilLocator,
    private val configWriter: JsonYaXUnitConfigWriter,
    private val reportParser: EnhancedReportParser
) : RunTestAction {

    private val errorHandlerFactory = ErrorHandlerFactory()

    override suspend fun run(request: TestExecutionRequest): TestExecutionResult {
        val startTime = Instant.now()
        logger.info { "Starting YaXUnit test execution with filter: $request" }

        return withContext(Dispatchers.IO) {
            try {
                // Локализуем утилиту 1С:Предприятие
                logger.debug { "Locating ENTERPRISE utility for version: ${request.platformVersion}" }
                val utilityLocation = utilLocator.locateUtility(UtilityType.THIN_CLIENT, request.platformVersion)
                logger.info { "Found ENTERPRISE utility at: ${utilityLocation.executablePath}" }
                
                // Создаем runner и выполняем тесты
                val runner = YaXUnitRunner(platformUtilityDsl, configWriter)
                logger.info { "Executing tests via ProcessYaXUnitRunner" }
                val executionResult = runner.executeTests(utilityLocation, request)
                
                // Парсим отчет если он был создан
                val report = parseTestReport(executionResult)
                
                val duration = Duration.between(startTime, Instant.now())
                
                // Формируем результат
                val testsRun = report?.summary?.totalTests ?: 0
                val testsPassed = report?.summary?.passed ?: 0
                val testsFailed = report?.summary?.failed ?: 0
                
                val errors = buildErrorList(executionResult, report)
                
                logger.info { "YaXUnit test execution completed: $testsRun tests, $testsPassed passed, $testsFailed failed in ${duration.toSeconds()}s" }

                TestExecutionResult(
                    success = executionResult.success,
                    testsRun = testsRun,
                    testsPassed = testsPassed,
                    testsFailed = testsFailed,
                    reportPath = executionResult.reportPath,
                    errors = errors,
                    duration = duration
                )

            } catch (e: Exception) {
                val duration = Duration.between(startTime, Instant.now())
                logger.error(e) { "YaXUnit test execution failed after ${duration.toSeconds()}s" }

                // Обрабатываем ошибку с помощью цепочки обработчиков
                val errorHandler = errorHandlerFactory.createErrorHandlerChain()
                val errorContext = ErrorContext(
                    request = request,
                    utilityLocation = request.platformVersion,
                    configPath = null,
                    attempt = 1,
                    maxAttempts = 3
                )

                val resolution = errorHandler.handle(e, errorContext)
                when (resolution) {
                    is ErrorResolution.Retry -> {
                        logger.info { "Retrying test execution: ${resolution.reason}" }
                        // TODO: Реализовать повторную попытку
                        throw TestExecuteException("YaXUnit test execution failed: ${e.message}", e)
                    }

                    is ErrorResolution.Fail -> {
                        logger.error { "Test execution failed: ${resolution.reason}" }
                        throw TestExecuteException("YaXUnit test execution failed: ${resolution.reason}", e)
                    }

                    else -> {
                        throw TestExecuteException("YaXUnit test execution failed: ${e.message}", e)
                    }
                }
            }
        }
    }
    
    /**
     * Парсит отчет о тестировании
     */
    private suspend fun parseTestReport(executionResult: YaXUnitExecutionResult): GenericTestReport? {
        return if (executionResult.reportPath != null && Files.exists(executionResult.reportPath)) {
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
    
    /**
     * Строит список ошибок
     */
    private fun buildErrorList(
        executionResult: YaXUnitExecutionResult,
        report: GenericTestReport?
    ): List<String> {
        val errors = mutableListOf<String>()
        
        // Добавляем ошибки выполнения процесса
        if (!executionResult.success) {
            errors.add("Process execution failed: ${executionResult.errorOutput}")
        }
        
        // Добавляем ошибки из отчета
        report?.testSuites?.forEach { testSuite ->
            testSuite.testCases.filter { it.status == TestStatus.FAILED }
                .forEach { testCase ->
                    val errorMsg = testCase.errorMessage ?: "Test failed without error message"
                    errors.add("${testCase.name}: $errorMsg")
                }
        }
        
        return errors
    }
} 