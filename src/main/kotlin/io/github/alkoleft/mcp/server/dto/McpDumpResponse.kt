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

/**
 * Результат выгрузки конфигурации в формате MCP
 *
 * Содержит информацию о результатах выгрузки конфигурации из ИБ в файлы, включая
 * статус, режим выгрузки, время выполнения и детали шагов (при ошибках).
 *
 * @param success Успешность выгрузки (true, если выгрузка завершилась без ошибок)
 * @param message Сообщение о результате выгрузки
 * @param mode Режим выгрузки, который был использован (FULL, INCREMENTAL, PARTIAL)
 * @param dumpTime Время выполнения выгрузки в миллисекундах
 * @param dumpedObjects Количество выгруженных объектов (если известно)
 * @param errors Список ошибок, возникших во время выгрузки
 * @param steps Список шагов выполнения выгрузки (заполняется только при ошибках для диагностики)
 */
data class McpDumpResponse(
    val success: Boolean,
    val message: String,
    val mode: String,
    val dumpTime: Long? = null,
    val dumpedObjects: Int? = null,
    val errors: List<String>? = null,
    val steps: List<ActionStepResult>? = null,
)
