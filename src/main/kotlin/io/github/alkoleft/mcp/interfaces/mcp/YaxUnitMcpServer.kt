package io.github.alkoleft.mcp.interfaces.mcp

import io.github.alkoleft.mcp.application.services.TestLauncherService
import io.github.alkoleft.mcp.core.modules.GenericTestReport
import io.github.alkoleft.mcp.core.modules.RunAllTestsRequest
import io.github.alkoleft.mcp.core.modules.RunListTestsRequest
import io.github.alkoleft.mcp.core.modules.RunModuleTestsRequest
import kotlinx.coroutines.runBlocking
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.nio.file.Paths

/**
 * MCP server implementation providing YAXUnit test execution commands.
 * Integrates with Spring AI MCP Server to handle MCP protocol interactions.
 */
private val logger = KotlinLogging.logger {  }

@Component
class YaxUnitMcpServer(
    private val testLauncher: TestLauncherService,
) {

    /**
     * Runs all tests in the specified 1C:Enterprise project
     */
    fun runAllTests(
        projectPath: String,
        ibConnection: String,
        testsPath: String? = null,
        platformVersion: String? = null,
    ): McpTestResult =
        runBlocking {
            logger.info { "MCP runAll command received - projectPath: $projectPath, ibConnection: $ibConnection" }

            try {
                val request =
                    RunAllTestsRequest(
                        projectPath = Paths.get(projectPath),
                        testsPath = testsPath?.let { Paths.get(it) } ?: Paths.get(projectPath).resolve("tests"),
                        ibConnection = ibConnection,
                        platformVersion = platformVersion,
                    )

                val result = testLauncher.runAll(request)

                McpTestResult(
                    success = result.success,
                    summary = createTestSummary(result.report),
                    duration = result.duration.toMillis(),
                    testSuites =
                        result.report.testSuites.map { suite ->
                            McpTestSuite(
                                name = suite.name,
                                tests = suite.tests,
                                passed = suite.passed,
                                failed = suite.failed,
                                skipped = suite.skipped,
                                duration = suite.duration.toMillis(),
                                testCases =
                                    suite.testCases.take(10).map { testCase ->
                                        McpTestCase(
                                            name = testCase.name,
                                            status = testCase.status.name,
                                            duration = testCase.duration.toMillis(),
                                            errorMessage = testCase.errorMessage,
                                        )
                                    },
                            )
                        },
                    error = result.error?.message,
                )
            } catch (e: Exception) {
                logger.error(e) { "${"MCP runAll command failed"}" }

                McpTestResult(
                    success = false,
                    summary = "Test execution failed: ${e.message}",
                    duration = 0,
                    testSuites = emptyList(),
                    error = e.message,
                )
            }
        }

    /**
     * Runs tests for a specific module in the 1C:Enterprise project
     */
    fun runModuleTests(
        projectPath: String,
        moduleName: String,
        ibConnection: String,
        testsPath: String? = null,
        platformVersion: String? = null,
    ): McpTestResult =
        runBlocking {
            logger.info { "MCP runModule command received - projectPath: $projectPath, module: $moduleName" }

            try {
                val request =
                    RunModuleTestsRequest(
                        projectPath = Paths.get(projectPath),
                        testsPath = testsPath?.let { Paths.get(it) } ?: Paths.get(projectPath).resolve("tests"),
                        ibConnection = ibConnection,
                        platformVersion = platformVersion,
                        moduleName = moduleName,
                    )

                val result = testLauncher.runModule(request)

                McpTestResult(
                    success = result.success,
                    summary = createTestSummary(result.report),
                    duration = result.duration.toMillis(),
                    testSuites =
                        result.report.testSuites.map { suite ->
                            McpTestSuite(
                                name = suite.name,
                                tests = suite.tests,
                                passed = suite.passed,
                                failed = suite.failed,
                                skipped = suite.skipped,
                                duration = suite.duration.toMillis(),
                                testCases =
                                    suite.testCases.map { testCase ->
                                        McpTestCase(
                                            name = testCase.name,
                                            status = testCase.status.name,
                                            duration = testCase.duration.toMillis(),
                                            errorMessage = testCase.errorMessage,
                                        )
                                    },
                            )
                        },
                    error = result.error?.message,
                )
            } catch (e: Exception) {
                logger.error(e) { "${"MCP runModule command failed"}" }

                McpTestResult(
                    success = false,
                    summary = "Module test execution failed: ${e.message}",
                    duration = 0,
                    testSuites = emptyList(),
                    error = e.message,
                )
            }
        }

    /**
     * Runs specific tests from a provided list
     */
    fun runTestList(
        projectPath: String,
        testNames: List<String>,
        ibConnection: String,
        testsPath: String? = null,
        platformVersion: String? = null,
    ): McpTestResult =
        runBlocking {
            logger.info { "MCP runList command received - projectPath: $projectPath, tests: ${testNames.size}" }

            try {
                val request =
                    RunListTestsRequest(
                        projectPath = Paths.get(projectPath),
                        testsPath = testsPath?.let { Paths.get(it) } ?: Paths.get(projectPath).resolve("tests"),
                        ibConnection = ibConnection,
                        platformVersion = platformVersion,
                        testNames = testNames,
                    )

                val result = testLauncher.runList(request)

                McpTestResult(
                    success = result.success,
                    summary = createTestSummary(result.report),
                    duration = result.duration.toMillis(),
                    testSuites =
                        result.report.testSuites.map { suite ->
                            McpTestSuite(
                                name = suite.name,
                                tests = suite.tests,
                                passed = suite.passed,
                                failed = suite.failed,
                                skipped = suite.skipped,
                                duration = suite.duration.toMillis(),
                                testCases =
                                    suite.testCases.map { testCase ->
                                        McpTestCase(
                                            name = testCase.name,
                                            status = testCase.status.name,
                                            duration = testCase.duration.toMillis(),
                                            errorMessage = testCase.errorMessage,
                                        )
                                    },
                            )
                        },
                    error = result.error?.message,
                )
            } catch (e: Exception) {
                logger.error(e) { "${"MCP runList command failed"}" }

                McpTestResult(
                    success = false,
                    summary = "Test list execution failed: ${e.message}",
                    duration = 0,
                    testSuites = emptyList(),
                    error = e.message,
                )
            }
        }

    /**
     * Gets project information and test structure
     */
    fun getProjectInfo(projectPath: String): McpProjectInfo =
        runBlocking {
            logger.info { "MCP getProjectInfo command received - projectPath: $projectPath" }

            try {
                val path = Paths.get(projectPath)
                val testsPath = path.resolve("tests")

                // Discover test modules (placeholder implementation)
                val modules = discoverTestModules(testsPath)

                McpProjectInfo(
                    projectPath = projectPath,
                    testsPath = testsPath.toString(),
                    totalModules = modules.size,
                    modules = modules,
                    configuration =
                        mapOf(
                            "platform" to "1C:Enterprise",
                            "framework" to "YAXUnit",
                            "hasTests" to if (modules.isNotEmpty()) "true" else "false",
                        ),
                )
            } catch (e: Exception) {
                logger.error(e) { "${"MCP getProjectInfo command failed"}" }

                McpProjectInfo(
                    projectPath = projectPath,
                    testsPath = "",
                    totalModules = 0,
                    modules = emptyList(),
                    configuration = mapOf("error" to (e.message ?: "Unknown error")),
                )
            }
        }

    /**
     * Creates a human-readable test summary
     */
    private fun createTestSummary(report: GenericTestReport): String {
        val summary = report.summary
        return "Tests: ${summary.totalTests}, " +
            "Passed: ${summary.passed}, " +
            "Failed: ${summary.failed}, " +
            "Skipped: ${summary.skipped}, " +
            "Success Rate: ${"%.1f".format(summary.successRate * 100)}%"
    }

    /**
     * Discovers test modules in the project (placeholder implementation)
     */
    private fun discoverTestModules(testsPath: java.nio.file.Path): List<String> =
        try {
            if (!java.nio.file.Files
                    .exists(testsPath)
            ) {
                emptyList()
            } else {
                java.nio.file.Files
                    .list(testsPath)
                    .filter {
                        java.nio.file.Files
                            .isDirectory(it)
                    }.map { it.fileName.toString() }
                    .filter { it.contains("Test", ignoreCase = true) || it.contains("Тест", ignoreCase = true) }
                    .sorted()
                    .toList()
            }
        } catch (e: Exception) {
            logger.warn("Failed to discover test modules in $testsPath", e)
            emptyList()
        }
}

/**
 * MCP-specific data transfer objects for JSON serialization
 */
data class McpTestResult(
    val success: Boolean,
    val summary: String,
    val duration: Long,
    val testSuites: List<McpTestSuite>,
    val error: String? = null,
)

data class McpTestSuite(
    val name: String,
    val tests: Int,
    val passed: Int,
    val failed: Int,
    val skipped: Int,
    val duration: Long,
    val testCases: List<McpTestCase>,
)

data class McpTestCase(
    val name: String,
    val status: String,
    val duration: Long,
    val errorMessage: String? = null,
)

data class McpProjectInfo(
    val projectPath: String,
    val testsPath: String,
    val totalModules: Int,
    val modules: List<String>,
    val configuration: Map<String, String>,
)
