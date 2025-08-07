package io.github.alkoleft.mcp.infrastructure.platform.dsl.configurator

import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.PlatformUtilityContext
import io.github.alkoleft.mcp.infrastructure.platform.dsl.configurator.commands.ApplyCfgCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.configurator.commands.CheckCanApplyConfigurationExtensionsCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.configurator.commands.CheckConfigCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.configurator.commands.CheckModulesCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.configurator.commands.ConfiguratorCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.configurator.commands.CreateCfgCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.configurator.commands.DeleteCfgCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.configurator.commands.DumpConfigToFilesCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.configurator.commands.DumpExtensionToFilesCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.configurator.commands.LoadCfgCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.configurator.commands.LoadConfigFromFilesCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.configurator.commands.UpdateDBCfgCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.executor.ProcessExecutor
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration
import kotlin.time.measureTime

/**
 * DSL для работы с конфигуратором 1С:Предприятие
 *
 * Предоставляет удобный интерфейс для выполнения операций конфигуратора
 * через fluent API и DSL синтаксис с немедленным выполнением команд.
 */
class ConfiguratorDsl(
    utilityContext: PlatformUtilityContext
) : BasePlatformDsl<ConfiguratorContext>(ConfiguratorContext(utilityContext)),
    ConfiguratorDslInterface<ConfiguratorResult> {
    override fun loadCfg(block: LoadCfgCommand.() -> Unit) =
        configureAndExecute(LoadCfgCommand(), block)

    override fun loadConfigFromFiles(block: LoadConfigFromFilesCommand.() -> Unit) =
        configureAndExecute(LoadConfigFromFilesCommand(), block)

    override fun updateDBCfg(block: UpdateDBCfgCommand.() -> Unit) =
        configureAndExecute(UpdateDBCfgCommand(), block)

    override fun checkCanApplyConfigurationExtensions(block: CheckCanApplyConfigurationExtensionsCommand.() -> Unit) =
        configureAndExecute(CheckCanApplyConfigurationExtensionsCommand(), block)

    override fun checkConfig(block: CheckConfigCommand.() -> Unit) =
        configureAndExecute(CheckConfigCommand(), block)

    override fun checkModules(block: CheckModulesCommand.() -> Unit) =
        configureAndExecute(CheckModulesCommand(), block)

    override fun dumpConfigToFiles(block: DumpConfigToFilesCommand.() -> Unit) =
        configureAndExecute(DumpConfigToFilesCommand(), block)

    override fun dumpExtensionToFiles(block: DumpExtensionToFilesCommand.() -> Unit) =
        configureAndExecute(DumpExtensionToFilesCommand(), block)

    override fun applyCfg(block: ApplyCfgCommand.() -> Unit) =
        configureAndExecute(ApplyCfgCommand(), block)

    override fun createCfg(block: CreateCfgCommand.() -> Unit) =
        configureAndExecute(CreateCfgCommand(), block)

    override fun deleteExtension(block: DeleteCfgCommand.() -> Unit) =
        configureAndExecute(DeleteCfgCommand(), block)

    /**
     * Выполняет команду конфигуратора с произвольными аргументами
     */
    private suspend fun executeCommand(
        command: ConfiguratorCommand
    ): ConfiguratorResult {
        val duration = measureTime {
            try {
                val executor = ProcessExecutor()

                val args = buildCommandArgsWithArgs(command.arguments)
                val result = executor.executeWithLogging(args)

                context.setResult(
                    success = result.exitCode == 0,
                    output = result.output,
                    error = result.error,
                    exitCode = result.exitCode,
                    duration = result.duration
                )

            } catch (e: Exception) {
                context.setResult(
                    success = false,
                    output = "",
                    error = e.message ?: "Unknown error",
                    exitCode = -1,
                    duration = Duration.ZERO
                )
            }
        }

        return ConfiguratorResult(
            success = context.buildResult().success,
            output = context.buildResult().output,
            error = context.buildResult().error,
            exitCode = context.buildResult().exitCode,
            duration = duration
        )
    }

    /**
     * Строит аргументы команды для конфигуратора с произвольными аргументами
     */
    private suspend fun buildCommandArgsWithArgs(
        commandArgs: List<String>
    ): List<String> {
        val args = mutableListOf<String>()

        // Базовые аргументы конфигуратора
        args.addAll(context.buildBaseArgs())

        // Команда и её аргументы (команда уже включена в commandArgs)
        args.addAll(commandArgs)

        return args
    }

    private fun <C : ConfiguratorCommand> configureAndExecute(
        command: C,
        configure: (C.() -> Unit)
    ): ConfiguratorResult {
        command.configure()
        return runBlocking {
            executeCommand(command)
        }
    }
}

/**
 * DSL для планирования операций с конфигуратором 1С:Предприятие
 *
 * Предоставляет удобный интерфейс для планирования операций конфигуратора
 * через fluent API и DSL синтаксис с последующим выполнением плана.
 */
class ConfiguratorPlanDsl(
    utilityContext: PlatformUtilityContext
) : BasePlatformDsl<ConfiguratorContext>(ConfiguratorContext(utilityContext)),
    ConfiguratorDslInterface<Unit> {
    private val commands = mutableListOf<ConfiguratorCommand>()

    override fun loadCfg(block: LoadCfgCommand.() -> Unit) {
        val command = LoadCfgCommand()
        command.block()
        commands.add(command)
    }

    override fun loadConfigFromFiles(block: LoadConfigFromFilesCommand.() -> Unit) {
        val command = LoadConfigFromFilesCommand()
        command.block()
        commands.add(command)
    }

    override fun updateDBCfg(block: UpdateDBCfgCommand.() -> Unit) {
        val command = UpdateDBCfgCommand()
        command.block()
        commands.add(command)
    }

    override fun checkCanApplyConfigurationExtensions(block: CheckCanApplyConfigurationExtensionsCommand.() -> Unit) {
        val command = CheckCanApplyConfigurationExtensionsCommand()
        command.block()
        commands.add(command)
    }

    override fun checkConfig(block: CheckConfigCommand.() -> Unit) {
        val command = CheckConfigCommand()
        command.block()
        commands.add(command)
    }

    override fun checkModules(block: CheckModulesCommand.() -> Unit) {
        val command = CheckModulesCommand()
        command.block()
        commands.add(command)
    }

    override fun dumpConfigToFiles(block: DumpConfigToFilesCommand.() -> Unit) {
        val command = DumpConfigToFilesCommand()
        command.block()
        commands.add(command)
    }

    override fun dumpExtensionToFiles(block: DumpExtensionToFilesCommand.() -> Unit) {
        val command = DumpExtensionToFilesCommand()
        command.block()
        commands.add(command)
    }

    override fun applyCfg(block: ApplyCfgCommand.() -> Unit) {
        val command = ApplyCfgCommand()
        command.block()
        commands.add(command)
    }

    override fun createCfg(block: CreateCfgCommand.() -> Unit) {
        val command = CreateCfgCommand()
        command.block()
        commands.add(command)
    }

    override fun deleteExtension(block: DeleteCfgCommand.() -> Unit) {
        val command = DeleteCfgCommand()
        command.block()
        commands.add(command)
    }

    /**
     * Строит план выполнения команд
     */
    fun buildPlan(): ConfiguratorPlan {
        return ConfiguratorPlan(commands.toList(), context)
    }
}

/**
 * Скорость соединения
 */
enum class ConnectionSpeed(val value: String) {
    NORMAL("Normal"),
    LOW("Low")
}

/**
 * Результат выполнения операций с конфигуратором
 */
data class ConfiguratorResult(
    val success: Boolean,
    val output: String,
    val error: String?,
    val exitCode: Int,
    val duration: Duration
) {
    companion object {
        val EMPTY = ConfiguratorResult(
            false, "", "", -1, Duration.ZERO
        )
    }
}