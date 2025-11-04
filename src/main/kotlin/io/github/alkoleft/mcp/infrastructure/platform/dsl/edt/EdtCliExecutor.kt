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

import io.github.alkoleft.mcp.core.modules.ShellCommandResult
import io.github.alkoleft.mcp.infrastructure.platform.dsl.process.CommandExecutor
import io.github.alkoleft.mcp.infrastructure.platform.dsl.process.InteractiveProcessExecutor
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.time.Duration

private val logger = KotlinLogging.logger {}

/**
 * Исполнитель команд EDT CLI через интерактивный процесс
 *
 * Предоставляет высокоуровневый интерфейс для работы с командами EDT CLI,
 * используя InteractiveProcessExecutor для управления процессом.
 */
class EdtCliExecutor(
    private val interactiveExecutor: InteractiveProcessExecutor,
) : CommandExecutor {
    /**
     * Результат выполнения команды с дополнительной обработкой
     */
    data class EdtCommandResult(
        override val success: Boolean,
        override val output: String,
        override val error: String?,
        override val duration: Duration,
        override val exitCode: Int = 0,
    ) : ShellCommandResult

    /**
     * Выполняет произвольную команду
     */
    override fun execute(commandArgs: List<String>): EdtCommandResult {
        val command = commandArgs.joinToString(" ")
        return processCommandResult(interactiveExecutor.executeCommand(command, 600000))
    }

    /**
     * Корректно завершает сессию EDT CLI
     */
    fun exit(): Boolean {
        logger.debug { "Завершение сессии EDT CLI" }
        return interactiveExecutor.exit()
    }

    /**
     * Обрабатывает результат команды
     */
    private fun processCommandResult(result: InteractiveProcessExecutor.EdtCommandResult): EdtCommandResult {
        val output = result.output
        if (!result.success) {
            return EdtCommandResult(
                success = false,
                output = output,
                error = result.error,
                duration = result.duration,
                exitCode = result.exitCode,
            )
        } else {
            return EdtCommandResult(
                success = output.isBlank(),
                output = output,
                error = output,
                duration = result.duration,
                exitCode = result.exitCode,
            )
        }
    }
}
