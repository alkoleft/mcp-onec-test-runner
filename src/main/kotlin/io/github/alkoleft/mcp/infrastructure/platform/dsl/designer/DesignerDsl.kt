package io.github.alkoleft.mcp.infrastructure.platform.dsl.designer

import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.BasePlatformDsl
import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.PlatformUtilityContext
import io.github.alkoleft.mcp.infrastructure.platform.dsl.designer.commands.ApplyCfgCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.designer.commands.CheckCanApplyConfigurationExtensionsCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.designer.commands.CheckConfigCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.designer.commands.CheckModulesCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.designer.commands.ConfiguratorCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.designer.commands.CreateCfgCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.designer.commands.DeleteCfgCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.designer.commands.DumpConfigToFilesCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.designer.commands.DumpExtensionToFilesCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.designer.commands.LoadCfgCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.designer.commands.LoadConfigFromFilesCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.designer.commands.UpdateDBCfgCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.process.ProcessExecutor
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration
import kotlin.time.measureTime

/**
 * DSL для работы с конфигуратором 1С:Предприятие
 *
 * Предоставляет удобный интерфейс для выполнения операций конфигуратора
 * через fluent API и DSL синтаксис с немедленным выполнением команд.
 */
class DesignerDsl(
    utilityContext: PlatformUtilityContext,
) : BasePlatformDsl<DesignerContext>(DesignerContext(utilityContext)) {
    fun loadCfg(block: LoadCfgCommand.() -> Unit) = configureAndExecute(LoadCfgCommand(), block)

    fun loadConfigFromFiles(block: LoadConfigFromFilesCommand.() -> Unit) = configureAndExecute(LoadConfigFromFilesCommand(), block)

    fun updateDBCfg(block: UpdateDBCfgCommand.() -> Unit) = configureAndExecute(UpdateDBCfgCommand(), block)

    fun checkCanApplyConfigurationExtensions(block: CheckCanApplyConfigurationExtensionsCommand.() -> Unit) =
        configureAndExecute(CheckCanApplyConfigurationExtensionsCommand(), block)

    fun checkConfig(block: CheckConfigCommand.() -> Unit) = configureAndExecute(CheckConfigCommand(), block)

    fun checkModules(block: CheckModulesCommand.() -> Unit) = configureAndExecute(CheckModulesCommand(), block)

    fun dumpConfigToFiles(block: DumpConfigToFilesCommand.() -> Unit) = configureAndExecute(DumpConfigToFilesCommand(), block)

    fun dumpExtensionToFiles(block: DumpExtensionToFilesCommand.() -> Unit) = configureAndExecute(DumpExtensionToFilesCommand(), block)

    fun applyCfg(block: ApplyCfgCommand.() -> Unit) = configureAndExecute(ApplyCfgCommand(), block)

    fun createCfg(block: CreateCfgCommand.() -> Unit) = configureAndExecute(CreateCfgCommand(), block)

    fun deleteExtension(block: DeleteCfgCommand.() -> Unit) = configureAndExecute(DeleteCfgCommand(), block)

    /**
     * Выполняет команду конфигуратора с произвольными аргументами
     */
    private suspend fun executeCommand(command: ConfiguratorCommand): ConfiguratorResult {
        val duration =
            measureTime {
                try {
                    val executor = ProcessExecutor()

                    val args = buildCommandArgsWithArgs(command.arguments)
                    val result = executor.executeWithLogging(args)

                    context.setResult(
                        success = result.exitCode == 0,
                        output = result.output,
                        error = result.error,
                        exitCode = result.exitCode,
                        duration = result.duration,
                    )
                } catch (e: Exception) {
                    context.setResult(
                        success = false,
                        output = "",
                        error = e.message ?: "Unknown error",
                        exitCode = -1,
                        duration = Duration.ZERO,
                    )
                }
            }

        return ConfiguratorResult(
            success = context.buildResult().success,
            output = context.buildResult().output,
            error = context.buildResult().error,
            exitCode = context.buildResult().exitCode,
            duration = duration,
        )
    }

    /**
     * Строит аргументы команды для конфигуратора с произвольными аргументами
     */
    private suspend fun buildCommandArgsWithArgs(commandArgs: List<String>): List<String> {
        val args = mutableListOf<String>()

        // Базовые аргументы конфигуратора
        args.addAll(context.buildBaseArgs())

        // Команда и её аргументы (команда уже включена в commandArgs)
        args.addAll(commandArgs)

        return args
    }

    private fun <C : ConfiguratorCommand> configureAndExecute(
        command: C,
        configure: (C.() -> Unit),
    ): ConfiguratorResult {
        command.configure()
        return runBlocking {
            executeCommand(command)
        }
    }
}

/**
 * Скорость соединения
 */
enum class ConnectionSpeed(
    val value: String,
) {
    NORMAL("Normal"),
    LOW("Low"),
}

/**
 * Результат выполнения операций с конфигуратором
 */
data class ConfiguratorResult(
    val success: Boolean,
    val output: String,
    val error: String?,
    val exitCode: Int,
    val duration: Duration,
) {
    companion object {
        val EMPTY =
            ConfiguratorResult(
                false,
                "",
                "",
                -1,
                Duration.ZERO,
            )
    }
}
