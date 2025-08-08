package io.github.alkoleft.mcp.interfaces.cli.commands.test

import io.github.alkoleft.mcp.application.services.TestLauncherService
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.core.modules.RunAllTestsRequest
import io.github.alkoleft.mcp.core.modules.TestExecutionResult
import io.github.alkoleft.mcp.core.modules.TestStatus
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component
import picocli.CommandLine.Command
import java.util.concurrent.Callable

private val logger = KotlinLogging.logger { }

/**
 * Run all tests command - executes all tests in the project.
 */
@Component
@Command(
    name = "run-all",
    description = ["Запуск всех тестов проекта"],
    mixinStandardHelpOptions = true,
)
class RunAllCommand(private val properties: ApplicationProperties, private val testLauncher: TestLauncherService) :
    Callable<Int> {
    // Beans will be obtained from Spring ApplicationContext via parent CLI

    override fun call(): Int =
        runBlocking {
            return@runBlocking try {
                logger.info { "Running all tests..." }

                val request = RunAllTestsRequest(properties)

                logger.info { "Executing all tests using centralized ApplicationProperties:" }
                logger.info { "  Project: ${request.projectPath}" }
                logger.info { "  Tests: ${request.testsPath}" }
                logger.info { "  Platform: ${request.platformVersion ?: "auto-detect"}" }

                val result = testLauncher.run(request)

                // Print results
                printTestResults(result)

                if (result.success) {
                    logger.info { "All tests completed successfully" }
                    0
                } else {
                    logger.error { "Tests failed: Unknown error" }
                    1
                }
            } catch (e: Exception) {
                logger.error(e) { "Failed to run tests" }
                1
            }
        }

    private fun printTestResults(result: TestExecutionResult) {
        val report = result.report
        val summary = report.summary

        println("\n=== TEST RESULTS ===")
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
