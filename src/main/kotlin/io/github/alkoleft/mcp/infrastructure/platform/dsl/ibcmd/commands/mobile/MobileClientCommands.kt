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

package io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.mobile

import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.common.IbcmdCommand

/**
 * 1. export — Экспорт мобильного клиента
 *
 * Экспортирует мобильный клиент для развертывания.
 */
data class MobileClientExportCommand(
    /**
     * Путь для экспорта мобильного клиента
     */
    val path: String,
) : IbcmdCommand {
    override val mode: String = "mobile-client"
    override val subCommand: String = "export"
    override val commandName: String = "mobile-client export"

    override val arguments = listOf(path)

    override fun getFullDescription(): String = "Экспорт мобильного клиента в: $path"
}

/**
 * 2. sign — Цифровая подпись мобильного клиента
 *
 * Подписывает мобильный клиент цифровой подписью.
 */
data class MobileClientSignCommand(
    /**
     * Путь к приватному ключу (обязательно, формат .pem)
     * --key=<path>, -k <path>
     */
    val key: String,
) : IbcmdCommand {
    override val mode: String = "mobile-client"
    override val subCommand: String = "sign"
    override val commandName: String = "mobile-client sign"

    override val arguments: List<String>
        get() {
            val args = mutableListOf<String>()

            args.addAll(listOf("--key", key))

            return args
        }

    override fun getFullDescription(): String = "Цифровая подпись мобильного клиента (ключ: $key)"
}
