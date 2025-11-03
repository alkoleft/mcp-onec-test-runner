package io.github.alkoleft.mcp.infrastructure.platform.dsl.designer

import io.github.alkoleft.mcp.core.modules.ShellCommandResult
import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.PlatformUtilityContext
import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.V8Dsl
import io.github.alkoleft.mcp.infrastructure.platform.dsl.designer.commands.ApplyCfgCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.designer.commands.CheckCanApplyConfigurationExtensionsCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.designer.commands.CheckConfigCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.designer.commands.CheckModulesCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.designer.commands.CreateCfgCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.designer.commands.DeleteCfgCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.designer.commands.DesignerCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.designer.commands.DumpConfigToFilesCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.designer.commands.DumpExtensionToFilesCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.designer.commands.LoadCfgCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.designer.commands.LoadConfigFromFilesCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.designer.commands.UpdateDBCfgCommand

/**
 * DSL для работы с конфигуратором 1С:Предприятие
 *
 * Предоставляет удобный интерфейс для выполнения операций конфигуратора
 * через fluent API и DSL синтаксис с немедленным выполнением команд.
 */
class DesignerDsl(
    context: PlatformUtilityContext,
) : V8Dsl<DesignerContext, DesignerCommand>(DesignerContext(context)) {
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

    private fun <C : DesignerCommand> configureAndExecute(
        command: C,
        configure: ((C.() -> Unit)?),
    ): ShellCommandResult = executeCommand(command.also { if (configure != null) it.configure() }, generateLogFilePath())
}
