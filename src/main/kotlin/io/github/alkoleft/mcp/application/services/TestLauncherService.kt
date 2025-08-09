package io.github.alkoleft.mcp.application.services

import io.github.alkoleft.mcp.application.actions.ActionFactory
import io.github.alkoleft.mcp.core.modules.RunAllTestsRequest
import io.github.alkoleft.mcp.core.modules.RunListTestsRequest
import io.github.alkoleft.mcp.core.modules.RunModuleTestsRequest
import io.github.alkoleft.mcp.core.modules.TestExecutionRequest
import io.github.alkoleft.mcp.core.modules.TestExecutionResult
import io.github.alkoleft.mcp.core.modules.TestLauncher
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger { }

/**
 * Test execution service using Actions architecture.
 * Orchestrates the complete test execution workflow.
 */
@Service
class TestLauncherService(
    private val actionFactory: ActionFactory,
) : TestLauncher {
    override suspend fun run(request: TestExecutionRequest): TestExecutionResult {
        when (request) {
            is RunAllTestsRequest -> logger.info { "Starting full test execution for project: ${request.projectPath}" }
            is RunListTestsRequest ->
                logger.info {
                    "Starting specific tests execution for: ${request.testNames} in project: ${request.projectPath}"
                }
            is RunModuleTestsRequest ->
                logger.info {
                    "Starting module test execution for: ${request.moduleName} in project: ${request.projectPath}"
                }
        }

        val testAction = actionFactory.createRunTestAction()
        return testAction.run(request)
    }
}
