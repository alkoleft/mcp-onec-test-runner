package io.github.alkoleft.mcp.interfaces.cli.commands.test

import io.github.alkoleft.mcp.application.services.TestLauncherService
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.core.modules.RunListTestsRequest
import io.github.alkoleft.mcp.core.modules.TestExecutionResult
import io.github.alkoleft.mcp.core.modules.TestStatus
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import java.util.concurrent.Callable

private val logger = KotlinLogging.logger { }

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
    @Option(
        names = ["--tests"],
        description = ["Список тестов для выполнения (например: m1.t1 m2.t2)"],
        required = true,
        arity = "1..*",
    )
    lateinit var testNames: Array<String>

    @Autowired
    private lateinit var testLauncher: TestLauncherService

    @Autowired
    private lateinit var properties: ApplicationProperties

    override fun call(): Int =
        runBlocking {
            return@runBlocking try {
                logger.info { "Running specific tests: ${testNames.joinToString(", ")}" }

                val request = RunListTestsRequest(testNames.toList(), properties)

                logger.info { "Executing test list using centralized ApplicationProperties:" }
                logger.info { "  Project: ${request.projectPath}" }
                logger.info { "  Tests: ${request.testsPath}" }
                logger.info { "  Test Names: ${testNames.joinToString(", ")}" }
                logger.info { "  Platform: ${request.platformVersion ?: "auto-detect"}" }

                val result = testLauncher.run(request)

                // Print results
                printTestResults(result)

                if (result.success) {
                    logger.info { "Test list completed successfully" }
                    0
                } else {
                    logger.error { "Test list failed: Unknown error" }
                    1
                }
            } catch (e: Exception) {
                logger.error(e) { "Failed to run test list" }
                1
            }
        }

    private fun printTestResults(
        result: TestExecutionResult
    ) {
        val report = result.report
        val summary = report.summary

        println("\n=== TEST LIST RESULTS ===")
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
