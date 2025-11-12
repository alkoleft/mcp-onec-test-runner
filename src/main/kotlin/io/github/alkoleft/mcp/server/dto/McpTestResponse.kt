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

package io.github.alkoleft.mcp.server.dto

import io.github.alkoleft.mcp.application.actions.common.ActionStepResult
import io.github.alkoleft.mcp.application.actions.test.yaxunit.GenericTestSuite

/**
 * Результат выполнения тестов в формате MCP
 *
 * Содержит полную информацию о результатах выполнения тестов YaXUnit, включая
 * статистику, детали тестов, пути к логам и информацию об ошибках.
 *
 * @param success Успешность выполнения тестов (true, если все тесты прошли успешно)
 * @param message Сообщение о результате выполнения
 * @param totalTests Общее количество выполненных тестов
 * @param passedTests Количество успешно пройденных тестов
 * @param failedTests Количество проваленных тестов
 * @param executionTime Время выполнения тестов в миллисекундах
 * @param testDetail Детальная информация о наборах тестов (test suites)
 * @param steps Список шагов выполнения (заполняется только при ошибках)
 * @param errors Список ошибок, возникших во время выполнения
 * @param enterpriseLogPath Путь к логу 1С:Предприятие
 * @param logFile Путь к файлу лога выполнения тестов
 */
data class McpTestResponse(
    val success: Boolean,
    val message: String,
    val totalTests: Int? = null,
    val passedTests: Int? = null,
    val failedTests: Int? = null,
    val executionTime: Long? = null,
    val testDetail: List<GenericTestSuite>? = null,
    val steps: List<ActionStepResult>? = null,
    val errors: List<String>,
    val enterpriseLogPath: String? = null,
    val logFile: String? = null,
)
