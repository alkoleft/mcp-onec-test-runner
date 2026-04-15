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
import io.github.alkoleft.mcp.infrastructure.platform.dsl.process.InteractiveProcessExecutor
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.time.Duration

private val logger = KotlinLogging.logger {}
private const val IGNORED_LEXER_BASED_CONVERTER_ERROR =
    "Only terminal rules are supported by lexer based converters but got ID which is an instance of ParserRule"

/**
 * Исполнитель команд EDT CLI через интерактивный процесс
 *
 * Предоставляет высокоуровневый интерфейс для работы с командами EDT CLI,
 * используя InteractiveProcessExecutor для управления процессом.
 */
class EdtCliExecutor(
    private val interactiveExecutor: InteractiveProcessExecutor,
    private val commandTimeoutMs: Long? = null,
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
        val command = renderCommand(commandArgs)
        return processCommandResult(interactiveExecutor.executeCommand(command, commandTimeoutMs))
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
            val relevantErrors = findRelevantErrors(output)
            val hasErrors = relevantErrors.isNotEmpty()
            return EdtCommandResult(
                success = !hasErrors,
                output = output,
                error = if (hasErrors) relevantErrors.joinToString(System.lineSeparator()) else null,
                duration = result.duration,
                exitCode = result.exitCode,
            )
        }
    }

    companion object {
        internal fun resolveCommandTimeout(commandTimeoutMs: Long?): Long? = commandTimeoutMs

        internal fun renderCommand(commandArgs: List<String>): String =
            commandArgs.joinToString(" ") { argument ->
                if (argument.any { it.isWhitespace() } || argument.contains('"')) {
                    "\"${argument.replace("\"", "\\\"")}\""
                } else {
                    argument
                }
            }

        internal fun findRelevantErrors(output: String): List<String> =
            output.lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .filter(::isErrorLine)
                .filterNot { it.contains(IGNORED_LEXER_BASED_CONVERTER_ERROR) }

        private fun isErrorLine(line: String): Boolean =
            line.startsWith("ERROR", ignoreCase = true) ||
                line.startsWith("CRITICAL", ignoreCase = true) ||
                line.startsWith("FATAL", ignoreCase = true)
    }
}
