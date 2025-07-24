package io.github.alkoleft.mcp.interfaces.cli.commands.test

import io.github.alkoleft.mcp.application.services.TestLauncherService
import io.github.alkoleft.mcp.core.modules.RunListTestsRequest
import io.github.alkoleft.mcp.core.modules.TestExecutionResult
import io.github.alkoleft.mcp.core.modules.TestStatus
import io.github.alkoleft.mcp.interfaces.cli.commands.TestCommand
import kotlinx.coroutines.runBlocking
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.ParentCommand
import java.util.concurrent.Callable

/**
 * Run test list command - executes specific tests from a provided list.
 */
@Component
@Command(
    name = "run-list",
    description = ["Запуск конкретных тестов по списку"],
    mixinStandardHelpOptions = true,
)
class RunListCommand : Callable<Int> {
    @ParentCommand
    private lateinit var testCommand: TestCommand

    @Option(
        names = ["--tests"],
        description = ["Список тестов для выполнения (например: m1.t1 m2.t2)"],
        required = true,
        arity = "1..*",
    )
    lateinit var testNames: Array<String>

    @Autowired
    private lateinit var testLauncher: TestLauncherService

private val logger = KotlinLogging.logger {  }

    override fun call(): Int =
        runBlocking {
            return@runBlocking try {
                logger.info("Running specific tests: ${testNames.joinToString(", ")}")

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
                    RunListTestsRequest(
                        projectPath = config.projectPath,
                        testsPath = config.testsPath,
                        ibConnection = config.ibConnection,
                        platformVersion = config.platformVersion,
                        testNames = testNames.toList(),
                    )

                logger.info("Executing test list with configuration:")
                logger.info("  Project: ${config.projectPath}")
                logger.info("  Tests: ${config.testsPath}")
                logger.info("  Test Names: ${testNames.joinToString(", ")}")
                logger.info("  Platform: ${config.platformVersion ?: "auto-detect"}")

                val result = testLauncher.runList(request)

                // Print results
                printTestResults(result, testNames.toList())

                if (result.success) {
                    logger.info("Test list completed successfully")
                    0
                } else {
                    logger.error("Test list failed: ${result.error?.message ?: "Unknown error"}")
                    1
                }
            } catch (e: Exception) {
                logger.error("Failed to run test list", e)
                1
            }
        }

    private fun printTestResults(
        result: TestExecutionResult,
        testNames: List<String>,
    ) {
        val report = result.report
        val summary = report.summary

        println("\n=== TEST LIST RESULTS ===")
        println("Requested Tests: ${testNames.joinToString(", ")}")
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

        println("\n=== EXECUTED TESTS ===")
        report.testSuites.forEach { suite ->
            suite.testCases.forEach { testCase ->
                val status =
                    when (testCase.status) {
                        TestStatus.PASSED -> "✓"
                        TestStatus.FAILED -> "✗"
                        TestStatus.SKIPPED -> "○"
                        TestStatus.ERROR -> "!"
                    }
                println("$status ${suite.name}.${testCase.name} (${testCase.duration})")
            }
        }
    }
}
