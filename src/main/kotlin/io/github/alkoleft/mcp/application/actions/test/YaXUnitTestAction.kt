package io.github.alkoleft.mcp.application.actions.test

import io.github.alkoleft.mcp.application.actions.RunTestAction
import io.github.alkoleft.mcp.application.actions.TestExecutionResult
import io.github.alkoleft.mcp.application.actions.exceptions.TestExecuteException
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.infrastructure.platform.dsl.PlatformUtilityDsl
import io.github.alkoleft.mcp.infrastructure.process.ProcessYaXUnitRunner
import io.github.alkoleft.mcp.infrastructure.process.JsonYaXUnitConfigWriter
import io.github.alkoleft.mcp.infrastructure.process.EnhancedReportParser
import io.github.alkoleft.mcp.core.modules.RunAllTestsRequest
import io.github.alkoleft.mcp.core.modules.RunModuleTestsRequest
import io.github.alkoleft.mcp.core.modules.RunListTestsRequest
import io.github.alkoleft.mcp.core.modules.UtilityType
import io.github.alkoleft.mcp.core.modules.ReportFormat
import io.github.alkoleft.mcp.infrastructure.platform.CrossPlatformUtilLocator
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.Instant

private val logger = KotlinLogging.logger { }

/**
 * Реализация RunTestAction для тестирования через YaXUnit
 * Поддерживает запуск всех тестов, тестов модуля и конкретных тестов
 */
class YaXUnitTestAction(
    private val platformUtilityDsl: PlatformUtilityDsl,
    private val utilLocator: CrossPlatformUtilLocator,
    private val configWriter: JsonYaXUnitConfigWriter,
    private val reportParser: EnhancedReportParser
) : RunTestAction {

    override suspend fun run(filter: String?, properties: ApplicationProperties): TestExecutionResult {
        val startTime = Instant.now()
        logger.info { "Starting YaXUnit test execution with filter: $filter" }

        return withContext(Dispatchers.IO) {
            try {
                // Создаем запрос на выполнение тестов на основе фильтра
                val request = createTestRequest(filter, properties)
                logger.debug { "Created test request: ${request.javaClass.simpleName}" }
                
                // Локализуем утилиту 1С:Предприятие
                logger.debug { "Locating ENTERPRISE utility for version: ${properties.platformVersion}" }
                val utilityLocation = utilLocator.locateUtility(UtilityType.ENTERPRISE, properties.platformVersion)
                logger.info { "Found ENTERPRISE utility at: ${utilityLocation.executablePath}" }
                
                // Создаем временную конфигурацию
                logger.debug { "Creating temporary configuration" }
                val configPath = configWriter.createTempConfig(request)
                logger.debug { "Configuration created at: $configPath" }
                
                // Создаем runner и выполняем тесты
                val runner = ProcessYaXUnitRunner(utilLocator, configWriter)
                logger.info { "Executing tests via ProcessYaXUnitRunner" }
                val executionResult = runner.executeTests(utilityLocation, configPath, request)
                
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
                throw TestExecuteException("YaXUnit test execution failed: ${e.message}", e)
            }
        }
    }
    
    /**
     * Парсит отчет о тестировании
     */
    private suspend fun parseTestReport(executionResult: io.github.alkoleft.mcp.core.modules.YaXUnitExecutionResult): io.github.alkoleft.mcp.core.modules.GenericTestReport? {
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
        executionResult: io.github.alkoleft.mcp.core.modules.YaXUnitExecutionResult,
        report: io.github.alkoleft.mcp.core.modules.GenericTestReport?
    ): List<String> {
        val errors = mutableListOf<String>()
        
        // Добавляем ошибки выполнения процесса
        if (!executionResult.success) {
            errors.add("Process execution failed: ${executionResult.errorOutput}")
        }
        
        // Добавляем ошибки из отчета
        report?.testSuites?.forEach { testSuite ->
            testSuite.testCases.filter { it.status == io.github.alkoleft.mcp.core.modules.TestStatus.FAILED }
                .forEach { testCase ->
                    val errorMsg = testCase.errorMessage ?: "Test failed without error message"
                    errors.add("${testCase.name}: $errorMsg")
                }
        }
        
        return errors
    }
    
    /**
     * Создает запрос на выполнение тестов на основе фильтра
     */
    private fun createTestRequest(filter: String?, properties: ApplicationProperties): io.github.alkoleft.mcp.core.modules.TestExecutionRequest {
        val testsPath = properties.testsPath ?: properties.basePath.resolve("tests")
        
        return when {
            filter == null || filter.isBlank() -> {
                logger.debug { "Creating RunAllTestsRequest" }
                RunAllTestsRequest(
                    projectPath = properties.basePath,
                    testsPath = testsPath,
                    ibConnection = properties.connection.connectionString,
                    platformVersion = properties.platformVersion
                )
            }
            filter.startsWith("module:") -> {
                val moduleName = filter.substringAfter("module:").trim()
                logger.debug { "Creating RunModuleTestsRequest for module: $moduleName" }
                createModuleTestRequest(moduleName, properties)
            }
            filter.contains(",") -> {
                val testNames = filter.split(",").map { it.trim() }
                logger.debug { "Creating RunListTestsRequest for tests: ${testNames.joinToString(", ")}" }
                createListTestRequest(testNames, properties)
            }
            else -> {
                // Одиночный тест
                logger.debug { "Creating RunListTestsRequest for single test: $filter" }
                createListTestRequest(listOf(filter), properties)
            }
        }
    }
    
    /**
     * Создает запрос для запуска тестов конкретного модуля
     */
    private fun createModuleTestRequest(moduleName: String, properties: ApplicationProperties): RunModuleTestsRequest {
        val testsPath = properties.testsPath ?: properties.basePath.resolve("tests")
        
        return RunModuleTestsRequest(
            projectPath = properties.basePath,
            testsPath = testsPath,
            ibConnection = properties.connection.connectionString,
            platformVersion = properties.platformVersion,
            moduleName = moduleName
        )
    }
    
    /**
     * Создает запрос для запуска конкретных тестов
     */
    private fun createListTestRequest(testNames: List<String>, properties: ApplicationProperties): RunListTestsRequest {
        val testsPath = properties.testsPath ?: properties.basePath.resolve("tests")
        
        return RunListTestsRequest(
            projectPath = properties.basePath,
            testsPath = testsPath,
            ibConnection = properties.connection.connectionString,
            platformVersion = properties.platformVersion,
            testNames = testNames
        )
    }
    
    /**
     * Запускает все тесты в проекте
     */
    suspend fun runAllTests(properties: ApplicationProperties): TestExecutionResult {
        logger.info { "Running all tests in project" }
        return run(null, properties)
    }
    
    /**
     * Запускает тесты конкретного модуля
     */
    suspend fun runModuleTests(moduleName: String, properties: ApplicationProperties): TestExecutionResult {
        logger.info { "Running tests for module: $moduleName" }
        return run("module:$moduleName", properties)
    }
    
    /**
     * Запускает конкретные тесты
     */
    suspend fun runSpecificTests(testNames: List<String>, properties: ApplicationProperties): TestExecutionResult {
        logger.info { "Running specific tests: ${testNames.joinToString(", ")}" }
        return run(testNames.joinToString(","), properties)
    }
    
    /**
     * Запускает один тест
     */
    suspend fun runSingleTest(testName: String, properties: ApplicationProperties): TestExecutionResult {
        logger.info { "Running single test: $testName" }
        return run(testName, properties)
    }
} 