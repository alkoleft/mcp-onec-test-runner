package io.github.alkoleft.mcp.infrastructure.platform.dsl.configurator

import io.github.alkoleft.mcp.infrastructure.platform.dsl.configurator.commands.ConfiguratorCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.executor.ProcessExecutor
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.time.Duration

private val logger = KotlinLogging.logger { }

/**
 * План выполнения команд конфигуратора
 */
data class ConfiguratorPlan(
    val commands: List<ConfiguratorCommand>,
    val context: ConfiguratorContext
) {
    /**
     * Выводит план команд
     */
    fun printPlan() {
        logger.info { "=== ПЛАН ВЫПОЛНЕНИЯ КОМАНД КОНФИГУРАТОРА ===" }
        commands.forEachIndexed { index, command ->
            logger.info { "${index + 1}. ${command.getFullDescription()}" }
        }
        logger.info { "=== КОНЕЦ ПЛАНА ===" }
    }

    /**
     * Выполняет все команды последовательно
     */
    suspend fun execute(): List<ConfiguratorResult> {
        val results = mutableListOf<ConfiguratorResult>()

        commands.forEachIndexed { index, command ->
            logger.info { "Выполняется команда ${index + 1}/${commands.size}: ${command.name}" }

            val result = executeCommand(command)
            results.add(result)

            if (!result.success) {
                logger.info { "Ошибка выполнения команды ${command.name}: ${result.error}" }
            }
        }

        return results
    }

    /**
     * Выполняет одну команду
     */
    private suspend fun executeCommand(command: ConfiguratorCommand): ConfiguratorResult {
        val executor = ProcessExecutor()
        val baseArgs = context.buildBaseArgs()
        val commandArgs = baseArgs + command.arguments

        return try {
            val result = executor.executeWithLogging(commandArgs)
            ConfiguratorResult(
                success = result.exitCode == 0,
                output = result.output,
                error = result.error,
                exitCode = result.exitCode,
                duration = result.duration
            )
        } catch (e: Exception) {
            ConfiguratorResult(
                success = false,
                output = "",
                error = e.message ?: "Unknown error",
                exitCode = -1,
                duration = Duration.ZERO
            )
        }
    }
}