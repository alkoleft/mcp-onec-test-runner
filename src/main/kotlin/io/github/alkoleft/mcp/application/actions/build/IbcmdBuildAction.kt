package io.github.alkoleft.mcp.application.actions.build

import io.github.alkoleft.mcp.application.actions.BuildResult
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.configuration.properties.SourceSet
import io.github.alkoleft.mcp.infrastructure.platform.dsl.PlatformDsl
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger { }

/**
 * Реализация BuildAction для сборки через ibcmd
 */
class IbcmdBuildAction(
    dsl: PlatformDsl,
) : AbstractBuildAction(dsl) {
    override suspend fun executeBuildDsl(
        properties: ApplicationProperties,
        sourceSet: SourceSet,
    ): BuildResult {
        logger.info { "Executing ibcmd build DSL" }
        // TODO: Implement actual ibcmd build logic
        return BuildResult(
            success = false,
            errors = listOf("DSL сборки не вернул результат"),
            sourceSet = emptyMap(),
        )
    }
}
