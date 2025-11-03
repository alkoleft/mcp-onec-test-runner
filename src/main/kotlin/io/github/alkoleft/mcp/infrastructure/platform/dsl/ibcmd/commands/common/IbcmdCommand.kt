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

package io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.common

import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.Command

/**
 * Базовый интерфейс для всех команд ibcmd
 *
 * Все команды должны реализовывать этот интерфейс для обеспечения
 * единообразного выполнения и формирования аргументов командной строки.
 */
interface IbcmdCommand : Command {
    /**
     * Имя команды для выполнения (например, "infobase create", "config load")
     */
    val commandName: String

    /**
     * Режим работы ibcmd (infobase, server, config, session, lock, mobile-app, mobile-client, extension)
     */
    val mode: String

    /**
     * Подкоманда в рамках режима (create, load, save, export, import и т.д.)
     */
    val subCommand: String
}
