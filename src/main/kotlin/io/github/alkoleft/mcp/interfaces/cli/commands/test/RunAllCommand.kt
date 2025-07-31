package io.github.alkoleft.mcp.interfaces.cli.commands.test

import io.github.alkoleft.mcp.application.services.TestLauncherService
import io.github.alkoleft.mcp.core.modules.RunAllTestsRequest
import io.github.alkoleft.mcp.core.modules.TestExecutionResult
import io.github.alkoleft.mcp.core.modules.TestStatus
import io.github.alkoleft.mcp.interfaces.cli.commands.TestCommand
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import picocli.CommandLine.Command
import picocli.CommandLine.ParentCommand
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
class RunAllCommand : Callable<Int> {
    @ParentCommand
    private lateinit var testCommand: TestCommand

    @Autowired
    private lateinit var testLauncher: TestLauncherService

    override fun call(): Int =
        runBlocking {
            return@runBlocking try {
                logger.info("Running all tests...")

                // Get configuration from parent commands
                val parentCli =
                    testCommand.javaClass
                        .getDeclaredField("parent")
                        .apply {
                            isAccessible = true
                        }.get(testCommand) as io.github.alkoleft.mcp.interfaces.cli.RunnerCli

                val config = parentCli.createConfiguration()
                val errors = config.validate()

                if (errors.isNotEmpty()) {
                    logger.error("Configuration validation failed:")
                    errors.forEach { logger.error("  - $it") }
                    return@runBlocking 1
                }

                val request =
                    RunAllTestsRequest(
                        projectPath = config.projectPath,
                        testsPath = config.testsPath,
                        ibConnection = config.ibConnection,
                        platformVersion = config.platformVersion,
                    )

                logger.info("Executing all tests with configuration:")
                logger.info("  Project: ${config.projectPath}")
                logger.info("  Tests: ${config.testsPath}")
                logger.info("  Platform: ${config.platformVersion ?: "auto-detect"}")

                val result = testLauncher.runAll(request)

                // Print results
                printTestResults(result)

                if (result.success) {
                    logger.info("All tests completed successfully")
                    0
                } else {
                    logger.error("Tests failed: ${result.error?.message ?: "Unknown error"}")
                    1
                }
            } catch (e: Exception) {
                logger.error("Failed to run tests", e)
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
