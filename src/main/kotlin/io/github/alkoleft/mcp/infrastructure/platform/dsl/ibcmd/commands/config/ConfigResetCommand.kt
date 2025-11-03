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
 * 5. reset — Возврат к конфигурации базы данных
 *
 * Возвращает конфигурацию к состоянию базы данных.
 */
data class ConfigResetCommand(
    /**
     * Имя расширения
     * --extension=<extension>, -e <extension>
     */
    var extension: String? = null,
) : IbcmdCommand {
    override val mode: String = "config"
    override val subCommand: String = "reset"
    override val commandName: String = "config reset"

    override val arguments: List<String>
        get() {
            val args = mutableListOf<String>()

            extension?.let { args.addAll(listOf("--extension", it)) }

            return args
        }

    override fun getFullDescription(): String =
        "Возврат к конфигурации базы данных" +
            (extension?.let { " (расширение: $it)" } ?: "")
}
