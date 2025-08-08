package io.github.alkoleft.mcp.application.services

import io.github.alkoleft.mcp.application.actions.ActionFactory
import io.github.alkoleft.mcp.application.actions.ChangeAnalysisResult
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.core.modules.TestExecutionRequest
import io.github.alkoleft.mcp.core.modules.TestExecutionResult
import org.springframework.stereotype.Service

@Service
class LauncherService(
    private val actionFactory: ActionFactory,
    private val properties: ApplicationProperties
) {
    suspend fun run(request: TestExecutionRequest): TestExecutionResult {
        val changes = actionFactory.createChangeAnalysisAction().analyze(properties)
        if (changes.hasChanges) {
            updateIB(changes)
        }
        return actionFactory.createRunTestAction().run(request)
    }

    private fun updateIB(changes: ChangeAnalysisResult) {

    }
}