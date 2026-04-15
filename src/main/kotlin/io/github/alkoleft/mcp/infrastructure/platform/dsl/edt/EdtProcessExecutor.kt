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

package io.github.alkoleft.mcp.infrastructure.platform.dsl.edt

import io.github.alkoleft.mcp.application.core.ShellCommandResult
import io.github.alkoleft.mcp.infrastructure.platform.dsl.process.CommandExecutor
import io.github.alkoleft.mcp.infrastructure.platform.dsl.process.ProcessExecutor

/**
 * Исполнитель EDT CLI в обычном процессе.
 *
 * Используется, когда интерактивный auto-start отключен:
 * каждая команда запускает 1cedtcli.exe отдельно, но всегда с корректным
 * путем к исполняемому файлу и рабочим каталогом workspace через `-data`.
 */
class EdtProcessExecutor(
    private val executablePath: String,
    private val workingDirectory: String? = null,
    private val commandTimeoutMs: Long? = null,
    private val processExecutor: ProcessExecutor = ProcessExecutor(),
) : CommandExecutor {
    override fun execute(commandArgs: List<String>): ShellCommandResult = processExecutor.execute(buildCommandArgs(commandArgs))

    internal fun buildCommandArgs(commandArgs: List<String>): List<String> =
        buildList {
            add(executablePath)
            if (!workingDirectory.isNullOrBlank()) {
                add("-data")
                add(workingDirectory)
            }
            commandTimeoutMs
                ?.takeIf { it > 0 }
                ?.let {
                    add("-timeout")
                    add(((it + 999L) / 1000L).coerceAtLeast(1).toString())
                }
            add("-command")
            addAll(commandArgs)
        }
}
