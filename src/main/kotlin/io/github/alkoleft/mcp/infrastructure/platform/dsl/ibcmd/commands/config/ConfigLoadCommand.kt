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
import java.nio.file.Path

/**
 * 1. load — Загрузка конфигурации
 *
 * Загружает конфигурацию из файла в информационную базу.
 */
data class ConfigLoadCommand(
    /**
     * Имя расширения
     * --extension=<extension>, -e <extension>
     */
    var extension: String? = null,
    /**
     * Подтверждение при наличии предупреждений
     * --force, -F
     */
    var force: Boolean = false,
    /**
     * Путь к файлу конфигурации
     */
    val path: Path,
) : IbcmdCommand {
    override val mode: String = "config"
    override val subCommand: String = "load"
    override val commandName: String = "config load"

    override val arguments: List<String>
        get() {
            val args = mutableListOf<String>()

            extension?.let { args.addAll(listOf("--extension", it)) }
            if (force) args.add("--force")
            args.add(path.toString())

            return args
        }

    override fun getFullDescription(): String {
        val details = mutableListOf<String>()
        extension?.let { details.add("расширение: $it") }
        if (force) details.add("принудительно")

        return "Загрузка конфигурации из файла: $path" +
            if (details.isNotEmpty()) " (${details.joinToString(", ")})" else ""
    }
}
