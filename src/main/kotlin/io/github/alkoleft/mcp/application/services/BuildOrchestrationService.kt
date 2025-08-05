package io.github.alkoleft.mcp.application.services

import io.github.alkoleft.mcp.application.actions.ActionFactory
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.core.modules.BuildDecision
import io.github.alkoleft.mcp.core.modules.BuildResult
import io.github.alkoleft.mcp.core.modules.BuildService
import io.github.alkoleft.mcp.core.modules.BuildType
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.nio.file.Path
import java.time.Duration

private val logger = KotlinLogging.logger { }

/**
 * Build orchestration service using Actions architecture.
 * Manages full and incremental builds based on file change detection.
 */
@Service
class BuildOrchestrationService(
    private val actionFactory: ActionFactory,
    private val applicationProperties: ApplicationProperties
) : BuildService {

    override suspend fun ensureBuild(projectPath: Path): BuildResult {
        logger.info { "Ensuring build is up to date for project: $projectPath" }

        val buildDecision = determineBuildStrategy(projectPath)

        return when (buildDecision) {
            is BuildDecision.NO_BUILD_NEEDED -> {
                logger.info { "No build needed - project is up to date" }
                BuildResult(
                    success = true,
                    duration = Duration.ZERO,
                    buildType = BuildType.SKIP,
                )
            }

            is BuildDecision.FULL_BUILD -> {
                logger.info { "Performing full build" }
                performFullBuild(projectPath)
            }

            is BuildDecision.INCREMENTAL_BUILD -> {
                logger.info { "Performing incremental build for modules: ${buildDecision.changedModules}" }
                performIncrementalBuild(projectPath, buildDecision.changedModules)
            }

            is BuildDecision.INCREMENTAL_TESTS -> {
                logger.info { "Only test files changed - no build required" }
                BuildResult(
                    success = true,
                    duration = Duration.ZERO,
                    buildType = BuildType.SKIP,
                )
            }
        }
    }

    override suspend fun determineBuildStrategy(projectPath: Path): BuildDecision {
        logger.info { "Determining build strategy for project: $projectPath" }

        val changeAction = actionFactory.createChangeAnalysisAction()
        val changes = changeAction.analyze(applicationProperties)

        return if (changes.hasChanges) {
            logger.info { "Changes detected, performing incremental build" }
            BuildDecision.INCREMENTAL_BUILD(changes.affectedModules)
        } else {
            logger.info { "No changes detected, no build needed" }
            BuildDecision.NO_BUILD_NEEDED
        }
    }

    override suspend fun performFullBuild(projectPath: Path): BuildResult {
        logger.info { "Performing full build for project: $projectPath" }

        val buildAction = actionFactory.createBuildAction(applicationProperties.tools.builder)
        val result = buildAction.build(applicationProperties)

        return BuildResult(
            success = result.success,
            duration = result.duration,
            buildType = BuildType.FULL,
            error = if (result.errors.isNotEmpty()) result.errors.joinToString("; ") else null
        )
    }

    override suspend fun performIncrementalBuild(projectPath: Path, changedModules: Set<String>): BuildResult {
        logger.info { "Performing incremental build for modules: $changedModules" }

        val buildAction = actionFactory.createBuildAction(applicationProperties.tools.builder)

        val results = mutableListOf<io.github.alkoleft.mcp.application.actions.BuildResult>()
        changedModules.forEach { module ->
            val result = buildAction.buildExtension(module, applicationProperties)
            results.add(result)
        }

        val success = results.all { it.success }
        val allErrors = results.flatMap { it.errors }
        val duration = results.maxOfOrNull { it.duration } ?: Duration.ZERO

        return BuildResult(
            success = success,
            duration = duration,
            buildType = BuildType.INCREMENTAL,
            error = if (allErrors.isNotEmpty()) allErrors.joinToString("; ") else null
        )
    }


}
