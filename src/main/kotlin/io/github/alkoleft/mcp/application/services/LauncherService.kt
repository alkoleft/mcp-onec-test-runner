package io.github.alkoleft.mcp.application.services

import io.github.alkoleft.mcp.application.actions.ActionFactory
import io.github.alkoleft.mcp.application.actions.BuildResult
import io.github.alkoleft.mcp.application.actions.ConvertResult
import io.github.alkoleft.mcp.application.actions.exceptions.AnalysisError
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.configuration.properties.ProjectFormat
import io.github.alkoleft.mcp.configuration.properties.SourceSet
import io.github.alkoleft.mcp.core.modules.TestExecutionError
import io.github.alkoleft.mcp.core.modules.TestExecutionRequest
import io.github.alkoleft.mcp.core.modules.TestExecutionResult
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import kotlin.time.Duration

private val logger = KotlinLogging.logger { }

@Service
class LauncherService(
    private val actionFactory: ActionFactory,
    private val properties: ApplicationProperties,
) {
    private val edtSourceSet: SourceSet = createEdtSourceSet()
    private val designerSourceSet: SourceSet = createDesignerSourceSet()

    fun run(request: TestExecutionRequest): TestExecutionResult {
        val buildResult = build()
        if (!buildResult.success) {
            val reason = if (buildResult.errors.isNotEmpty()) buildResult.errors.joinToString("; ") else "Build failed"
            throw TestExecutionError.BuildFailed(reason)
        }
        return actionFactory.createRunTestAction().run(request)
    }

    fun build(): BuildResult {
        val changeAnalyzer = actionFactory.createChangeAnalysisAction()
        val changes = changeAnalyzer.run(properties)
        if (!changes.hasChanges) {
            logger.info { "Исходные файлы не изменены. Обновление базы пропущено" }
            return BuildResult(
                success = true,
                errors = emptyList(),
                duration = Duration.ZERO,
                sourceSet = emptyMap(),
            )
        }
        val changedSourceSets = properties.sourceSet.subSourceSet { it.name in changes.sourceSetChanges.keys }

        if (changedSourceSets.isEmpty()) {
            throw AnalysisError("Не удалось распределить изменения по субпроектам.")
        }
        logger.info { "Обнаружены изменения: ${changedSourceSets.joinToString { it.name }}" }

        if (properties.format == ProjectFormat.EDT) {
            val convertResult = convertSources(changedSourceSets, designerSourceSet)
            if (!convertResult.success) {
                logger.error { "Ошибки конвертации исходников EDT: ${convertResult.errors.joinToString()}" }
                return BuildResult(
                    success = false,
                    errors = convertResult.errors,
                    duration = Duration.ZERO,
                    sourceSet = emptyMap(),
                )
            }
        }

        val result = updateIB(changedSourceSets)

        var success = true
        val errors = mutableListOf<String>()
        result.sourceSet.forEach { (name, result) ->
            success = success && result.success
            if (result.success) {
                changeAnalyzer.saveSourceSetState(properties, changes.sourceSetChanges[name]!!)
            } else {
                result.error?.takeIf { it.isNotBlank() }?.let { errors.add(it) }
            }
        }

        return if (success && result.success) {
            result
        } else {
            BuildResult(
                success = false,
                errors = errors.ifEmpty { result.errors },
                duration = result.duration,
                sourceSet = result.sourceSet,
            )
        }
    }

    private fun convertSources(
        changedSourceSets: SourceSet,
        destination: SourceSet,
    ): ConvertResult =
        actionFactory.convertAction().run(
            properties,
            edtSourceSet.subSourceSet { changedSourceSets.find { item -> item.name == it.name } != null },
            destination,
        )

    private fun updateIB(changedSourceSets: SourceSet): BuildResult {
        val builder = actionFactory.createBuildAction(properties.tools.builder)
        return builder.run(
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
