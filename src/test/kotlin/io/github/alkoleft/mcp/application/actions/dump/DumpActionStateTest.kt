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

package io.github.alkoleft.mcp.application.actions.dump

import io.github.alkoleft.mcp.application.actions.common.DumpMode
import io.github.alkoleft.mcp.infrastructure.platform.dsl.process.ProcessResult
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration

class DumpActionStateTest {
    @Test
    fun `should create successful result when dump succeeds`() {
        // Arrange
        val state = DumpActionState(DumpMode.FULL)
        val processResult = ProcessResult(
            success = true,
            exitCode = 0,
            output = "Выгрузка выполнена успешно",
            error = null,
            duration = Duration.ZERO,
        )

        // Act
        state.registerDumpResult(processResult, "Выгрузка конфигурации", 150)
        val result = state.toResult("Выгрузка")

        // Assert
        assertTrue(result.success)
        assertEquals(DumpMode.FULL, result.mode)
        assertEquals(150, result.dumpedObjects)
        assertTrue(result.errors.isEmpty())
        assertEquals("Выгрузка успешно", result.message)
    }

    @Test
    fun `should create failed result when dump fails`() {
        // Arrange
        val state = DumpActionState(DumpMode.INCREMENTAL)
        val processResult = ProcessResult(
            success = false,
            exitCode = 1,
            output = "",
            error = "Ошибка при выгрузке: файл занят",
            duration = Duration.ZERO,
        )

        // Act
        state.registerDumpResult(processResult, "Инкрементальная выгрузка")
        val result = state.toResult("Выгрузка")

        // Assert
        assertFalse(result.success)
        assertEquals(DumpMode.INCREMENTAL, result.mode)
        assertTrue(result.errors.contains("Ошибка при выгрузке: файл занят"))
        assertEquals("Выгрузка неудачно", result.message)
    }

    @Test
    fun `should track steps correctly`() {
        // Arrange
        val state = DumpActionState(DumpMode.PARTIAL)
        val processResult = ProcessResult(
            success = true,
            exitCode = 0,
            output = "OK",
            error = null,
            duration = Duration.ZERO,
        )

        // Act
        state.registerDumpResult(processResult, "Частичная выгрузка объектов", 5)
        val result = state.toResult("Выгрузка")

        // Assert
        assertEquals(1, result.steps.size)
        assertTrue(result.steps[0].message.contains("Частичная выгрузка объектов"))
        assertTrue(result.steps[0].success)
    }
}
