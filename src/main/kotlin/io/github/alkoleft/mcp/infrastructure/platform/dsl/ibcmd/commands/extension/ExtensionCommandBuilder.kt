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

package io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.extension

import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.IbcmdDsl
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.common.CommandBuilder

/**
 * Builder для создания команд режима extension
 */
class ExtensionCommandBuilder(dsl: IbcmdDsl) : CommandBuilder(dsl) {
    /**
     * Создает команду создания расширения
     */
    fun create(
        name: String,
        namePrefix: String,
        configure: ExtensionCreateCommand.() -> Unit = { },
    ) = configureAndExecute(ExtensionCreateCommand(name = name, namePrefix = namePrefix), configure)

    /**
     * Создает команду получения информации о расширении
     */
    fun info(
        name: String,
        configure: ExtensionInfoCommand.() -> Unit = { },
    ) = configureAndExecute(ExtensionInfoCommand(name = name), configure)

    /**
     * Создает команду получения списка расширений
     */
    fun list(configure: ExtensionListCommand.() -> Unit = { }) = configureAndExecute(ExtensionListCommand(), configure)

    /**
     * Создает команду обновления расширения
     */
    fun update(
        name: String,
        configure: ExtensionUpdateCommand.() -> Unit = { },
    ) = configureAndExecute(ExtensionUpdateCommand(name = name), configure)

    /**
     * Создает команду удаления расширения
     */
    fun delete(configure: ExtensionDeleteCommand.() -> Unit = { }) = configureAndExecute(ExtensionDeleteCommand(), configure)
}