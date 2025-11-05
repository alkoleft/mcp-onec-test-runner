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

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LogParserTest {
    @Test
    fun `should extract multiple error blocks from log file`(
        @TempDir tempDir: Path,
    ) {
        val logContent =
            """
            |23:21:01.485 [Клиент][INF]: Инициализация
            |23:21:01.917 [Клиент][ERR]: Первая ошибка
            |Описание первой ошибки
            |23:21:02.100 [Клиент][INF]: Информационное сообщение
            |23:21:02.200 [Клиент][ERR]: Вторая ошибка
            |Описание второй ошибки
            |Дополнительная информация
            |23:21:02.300 [Клиент][INF]: Конец
            """.trimMargin()
        val logFile = tempDir.resolve("test.log")
        Files.writeString(logFile, logContent)
        val parser = LogParser()
        val errors = parser.extractErrors(logFile.toString())
        assertEquals(2, errors.size)
        assertTrue(errors[0].contains("Первая ошибка"))
        assertTrue(errors[0].contains("Описание первой ошибки"))
        assertTrue(errors[1].contains("Вторая ошибка"))
        assertTrue(errors[1].contains("Описание второй ошибки"))
        assertTrue(errors[1].contains("Дополнительная информация"))
    }

    @Test
    fun `should return empty list when no errors found`(
        @TempDir tempDir: Path,
    ) {
        val logContent =
            """
            |23:21:01.485 [Клиент][INF]: Инициализация
            |23:21:01.518 [Клиент][INF]: Загрузка тестовых сценариев
            |23:21:02.276 [Клиент][INF]: Загрузка сценариев завершена. 0 сценариев.
            """.trimMargin()
        val logFile = tempDir.resolve("test.log")
        Files.writeString(logFile, logContent)
        val parser = LogParser()
        val errors = parser.extractErrors(logFile.toString())
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `should return empty list when file does not exist`() {
        val parser = LogParser()
        val nonExistentPath =
            java.nio.file.Paths
                .get("/nonexistent/path/to/file.log")
        val errors = parser.extractErrors(nonExistentPath.toString())
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `should handle error at end of file`(
        @TempDir tempDir: Path,
    ) {
        val logContent =
            """
            |23:21:01.485 [Клиент][INF]: Инициализация
            |23:21:01.917 [Клиент][ERR]: Последняя ошибка
            |Описание ошибки
            """.trimMargin()
        val logFile = tempDir.resolve("test.log")
        Files.writeString(logFile, logContent)
        val parser = LogParser()
        val errors = parser.extractErrors(logFile.toString())
        assertEquals(1, errors.size)
        assertTrue(errors[0].contains("Последняя ошибка"))
        assertTrue(errors[0].contains("Описание ошибки"))
    }
}
