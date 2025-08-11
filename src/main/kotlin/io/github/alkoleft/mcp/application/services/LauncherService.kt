package io.github.alkoleft.mcp.application.services

import io.github.alkoleft.mcp.application.actions.ActionFactory
import io.github.alkoleft.mcp.application.actions.BuildResult
import io.github.alkoleft.mcp.application.actions.ConvertResult
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.configuration.properties.ProjectFormat
import io.github.alkoleft.mcp.configuration.properties.SourceSet
import io.github.alkoleft.mcp.core.modules.TestExecutionError
import io.github.alkoleft.mcp.core.modules.TestExecutionRequest
import io.github.alkoleft.mcp.core.modules.TestExecutionResult
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.time.Duration

private val logger = KotlinLogging.logger { }

@Service
class LauncherService(
    private val actionFactory: ActionFactory,
    private val properties: ApplicationProperties,
) {
    private val edtSourceSet: SourceSet = createEdtSourceSet()
    private val designerSourceSet: SourceSet = createDesignerSourceSet()

    suspend fun run(request: TestExecutionRequest): TestExecutionResult {
        val buildResult = build()
        if (!buildResult.success) {
            val reason = if (buildResult.errors.isNotEmpty()) buildResult.errors.joinToString("; ") else "Build failed"
            throw TestExecutionError.BuildFailed(reason)
        }
        return actionFactory.createRunTestAction().run(request)
    }

    suspend fun build(): BuildResult {
        val changeAnalyzer = actionFactory.createChangeAnalysisAction()
        val changes = changeAnalyzer.analyzeBySourceSet(properties)
        if (!changes.hasChanges) {
            logger.info { "Исходные файлы не изменены. Обновление базы пропущено" }
            return BuildResult(
                success = true,
                configurationBuilt = false,
                errors = emptyList(),
                duration = Duration.ZERO,
                sourceSet = emptyMap(),
            )
        }
        val changedSourceSets =
            properties.sourceSet.subSourceSet { sourceSetItem ->
                changes.sourceSetChanges.contains(sourceSetItem.name)
            }

        logger.info { "Обнаружены изменения: ${changedSourceSets.joinToString { it.name }}" }

        if (properties.format == ProjectFormat.EDT) {
            val convertResult = convertSources(changedSourceSets, designerSourceSet)
            if (convertResult.success.not()) {
                logger.error { "Ошибки конвертации исходников EDT: ${convertResult.errors.joinToString()}" }
                return BuildResult(
                    success = false,
                    configurationBuilt = false,
                    errors = convertResult.errors,
                    duration = Duration.ZERO,
                    sourceSet = emptyMap(),
                )
            }
        }

        val result = updateIB(changedSourceSets)

        var success = true
        val errors = mutableListOf<String>()
        result.sourceSet.forEach { (name, cfgResult) ->
            success = success && cfgResult.success
            if (cfgResult.success) {
                changeAnalyzer.saveSourceSetState(properties, changes.sourceSetChanges[name]!!)
            } else if (!cfgResult.success && !cfgResult.error.isNullOrBlank()) {
                errors += cfgResult.error
            }
        }

        return if (success && result.success) {
            result
        } else {
            BuildResult(
                success = false,
                configurationBuilt = result.configurationBuilt,
                errors = errors.ifEmpty { result.errors },
                duration = result.duration,
                sourceSet = result.sourceSet,
            )
        }
    }

    private suspend fun convertSources(
        changedSourceSets: SourceSet,
        destination: SourceSet,
    ): ConvertResult =
        actionFactory.convertAction().run(
            properties,
            edtSourceSet.subSourceSet { changedSourceSets.find { item -> item.name == it.name } != null },
            destination,
        )

    private suspend fun updateIB(changedSourceSets: SourceSet): BuildResult {
        val builder = actionFactory.createBuildAction(properties.tools.builder)
        return builder.build(
            properties,
            designerSourceSet.subSourceSet { changedSourceSets.find { item -> item.name == it.name } != null },
        )
    }

    private fun createEdtSourceSet() =
        if (properties.format == ProjectFormat.EDT) {
            SourceSet(
                properties.basePath,
                properties.sourceSet,
            )
        } else {
            SourceSet.EMPTY
        }

    private fun createDesignerSourceSet() =
        if (properties.format == ProjectFormat.EDT) {
            SourceSet(
                properties.workPath,
                properties.sourceSet.map { it.copy(path = it.name) },
            )
        } else {
            SourceSet(
                properties.basePath,
                properties.sourceSet,
            )
        }
}
