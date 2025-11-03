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

package io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.config

import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.common.IbcmdCommand

/**
 * 6. repair — Восстановление конфигурации после незавершённой операции
 *
 * Восстанавливает конфигурацию после сбоя операции.
 */
data class ConfigRepairCommand(
    /**
     * Завершить незавершённую операцию
     * --commit
     */
    var commit: Boolean = false,
    /**
     * Отменить незавершённую операцию
     * --rollback
     */
    var rollback: Boolean = false,
    /**
     * Восстановить структуру метаданных
     * --fix-metadata
     */
    var fixMetadata: Boolean = false,
) : IbcmdCommand {
    override val mode: String = "config"
    override val subCommand: String = "repair"
    override val commandName: String = "config repair"

    override val arguments: List<String>
        get() {
            val args = mutableListOf<String>()

            if (commit) args.add("--commit")
            if (rollback) args.add("--rollback")
            if (fixMetadata) args.add("--fix-metadata")

            return args
        }

    override fun getFullDescription(): String {
        val details = mutableListOf<String>()
        if (commit) details.add("завершить операцию")
        if (rollback) details.add("отменить операцию")
        if (fixMetadata) details.add("восстановить метаданные")

        return "Восстановление конфигурации после незавершённой операции" +
            if (details.isNotEmpty()) " (${details.joinToString(", ")})" else ""
    }
}
