package io.github.alkoleft.mcp.interfaces.cli.commands.test

import io.github.alkoleft.mcp.application.services.TestLauncherService
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.core.modules.RunModuleTestsRequest
import io.github.alkoleft.mcp.core.modules.TestExecutionResult
import io.github.alkoleft.mcp.core.modules.TestStatus
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import java.util.concurrent.Callable

private val logger = KotlinLogging.logger { }

/**
 * Run module tests command - executes tests for a specific module.
 */
@Component
@Command(
    name = "run-module",
    description = ["Запуск тестов конкретного модуля"],
    mixinStandardHelpOptions = true,
)
class RunModuleCommand(var properties: ApplicationProperties, var testLauncher: TestLauncherService) : Callable<Int> {
    @Option(
        names = ["--module"],
        description = ["Имя модуля для тестирования"],
        required = true,
    )
    lateinit var moduleName: String

    override fun call(): Int =
        runBlocking {
            return@runBlocking try {
                logger.info { "Running tests for module: $moduleName" }

                val request = RunModuleTestsRequest(moduleName, properties)

                logger.info { "Executing module tests with configuration:" }
                logger.info { "  Project: ${request.projectPath}" }
                logger.info { "  Tests: ${request.testsPath}" }
                logger.info { "  Module: $moduleName" }
                logger.info { "  Platform: ${request.platformVersion ?: "auto-detect"}" }

                val result = testLauncher.run(request)

                // Print results
                printTestResults(result, moduleName)

                if (result.success) {
                    logger.info { "Module tests completed successfully" }
                    0
                } else {
                    logger.error { "Module tests failed: ${result.error?.message ?: "Unknown error"}" }
                    1
                }
            } catch (e: Exception) {
                logger.error(e) { "Failed to run module tests" }
                1
            }
        }

    private fun printTestResults(
        result: TestExecutionResult,
        moduleName: String,
    ) {
        val report = result.report
        val summary = report.summary

        println("\n=== MODULE TEST RESULTS: $moduleName ===")
        println("Duration: ${result.duration}")
        println("Total Tests: ${summary.totalTests}")
        println("Passed: ${summary.passed}")
        println("Failed: ${summary.failed}")
        println("Skipped: ${summary.skipped}")
        println("Success Rate: ${"%.1f".format(summary.successRate * 100)}%")

        if (summary.failed > 0) {
            println("\n=== FAILED TESTS ===")
            report.testSuites.forEach { suite ->
                suite.testCases.filter { it.status == TestStatus.FAILED }.forEach { testCase ->
                    println("${suite.name}.${testCase.name}: ${testCase.errorMessage ?: "Unknown error"}")
                }
            }
        }

        println("\n=== TEST SUITES ===")
        report.testSuites.forEach { suite ->
            println("${suite.name}: ${suite.passed}/${suite.tests} passed (${suite.duration})")
        }
    }
}
