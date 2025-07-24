package io.github.alkoleft.mcp.application.services

import io.github.alkoleft.mcp.core.modules.BuildService
import io.github.alkoleft.mcp.core.modules.GenericTestReport
import io.github.alkoleft.mcp.core.modules.ReportParser
import io.github.alkoleft.mcp.core.modules.RunAllTestsRequest
import io.github.alkoleft.mcp.core.modules.RunListTestsRequest
import io.github.alkoleft.mcp.core.modules.RunModuleTestsRequest
import io.github.alkoleft.mcp.core.modules.TestExecutionError
import io.github.alkoleft.mcp.core.modules.TestExecutionRequest
import io.github.alkoleft.mcp.core.modules.TestExecutionResult
import io.github.alkoleft.mcp.core.modules.TestLauncher
import io.github.alkoleft.mcp.core.modules.TestMetadata
import io.github.alkoleft.mcp.core.modules.TestSummary
import io.github.alkoleft.mcp.core.modules.UtilLocator
import io.github.alkoleft.mcp.core.modules.UtilityLocation
import io.github.alkoleft.mcp.core.modules.UtilityType
import io.github.alkoleft.mcp.core.modules.YaXUnitConfigWriter
import io.github.alkoleft.mcp.core.modules.YaXUnitExecutionResult
import io.github.alkoleft.mcp.core.modules.YaXUnitRunner
import io.github.alkoleft.mcp.infrastructure.config.ProjectConfiguration
import kotlinx.coroutines.coroutineScope
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import java.time.Instant

private val logger = KotlinLogging.logger {  }

/**
 * Test execution facade that orchestrates the complete test execution workflow.
 * Implements the TestLauncher interface following the Enhanced Modular Layered Architecture.
 */
@Service
class TestLauncherService(
    private val buildService: BuildService,
    private val utilLocator: UtilLocator,
    private val yaXUnitRunner: YaXUnitRunner,
    private val reportParser: ReportParser,
    private val yaXUnitConfigWriter: YaXUnitConfigWriter,
    private val projectConfiguration: ProjectConfiguration,
) : TestLauncher {

    override suspend fun runAll(request: RunAllTestsRequest): TestExecutionResult {
        logger.info("Starting full test execution for project: ${request.projectPath}")

        return executeTestWorkflow(request) { utilityLocation, configPath ->
            yaXUnitRunner.executeTests(utilityLocation, configPath, request)
        }
    }

    override suspend fun runModule(request: RunModuleTestsRequest): TestExecutionResult {
        logger.info("Starting module test execution for: ${request.moduleName} in project: ${request.projectPath}")

        return executeTestWorkflow(request) { utilityLocation, configPath ->
            yaXUnitRunner.executeTests(utilityLocation, configPath, request)
        }
    }

    override suspend fun runList(request: RunListTestsRequest): TestExecutionResult {
        logger.info("Starting specific tests execution for: ${request.testNames} in project: ${request.projectPath}")

        return executeTestWorkflow(request) { utilityLocation, configPath ->
            yaXUnitRunner.executeTests(utilityLocation, configPath, request)
        }
    }

    /**
     * Запускает все тесты используя конфигурацию проекта
     * @return Результат выполнения всех тестов
     */
    suspend fun runAll(): TestExecutionResult {
        val projectPath = Paths.get(".")
        val testsPath = Paths.get(projectConfiguration.project.testsPath ?: "./tests")
        val ibConnection = projectConfiguration.informationBase?.connection ?: ""
        val platformVersion = projectConfiguration.platform.version

        logger.info("Starting full test execution using project configuration")

        val request = RunAllTestsRequest(
            projectPath = projectPath,
            testsPath = testsPath,
            ibConnection = ibConnection,
            platformVersion = platformVersion
        )

        return executeTestWorkflow(request) { utilityLocation, configPath ->
            yaXUnitRunner.executeTests(utilityLocation, configPath, request)
        }
    }

    /**
     * Запускает тесты модуля используя конфигурацию проекта
     * @param moduleName Имя модуля для тестирования
     * @return Результат выполнения тестов модуля
     */
    suspend fun runModule(moduleName: String): TestExecutionResult {
        val projectPath = Paths.get(".")
        val testsPath = Paths.get(projectConfiguration.project.testsPath ?: "./tests")
        val ibConnection = projectConfiguration.informationBase?.connection ?: ""
        val platformVersion = projectConfiguration.platform.version

        logger.info("Starting module test execution for: $moduleName using project configuration")

        val request = RunModuleTestsRequest(
            projectPath = projectPath,
            testsPath = testsPath,
            ibConnection = ibConnection,
            platformVersion = platformVersion,
            moduleName = moduleName
        )

        return executeTestWorkflow(request) { utilityLocation, configPath ->
            yaXUnitRunner.executeTests(utilityLocation, configPath, request)
        }
    }

    /**
     * Запускает тесты из списка используя конфигурацию проекта
     * @param testNames Список имен тестов для выполнения
     * @return Результат выполнения указанных тестов
     */
    suspend fun runList(testNames: List<String>): TestExecutionResult {
        val projectPath = Paths.get(".")
        val testsPath = Paths.get(projectConfiguration.project.testsPath ?: "./tests")
        val ibConnection = projectConfiguration.informationBase?.connection ?: ""
        val platformVersion = projectConfiguration.platform.version

        logger.info("Starting specific tests execution for: $testNames using project configuration")

        val request = RunListTestsRequest(
            projectPath = projectPath,
            testsPath = testsPath,
            ibConnection = ibConnection,
            platformVersion = platformVersion,
            testNames = testNames
        )

        return executeTestWorkflow(request) { utilityLocation, configPath ->
            yaXUnitRunner.executeTests(utilityLocation, configPath, request)
        }
    }

    /**
     * Core test execution workflow following the pipeline defined in the requirements:
     * 1. Locate 1C utilities
     * 2. Ensure build is up to date
     * 3. Create YAXUnit configuration
     * 4. Execute tests
     * 5. Parse results
     */
    private suspend fun executeTestWorkflow(
        request: TestExecutionRequest,
        testExecution: suspend (UtilityLocation, java.nio.file.Path) -> YaXUnitExecutionResult,
    ): TestExecutionResult =
        coroutineScope {
            val startTime = Instant.now()

            try {
                // Phase 1: Locate 1C utilities
                logger.debug("Phase 1: Locating 1C utilities")
                val utilityLocation =
                    utilLocator.locateUtility(
                        UtilityType.COMPILER_1CV8C,
                        request.platformVersion,
                    )

                if (!utilLocator.validateUtility(utilityLocation)) {
                    return@coroutineScope TestExecutionResult(
                        success = false,
                        report = createEmptyReport(),
                        duration = Duration.between(startTime, Instant.now()),
                        error = TestExecutionError.UtilNotFound("1cv8c at ${utilityLocation.executablePath}"),
                    )
                }

                // Phase 2: Ensure build is up to date
                logger.debug("Phase 2: Ensuring build is up to date")
                val buildResult = buildService.ensureBuild(request.projectPath)

                if (!buildResult.success) {
                    return@coroutineScope TestExecutionResult(
                        success = false,
                        report = createEmptyReport(),
                        duration = Duration.between(startTime, Instant.now()),
                        error = TestExecutionError.BuildFailed(buildResult.error ?: "Unknown build error"),
                    )
                }

                // Phase 3: Create YAXUnit configuration
                logger.debug("Phase 3: Creating YAXUnit configuration")
                val configPath = yaXUnitConfigWriter.createTempConfig(request)

                try {
                    // Phase 4: Execute tests
                    logger.debug("Phase 4: Executing tests with YAXUnit")
                    val executionResult = testExecution(utilityLocation, configPath)

                    if (!executionResult.success) {
                        return@coroutineScope TestExecutionResult(
                            success = false,
                            report = createEmptyReport(),
                            duration = Duration.between(startTime, Instant.now()),
                            error =
                                TestExecutionError.TestRunFailed(
                                    "Exit code: ${executionResult.exitCode}, Error: ${executionResult.errorOutput}",
                                ),
                        )
                    }

                    // Phase 5: Parse test results
                    logger.debug("Phase 5: Parsing test results")
                    val report = parseTestReport(executionResult.reportPath)

                    val totalDuration = Duration.between(startTime, Instant.now())
                    logger.info("Test execution completed successfully in ${totalDuration.toMillis()}ms")

                    TestExecutionResult(
                        success = true,
                        report = report,
                        duration = totalDuration,
                    )
                } finally {
                    // Cleanup temporary configuration file
                    cleanupTempFiles(configPath)
                }
            } catch (e: Exception) {
                logger.error("Test execution failed with exception", e)

                TestExecutionResult(
                    success = false,
                    report = createEmptyReport(),
                    duration = Duration.between(startTime, Instant.now()),
                    error =
                        when (e) {
                            is TestExecutionError -> e
                            else -> TestExecutionError.TestRunFailed("Unexpected error: ${e.message}")
                        },
                )
            }
        }

    /**
     * Parses test report from the execution result
     */
    private suspend fun parseTestReport(reportPath: java.nio.file.Path?): GenericTestReport {
        if (reportPath == null || !Files.exists(reportPath)) {
            throw TestExecutionError.ReportParsingFailed("Report file not found")
        }

        return try {
            FileInputStream(reportPath.toFile()).use { inputStream ->
                val format = reportParser.detectFormat(inputStream)
                inputStream.use {
                    reportParser.parseReport(it, format)
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to parse test report from: $reportPath", e)
            throw TestExecutionError.ReportParsingFailed("Report parsing failed: ${e.message}")
        }
    }

    /**
     * Creates an empty report for error cases
     */
    private fun createEmptyReport(): GenericTestReport =
        GenericTestReport(
            metadata = TestMetadata(),
            summary =
                TestSummary(
                    totalTests = 0,
                    passed = 0,
                    failed = 0,
                    skipped = 0,
                    errors = 0,
                ),
            testSuites = emptyList(),
            timestamp = Instant.now(),
            duration = Duration.ZERO,
        )

    /**
     * Cleanup temporary files created during test execution
     */
    private fun cleanupTempFiles(configPath: java.nio.file.Path) {
        try {
            if (Files.exists(configPath)) {
                Files.delete(configPath)
                logger.debug("Cleaned up temporary config file: $configPath")
            }
        } catch (e: Exception) {
            logger.warn("Failed to cleanup temporary file: $configPath", e)
        }
    }
}
