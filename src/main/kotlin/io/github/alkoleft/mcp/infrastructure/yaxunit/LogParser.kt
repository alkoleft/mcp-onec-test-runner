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

package io.github.alkoleft.mcp.infrastructure.yaxunit

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists

private val logger = KotlinLogging.logger { }

/**
 * Парсер для извлечения ошибок из логов YAxUnit.
 * Извлекает все блоки ошибок, включая многострочные описания.
 */
@Component
class LogParser {
    companion object {
        private const val ERROR_MARKER = "[ERR]"
        private const val TIME_PATTERN_LENGTH = 12
        private val TIME_PATTERN = Regex("^\\d{2}:\\d{2}:\\d{2}\\.\\d{3}")
    }

    /**
     * Извлекает все ошибки из лога YAxUnit по пути Path.
     * Читает файл построчно без загрузки всего содержимого в память.
     *
     * @param logFile путь к файлу лога
     * @return список строк с ошибками, каждая строка содержит весь блок ошибки
     */
    fun extractErrors(logFile: String): List<String> {
        val logFilePath = Path.of(logFile)
        if (!logFilePath.exists()) {
            logger.warn { "Файл лога не найден: $logFilePath" }
            return emptyList()
        }
        val errors = mutableListOf<String>()
        val errorBlockBuilder = StringBuilder()
        var inErrorBlock = false
        forEachLine(logFilePath) { line ->
            when {
                isErrorLine(line) -> {
                    if (inErrorBlock) {
                        errors.add(errorBlockBuilder.toString().trim())
                        errorBlockBuilder.clear()
                    }
                    inErrorBlock = true
                    errorBlockBuilder.append(line)
                }

                inErrorBlock && isLogEntryLine(line) -> {
                    errors.add(errorBlockBuilder.toString().trim())
                    errorBlockBuilder.clear()
                    inErrorBlock = false
                }

                inErrorBlock -> {
                    errorBlockBuilder.append('\n').append(line)
                }
            }
        }
        if (inErrorBlock) {
            errors.add(errorBlockBuilder.toString().trim())
        }
        return errors
    }

    fun forEachLine(
        file: Path,
        block: (String) -> Unit,
    ) = Files.newBufferedReader(file).use { reader ->
        reader.lineSequence().forEach(block)
    }

    /**
     * Проверяет, является ли строка строкой с ошибкой (содержит [ERR]).
     *
     * @param line строка для проверки
     * @return true если строка содержит ошибку
     */
    private fun isErrorLine(line: String): Boolean = line.contains(ERROR_MARKER)

    /**
     * Проверяет, является ли строка началом новой записи лога (начинается с временной метки).
     * Формат временной метки: ЧЧ:ММ:СС.ммм
     * Оптимизировано: сначала проверяет длину, затем использует предкомпилированный regex.
     *
     * @param line строка для проверки
     * @return true если строка начинается с временной метки
     */
    private fun isLogEntryLine(line: String): Boolean {
        val trimmedLine = line.trim()
        if (trimmedLine.length < TIME_PATTERN_LENGTH) {
            return false
        }
        return TIME_PATTERN.containsMatchIn(trimmedLine)
    }
}
