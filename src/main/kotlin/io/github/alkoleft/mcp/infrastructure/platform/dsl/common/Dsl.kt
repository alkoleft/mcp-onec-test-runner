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

package io.github.alkoleft.mcp.infrastructure.platform.dsl.common

import io.github.alkoleft.mcp.infrastructure.platform.dsl.process.ProcessExecutor
import io.github.alkoleft.mcp.infrastructure.platform.dsl.process.ProcessResult
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.nio.file.Path

private val logger = KotlinLogging.logger { }

/**
 * Базовый DSL класс для выполнения команд
 *
 * @param T тип контекста DSL
 * @param C тип команды
 * @param context контекст DSL
 */
abstract class Dsl<T : DslContext, C : Command>(
    protected val context: T,
) {
    /**
     * Выполняет команду
     *
     * @param command команда для выполнения
     * @return результат выполнения команды
     */
    protected open fun executeCommand(
        command: C,
        logPath: Path? = null,
    ): ProcessResult {
        val executor = ProcessExecutor()
        val args = buildCommandArgs(command, logPath)

        logger.info { command.getFullDescription() }

        return if (logPath != null) {
            executor.executeWithLogging(args, logPath)
        } else {
            executor.execute(args)
        }
    }

    /**
     * Строит аргументы команды для конфигуратора с произвольными аргументами
     *
     * @param commandArgs аргументы команды
     * @return полный список аргументов для выполнения
     */
    protected open fun buildCommandArgs(
        command: C,
        logPath: Path? = null,
    ): List<String> {
        if (logPath != null) {
            throw IllegalArgumentException("Не поддерживается работа с файлом лога")
        }
        val args = mutableListOf<String>()

        // Базовые аргументы
        args.addAll(context.buildBaseArgs())

        // Команда и её аргументы (команда уже включена в commandArgs)
        args.addAll(command.arguments)

        return args
    }

    /**
     * Генерирует путь к файлу логов с временной меткой
     */
    protected fun generateLogFilePath(): Path = File.createTempFile("onec_log_", ".log").toPath()
}
