package io.github.alkoleft.mcp.infrastructure.platform.dsl.enterprise

import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.Command
import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.PlatformUtilities
import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.V8Dsl
import io.github.alkoleft.mcp.infrastructure.platform.dsl.process.ProcessExecutor
import io.github.alkoleft.mcp.infrastructure.platform.dsl.process.ProcessResult
import org.springframework.stereotype.Component
import kotlin.time.Duration

/**
 * DSL для работы с 1С:Предприятие
 */
@Component
class EnterpriseDsl(
    utilityContext: PlatformUtilities,
) : V8Dsl<EnterpriseContext, Command>(EnterpriseContext(utilityContext)) {
    fun runArguments(value: String) {
        context.runArguments = value
    }

    /**
     * Запускает 1С:Предприятие с указанными параметрами
     */
    fun run(): ProcessResult =
        try {
            val logPath = generateLogFilePath()
            val args = context.buildBaseArgs(logPath)

            ProcessExecutor().executeWithLogging(args, logPath)
        } catch (e: Exception) {
            ProcessResult(false, "", e.message ?: "Неизвестная ошибка", -1, Duration.ZERO)
        }
}
