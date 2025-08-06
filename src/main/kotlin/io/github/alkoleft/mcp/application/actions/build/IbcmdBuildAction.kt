package io.github.alkoleft.mcp.application.actions.build

import io.github.alkoleft.mcp.application.actions.BuildResult
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.configuration.properties.SourceSet
import io.github.alkoleft.mcp.infrastructure.platform.dsl.PlatformUtilityDsl
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger { }

/**
 * Реализация BuildAction для сборки через ibcmd
 */
class IbcmdBuildAction(
    dsl: PlatformUtilityDsl
) : AbstractBuildAction(dsl) {
    override suspend fun executeBuildDsl(properties: ApplicationProperties, sourceSet: SourceSet): BuildResult {
        logger.info { "Executing ibcmd build DSL" }
        // TODO: Implement actual ibcmd build logic
        return BuildResult(
            success = false,
            errors = listOf("DSL сборки не вернул результат"),
            sourceSet = emptyMap()
        )
    }

    override suspend fun executeConfigurationBuildDsl(properties: ApplicationProperties): BuildResult {
        logger.info { "Executing ibcmd configuration build DSL" }
        // TODO: Implement actual ibcmd configuration build logic
        return BuildResult(
            success = false,
            errors = listOf("DSL сборки не вернул результат"),
            sourceSet = emptyMap()
        )
    }

    override suspend fun executeExtensionBuildDsl(
        extensionName: String,
        properties: ApplicationProperties
    ): BuildResult {
        logger.info { "Executing ibcmd extension build DSL for: $extensionName" }
        // TODO: Implement actual ibcmd extension build logic
        return BuildResult(
            success = false,
            errors = listOf("DSL сборки не вернул результат"),
            sourceSet = emptyMap()
        )
    }
} 