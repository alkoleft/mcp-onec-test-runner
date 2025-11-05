/*
 * This file is part of METR.
 *
 * Copyright (C) 2025 Aleksey Koryakin <alkoleft@gmail.com> and contributors.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * METR is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * METR is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with METR.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.alkoleft.mcp.infrastructure.platform.dsl.designer

import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.PlatformUtilities
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
import io.github.alkoleft.mcp.infrastructure.platform.dsl.process.ProcessExecutor
import io.github.alkoleft.mcp.infrastructure.platform.dsl.process.ProcessResult

/**
 * DSL для работы с конфигуратором 1С:Предприятие
 *
 * Предоставляет удобный интерфейс для выполнения операций конфигуратора
 * через fluent API и DSL синтаксис с немедленным выполнением команд.
 */
class DesignerDsl(
    context: PlatformUtilities,
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

    fun launch(): ProcessResult {
        val command =
            object : DesignerCommand() {
                override val name: String = "launch"
                override val description: String = "Запуск конфигуратора"
                override val parameters: Map<String, String> = emptyMap()
                override val arguments: List<String> = emptyList()
            }
        return ProcessExecutor().launch(buildCommandArgs(command))
    }

    private fun <C : DesignerCommand> configureAndExecute(
        command: C,
        configure: ((C.() -> Unit)?),
    ): ProcessResult = executeCommand(command.also { if (configure != null) it.configure() }, generateLogFilePath())
}
