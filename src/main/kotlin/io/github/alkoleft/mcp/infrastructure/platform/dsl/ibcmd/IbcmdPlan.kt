package io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd

import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.common.IbcmdCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.process.ProcessExecutor
import io.github.alkoleft.mcp.infrastructure.platform.dsl.process.ProcessResult
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.time.Duration

private val logger = KotlinLogging.logger { }

/**
 * План выполнения команд ibcmd.
 * Содержит список команд и контекст для их последовательного выполнения.
 */
data class IbcmdPlan(
    val commands: List<IbcmdCommand>,
    val context: IbcmdContext,
) {
    /**
     * Выводит план команд в лог.
     * Показывает нумерацию и полное описание каждой команды.
     */
    fun printPlan() {
        logger.info { "=== ПЛАН ВЫПОЛНЕНИЯ КОМАНД IBMCMD ===" }
        commands.forEachIndexed { index, command ->
            logger.info { "${index + 1}. ${command.getFullDescription()}" }
        }
        logger.info { "=== КОНЕЦ ПЛАНА ===" }
    }

    /**
     * Выполняет все команды последовательно.
     * Собирает результаты выполнения каждой команды.
     *
     * @return Список результатов выполнения процессов.
     */
    suspend fun execute(): List<ProcessResult> {
        val results = mutableListOf<ProcessResult>()

        commands.forEachIndexed { index, command ->
            logger.info { "Выполняется команда ${index + 1}/${commands.size}: ${command.commandName}" }

            val result = executeCommand(command)
            results.add(result)

            if (!result.success) {
                logger.info { "Ошибка выполнения команды ${command.commandName}: ${result.error}" }
            }
        }

        return results
    }

    /**
     * Выполняет одну команду ibcmd.
     * Строит аргументы команды и запускает процесс через ProcessExecutor.
     *
     * @param command Команда для выполнения.
     * @return Результат выполнения процесса.
     */
    private suspend fun executeCommand(command: IbcmdCommand): ProcessResult {
        val executor = ProcessExecutor()
        val baseArgs = context.buildBaseArgs()
        val commandParts = command.commandName.split(" ")
        val mode = commandParts.first()
        val subCommand = commandParts.drop(1).joinToString(" ")
        val fullCommand =
            if (mode == "config") {
                "infobase config $subCommand"
            } else {
                command.commandName
            }
        val commandArgs = listOf(context.utilityPath) + fullCommand.split(" ") + baseArgs + command.arguments

        return try {
            val result = executor.execute(commandArgs)
            ProcessResult(
                success = result.exitCode == 0,
                output = result.output,
                error = result.error,
                exitCode = result.exitCode,
                duration = result.duration,
            )
        } catch (e: Exception) {
            ProcessResult(
                success = false,
                output = "",
                error = e.message ?: "Неизвестная ошибка",
                exitCode = -1,
                duration = Duration.ZERO,
            )
        }
    }
}
