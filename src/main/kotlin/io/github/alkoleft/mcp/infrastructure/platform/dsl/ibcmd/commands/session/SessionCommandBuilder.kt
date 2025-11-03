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

package io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.session

import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.IbcmdDsl
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.common.CommandBuilder

/**
 * Builder для создания команд режима session
 */
class SessionCommandBuilder(
    dsl: IbcmdDsl,
) : CommandBuilder(dsl) {
    /**
     * Создает команду получения информации о сеансе
     */
    fun info(
        session: String,
        configure: SessionInfoCommand.() -> Unit = { },
    ) = configureAndExecute(SessionInfoCommand(session = session), configure)

    /**
     * Создает команду получения списка сеансов
     */
    fun list(configure: SessionListCommand.() -> Unit = { }) = configureAndExecute(SessionListCommand(), configure)

    /**
     * Создает команду завершения сеанса
     */
    fun terminate(
        session: String,
        configure: SessionTerminateCommand.() -> Unit = { },
    ) = configureAndExecute(SessionTerminateCommand(session = session), configure)
}
