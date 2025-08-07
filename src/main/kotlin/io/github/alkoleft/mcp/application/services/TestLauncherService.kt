package io.github.alkoleft.mcp.application.services

import io.github.alkoleft.mcp.application.actions.ActionFactory
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.core.modules.GenericTestReport
import io.github.alkoleft.mcp.core.modules.RunAllTestsRequest
import io.github.alkoleft.mcp.core.modules.RunListTestsRequest
import io.github.alkoleft.mcp.core.modules.RunModuleTestsRequest
import io.github.alkoleft.mcp.core.modules.TestExecutionError
import io.github.alkoleft.mcp.core.modules.TestExecutionResult
import io.github.alkoleft.mcp.core.modules.TestLauncher
import io.github.alkoleft.mcp.core.modules.TestMetadata
import io.github.alkoleft.mcp.core.modules.TestSummary
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant

private val logger = KotlinLogging.logger { }

/**
 * Test execution service using Actions architecture.
 * Orchestrates the complete test execution workflow.
 */
@Service
class TestLauncherService(
    private val actionFactory: ActionFactory,
    private val properties: ApplicationProperties
) : TestLauncher {

    override suspend fun runAll(request: RunAllTestsRequest): TestExecutionResult {
        logger.info { "Starting full test execution for project: ${request.projectPath}" }

        val testAction = actionFactory.createRunTestAction()
        val result = testAction.run(properties, null)

        return TestExecutionResult(
            success = result.success,
            report = createEmptyReport(),
            duration = result.duration,
            error = if (result.errors.isNotEmpty()) TestExecutionError.TestRunFailed(result.errors.joinToString("; ")) else null
        )
    }

    override suspend fun runModule(request: RunModuleTestsRequest): TestExecutionResult {
        logger.info { "Starting module test execution for: ${request.moduleName} in project: ${request.projectPath}" }

        val testAction = actionFactory.createRunTestAction()
        val result = testAction.run(properties, request.moduleName)

        return TestExecutionResult(
            success = result.success,
            report = createEmptyReport(),
            duration = result.duration,
            error = if (result.errors.isNotEmpty()) TestExecutionError.TestRunFailed(result.errors.joinToString("; ")) else null
        )
    }

    override suspend fun runList(request: RunListTestsRequest): TestExecutionResult {
        logger.info { "Starting specific tests execution for: ${request.testNames} in project: ${request.projectPath}" }

        val testAction = actionFactory.createRunTestAction()
        val result = testAction.run(properties, request.testNames.firstOrNull())

        return TestExecutionResult(
            success = result.success,
            report = createEmptyReport(),
            duration = result.duration,
            error = if (result.errors.isNotEmpty()) TestExecutionError.TestRunFailed(result.errors.joinToString("; ")) else null
        )
    }

    private fun createEmptyReport(): GenericTestReport {
        return GenericTestReport(
            metadata = TestMetadata(),
            summary = TestSummary(
                totalTests = 0,
                passed = 0,
                failed = 0,
                skipped = 0,
                errors = 0
            ),
            testSuites = emptyList(),
            timestamp = Instant.now(),
            duration = Duration.ZERO
        )
    }
}
