package io.github.alkoleft.mcp.infrastructure.platform.dsl.enterprise

import io.github.alkoleft.mcp.core.modules.ShellCommandResult
import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.Command
import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.PlatformUtilityContext
import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.V8Dsl
import io.github.alkoleft.mcp.infrastructure.platform.dsl.process.ProcessExecutor
import io.github.alkoleft.mcp.infrastructure.platform.dsl.process.ProcessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component

/**
 * DSL для работы с 1С:Предприятие
 */
@Component
class EnterpriseDsl(
    utilityContext: PlatformUtilityContext,
) : V8Dsl<EnterpriseContext, Command>(EnterpriseContext(utilityContext)) {
    fun runArguments(value: String) {
        context.runArguments = value
    }

    /**
     * Запускает 1С:Предприятие с указанными параметрами
     */
    suspend fun run(): ProcessResult =
        withContext(Dispatchers.IO) {
            try {
                val args = context.buildBaseArgs()
                val executor = ProcessExecutor()
                val result = executor.executeWithLogging(args)

                context.setResult(
                    success = result.exitCode == 0,
                    output = result.output,
                    error = result.error,
                    exitCode = result.exitCode,
                    duration = result.duration,
                )
                context.buildResult()
            } catch (e: Exception) {
                context.setResult(false, "", e.message, -1, kotlin.time.Duration.ZERO)
                context.buildResult()
            }
        }

    override suspend fun executeCommand(command: Command): ShellCommandResult {
        TODO("Not yet implemented")
    }
}
