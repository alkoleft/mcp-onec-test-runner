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

package io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.lock

import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.common.IbcmdCommand

/**
 * 1. list — Получение списка блокировок
 *
 * Получает список всех активных блокировок в информационной базе.
 */
data class LockListCommand(
    /**
     * Идентификатор сеанса (опционально, для фильтрации по сеансу)
     * --session=<uuid>
     */
    val session: String? = null,
) : IbcmdCommand {
    override val mode: String = "lock"
    override val subCommand: String = "list"
    override val commandName: String = "lock list"

    override val arguments: List<String>
        get() {
            val args = mutableListOf<String>()

            session?.let { args.addAll(listOf("--session", it)) }

            return args
        }

    override fun getFullDescription(): String =
        "Получение списка блокировок" +
            (session?.let { " (сеанс: $it)" } ?: "")
}
