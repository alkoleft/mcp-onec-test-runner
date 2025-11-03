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
 * 9. support disable — Снятие конфигурации с поддержки
 *
 * Снимает конфигурацию с поддержки.
 */
data class ConfigSupportDisableCommand(
    /**
     * Снятие с поддержки принудительно
     * --force, -F
     */
    var force: Boolean = false,
) : IbcmdCommand {
    override val mode: String = "config"
    override val subCommand: String = "support disable"
    override val commandName: String = "config support disable"

    override val arguments: List<String>
        get() {
            val args = mutableListOf<String>()

            if (force) args.add("--force")

            return args
        }

    override fun getFullDescription(): String = "Снятие конфигурации с поддержки" + if (force) " (принудительно)" else ""
}
