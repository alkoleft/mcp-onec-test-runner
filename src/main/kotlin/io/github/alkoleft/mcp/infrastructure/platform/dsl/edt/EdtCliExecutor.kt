package io.github.alkoleft.mcp.infrastructure.platform.dsl.edt

import io.github.alkoleft.mcp.core.modules.ShellCommandResult
import io.github.alkoleft.mcp.infrastructure.platform.dsl.process.CommandExecutor
import io.github.alkoleft.mcp.infrastructure.platform.dsl.process.InteractiveProcessExecutor
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.time.Duration

private val logger = KotlinLogging.logger {}

/**
 * Исполнитель команд EDT CLI через интерактивный процесс
 *
 * Предоставляет высокоуровневый интерфейс для работы с командами EDT CLI,
 * используя InteractiveProcessExecutor для управления процессом.
 */
class EdtCliExecutor(
    private val interactiveExecutor: InteractiveProcessExecutor,
) : CommandExecutor {
    /**
     * Результат выполнения команды с дополнительной обработкой
     */
    data class EdtCommandResult(
        override val success: Boolean,
        override val output: String,
        override val error: String?,
        override val duration: Duration,
        val command: String,
        val hasErrors: Boolean = false,
        val errorDetails: String? = null,
        override val exitCode: Int = 0,
    ) : ShellCommandResult

    /**
     * Выполняет произвольную команду
     */
    override suspend fun execute(commandArgs: List<String>): EdtCommandResult {
        val command = commandArgs.joinToString(" ")
        logger.debug { "Выполнение команды $command" }
        return processCommandResult(interactiveExecutor.executeCommand(command, 600000))
    }

    /**
     * Корректно завершает сессию EDT CLI
     */
    suspend fun exit(): Boolean {
        logger.debug { "Завершение сессии EDT CLI" }
        return interactiveExecutor.exit()
    }

    /**
     * Обрабатывает результат команды
     */
    private fun processCommandResult(result: InteractiveProcessExecutor.CommandResult): EdtCommandResult {
        if (!result.success) {
            return EdtCommandResult(
                success = false,
                output = result.output,
                error = result.error,
                duration = result.duration,
                command = result.command,
                hasErrors = true,
                errorDetails = result.error,
                exitCode = result.exitCode,
            )
        }

        val output = result.output
        val command = result.command

        return EdtCommandResult(
            success = output.isBlank(),
            output = output,
            error = output,
            duration = result.duration,
            command = command,
            exitCode = result.exitCode,
        )
    }
}
