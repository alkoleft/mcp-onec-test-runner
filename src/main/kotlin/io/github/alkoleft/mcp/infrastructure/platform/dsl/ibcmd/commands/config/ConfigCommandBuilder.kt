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
 *
 */

package io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.config

import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.IbcmdDsl
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.common.CommandBuilder
import java.nio.file.Path

/**
 * Builder для создания команд режима config
 */
class ConfigCommandBuilder(
    dsl: IbcmdDsl,
) : CommandBuilder(dsl) {
    /**
     * Создает команду загрузки конфигурации
     */
    fun load(
        path: Path,
        configure: ConfigLoadCommand.() -> Unit = { },
    ) = configureAndExecute(ConfigLoadCommand(path = path), configure)

    /**
     * Создает команду выгрузки конфигурации
     */
    fun save(
        path: Path,
        configure: ConfigSaveCommand.() -> Unit = { },
    ) = configureAndExecute(ConfigSaveCommand(path = path), configure)

    /**
     * Создает команду проверки конфигурации
     */
    fun check(configure: ConfigCheckCommand.() -> Unit = { }) = configureAndExecute(ConfigCheckCommand(), configure)

    /**
     * Создает команду применения конфигурации
     */
    fun apply(configure: ConfigApplyCommand.() -> Unit = { }) = configureAndExecute(ConfigApplyCommand(), configure)

    /**
     * Создает команду сброса конфигурации
     */
    fun reset(configure: ConfigResetCommand.() -> Unit = { }) = configureAndExecute(ConfigResetCommand(), configure)

    /**
     * Создает команду экспорта конфигурации в XML
     */
    fun export(
        path: Path,
        configure: ConfigExportCommand.() -> Unit = { },
    ) = configureAndExecute(ConfigExportCommand(path = path), configure)

    /**
     * Создает команду импорта конфигурации из XML
     */
    fun import(
        path: Path,
        configure: ConfigImportCommand.() -> Unit = {},
    ) = configureAndExecute(ConfigImportCommand(path = path), configure)

    /**
     * Подписывает конфигурацию или расширение.
     */
    fun sign(configure: ConfigSignCommand.() -> Unit = {}) = configureAndExecute(ConfigSignCommand(), configure)

    /**
     * Выводит список разделителей информационной базы.
     */
    fun dataSeparationList(configure: ConfigDataSeparationListCommand.() -> Unit = {}) =
        configureAndExecute(ConfigDataSeparationListCommand(), configure)

    /**
     * Восстанавливает конфигурацию после сбоя операции.
     */
    fun repair(configure: ConfigRepairCommand.() -> Unit = {}) = configureAndExecute(ConfigRepairCommand(), configure)

    /**
     * Снимает конфигурацию с поддержки.
     */
    fun supportDisable(configure: ConfigSupportDisableCommand.() -> Unit = {}) =
        configureAndExecute(ConfigSupportDisableCommand(), configure)

    /**
     * Получает идентификатор поколения конфигурации.
     */
    fun generationId(configure: ConfigGenerationIdCommand.() -> Unit = {}) = configureAndExecute(ConfigGenerationIdCommand(), configure)

    /**
     * Создание, получение информации, список, обновление, удаление расширений.
     *  * Подкоманды: create, info, list, update, delete
     */
    fun extension(
        name: String,
        subCommand: String,
        configure: ConfigExtensionCommand.() -> Unit = {},
    ) = configureAndExecute(ConfigExtensionCommand(name = name, extensionSubCommand = subCommand), configure)
}
