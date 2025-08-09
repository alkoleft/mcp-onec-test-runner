package io.github.alkoleft.mcp.application.services

import io.github.alkoleft.mcp.application.actions.ActionFactory
import io.github.alkoleft.mcp.application.actions.ChangeAnalysisAction
import io.github.alkoleft.mcp.application.actions.FileSystemChangeAnalysisResult
import io.github.alkoleft.mcp.application.actions.exceptions.BuildError
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.core.modules.TestExecutionRequest
import io.github.alkoleft.mcp.core.modules.TestExecutionResult
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger { }

@Service
class LauncherService(
    private val actionFactory: ActionFactory,
    private val properties: ApplicationProperties,
) {
    suspend fun run(request: TestExecutionRequest): TestExecutionResult {
        build()
        return actionFactory.createRunTestAction().run(request)
    }

    suspend fun build() {
        val changeAnalyzer = actionFactory.createChangeAnalysisAction()
        val changes = changeAnalyzer.analyzeBySourceSet(properties)
        if (changes.hasChanges) {
            updateIB(changes, changeAnalyzer)
        } else {
            logger.info { "Исходные файлы не изменены. Обновление базы пропущено" }
        }
    }

    private suspend fun updateIB(
        changes: FileSystemChangeAnalysisResult,
        changeAnalyzer: ChangeAnalysisAction,
    ) {
        val builder = actionFactory.createBuildAction(properties.tools.builder)
        val changedSourceSets =
            properties.sourceSet.subSourceSet { sourceSetItem ->
                changes.sourceSetChanges.contains(sourceSetItem.name)
            }

        logger.info { "Обнаружены изменения: ${changedSourceSets.joinToString { it.name }}" }
        var success = true
        val result = builder.build(properties, changedSourceSets)
        result.sourceSet.forEach { (name, result) ->
            success = success && result.success
            if (result.success) {
                changeAnalyzer.saveSourceSetState(properties, changes.sourceSetChanges[name]!!)
            }
        }

        if (!success) {
            throw BuildError("Не удалось выполнить обновление")
        }
    }
}
