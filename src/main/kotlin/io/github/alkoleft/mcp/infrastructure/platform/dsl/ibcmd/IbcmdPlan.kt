package io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd

import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.common.IbcmdCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.process.ProcessExecutor
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.time.Duration

private val logger = KotlinLogging.logger { }

/**
 * План выполнения команд ibcmd
 */
data class IbcmdPlan(
    val commands: List<IbcmdCommand>,
    val context: IbcmdContext
) {
    /**
     * Выводит план команд
     */
    fun printPlan() {
        logger.info { "=== ПЛАН ВЫПОЛНЕНИЯ КОМАНД IBMCMD ===" }
        commands.forEachIndexed { index, command ->
            logger.info { "${index + 1}. ${command.getFullDescription()}" }
        }
        logger.info { "=== КОНЕЦ ПЛАНА ===" }
    }

    /**
     * Выполняет все команды последовательно
     */
    suspend fun execute(): List<IbcmdResult> {
        val results = mutableListOf<IbcmdResult>()

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
     * Выполняет одну команду
     */
    private suspend fun executeCommand(command: IbcmdCommand): IbcmdResult {
        val executor = ProcessExecutor()
        val baseArgs = context.buildBaseArgs()
        val commandArgs = listOf(context.utilityPath, *command.commandName.split(" ").toTypedArray()) + baseArgs + command.arguments


        return try {
            val result = executor.execute(commandArgs)
            IbcmdResult(
                success = result.exitCode == 0,
                output = result.output,
                error = result.error ?: "",
                exitCode = result.exitCode,
                duration = result.duration
            )
        } catch (e: Exception) {
            IbcmdResult(
                success = false,
                output = "",
                error = e.message ?: "Unknown error",
                exitCode = -1,
                duration = Duration.ZERO
            )
        }
    }
} 