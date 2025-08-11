package io.github.alkoleft.mcp.application.actions.convert

import io.github.alkoleft.mcp.application.actions.ConvertAction
import io.github.alkoleft.mcp.application.actions.ConvertResult
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.configuration.properties.SourceSet
import io.github.alkoleft.mcp.core.modules.ShellCommandResult
import io.github.alkoleft.mcp.infrastructure.platform.dsl.PlatformDsl

class InteractiveSessionConvertAction(
    private val dsl: PlatformDsl,
) : ConvertAction {
    override suspend fun run(
        properties: ApplicationProperties,
        sourceSet: SourceSet,
        destination: SourceSet,
    ): ConvertResult {
        val results = mutableMapOf<String, ShellCommandResult>()
        dsl.edt {
            sourceSet.forEach {
                results[it.name] =
                    export(
                        projectName = it.name,
                        configurationFiles = destination.pathByName(it.name),
                    )
            }
        }
        return ConvertResult(success = results.values.none { !it.success }, sourceSet = results.toMap())
    }
}
